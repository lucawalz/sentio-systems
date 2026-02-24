"""Unit tests for birder_ai FastAPI service."""
import pickle
import sys
from io import BytesIO
from unittest.mock import AsyncMock, MagicMock, patch

import numpy as np
import pytest
from fastapi.testclient import TestClient
from PIL import Image


# ---------------------------------------------------------------------------
# ModelManager tests
# ---------------------------------------------------------------------------

class TestModelManager:
    def test_initial_state_not_loaded(self):
        from birder_ai import ModelManager
        manager = ModelManager()
        assert manager.birder_loaded is False

    def test_get_model_status_returns_dict(self):
        from birder_ai import ModelManager
        manager = ModelManager()
        status = manager.get_model_status()
        assert isinstance(status, dict)
        assert "birder_model_loaded" in status
        assert status["birder_model_loaded"] is False

    def test_already_loaded_skips_reload(self):
        from birder_ai import ModelManager
        manager = ModelManager()
        manager._birder_net = MagicMock()
        result = manager.load_birder_model()
        assert result is True

    def test_load_model_fails_gracefully_on_import_error(self):
        from birder_ai import ModelManager
        manager = ModelManager()
        with patch.dict(sys.modules, {"birder": None}):
            result = manager.load_birder_model()
        assert result is False
        assert manager.birder_loaded is False


# ---------------------------------------------------------------------------
# BirdClassificationService tests
# ---------------------------------------------------------------------------

class TestBirdClassificationService:
    def test_classify_returns_unknown_when_model_not_loaded(self, test_pil_image):
        from birder_ai import BirdClassificationService
        service = BirdClassificationService()
        # Simulate model not loaded: patch the loader to return False
        with patch.object(service.model_manager, "load_birder_model", return_value=False):
            result = service.classify_bird_species(test_pil_image)
        assert result.bird_detected is False
        assert result.top_species == "Unknown"
        assert result.top_confidence == 0.0
        assert result.predictions == []

    def test_classify_with_loaded_model(self, test_pil_image):
        from birder_ai import BirdClassificationService, ClassificationPrediction, ClassificationResult
        service = BirdClassificationService()

        mock_output = np.array([[0.9, 0.05, 0.05]])
        mock_infer = MagicMock(return_value=(mock_output, None))

        service.model_manager._birder_net = MagicMock()
        service.model_manager._birder_transform = MagicMock()
        service.model_manager._birder_model_info = MagicMock()
        service.model_manager._idx_to_class = {0: "robin", 1: "sparrow", 2: "dove"}

        mock_birder_pkg = MagicMock()
        mock_birder_pkg.inference.classification.infer_image = mock_infer

        with patch.dict(sys.modules, {
            "birder": mock_birder_pkg,
            "birder.inference": mock_birder_pkg.inference,
            "birder.inference.classification": mock_birder_pkg.inference.classification,
        }):
            result = service.classify_bird_species(test_pil_image)

        assert isinstance(result, ClassificationResult)
        assert result.bird_detected is True
        assert result.top_species == "robin"
        assert result.top_confidence > 0.5

    def test_classify_handles_exception(self, test_pil_image):
        from birder_ai import BirdClassificationService
        service = BirdClassificationService()
        service.model_manager._birder_net = MagicMock()

        mock_birder_pkg = MagicMock()
        mock_birder_pkg.inference.classification.infer_image.side_effect = RuntimeError("GPU OOM")

        with patch.dict(sys.modules, {
            "birder": mock_birder_pkg,
            "birder.inference": mock_birder_pkg.inference,
            "birder.inference.classification": mock_birder_pkg.inference.classification,
        }):
            result = service.classify_bird_species(test_pil_image)

        assert result.bird_detected is False
        assert result.top_species == "Unknown"


# ---------------------------------------------------------------------------
# FastAPI endpoint tests
# ---------------------------------------------------------------------------

class TestApiEndpoints:
    @pytest.fixture(autouse=True)
    def client(self):
        import birder_ai
        self._module = birder_ai
        self._client = TestClient(birder_ai.app, raise_server_exceptions=False)
        return self._client

    def test_root_returns_api_info(self):
        resp = self._client.get("/")
        assert resp.status_code == 200
        data = resp.json()
        assert "endpoints" in data
        assert "message" in data

    def test_health_returns_200(self):
        resp = self._client.get("/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "healthy"
        assert "model_loaded" in data

    def test_detect_with_valid_image(self, test_image_bytes):
        from birder_ai import ClassificationPrediction, ClassificationResult

        mock_result = ClassificationResult(
            predictions=[ClassificationPrediction(species="robin", confidence=0.92)],
            top_species="robin",
            top_confidence=0.92,
            bird_detected=True,
        )

        with patch.object(self._module.classification_service, "classify_bird_species", return_value=mock_result):
            resp = self._client.post(
                "/detect",
                files={"file": ("bird.jpg", test_image_bytes, "image/jpeg")},
            )

        assert resp.status_code == 200
        data = resp.json()
        assert data["detection"]["bird_detected"] is True
        assert data["classification"]["top_species"] == "robin"
        assert data["classification"]["top_confidence"] == pytest.approx(0.92)
        assert len(data["classification"]["predictions"]) == 1

    def test_detect_no_bird_detected(self, test_image_bytes):
        from birder_ai import ClassificationResult

        mock_result = ClassificationResult(
            predictions=[],
            top_species="Unknown",
            top_confidence=0.0,
            bird_detected=False,
        )

        with patch.object(self._module.classification_service, "classify_bird_species", return_value=mock_result):
            resp = self._client.post(
                "/detect",
                files={"file": ("empty.jpg", test_image_bytes, "image/jpeg")},
            )

        assert resp.status_code == 200
        data = resp.json()
        assert data["detection"]["bird_detected"] is False

    def test_detect_invalid_image_bytes(self):
        resp = self._client.post(
            "/detect",
            files={"file": ("bad.jpg", b"not-an-image", "image/jpeg")},
        )
        assert resp.status_code == 500

    def test_queue_submit_returns_job_id(self, test_image_bytes):
        mock_job = MagicMock()
        mock_job.job_id = "abc-123"
        mock_pool = AsyncMock()
        mock_pool.enqueue_job = AsyncMock(return_value=mock_job)

        async def _mock_pool():
            return mock_pool

        with patch("birder_ai.get_redis_pool", _mock_pool):
            resp = self._client.post(
                "/queue/submit",
                files={"file": ("bird.jpg", test_image_bytes, "image/jpeg")},
            )

        assert resp.status_code == 200
        data = resp.json()
        assert data["job_id"] == "abc-123"
        assert data["status"] == "queued"

    def test_queue_result_pending_when_no_result(self):
        mock_redis = AsyncMock()
        mock_redis.get = AsyncMock(return_value=None)
        mock_redis.close = AsyncMock()

        with patch("redis.asyncio.Redis", return_value=mock_redis):
            resp = self._client.get("/queue/result/unknown-job")

        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "pending"
        assert data["job_id"] == "unknown-job"

    def test_queue_result_complete_when_result_exists(self):
        payload = {"success": True, "classification": {"top_species": "robin"}}
        raw = pickle.dumps(payload)

        mock_redis = AsyncMock()
        mock_redis.get = AsyncMock(return_value=raw)
        mock_redis.close = AsyncMock()

        with patch("redis.asyncio.Redis", return_value=mock_redis):
            resp = self._client.get("/queue/result/job-456")

        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "complete"
        assert data["result"]["success"] is True

    def test_queue_stats_returns_dict(self):
        mock_redis = AsyncMock()
        mock_redis.llen = AsyncMock(return_value=3)
        mock_redis.info = AsyncMock(return_value={"connected_clients": 2})
        mock_redis.close = AsyncMock()

        with patch("redis.asyncio.Redis", return_value=mock_redis):
            resp = self._client.get("/queue/stats")

        assert resp.status_code == 200
        data = resp.json()
        assert "pending_jobs" in data or "error" in data  # graceful on redis error too

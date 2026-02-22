"""Unit tests for speciesnet_ai FastAPI service."""
import pickle
import sys
from io import BytesIO
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi.testclient import TestClient
from PIL import Image


# ---------------------------------------------------------------------------
# SpeciesNetManager tests
# ---------------------------------------------------------------------------

class TestSpeciesNetManager:
    def test_initial_state_not_loaded(self):
        from speciesnet_ai import SpeciesNetManager
        manager = SpeciesNetManager()
        assert manager.is_loaded is False

    def test_already_loaded_skips_reload(self):
        from speciesnet_ai import SpeciesNetManager
        manager = SpeciesNetManager()
        manager._classifier = MagicMock()
        manager._model_loaded = True
        result = manager.load_model()
        assert result is True

    def test_load_model_fails_gracefully_on_import_error(self):
        from speciesnet_ai import SpeciesNetManager
        manager = SpeciesNetManager()
        with patch.dict(sys.modules, {"speciesnet": None, "speciesnet.classifier": None}):
            result = manager.load_model()
        assert result is False
        assert manager.is_loaded is False

    def test_classify_returns_unknown_when_model_not_loaded(self, test_pil_image):
        from speciesnet_ai import SpeciesNetManager
        manager = SpeciesNetManager()
        # Simulate model not loaded: patch the loader to return False
        with patch.object(manager, "load_model", return_value=False):
            result = manager.classify(test_pil_image, "test.jpg")
        assert result.animal_detected is False
        assert result.top_species == "Unknown"
        assert result.top_confidence == 0.0

    def test_classify_with_mocked_classifier(self, test_pil_image):
        from speciesnet_ai import SpeciesNetManager

        mock_clf = MagicMock()
        mock_clf.preprocess.return_value = MagicMock()
        mock_clf.predict.return_value = {
            "classifications": {
                "classes": ["Turdus merula", "Passer domesticus"],
                "scores": [0.85, 0.10],
            }
        }

        manager = SpeciesNetManager()
        manager._classifier = mock_clf
        manager._model_loaded = True

        result = manager.classify(test_pil_image, "blackbird.jpg")
        assert result.animal_detected is True
        assert result.top_species == "Turdus merula"
        assert result.top_confidence == pytest.approx(0.85)

    def test_classify_handles_preprocessing_failure(self, test_pil_image):
        from speciesnet_ai import SpeciesNetManager
        manager = SpeciesNetManager()
        mock_clf = MagicMock()
        mock_clf.preprocess.return_value = None
        manager._classifier = mock_clf
        manager._model_loaded = True

        result = manager.classify(test_pil_image)
        assert result.animal_detected is False

    def test_classify_handles_failure_in_result(self, test_pil_image):
        from speciesnet_ai import SpeciesNetManager
        manager = SpeciesNetManager()
        mock_clf = MagicMock()
        mock_clf.preprocess.return_value = MagicMock()
        mock_clf.predict.return_value = {"failures": "model error"}
        manager._classifier = mock_clf
        manager._model_loaded = True

        result = manager.classify(test_pil_image)
        assert result.animal_detected is False

    def test_classify_handles_exception(self, test_pil_image):
        from speciesnet_ai import SpeciesNetManager
        manager = SpeciesNetManager()
        mock_clf = MagicMock()
        mock_clf.preprocess.side_effect = RuntimeError("GPU OOM")
        manager._classifier = mock_clf
        manager._model_loaded = True

        result = manager.classify(test_pil_image)
        assert result.animal_detected is False


# ---------------------------------------------------------------------------
# FastAPI endpoint tests
# ---------------------------------------------------------------------------

class TestApiEndpoints:
    @pytest.fixture(autouse=True)
    def client(self):
        import speciesnet_ai
        self._module = speciesnet_ai
        self._client = TestClient(speciesnet_ai.app, raise_server_exceptions=False)
        return self._client

    def test_root_returns_api_info(self):
        resp = self._client.get("/")
        assert resp.status_code == 200
        data = resp.json()
        assert "endpoints" in data

    def test_health_returns_200(self):
        resp = self._client.get("/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "healthy"
        assert "model_loaded" in data

    def test_detect_with_valid_image(self, test_image_bytes):
        from speciesnet_ai import ClassificationPrediction, ClassificationResult

        mock_result = ClassificationResult(
            predictions=[ClassificationPrediction(species="Vulpes vulpes", confidence=0.91)],
            top_species="Vulpes vulpes",
            top_confidence=0.91,
            animal_detected=True,
        )

        with patch.object(self._module.classification_service, "classify_species", return_value=mock_result):
            resp = self._client.post(
                "/detect",
                files={"file": ("fox.jpg", test_image_bytes, "image/jpeg")},
            )

        assert resp.status_code == 200
        data = resp.json()
        assert data["detection"]["animal_detected"] is True
        assert data["classification"]["top_species"] == "Vulpes vulpes"
        assert data["classification"]["top_confidence"] == pytest.approx(0.91)

    def test_detect_no_animal_detected(self, test_image_bytes):
        from speciesnet_ai import ClassificationResult

        mock_result = ClassificationResult(
            predictions=[],
            top_species="Unknown",
            top_confidence=0.0,
            animal_detected=False,
        )

        with patch.object(self._module.classification_service, "classify_species", return_value=mock_result):
            resp = self._client.post(
                "/detect",
                files={"file": ("empty.jpg", test_image_bytes, "image/jpeg")},
            )

        assert resp.status_code == 200
        data = resp.json()
        assert data["detection"]["animal_detected"] is False

    def test_detect_invalid_image_bytes(self):
        resp = self._client.post(
            "/detect",
            files={"file": ("bad.jpg", b"not-an-image", "image/jpeg")},
        )
        assert resp.status_code == 500

    def test_detect_response_has_classification_details(self, test_image_bytes):
        from speciesnet_ai import ClassificationResult

        mock_result = ClassificationResult(
            predictions=[], top_species="Unknown", top_confidence=0.0, animal_detected=False
        )

        with patch.object(self._module.classification_service, "classify_species", return_value=mock_result):
            resp = self._client.post(
                "/detect", files={"file": ("img.jpg", test_image_bytes, "image/jpeg")}
            )

        data = resp.json()
        assert "classification_details" in data
        assert "model_loaded" in data["classification_details"]

    def test_queue_submit_returns_job_id(self, test_image_bytes):
        mock_job = MagicMock()
        mock_job.job_id = "sn-job-789"
        mock_pool = AsyncMock()
        mock_pool.enqueue_job = AsyncMock(return_value=mock_job)

        async def _mock_pool():
            return mock_pool

        with patch("speciesnet_ai.get_redis_pool", _mock_pool):
            resp = self._client.post(
                "/queue/submit",
                files={"file": ("fox.jpg", test_image_bytes, "image/jpeg")},
            )

        assert resp.status_code == 200
        data = resp.json()
        assert data["job_id"] == "sn-job-789"
        assert data["status"] == "queued"

    def test_queue_result_pending(self):
        mock_redis = AsyncMock()
        mock_redis.get = AsyncMock(return_value=None)
        mock_redis.close = AsyncMock()

        with patch("redis.asyncio.Redis", return_value=mock_redis):
            resp = self._client.get("/queue/result/not-found")

        assert resp.status_code == 200
        assert resp.json()["status"] == "pending"

    def test_queue_result_complete(self):
        payload = {"success": True, "classification": {"top_species": "Vulpes vulpes"}}
        raw = pickle.dumps(payload)

        mock_redis = AsyncMock()
        mock_redis.get = AsyncMock(return_value=raw)
        mock_redis.close = AsyncMock()

        with patch("redis.asyncio.Redis", return_value=mock_redis):
            resp = self._client.get("/queue/result/sn-job-001")

        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "complete"

    def test_queue_stats_returns_structure(self):
        mock_redis = AsyncMock()
        mock_redis.llen = AsyncMock(return_value=5)
        mock_redis.info = AsyncMock(return_value={"connected_clients": 1})
        mock_redis.close = AsyncMock()

        with patch("redis.asyncio.Redis", return_value=mock_redis):
            resp = self._client.get("/queue/stats")

        assert resp.status_code == 200

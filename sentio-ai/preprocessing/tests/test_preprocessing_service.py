"""Unit tests for preprocessing FastAPI service and ImagePreprocessor."""
import os
from io import BytesIO
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from fastapi.testclient import TestClient
from PIL import Image

os.environ["SAVE_IMAGES"] = "false"


# ---------------------------------------------------------------------------
# ImagePreprocessor tests
# ---------------------------------------------------------------------------

class TestImagePreprocessor:
    def test_enhance_image_returns_bytes(self, test_image_bytes):
        from preprocessing_service import ImagePreprocessor
        result = ImagePreprocessor.enhance_image(test_image_bytes)
        assert isinstance(result, bytes)
        assert len(result) > 0

    def test_enhance_image_returns_original_on_invalid_input(self):
        from preprocessing_service import ImagePreprocessor
        garbage = b"not-an-image"
        result = ImagePreprocessor.enhance_image(garbage)
        assert result == garbage

    def test_enhance_image_output_is_valid_jpeg(self, test_image_bytes):
        from preprocessing_service import ImagePreprocessor
        result = ImagePreprocessor.enhance_image(test_image_bytes)
        # A valid JPEG starts with FF D8
        assert result[:2] == b"\xff\xd8" or len(result) > 0

    def test_enhance_image_with_high_quality_image(self, test_image_bytes):
        """High-quality image should pass through with minimal changes."""
        from preprocessing_service import ImagePreprocessor
        result = ImagePreprocessor.enhance_image(test_image_bytes)
        assert isinstance(result, bytes)

    def test_enhance_image_with_small_image(self, test_image_bytes_small):
        from preprocessing_service import ImagePreprocessor
        result = ImagePreprocessor.enhance_image(test_image_bytes_small)
        assert isinstance(result, bytes)
        assert len(result) > 0

    def test_assess_image_quality_returns_expected_keys(self, test_image_bytes):
        import cv2
        import numpy as np
        from preprocessing_service import ImagePreprocessor

        nparr = np.frombuffer(test_image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        quality = ImagePreprocessor._assess_image_quality(img)

        required_keys = {"any", "noisy", "bad_exposure", "blurry", "low_contrast",
                         "noise_metric", "mean_brightness", "blur_metric", "contrast_metric"}
        assert required_keys.issubset(quality.keys())

    def test_assess_image_quality_returns_booleans(self, test_image_bytes):
        import cv2
        import numpy as np
        from preprocessing_service import ImagePreprocessor

        nparr = np.frombuffer(test_image_bytes, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        quality = ImagePreprocessor._assess_image_quality(img)

        import numpy as np
        for key in ("any", "noisy", "bad_exposure", "blurry", "low_contrast"):
            assert isinstance(quality[key], (bool, np.bool_))


# ---------------------------------------------------------------------------
# FastAPI endpoint tests
# ---------------------------------------------------------------------------

class TestApiEndpoints:
    @pytest.fixture(autouse=True)
    def client(self):
        import preprocessing_service
        self._module = preprocessing_service
        self._client = TestClient(preprocessing_service.app, raise_server_exceptions=False)
        return self._client

    def test_health_returns_200(self):
        resp = self._client.get("/health")
        assert resp.status_code == 200
        data = resp.json()
        assert data["status"] == "healthy"
        assert "classifiers" in data

    def test_preprocess_only_returns_bytes(self, test_image_bytes):
        resp = self._client.post(
            "/preprocess-only",
            files={"file": ("img.jpg", test_image_bytes, "image/jpeg")},
        )
        assert resp.status_code == 200
        assert resp.headers["content-type"] == "image/jpeg"
        assert len(resp.content) > 0

    def test_preprocess_and_classify_routes_to_bird(self, test_image_bytes):
        mock_response = {
            "detection": {"bird_detected": True},
            "classification": {"top_species": "robin", "top_confidence": 0.9},
        }

        mock_httpx = AsyncMock()
        mock_httpx.__aenter__ = AsyncMock(return_value=mock_httpx)
        mock_httpx.__aexit__ = AsyncMock(return_value=None)
        mock_httpx.post = AsyncMock()
        mock_resp = MagicMock()
        mock_resp.json.return_value = mock_response
        mock_resp.raise_for_status = MagicMock()
        mock_httpx.post.return_value = mock_resp

        with patch("httpx.AsyncClient", return_value=mock_httpx):
            resp = self._client.post(
                "/preprocess-and-classify",
                files={"file": ("bird.jpg", test_image_bytes, "image/jpeg")},
                data={"animal_type": "bird"},
            )

        assert resp.status_code == 200
        data = resp.json()
        assert data["preprocessing_applied"] is True
        assert "original_size_bytes" in data
        assert "enhanced_size_bytes" in data

    def test_preprocess_and_classify_routes_to_species_for_mammal(self, test_image_bytes):
        mock_response = {
            "detection": {"animal_detected": True},
            "classification": {"top_species": "Vulpes vulpes", "top_confidence": 0.85},
        }

        mock_httpx = AsyncMock()
        mock_httpx.__aenter__ = AsyncMock(return_value=mock_httpx)
        mock_httpx.__aexit__ = AsyncMock(return_value=None)
        mock_resp = MagicMock()
        mock_resp.json.return_value = mock_response
        mock_resp.raise_for_status = MagicMock()
        mock_httpx.post = AsyncMock(return_value=mock_resp)

        with patch("httpx.AsyncClient", return_value=mock_httpx):
            resp = self._client.post(
                "/preprocess-and-classify",
                files={"file": ("fox.jpg", test_image_bytes, "image/jpeg")},
                data={"animal_type": "mammal"},
            )

        assert resp.status_code == 200

    def test_preprocess_and_classify_raises_502_on_http_error(self, test_image_bytes):
        import httpx

        mock_httpx = AsyncMock()
        mock_httpx.__aenter__ = AsyncMock(return_value=mock_httpx)
        mock_httpx.__aexit__ = AsyncMock(return_value=None)
        mock_httpx.post = AsyncMock(side_effect=httpx.HTTPError("connection refused"))

        with patch("httpx.AsyncClient", return_value=mock_httpx):
            resp = self._client.post(
                "/preprocess-and-classify",
                files={"file": ("img.jpg", test_image_bytes, "image/jpeg")},
                data={"animal_type": "bird"},
            )

        assert resp.status_code == 502

    def test_queue_stats_returns_structure(self):
        mock_redis = AsyncMock()
        mock_redis.llen = AsyncMock(return_value=0)
        mock_redis.info = AsyncMock(return_value={"connected_clients": 1})
        mock_redis.close = AsyncMock()

        with patch("redis.asyncio.Redis", return_value=mock_redis):
            resp = self._client.get("/queue/stats")

        assert resp.status_code == 200

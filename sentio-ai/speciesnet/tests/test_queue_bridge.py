"""Unit tests for speciesnet queue bridge."""
import base64
from io import BytesIO
from unittest.mock import MagicMock

import pytest
from PIL import Image


def _image_base64() -> str:
    img = Image.new("RGB", (32, 32), color=(70, 110, 90))
    buf = BytesIO()
    img.save(buf, format="JPEG")
    return base64.b64encode(buf.getvalue()).decode("utf-8")


class TestQueueBridgeConstants:
    def test_java_queue_name(self):
        from queue_bridge import JAVA_QUEUE
        assert JAVA_QUEUE == "speciesnet:queue:java"

    def test_result_prefix(self):
        from queue_bridge import RESULT_PREFIX
        assert RESULT_PREFIX == "arq:result:"

    def test_result_ttl_one_hour(self):
        from queue_bridge import RESULT_TTL
        assert RESULT_TTL == 3600


class TestProcessJob:
    @pytest.mark.asyncio
    async def test_process_job_success(self):
        from queue_bridge import process_job
        from speciesnet_ai import ClassificationPrediction, ClassificationResult

        mock_result = ClassificationResult(
            predictions=[ClassificationPrediction(species="Canis lupus", confidence=0.82)],
            top_species="Canis lupus",
            top_confidence=0.82,
            animal_detected=True,
        )
        mock_service = MagicMock()
        mock_service.classify_species.return_value = mock_result

        job_data = {
            "job_id": "sn-bridge-001",
            "image_base64": _image_base64(),
            "filename": "wolf.jpg",
        }

        result = await process_job(mock_service, job_data)

        assert result["status"] == "complete"
        assert result["success"] is True
        assert result["job_id"] == "sn-bridge-001"
        assert result["detection"]["animal_detected"] is True
        assert result["classification"]["top_species"] == "Canis lupus"

    @pytest.mark.asyncio
    async def test_process_job_error_on_exception(self):
        from queue_bridge import process_job

        mock_service = MagicMock()
        mock_service.classify_species.side_effect = RuntimeError("inference error")

        job_data = {
            "job_id": "sn-bridge-002",
            "image_base64": _image_base64(),
        }

        result = await process_job(mock_service, job_data)

        assert result["success"] is False
        assert result["job_id"] == "sn-bridge-002"

    @pytest.mark.asyncio
    async def test_process_job_bad_base64_fails_gracefully(self):
        from queue_bridge import process_job

        mock_service = MagicMock()
        job_data = {
            "job_id": "sn-bridge-bad",
            "image_base64": "!!!invalid!!!",
        }

        result = await process_job(mock_service, job_data)
        assert result["success"] is False

    @pytest.mark.asyncio
    async def test_process_job_missing_job_id_falls_back(self):
        from queue_bridge import process_job

        mock_service = MagicMock()
        mock_service.classify_species.side_effect = KeyError("missing")
        job_data = {"image_base64": _image_base64()}

        result = await process_job(mock_service, job_data)
        assert result["success"] is False
        assert result["job_id"] == "unknown"

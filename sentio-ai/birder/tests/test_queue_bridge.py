"""Unit tests for birder queue bridge."""
import base64
from io import BytesIO
from unittest.mock import MagicMock

import pytest
from PIL import Image


def _image_base64() -> str:
    img = Image.new("RGB", (32, 32), color=(60, 90, 120))
    buf = BytesIO()
    img.save(buf, format="JPEG")
    return base64.b64encode(buf.getvalue()).decode("utf-8")


class TestQueueBridgeConstants:
    def test_java_queue_name(self):
        from queue_bridge import JAVA_QUEUE
        assert JAVA_QUEUE == "birder:queue:java"

    def test_result_prefix(self):
        from queue_bridge import RESULT_PREFIX
        assert RESULT_PREFIX == "arq:result:"

    def test_result_ttl_one_hour(self):
        from queue_bridge import RESULT_TTL
        assert RESULT_TTL == 3600

    def test_result_channel(self):
        from queue_bridge import RESULT_CHANNEL
        assert RESULT_CHANNEL == "classification:results"


class TestProcessJob:
    @pytest.mark.asyncio
    async def test_process_job_success(self):
        from birder_ai import ClassificationPrediction, ClassificationResult
        from queue_bridge import process_job

        mock_result = ClassificationResult(
            predictions=[ClassificationPrediction(species="finch", confidence=0.78)],
            top_species="finch",
            top_confidence=0.78,
            bird_detected=True,
        )
        mock_service = MagicMock()
        mock_service.classify_bird_species.return_value = mock_result

        job_data = {
            "job_id": "test-job-001",
            "image_base64": _image_base64(),
            "filename": "finch.jpg",
        }

        result = await process_job(mock_service, job_data)

        assert result["status"] == "complete"
        assert result["success"] is True
        assert result["job_id"] == "test-job-001"
        assert result["detection"]["bird_detected"] is True
        assert result["classification"]["top_species"] == "finch"

    @pytest.mark.asyncio
    async def test_process_job_returns_error_on_exception(self):
        from queue_bridge import process_job

        mock_service = MagicMock()
        mock_service.classify_bird_species.side_effect = RuntimeError("inference failed")

        job_data = {
            "job_id": "test-job-002",
            "image_base64": _image_base64(),
            "filename": "crash.jpg",
        }

        result = await process_job(mock_service, job_data)

        assert result["status"] == "complete"
        assert result["success"] is False
        assert result["job_id"] == "test-job-002"

    @pytest.mark.asyncio
    async def test_process_job_missing_job_id_handled(self):
        from queue_bridge import process_job

        mock_service = MagicMock()
        mock_service.classify_bird_species.side_effect = KeyError("job_id")

        job_data = {"image_base64": _image_base64()}
        result = await process_job(mock_service, job_data)

        assert result["success"] is False
        assert result["job_id"] == "unknown"

    @pytest.mark.asyncio
    async def test_process_job_invalid_base64_fails_gracefully(self):
        from queue_bridge import process_job

        mock_service = MagicMock()

        job_data = {
            "job_id": "test-job-bad-b64",
            "image_base64": "!!!not-valid-base64!!!",
            "filename": "bad.jpg",
        }

        result = await process_job(mock_service, job_data)
        assert result["success"] is False

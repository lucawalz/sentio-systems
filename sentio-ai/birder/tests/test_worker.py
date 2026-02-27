"""Unit tests for birder ARQ worker."""
from io import BytesIO
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from PIL import Image


def _make_image_bytes() -> bytes:
    img = Image.new("RGB", (32, 32), color=(80, 120, 160))
    buf = BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.getvalue()


class TestClassifyBirdWorker:
    @pytest.mark.asyncio
    async def test_classify_bird_success(self):
        from birder_ai import BirdClassificationService, ClassificationPrediction, ClassificationResult
        from worker import classify_bird

        mock_result = ClassificationResult(
            predictions=[ClassificationPrediction(species="sparrow", confidence=0.88)],
            top_species="sparrow",
            top_confidence=0.88,
            bird_detected=True,
        )
        mock_service = MagicMock(spec=BirdClassificationService)
        mock_service.classify_bird_species.return_value = mock_result

        ctx = {"service": mock_service}
        result = await classify_bird(ctx, _make_image_bytes(), "sparrow.jpg")

        assert result["success"] is True
        assert result["detection"]["bird_detected"] is True
        assert result["classification"]["top_species"] == "sparrow"
        assert result["classification"]["top_confidence"] == pytest.approx(0.88)
        assert len(result["classification"]["predictions"]) == 1

    @pytest.mark.asyncio
    async def test_classify_bird_handles_exception(self):
        from worker import classify_bird

        mock_service = MagicMock()
        mock_service.classify_bird_species.side_effect = RuntimeError("model crashed")
        ctx = {"service": mock_service}

        result = await classify_bird(ctx, _make_image_bytes(), "bad.jpg")

        assert result["success"] is False
        assert "error" in result

    @pytest.mark.asyncio
    async def test_classify_bird_invalid_image_bytes(self):
        from worker import classify_bird

        mock_service = MagicMock()
        ctx = {"service": mock_service}

        result = await classify_bird(ctx, b"garbage", "bad.jpg")

        assert result["success"] is False

    @pytest.mark.asyncio
    async def test_startup_puts_service_in_ctx(self):
        from worker import startup

        mock_service = MagicMock()
        mock_service.model_manager.load_birder_model.return_value = True

        with patch("birder_ai.BirdClassificationService", return_value=mock_service):
            ctx = {}
            await startup(ctx)

        assert "service" in ctx
        assert ctx["service"] is mock_service

    @pytest.mark.asyncio
    async def test_shutdown_completes_without_error(self):
        from worker import shutdown
        ctx = {"service": MagicMock()}
        await shutdown(ctx)


class TestWorkerSettings:
    def test_queue_name(self):
        from worker import WorkerSettings
        assert WorkerSettings.queue_name == "birder:queue"

    def test_classify_bird_in_functions(self):
        from worker import WorkerSettings, classify_bird
        assert classify_bird in WorkerSettings.functions

    def test_job_timeout_positive(self):
        from worker import WorkerSettings
        assert WorkerSettings.job_timeout > 0

    def test_keep_result_set(self):
        from worker import WorkerSettings
        assert WorkerSettings.keep_result > 0

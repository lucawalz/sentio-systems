"""Unit tests for preprocessing ARQ worker."""
import os
from io import BytesIO
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from PIL import Image

os.environ["SAVE_IMAGES"] = "false"


def _make_image_bytes() -> bytes:
    img = Image.new("RGB", (64, 64), color=(100, 140, 180))
    buf = BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.getvalue()


class TestPreprocessAndClassifyWorker:
    @pytest.mark.asyncio
    async def test_routes_bird_to_birder_queue(self):
        from worker import preprocess_and_classify

        mock_preprocessor = MagicMock()
        mock_preprocessor.enhance_image.return_value = _make_image_bytes()

        mock_job = MagicMock()
        mock_job.job_id = "route-bird-001"
        mock_job.result = AsyncMock(return_value={"success": True, "top_species": "robin"})

        mock_pool = AsyncMock()
        mock_pool.enqueue_job = AsyncMock(return_value=mock_job)

        ctx = {"preprocessor": mock_preprocessor, "redis_pool": mock_pool}

        result = await preprocess_and_classify(ctx, _make_image_bytes(), "bird", "bird.jpg")

        assert result["success"] is True
        assert result["preprocessing_applied"] is True
        assert result["routed_to"] == "birder"
        mock_pool.enqueue_job.assert_called_once()
        call_kwargs = mock_pool.enqueue_job.call_args
        assert call_kwargs[1]["_queue_name"] == "birder:queue"

    @pytest.mark.asyncio
    async def test_routes_mammal_to_speciesnet_queue(self):
        from worker import preprocess_and_classify

        mock_preprocessor = MagicMock()
        mock_preprocessor.enhance_image.return_value = _make_image_bytes()

        mock_job = MagicMock()
        mock_job.result = AsyncMock(return_value={"success": True})

        mock_pool = AsyncMock()
        mock_pool.enqueue_job = AsyncMock(return_value=mock_job)

        ctx = {"preprocessor": mock_preprocessor, "redis_pool": mock_pool}

        result = await preprocess_and_classify(ctx, _make_image_bytes(), "mammal", "deer.jpg")

        assert result["routed_to"] == "speciesnet"
        call_kwargs = mock_pool.enqueue_job.call_args
        assert call_kwargs[1]["_queue_name"] == "speciesnet:queue"

    @pytest.mark.asyncio
    async def test_handles_preprocessing_exception(self):
        from worker import preprocess_and_classify

        mock_preprocessor = MagicMock()
        mock_preprocessor.enhance_image.side_effect = RuntimeError("cv2 error")

        ctx = {"preprocessor": mock_preprocessor, "redis_pool": AsyncMock()}

        result = await preprocess_and_classify(ctx, _make_image_bytes(), "bird", "bad.jpg")

        assert result["success"] is False
        assert "error" in result

    @pytest.mark.asyncio
    async def test_startup_initializes_preprocessor_and_pool(self):
        from worker import startup

        mock_preprocessor = MagicMock()
        mock_pool = AsyncMock()

        with patch("preprocessing_service.ImagePreprocessor", return_value=mock_preprocessor), \
             patch("worker.create_pool", AsyncMock(return_value=mock_pool)):
            ctx = {}
            await startup(ctx)

        assert "preprocessor" in ctx
        assert "redis_pool" in ctx

    @pytest.mark.asyncio
    async def test_shutdown_closes_pool(self):
        from worker import shutdown

        mock_pool = AsyncMock()
        mock_pool.close = AsyncMock()
        ctx = {"redis_pool": mock_pool}
        await shutdown(ctx)
        mock_pool.close.assert_called_once()


class TestWorkerSettings:
    def test_queue_name(self):
        from worker import WorkerSettings
        assert WorkerSettings.queue_name == "preprocessing:queue"

    def test_preprocess_function_registered(self):
        from worker import WorkerSettings, preprocess_and_classify
        assert preprocess_and_classify in WorkerSettings.functions

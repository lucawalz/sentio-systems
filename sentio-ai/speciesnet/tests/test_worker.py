"""Unit tests for speciesnet ARQ worker."""
from io import BytesIO
from unittest.mock import AsyncMock, MagicMock, patch

import pytest
from PIL import Image


def _make_image_bytes() -> bytes:
    img = Image.new("RGB", (32, 32), color=(90, 140, 100))
    buf = BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.getvalue()


class TestClassifySpeciesWorker:
    @pytest.mark.asyncio
    async def test_classify_species_success(self):
        from speciesnet_ai import ClassificationPrediction, ClassificationResult
        from worker import classify_species

        mock_result = ClassificationResult(
            predictions=[ClassificationPrediction(species="Vulpes vulpes", confidence=0.87)],
            top_species="Vulpes vulpes",
            top_confidence=0.87,
            animal_detected=True,
        )
        mock_service = MagicMock()
        mock_service.classify_species.return_value = mock_result

        ctx = {"service": mock_service}
        result = await classify_species(ctx, _make_image_bytes(), "fox.jpg")

        assert result["success"] is True
        assert result["detection"]["animal_detected"] is True
        assert result["classification"]["top_species"] == "Vulpes vulpes"

    @pytest.mark.asyncio
    async def test_classify_species_handles_exception(self):
        from worker import classify_species

        mock_service = MagicMock()
        mock_service.classify_species.side_effect = RuntimeError("model error")
        ctx = {"service": mock_service}

        result = await classify_species(ctx, _make_image_bytes(), "crash.jpg")

        assert result["success"] is False
        assert "error" in result

    @pytest.mark.asyncio
    async def test_classify_species_invalid_bytes(self):
        from worker import classify_species

        mock_service = MagicMock()
        ctx = {"service": mock_service}
        result = await classify_species(ctx, b"garbage", "bad.jpg")

        assert result["success"] is False

    @pytest.mark.asyncio
    async def test_startup_loads_model_into_ctx(self):
        from worker import startup

        mock_service = MagicMock()
        mock_service.manager.load_model.return_value = True

        with patch("speciesnet_ai.SpeciesClassificationService", return_value=mock_service):
            ctx = {}
            await startup(ctx)

        assert "service" in ctx
        assert ctx["service"] is mock_service

    @pytest.mark.asyncio
    async def test_shutdown_completes_without_error(self):
        from worker import shutdown
        await shutdown({"service": MagicMock()})


class TestWorkerSettings:
    def test_queue_name(self):
        from worker import WorkerSettings
        assert WorkerSettings.queue_name == "speciesnet:queue"

    def test_classify_species_in_functions(self):
        from worker import WorkerSettings, classify_species
        assert classify_species in WorkerSettings.functions

    def test_keep_result_set(self):
        from worker import WorkerSettings
        assert WorkerSettings.keep_result > 0

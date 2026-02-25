import base64
from io import BytesIO
from unittest.mock import MagicMock

import pytest
from PIL import Image


@pytest.fixture
def test_image_bytes() -> bytes:
    img = Image.new("RGB", (64, 64), color=(100, 150, 200))
    buf = BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.getvalue()


@pytest.fixture
def test_image_base64(test_image_bytes) -> str:
    return base64.b64encode(test_image_bytes).decode("utf-8")


@pytest.fixture
def test_pil_image() -> Image.Image:
    return Image.new("RGB", (64, 64), color=(100, 150, 200))


@pytest.fixture
def mock_classification_result():
    import sys
    mock_birder = MagicMock()
    sys.modules.setdefault("birder", mock_birder)
    sys.modules.setdefault("birder.inference", mock_birder.inference)
    sys.modules.setdefault("birder.inference.classification", mock_birder.inference.classification)
    return mock_birder

import base64
from io import BytesIO
from unittest.mock import MagicMock

import pytest
from PIL import Image


@pytest.fixture
def test_image_bytes() -> bytes:
    img = Image.new("RGB", (64, 64), color=(80, 130, 180))
    buf = BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.getvalue()


@pytest.fixture
def test_image_base64(test_image_bytes) -> str:
    return base64.b64encode(test_image_bytes).decode("utf-8")


@pytest.fixture
def test_pil_image() -> Image.Image:
    return Image.new("RGB", (64, 64), color=(80, 130, 180))

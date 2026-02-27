import os
from io import BytesIO

import pytest
from PIL import Image

# Disable image saving during tests to avoid creating directories
os.environ.setdefault("SAVE_IMAGES", "false")


@pytest.fixture
def test_image_bytes() -> bytes:
    img = Image.new("RGB", (128, 128), color=(120, 160, 80))
    buf = BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.getvalue()


@pytest.fixture
def test_image_bytes_small() -> bytes:
    img = Image.new("RGB", (32, 32), color=(200, 100, 50))
    buf = BytesIO()
    img.save(buf, format="JPEG")
    buf.seek(0)
    return buf.getvalue()

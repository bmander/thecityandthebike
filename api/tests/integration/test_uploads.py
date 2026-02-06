import io
import os
import shutil
import tempfile

import pytest
from fastapi.staticfiles import StaticFiles

from app.routers.uploads import UPLOAD_DIR


class TestUploadsRouter:
    """Test cases for the uploads router."""

    @pytest.fixture(autouse=True)
    def setup_upload_dir(self):
        """Create a temporary upload directory for tests."""
        # Store original dir
        original_dir = UPLOAD_DIR

        # Create temp dir with images subdirectory
        temp_dir = tempfile.mkdtemp()
        os.makedirs(os.path.join(temp_dir, "images"), exist_ok=True)
        import app.routers.uploads as uploads_module
        uploads_module.UPLOAD_DIR = temp_dir

        # Mount static files on test app for round-trip testing
        from tests.conftest import test_app
        test_app.mount("/uploads", StaticFiles(directory=temp_dir), name="uploads")

        yield

        # Cleanup
        shutil.rmtree(temp_dir, ignore_errors=True)
        uploads_module.UPLOAD_DIR = original_dir
        # Remove the static files mount
        test_app.router.routes = [r for r in test_app.router.routes if getattr(r, 'name', None) != 'uploads']

    def test_upload_image_success(self, client, auth_headers):
        """Test successful image upload."""
        # Create a simple JPEG file content
        image_content = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00" + b"\x00" * 100

        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.jpg", io.BytesIO(image_content), "image/jpeg")},
        )

        assert response.status_code == 201
        data = response.json()
        assert "url" in data
        assert "filename" in data
        assert data["filename"].endswith(".jpg")
        assert data["url"].startswith("/uploads/images/")

    def test_upload_image_png_success(self, client, auth_headers):
        """Test successful PNG image upload."""
        # PNG header
        png_content = b"\x89PNG\r\n\x1a\n" + b"\x00" * 100

        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.png", io.BytesIO(png_content), "image/png")},
        )

        assert response.status_code == 201
        data = response.json()
        assert data["filename"].endswith(".png")

    def test_upload_image_no_auth_fails(self, client):
        """Test that upload without auth returns 401."""
        image_content = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00" + b"\x00" * 100

        response = client.post(
            "/uploads/images",
            files={"image": ("test.jpg", io.BytesIO(image_content), "image/jpeg")},
        )

        assert response.status_code == 401

    def test_upload_image_invalid_extension_fails(self, client, auth_headers):
        """Test that upload with invalid extension returns 400."""
        txt_content = b"This is not an image"

        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.txt", io.BytesIO(txt_content), "text/plain")},
        )

        assert response.status_code == 400
        data = response.json()
        assert "not allowed" in data["detail"]["msg"].lower()

    def test_upload_image_no_file_fails(self, client, auth_headers):
        """Test that upload without a file returns 422."""
        response = client.post(
            "/uploads/images",
            headers=auth_headers,
        )

        assert response.status_code == 422

    def test_upload_image_different_extensions(self, client, auth_headers):
        """Test upload with various allowed extensions."""
        extensions = [".jpg", ".jpeg", ".png", ".gif", ".webp"]

        for ext in extensions:
            image_content = b"\x00" * 100

            response = client.post(
                "/uploads/images",
                headers=auth_headers,
                files={"image": (f"test{ext}", io.BytesIO(image_content), "image/png")},
            )

            assert response.status_code == 201, f"Failed for extension {ext}"
            data = response.json()
            assert data["filename"].endswith(ext)

    def test_upload_then_retrieve_round_trip(self, client, auth_headers):
        """Test that an uploaded file can be retrieved via its returned URL."""
        image_content = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00" + b"\x00" * 100

        # Upload
        upload_response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.jpg", io.BytesIO(image_content), "image/jpeg")},
        )
        assert upload_response.status_code == 201
        url = upload_response.json()["url"]

        # Retrieve
        get_response = client.get(url)
        assert get_response.status_code == 200
        assert get_response.content == image_content

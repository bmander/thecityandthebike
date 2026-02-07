import io
import os
import shutil
import tempfile

import pytest
from PIL import Image

from app.routers.uploads import UPLOAD_DIR
from tests.conftest import create_test_image


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

        yield

        # Cleanup
        shutil.rmtree(temp_dir, ignore_errors=True)
        uploads_module.UPLOAD_DIR = original_dir

    def test_upload_image_success(self, client, auth_headers):
        """Test successful image upload returns url, filename, and thumbnail_url."""
        image_buf = create_test_image()

        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.jpg", image_buf, "image/jpeg")},
        )

        assert response.status_code == 201
        data = response.json()
        assert "url" in data
        assert "filename" in data
        assert "thumbnail_url" in data
        assert data["filename"].endswith(".jpg")
        assert data["url"].startswith("/uploads/images/")
        assert data["thumbnail_url"].startswith("/uploads/images/thumb_")

    def test_upload_image_png_success(self, client, auth_headers):
        """Test successful PNG image upload generates a JPEG thumbnail."""
        image_buf = create_test_image(format="PNG")

        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.png", image_buf, "image/png")},
        )

        assert response.status_code == 201
        data = response.json()
        assert data["filename"].endswith(".png")
        # Thumbnail is always JPEG
        assert data["thumbnail_url"].endswith(".jpg")

    def test_upload_image_no_auth_fails(self, client):
        """Test that upload without auth returns 401."""
        image_buf = create_test_image()

        response = client.post(
            "/uploads/images",
            files={"image": ("test.jpg", image_buf, "image/jpeg")},
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
        extensions_and_formats = [
            (".jpg", "JPEG"),
            (".jpeg", "JPEG"),
            (".png", "PNG"),
            (".gif", "GIF"),
            (".webp", "WEBP"),
        ]

        for ext, fmt in extensions_and_formats:
            image_buf = create_test_image(format=fmt)

            response = client.post(
                "/uploads/images",
                headers=auth_headers,
                files={"image": (f"test{ext}", image_buf, "image/png")},
            )

            assert response.status_code == 201, f"Failed for extension {ext}"
            data = response.json()
            assert data["filename"].endswith(ext)

    def test_upload_then_retrieve_round_trip(self, client, auth_headers):
        """Test that an uploaded file can be retrieved via its returned URL."""
        image_buf = create_test_image()
        original_bytes = image_buf.getvalue()

        # Upload
        upload_response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.jpg", image_buf, "image/jpeg")},
        )
        assert upload_response.status_code == 201
        url = upload_response.json()["url"]

        # Retrieve
        get_response = client.get(url)
        assert get_response.status_code == 200
        assert get_response.content == original_bytes

    def test_upload_thumbnail_retrievable(self, client, auth_headers):
        """Test that the thumbnail can be retrieved via its returned URL."""
        image_buf = create_test_image(width=800, height=600)

        # Upload
        upload_response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.jpg", image_buf, "image/jpeg")},
        )
        assert upload_response.status_code == 201
        thumbnail_url = upload_response.json()["thumbnail_url"]

        # Retrieve thumbnail
        get_response = client.get(thumbnail_url)
        assert get_response.status_code == 200

        # Verify thumbnail is a valid image with reduced dimensions
        thumb_img = Image.open(io.BytesIO(get_response.content))
        assert max(thumb_img.size) <= 300

    def test_get_nonexistent_image_returns_404(self, client):
        """Test that requesting a nonexistent image returns 404."""
        response = client.get("/uploads/images/nonexistent.jpg")
        assert response.status_code == 404

    def test_upload_small_image_thumbnail_not_upscaled(self, client, auth_headers):
        """Test that a small image thumbnail is not upscaled beyond its original size."""
        image_buf = create_test_image(width=200, height=150)

        upload_response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("small.jpg", image_buf, "image/jpeg")},
        )
        assert upload_response.status_code == 201
        thumbnail_url = upload_response.json()["thumbnail_url"]

        get_response = client.get(thumbnail_url)
        assert get_response.status_code == 200

        thumb_img = Image.open(io.BytesIO(get_response.content))
        assert thumb_img.size == (200, 150)

    def test_upload_corrupt_file_returns_null_thumbnail(self, client, auth_headers):
        """Test that uploading garbage bytes with a valid extension succeeds but thumbnail is null."""
        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("corrupt.jpg", io.BytesIO(b"not an image"), "image/jpeg")},
        )

        assert response.status_code == 201
        data = response.json()
        assert data["thumbnail_url"] is None

    def test_upload_rgba_png_thumbnail_is_valid_jpeg(self, client, auth_headers):
        """Test that an RGBA PNG upload produces a valid JPEG thumbnail."""
        image_buf = create_test_image(width=800, height=600, format="PNG", mode="RGBA")

        upload_response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("rgba.png", image_buf, "image/png")},
        )
        assert upload_response.status_code == 201
        thumbnail_url = upload_response.json()["thumbnail_url"]

        get_response = client.get(thumbnail_url)
        assert get_response.status_code == 200

        thumb_img = Image.open(io.BytesIO(get_response.content))
        assert thumb_img.format == "JPEG"
        assert thumb_img.mode == "RGB"

    def test_path_traversal_returns_404(self, client):
        """Test that path traversal attempts return 404."""
        response = client.get("/uploads/images/../../etc/passwd")
        assert response.status_code == 404

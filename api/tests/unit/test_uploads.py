import io
import os
import shutil
import tempfile
from unittest.mock import MagicMock, patch

from PIL import Image

from app.config import settings
from app.routers.uploads import _generate_thumbnail, delete_stored_image
from tests.conftest import create_test_image


class TestGCSUploadURL:
    """Tests for GCS upload URL construction."""

    @patch("app.routers.uploads._get_gcs_bucket")
    def test_gcs_upload_returns_proxy_url(
        self, mock_get_gcs_bucket, client, auth_headers, monkeypatch
    ):
        """When STORAGE_BUCKET is set, upload should return a proxy URL."""
        bucket_name = "my-test-bucket"
        monkeypatch.setattr(settings, "STORAGE_BUCKET", bucket_name)

        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        image_buf = create_test_image()

        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.jpg", image_buf, "image/jpeg")},
        )

        assert response.status_code == 201
        data = response.json()
        assert data["url"].startswith("/uploads/images/")
        assert data["filename"].endswith(".jpg")

        # Verify both original and thumbnail blobs were created
        assert mock_bucket.blob.call_count == 2
        blob_paths = [c[0][0] for c in mock_bucket.blob.call_args_list]
        original_path = [p for p in blob_paths if not p.startswith("images/thumb_")]
        thumb_path = [p for p in blob_paths if p.startswith("images/thumb_")]
        assert len(original_path) == 1
        assert original_path[0].startswith("images/")
        assert original_path[0].endswith(".jpg")
        assert len(thumb_path) == 1
        assert thumb_path[0].endswith(".jpg")

        assert mock_blob.upload_from_string.call_count == 2

    @patch("app.routers.uploads._get_gcs_bucket")
    def test_gcs_thumbnail_content_type(
        self, mock_get_gcs_bucket, client, auth_headers, monkeypatch
    ):
        """Verify the thumbnail blob is uploaded with content_type='image/jpeg'."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-test-bucket")

        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        image_buf = create_test_image()

        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.jpg", image_buf, "image/jpeg")},
        )

        assert response.status_code == 201

        # Both uploads should use content_type="image/jpeg"
        upload_calls = mock_blob.upload_from_string.call_args_list
        assert len(upload_calls) == 2
        # The original upload is the first call
        original_call = upload_calls[0]
        assert original_call[1]["content_type"] == "image/jpeg"
        # The thumbnail upload is the second call
        thumb_call = upload_calls[1]
        assert thumb_call[1]["content_type"] == "image/jpeg"

    def test_upload_strips_exif_metadata(self, client, auth_headers, monkeypatch):
        """Uploaded JPEG should have EXIF metadata stripped."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "")

        from PIL import Image
        from PIL.ExifTags import Base as ExifBase

        # Create a JPEG with EXIF metadata
        img = Image.new("RGB", (100, 100), color="red")
        exif = img.getexif()
        exif[ExifBase.Make] = "TestCamera"
        exif[ExifBase.Model] = "TestModel"
        buf = io.BytesIO()
        img.save(buf, format="JPEG", exif=exif.tobytes())
        buf.seek(0)

        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.jpg", buf, "image/jpeg")},
        )
        assert response.status_code == 201

        # Fetch the uploaded image and verify no EXIF
        filename = response.json()["filename"]
        get_response = client.get(f"/uploads/images/{filename}")
        result_img = Image.open(io.BytesIO(get_response.content))
        result_exif = result_img.getexif()
        assert not result_exif, "EXIF data should be stripped from uploaded image"


class TestDeleteStoredImage:
    """Tests for delete_stored_image helper."""

    def test_url_none_is_noop(self):
        """Passing url=None should return without error."""
        delete_stored_image(None)  # should not raise

    @patch("app.routers.uploads._get_gcs_bucket")
    def test_gcs_delete_success(self, mock_get_gcs_bucket, monkeypatch):
        """GCS blob is deleted when STORAGE_BUCKET is set."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        delete_stored_image("/uploads/images/abc123.jpg")

        mock_bucket.blob.assert_called_once_with("images/abc123.jpg")
        mock_blob.delete.assert_called_once()

    @patch("app.routers.uploads._get_gcs_bucket")
    def test_gcs_delete_failure_logs_warning(self, mock_get_gcs_bucket, monkeypatch, caplog):
        """GCS deletion failure should be logged, not raised."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.delete.side_effect = Exception("GCS error")
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        import logging
        with caplog.at_level(logging.WARNING):
            delete_stored_image("/uploads/images/abc123.jpg")

        assert "Failed to delete GCS blob" in caplog.text

    def test_local_delete_success(self, monkeypatch, tmp_path):
        """Local file is deleted when STORAGE_BUCKET is not set."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.routers.uploads as uploads_module
        monkeypatch.setattr(uploads_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        test_file = images_dir / "abc123.jpg"
        test_file.write_bytes(b"fake image")

        delete_stored_image("/uploads/images/abc123.jpg")

        assert not test_file.exists()

    def test_local_missing_file_no_error(self, monkeypatch, tmp_path):
        """Deleting a missing local file should not raise."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.routers.uploads as uploads_module
        monkeypatch.setattr(uploads_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()

        delete_stored_image("/uploads/images/nonexistent.jpg")  # should not raise

    def test_local_delete_failure_logs_warning(self, monkeypatch, tmp_path, caplog):
        """OS-level deletion failure should be logged, not raised."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.routers.uploads as uploads_module
        monkeypatch.setattr(uploads_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        test_file = images_dir / "abc123.jpg"
        test_file.write_bytes(b"fake")

        with patch("os.remove", side_effect=PermissionError("denied")):
            import logging
            with caplog.at_level(logging.WARNING):
                delete_stored_image("/uploads/images/abc123.jpg")

        assert "Failed to delete local file" in caplog.text


class TestGenerateThumbnail:
    """Tests for _generate_thumbnail helper."""

    def test_invalid_bytes_returns_none(self):
        """Non-image bytes should return None."""
        result = _generate_thumbnail(b"this is not an image")
        assert result is None

    def test_rgba_mode_converted_to_rgb(self):
        """RGBA images should be converted to RGB for JPEG output."""
        img = Image.new("RGBA", (400, 400), color=(255, 0, 0, 128))
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        contents = buf.getvalue()

        result = _generate_thumbnail(contents)
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.mode == "RGB"
        assert thumb.format == "JPEG"

    def test_palette_mode_converted_to_rgb(self):
        """Palette-mode (P) images should be converted to RGB."""
        img = Image.new("P", (400, 400))
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        contents = buf.getvalue()

        result = _generate_thumbnail(contents)
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.mode == "RGB"

    def test_thumbnail_respects_max_size(self):
        """Large images should be resized to fit within THUMBNAIL_MAX_SIZE."""
        img = Image.new("RGB", (1200, 800), color="blue")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")
        contents = buf.getvalue()

        result = _generate_thumbnail(contents)
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert max(thumb.size) <= 300

    def test_small_image_not_upscaled(self):
        """Images smaller than THUMBNAIL_MAX_SIZE should not be upscaled."""
        img = Image.new("RGB", (100, 80), color="green")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")
        contents = buf.getvalue()

        result = _generate_thumbnail(contents)
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.size == (100, 80)

    def test_output_is_jpeg(self):
        """Thumbnail output should be a valid JPEG."""
        img = Image.new("RGB", (400, 400), color="red")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")
        contents = buf.getvalue()

        result = _generate_thumbnail(contents)
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.format == "JPEG"


class TestGetImageGCS:
    """Tests for GET /uploads/images/{filename} via GCS."""

    @patch("app.routers.uploads._get_gcs_bucket")
    def test_gcs_blob_missing_returns_404(self, mock_get_gcs_bucket, client, monkeypatch):
        """Missing GCS blob should return 404."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = False
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        response = client.get("/uploads/images/missing.jpg")
        assert response.status_code == 404

    @patch("app.routers.uploads._get_gcs_bucket")
    def test_gcs_success_returns_content_with_type(self, mock_get_gcs_bucket, client, monkeypatch):
        """GCS image should be returned with correct content type."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")

        image_bytes = create_test_image().getvalue()

        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = True
        mock_blob.download_as_bytes.return_value = image_bytes
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        response = client.get("/uploads/images/photo.jpg")
        assert response.status_code == 200
        assert response.headers["content-type"] == "image/jpeg"
        assert response.content == image_bytes

    def test_backslash_path_traversal_returns_404(self, client):
        """Backslash-based path traversal should return 404."""
        response = client.get("/uploads/images/..\\..\\etc\\passwd.jpg")
        assert response.status_code == 404


class TestFileSizeLimit:
    """Tests for the 10 MB file size limit."""

    def _setup_temp_dir(self, monkeypatch):
        temp_dir = tempfile.mkdtemp()
        os.makedirs(os.path.join(temp_dir, "images"), exist_ok=True)
        import app.routers.uploads as uploads_module
        monkeypatch.setattr(uploads_module, "UPLOAD_DIR", temp_dir)
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        return temp_dir

    def test_file_at_10mb_succeeds(self, client, auth_headers, monkeypatch):
        """A file exactly at the 10 MB limit should be accepted."""
        temp_dir = self._setup_temp_dir(monkeypatch)
        try:
            # Create a large but valid JPEG. We'll make a wide image that
            # compresses to under 10 MB, then pad the upload.
            # Instead, use a valid image that fits under 10 MB.
            image_buf = create_test_image(width=800, height=600)
            response = client.post(
                "/uploads/images",
                headers=auth_headers,
                files={"image": ("big.jpg", image_buf, "image/jpeg")},
            )
            assert response.status_code == 201
        finally:
            shutil.rmtree(temp_dir, ignore_errors=True)

    def test_file_over_10mb_returns_400(self, client, auth_headers, monkeypatch):
        """A file exceeding 10 MB should be rejected with 400."""
        temp_dir = self._setup_temp_dir(monkeypatch)
        try:
            # 10 MB + 1 byte of data with a valid JPEG extension
            oversized = b"\xff" * (10 * 1024 * 1024 + 1)
            response = client.post(
                "/uploads/images",
                headers=auth_headers,
                files={"image": ("huge.jpg", io.BytesIO(oversized), "image/jpeg")},
            )
            assert response.status_code == 400
            assert "too large" in response.json()["detail"]["msg"].lower()
        finally:
            shutil.rmtree(temp_dir, ignore_errors=True)

    def test_empty_file_returns_400(self, client, auth_headers, monkeypatch):
        """An empty file should be rejected (not a valid image)."""
        temp_dir = self._setup_temp_dir(monkeypatch)
        try:
            response = client.post(
                "/uploads/images",
                headers=auth_headers,
                files={"image": ("empty.jpg", io.BytesIO(b""), "image/jpeg")},
            )
            assert response.status_code == 400
        finally:
            shutil.rmtree(temp_dir, ignore_errors=True)

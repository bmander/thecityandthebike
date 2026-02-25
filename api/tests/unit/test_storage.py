import io
from unittest.mock import MagicMock, patch

from PIL import Image

from app.config import settings
from app.services.storage import (
    delete_image,
    generate_thumbnail,
    image_exists,
    resolve_image_url,
    retrieve_image,
    store_image,
)


class TestDeleteImage:
    """Tests for delete_image helper."""

    def test_url_none_is_noop(self):
        """Passing url=None should return without error."""
        delete_image(None)  # should not raise

    @patch("app.services.storage._get_gcs_bucket")
    def test_gcs_delete_success(self, mock_get_gcs_bucket, monkeypatch):
        """GCS blob is deleted when STORAGE_BUCKET is set."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        delete_image("/uploads/images/abc123.jpg")

        mock_bucket.blob.assert_called_once_with("images/abc123.jpg")
        mock_blob.delete.assert_called_once()

    @patch("app.services.storage._get_gcs_bucket")
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
            delete_image("/uploads/images/abc123.jpg")

        assert "Failed to delete GCS blob" in caplog.text

    def test_local_delete_success(self, monkeypatch, tmp_path):
        """Local file is deleted when STORAGE_BUCKET is not set."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        test_file = images_dir / "abc123.jpg"
        test_file.write_bytes(b"fake image")

        delete_image("/uploads/images/abc123.jpg")

        assert not test_file.exists()

    def test_local_missing_file_no_error(self, monkeypatch, tmp_path):
        """Deleting a missing local file should not raise."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()

        delete_image("/uploads/images/nonexistent.jpg")  # should not raise

    def test_local_delete_failure_logs_warning(self, monkeypatch, tmp_path, caplog):
        """OS-level deletion failure should be logged, not raised."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        test_file = images_dir / "abc123.jpg"
        test_file.write_bytes(b"fake")

        with patch("os.remove", side_effect=PermissionError("denied")):
            import logging
            with caplog.at_level(logging.WARNING):
                delete_image("/uploads/images/abc123.jpg")

        assert "Failed to delete local file" in caplog.text


class TestGenerateThumbnail:
    """Tests for generate_thumbnail helper."""

    def test_invalid_bytes_returns_none(self):
        """Non-image bytes should return None."""
        result = generate_thumbnail(b"this is not an image")
        assert result is None

    def test_rgba_mode_converted_to_rgb(self):
        """RGBA images should be converted to RGB for JPEG output."""
        img = Image.new("RGBA", (400, 400), color=(255, 0, 0, 128))
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        contents = buf.getvalue()

        result = generate_thumbnail(contents)
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

        result = generate_thumbnail(contents)
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.mode == "RGB"

    def test_thumbnail_respects_max_size(self):
        """Large images should be resized to fit within THUMBNAIL_MAX_SIZE."""
        img = Image.new("RGB", (1200, 800), color="blue")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")
        contents = buf.getvalue()

        result = generate_thumbnail(contents)
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert max(thumb.size) <= 300

    def test_small_image_not_upscaled(self):
        """Images smaller than THUMBNAIL_MAX_SIZE should not be upscaled."""
        img = Image.new("RGB", (100, 80), color="green")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")
        contents = buf.getvalue()

        result = generate_thumbnail(contents)
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.size == (100, 80)

    def test_output_is_jpeg(self):
        """Thumbnail output should be a valid JPEG."""
        img = Image.new("RGB", (400, 400), color="red")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")
        contents = buf.getvalue()

        result = generate_thumbnail(contents)
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.format == "JPEG"


class TestStoreImage:
    """Tests for store_image."""

    def test_local_store(self, monkeypatch, tmp_path):
        """store_image writes to the local filesystem when STORAGE_BUCKET is not set."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()

        store_image(b"fake image data", "test.jpg", "image/jpeg")

        stored = images_dir / "test.jpg"
        assert stored.exists()
        assert stored.read_bytes() == b"fake image data"

    @patch("app.services.storage._get_gcs_bucket")
    def test_gcs_store(self, mock_get_gcs_bucket, monkeypatch):
        """store_image uploads to GCS when STORAGE_BUCKET is set."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        store_image(b"fake image data", "test.jpg", "image/jpeg")

        mock_bucket.blob.assert_called_once_with("images/test.jpg")
        mock_blob.upload_from_string.assert_called_once_with(
            b"fake image data", content_type="image/jpeg"
        )


class TestRetrieveImage:
    """Tests for retrieve_image."""

    def test_local_retrieve(self, monkeypatch, tmp_path):
        """retrieve_image reads from local filesystem."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        (images_dir / "test.jpg").write_bytes(b"image data")

        result = retrieve_image("test.jpg")
        assert result == b"image data"

    def test_local_missing_returns_none(self, monkeypatch, tmp_path):
        """retrieve_image returns None for missing local file."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()

        result = retrieve_image("nonexistent.jpg")
        assert result is None

    @patch("app.services.storage._get_gcs_bucket")
    def test_gcs_retrieve(self, mock_get_gcs_bucket, monkeypatch):
        """retrieve_image downloads from GCS when STORAGE_BUCKET is set."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = True
        mock_blob.download_as_bytes.return_value = b"gcs data"
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        result = retrieve_image("test.jpg")
        assert result == b"gcs data"

    @patch("app.services.storage._get_gcs_bucket")
    def test_gcs_missing_returns_none(self, mock_get_gcs_bucket, monkeypatch):
        """retrieve_image returns None for missing GCS blob."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = False
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        result = retrieve_image("nonexistent.jpg")
        assert result is None


class TestImageExists:
    """Tests for image_exists."""

    def test_local_exists(self, monkeypatch, tmp_path):
        """image_exists returns True for existing local file."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        (images_dir / "test.jpg").write_bytes(b"data")

        assert image_exists("test.jpg") is True

    def test_local_not_exists(self, monkeypatch, tmp_path):
        """image_exists returns False for missing local file."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()

        assert image_exists("nonexistent.jpg") is False

    @patch("app.services.storage._get_gcs_bucket")
    def test_gcs_exists(self, mock_get_gcs_bucket, monkeypatch):
        """image_exists returns True for existing GCS blob."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = True
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        assert image_exists("test.jpg") is True

    @patch("app.services.storage._get_gcs_bucket")
    def test_gcs_not_exists(self, mock_get_gcs_bucket, monkeypatch):
        """image_exists returns False for missing GCS blob."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = False
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        assert image_exists("nonexistent.jpg") is False


class TestResolveImageUrl:
    """Tests for resolve_image_url."""

    def test_simple_filename(self):
        assert resolve_image_url("abc.jpg") == "/uploads/images/abc.jpg"

    def test_uuid_filename(self):
        assert resolve_image_url("550e8400-e29b-41d4-a716-446655440000.jpg") == \
            "/uploads/images/550e8400-e29b-41d4-a716-446655440000.jpg"

    def test_thumb_filename(self):
        assert resolve_image_url("thumb_abc.jpg") == "/uploads/images/thumb_abc.jpg"

import datetime
import io
import logging
from unittest.mock import MagicMock, patch

import pytest
from PIL import Image

from app.config import settings
from app.services.media import (
    blob_exists,
    delete_stored_image,
    generate_signed_url,
    generate_thumbnail,
    reencode_jpeg,
    reencode_png,
    store_image,
    validate_image,
)


class TestValidateImage:
    """Tests for validate_image()."""

    def test_valid_jpeg(self):
        img = Image.new("RGB", (100, 100), color="red")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")
        validate_image(buf.getvalue())  # should not raise

    def test_valid_png(self):
        img = Image.new("RGBA", (100, 100), color=(0, 255, 0, 128))
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        validate_image(buf.getvalue())  # should not raise

    def test_invalid_bytes_raises(self):
        with pytest.raises(ValueError, match="not a valid image"):
            validate_image(b"this is not an image")

    def test_empty_bytes_raises(self):
        with pytest.raises(ValueError, match="not a valid image"):
            validate_image(b"")


class TestReencodeJpeg:
    """Tests for reencode_jpeg()."""

    def test_produces_valid_jpeg(self):
        img = Image.new("RGB", (100, 100), color="blue")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")

        result = reencode_jpeg(buf.getvalue())
        out = Image.open(io.BytesIO(result))
        assert out.format == "JPEG"

    def test_strips_exif(self):
        from PIL.ExifTags import Base as ExifBase

        img = Image.new("RGB", (100, 100), color="red")
        exif = img.getexif()
        exif[ExifBase.Make] = "TestCamera"
        buf = io.BytesIO()
        img.save(buf, format="JPEG", exif=exif.tobytes())

        result = reencode_jpeg(buf.getvalue())
        out = Image.open(io.BytesIO(result))
        assert not out.getexif()

    def test_rgba_converted_to_rgb(self):
        img = Image.new("RGBA", (100, 100), color=(255, 0, 0, 128))
        buf = io.BytesIO()
        img.save(buf, format="PNG")

        result = reencode_jpeg(buf.getvalue())
        out = Image.open(io.BytesIO(result))
        assert out.mode == "RGB"
        assert out.format == "JPEG"

    def test_palette_converted_to_rgb(self):
        img = Image.new("P", (100, 100))
        buf = io.BytesIO()
        img.save(buf, format="PNG")

        result = reencode_jpeg(buf.getvalue())
        out = Image.open(io.BytesIO(result))
        assert out.mode == "RGB"


class TestReencodePng:
    """Tests for reencode_png()."""

    def test_produces_valid_png(self):
        img = Image.new("RGBA", (100, 100), color=(0, 255, 0, 128))
        buf = io.BytesIO()
        img.save(buf, format="PNG")

        result = reencode_png(buf.getvalue())
        out = Image.open(io.BytesIO(result))
        assert out.format == "PNG"

    def test_preserves_rgba_mode(self):
        img = Image.new("RGBA", (100, 100), color=(0, 255, 0, 128))
        buf = io.BytesIO()
        img.save(buf, format="PNG")

        result = reencode_png(buf.getvalue())
        out = Image.open(io.BytesIO(result))
        assert out.mode == "RGBA"


class TestGenerateThumbnail:
    """Tests for generate_thumbnail()."""

    def test_invalid_bytes_returns_none(self):
        result = generate_thumbnail(b"this is not an image")
        assert result is None

    def test_rgba_mode_converted_to_rgb(self):
        img = Image.new("RGBA", (400, 400), color=(255, 0, 0, 128))
        buf = io.BytesIO()
        img.save(buf, format="PNG")

        result = generate_thumbnail(buf.getvalue())
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.mode == "RGB"
        assert thumb.format == "JPEG"

    def test_palette_mode_converted_to_rgb(self):
        img = Image.new("P", (400, 400))
        buf = io.BytesIO()
        img.save(buf, format="PNG")

        result = generate_thumbnail(buf.getvalue())
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.mode == "RGB"

    def test_thumbnail_respects_max_size(self):
        img = Image.new("RGB", (1200, 800), color="blue")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")

        result = generate_thumbnail(buf.getvalue())
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert max(thumb.size) <= 300

    def test_small_image_not_upscaled(self):
        img = Image.new("RGB", (100, 80), color="green")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")

        result = generate_thumbnail(buf.getvalue())
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.size == (100, 80)

    def test_output_is_jpeg(self):
        img = Image.new("RGB", (400, 400), color="red")
        buf = io.BytesIO()
        img.save(buf, format="JPEG")

        result = generate_thumbnail(buf.getvalue())
        assert result is not None
        thumb = Image.open(io.BytesIO(result))
        assert thumb.format == "JPEG"


class TestDeleteStoredImage:
    """Tests for delete_stored_image()."""

    def test_url_none_is_noop(self):
        delete_stored_image(None)  # should not raise

    @patch("app.services.media.get_gcs_bucket")
    def test_gcs_delete_success(self, mock_get_gcs_bucket, monkeypatch):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        delete_stored_image("/uploads/images/abc123.jpg")

        mock_bucket.blob.assert_called_once_with("images/abc123.jpg")
        mock_blob.delete.assert_called_once()

    @patch("app.services.media.get_gcs_bucket")
    def test_gcs_delete_failure_logs_warning(self, mock_get_gcs_bucket, monkeypatch, caplog):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.delete.side_effect = Exception("GCS error")
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        with caplog.at_level(logging.WARNING):
            delete_stored_image("/uploads/images/abc123.jpg")

        assert "Failed to delete GCS blob" in caplog.text

    def test_local_delete_success(self, monkeypatch, tmp_path):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.media as media_module
        monkeypatch.setattr(media_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        test_file = images_dir / "abc123.jpg"
        test_file.write_bytes(b"fake image")

        delete_stored_image("/uploads/images/abc123.jpg")

        assert not test_file.exists()

    def test_local_missing_file_no_error(self, monkeypatch, tmp_path):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.media as media_module
        monkeypatch.setattr(media_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()

        delete_stored_image("/uploads/images/nonexistent.jpg")  # should not raise

    def test_local_delete_failure_logs_warning(self, monkeypatch, tmp_path, caplog):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.media as media_module
        monkeypatch.setattr(media_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        test_file = images_dir / "abc123.jpg"
        test_file.write_bytes(b"fake")

        with patch("os.remove", side_effect=PermissionError("denied")):
            with caplog.at_level(logging.WARNING):
                delete_stored_image("/uploads/images/abc123.jpg")

        assert "Failed to delete local file" in caplog.text


class TestStoreImage:
    """Tests for store_image()."""

    def test_local_store(self, monkeypatch, tmp_path):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        import app.services.media as media_module
        monkeypatch.setattr(media_module, "UPLOAD_DIR", str(tmp_path))

        images_dir = tmp_path / "images"
        images_dir.mkdir()

        store_image(b"fake image data", "test.jpg", "image/jpeg")

        assert (images_dir / "test.jpg").read_bytes() == b"fake image data"

    @patch("app.services.media.get_gcs_bucket")
    def test_gcs_store(self, mock_get_gcs_bucket, monkeypatch):
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


class TestGenerateSignedUrl:
    """Tests for generate_signed_url()."""

    @patch("app.services.media.get_gcs_bucket")
    def test_generates_signed_url(self, mock_get_gcs_bucket, monkeypatch):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        monkeypatch.setattr(settings, "SIGNED_URL_EXPIRATION", 3600)

        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.generate_signed_url.return_value = "https://storage.googleapis.com/signed"
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        result = generate_signed_url("photo.jpg")

        assert result == "https://storage.googleapis.com/signed"
        mock_bucket.blob.assert_called_once_with("images/photo.jpg")
        mock_blob.generate_signed_url.assert_called_once_with(
            version="v4",
            expiration=datetime.timedelta(seconds=3600),
            method="GET",
        )

    @patch("app.services.media.get_gcs_bucket")
    def test_expiration_uses_config(self, mock_get_gcs_bucket, monkeypatch):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        monkeypatch.setattr(settings, "SIGNED_URL_EXPIRATION", 7200)

        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.generate_signed_url.return_value = "https://signed"
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        generate_signed_url("photo.jpg")

        call_kwargs = mock_blob.generate_signed_url.call_args[1]
        assert call_kwargs["expiration"] == datetime.timedelta(seconds=7200)


class TestBlobExists:
    """Tests for blob_exists()."""

    @patch("app.services.media.get_gcs_bucket")
    def test_blob_exists_true(self, mock_get_gcs_bucket, monkeypatch):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = True
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        assert blob_exists("photo.jpg") is True
        mock_bucket.blob.assert_called_once_with("images/photo.jpg")

    @patch("app.services.media.get_gcs_bucket")
    def test_blob_exists_false(self, mock_get_gcs_bucket, monkeypatch):
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = False
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        assert blob_exists("missing.jpg") is False

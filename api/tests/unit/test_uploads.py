import io
import os
import shutil
import tempfile
from unittest.mock import MagicMock, patch

from PIL import Image

from app.config import settings
from tests.conftest import create_test_image


class TestGCSUploadURL:
    """Tests for GCS upload URL construction."""

    @patch("app.services.storage._get_gcs_bucket")
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

    @patch("app.services.storage._get_gcs_bucket")
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
        get_response = client.get(
            f"/uploads/images/{filename}", follow_redirects=False
        )
        result_img = Image.open(io.BytesIO(get_response.content))
        result_exif = result_img.getexif()
        assert not result_exif, "EXIF data should be stripped from uploaded image"


class TestGetImageGCS:
    """Tests for GET /uploads/images/{filename} via GCS."""

    @patch("app.services.storage._get_gcs_bucket")
    def test_gcs_blob_missing_returns_404(self, mock_get_gcs_bucket, client, monkeypatch):
        """Missing GCS blob should return 404."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = False
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        response = client.get("/uploads/images/missing.jpg", follow_redirects=False)
        assert response.status_code == 404

    @patch("google.auth.default")
    @patch("app.services.storage._get_gcs_bucket")
    def test_gcs_success_returns_redirect_to_signed_url(
        self, mock_get_gcs_bucket, mock_default, client, monkeypatch
    ):
        """GCS image should return 307 redirect to a signed URL."""
        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        monkeypatch.setattr(settings, "SIGNED_URL_EXPIRATION", 3600)

        mock_creds = MagicMock()
        mock_creds.service_account_email = "sa@project.iam.gserviceaccount.com"
        mock_default.return_value = (mock_creds, "project-id")

        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = True
        mock_blob.generate_signed_url.return_value = (
            "https://storage.googleapis.com/my-bucket/images/photo.jpg?X-Goog-Signature=abc"
        )
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        response = client.get("/uploads/images/photo.jpg", follow_redirects=False)
        assert response.status_code == 307
        assert "storage.googleapis.com" in response.headers["location"]

    @patch("google.auth.default")
    @patch("app.services.storage._get_gcs_bucket")
    def test_signed_url_expiration_uses_config(
        self, mock_get_gcs_bucket, mock_default, client, monkeypatch
    ):
        """Signed URL should use SIGNED_URL_EXPIRATION from settings."""
        import datetime

        monkeypatch.setattr(settings, "STORAGE_BUCKET", "my-bucket")
        monkeypatch.setattr(settings, "SIGNED_URL_EXPIRATION", 7200)

        mock_creds = MagicMock()
        mock_creds.service_account_email = "sa@project.iam.gserviceaccount.com"
        mock_default.return_value = (mock_creds, "project-id")

        mock_bucket = MagicMock()
        mock_blob = MagicMock()
        mock_blob.exists.return_value = True
        mock_blob.generate_signed_url.return_value = "https://signed"
        mock_bucket.blob.return_value = mock_blob
        mock_get_gcs_bucket.return_value = mock_bucket

        client.get("/uploads/images/photo.jpg", follow_redirects=False)

        call_kwargs = mock_blob.generate_signed_url.call_args[1]
        assert call_kwargs["expiration"] == datetime.timedelta(seconds=7200)
        assert call_kwargs["service_account_email"] == "sa@project.iam.gserviceaccount.com"

    def test_backslash_path_traversal_returns_404(self, client):
        """Backslash-based path traversal should return 404."""
        response = client.get("/uploads/images/..\\..\\etc\\passwd.jpg")
        assert response.status_code == 404


class TestFileSizeLimit:
    """Tests for the 10 MB file size limit."""

    def _setup_temp_dir(self, monkeypatch):
        temp_dir = tempfile.mkdtemp()
        os.makedirs(os.path.join(temp_dir, "images"), exist_ok=True)
        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", temp_dir)
        monkeypatch.setattr(settings, "STORAGE_BUCKET", None)
        return temp_dir

    def test_file_at_10mb_succeeds(self, client, auth_headers, monkeypatch):
        """A file exactly at the 10 MB limit should be accepted."""
        temp_dir = self._setup_temp_dir(monkeypatch)
        try:
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

import io
from unittest.mock import MagicMock, patch

from app.config import settings


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

        image_content = b"\xff\xd8\xff\xe0\x00\x10JFIF\x00" + b"\x00" * 100

        response = client.post(
            "/uploads/images",
            headers=auth_headers,
            files={"image": ("test.jpg", io.BytesIO(image_content), "image/jpeg")},
        )

        assert response.status_code == 201
        data = response.json()
        assert data["url"].startswith("/uploads/images/")
        assert data["filename"].endswith(".jpg")

        # Verify blob was created with correct path
        mock_bucket.blob.assert_called_once()
        blob_path = mock_bucket.blob.call_args[0][0]
        assert blob_path.startswith("images/")
        assert blob_path.endswith(".jpg")
        mock_blob.upload_from_string.assert_called_once()

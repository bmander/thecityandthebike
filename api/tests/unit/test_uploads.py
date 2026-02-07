import io
from unittest.mock import MagicMock, patch, call

from PIL import Image

from app.config import settings


def _create_test_image(width=800, height=600, format="JPEG"):
    """Create a real image in memory for testing."""
    img = Image.new("RGB", (width, height), color="red")
    buf = io.BytesIO()
    img.save(buf, format=format)
    buf.seek(0)
    return buf


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

        image_buf = _create_test_image()

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

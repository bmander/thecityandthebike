from unittest.mock import MagicMock, patch, call

from app.config import settings
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

        # Find the thumbnail upload call (second call)
        upload_calls = mock_blob.upload_from_string.call_args_list
        assert len(upload_calls) == 2
        # The thumbnail upload is the second call
        thumb_call = upload_calls[1]
        assert thumb_call[1]["content_type"] == "image/jpeg"

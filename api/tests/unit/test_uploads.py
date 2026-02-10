import io
from unittest.mock import MagicMock, patch

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

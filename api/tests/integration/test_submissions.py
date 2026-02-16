import os
import shutil
import tempfile
from datetime import date, datetime, timedelta, timezone
from unittest.mock import patch

import pytest
from sqlalchemy.orm import Query

from app.dependencies import create_access_token, get_password_hash
from app.models import User, Bike, FenderSubmission
from app.services.storage import UPLOAD_DIR
from tests.conftest import create_test_image


class TestGetSubmissions:
    """Tests for GET /submissions endpoint (cursor-paginated)."""

    def test_get_submissions_empty(self, client, auth_headers):
        """Empty database should return cursor-paginated response with no items."""
        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["has_more"] is False
        assert data["next_cursor"] is None

    def test_get_submissions_with_data(
        self, client, auth_headers, test_submission
    ):
        """Should return all submissions in cursor-paginated wrapper."""
        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 1
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)
        assert data["items"][0]["username"] == "testuser"
        assert data["has_more"] is False

    def test_get_submissions_includes_all_users(
        self, client, auth_headers, test_submission, test_user, db_session
    ):
        """Should include submissions from all users."""
        other_user = User(
            username="otheruser",
            email="other@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add(other_user)

        other_bike = Bike(
            bike_qr_id="BIKE-OTHER",
            first_seen_at=datetime.now(timezone.utc),
            last_seen_at=datetime.now(timezone.utc),
        )
        db_session.add(other_bike)
        db_session.commit()

        other_submission = FenderSubmission(
            user_id=other_user.user_id,
            bike_id=other_bike.id,
            image_url="https://example.com/other.jpg",
            captured_date=date.today(),
        )
        db_session.add(other_submission)
        db_session.commit()

        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 2

        user_ids = {sub["user_id"] for sub in data["items"]}
        assert str(test_user.user_id) in user_ids
        assert str(other_user.user_id) in user_ids

        usernames = {sub["username"] for sub in data["items"]}
        assert "testuser" in usernames
        assert "otheruser" in usernames

    def test_get_submissions_no_auth(self, client):
        """Request without auth should return 200 (public endpoint)."""
        response = client.get("/submissions")
        assert response.status_code == 200

    def test_get_submissions_cursor_traversal(
        self, client, auth_headers, test_user, test_bike, db_session
    ):
        """Should page through all items with cursor without duplicates."""
        now = datetime.now(timezone.utc)
        for i in range(5):
            sub = FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/img{i}.jpg",
                captured_date=date.today(),
                uploaded_at=now - timedelta(seconds=i),
            )
            db_session.add(sub)
        db_session.commit()

        # First page
        response = client.get("/submissions?limit=2", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 2
        assert data["has_more"] is True
        assert data["next_cursor"] is not None

        all_ids = [item["submission_id"] for item in data["items"]]

        # Second page
        response = client.get(
            f"/submissions?limit=2&cursor={data['next_cursor']}", headers=auth_headers
        )
        data = response.json()
        assert len(data["items"]) == 2
        assert data["has_more"] is True
        all_ids.extend(item["submission_id"] for item in data["items"])

        # Third page
        response = client.get(
            f"/submissions?limit=2&cursor={data['next_cursor']}", headers=auth_headers
        )
        data = response.json()
        assert len(data["items"]) == 1
        assert data["has_more"] is False
        assert data["next_cursor"] is None
        all_ids.extend(item["submission_id"] for item in data["items"])

        # No duplicates
        assert len(all_ids) == len(set(all_ids)) == 5

    def test_get_submissions_invalid_cursor(self, client, auth_headers):
        """Invalid cursor should return 422."""
        response = client.get("/submissions?cursor=not-valid", headers=auth_headers)
        assert response.status_code == 422

    def test_get_submissions_invalid_limit_zero(self, client, auth_headers):
        """Limit of 0 should return 422."""
        response = client.get("/submissions?limit=0", headers=auth_headers)
        assert response.status_code == 422

    def test_get_submissions_invalid_limit_too_large(self, client, auth_headers):
        """Limit greater than 100 should return 422."""
        response = client.get("/submissions?limit=101", headers=auth_headers)
        assert response.status_code == 422

    def test_get_submissions_descending_order(
        self, client, auth_headers, test_user, test_bike, db_session
    ):
        """Items should be returned in uploaded_at DESC order."""
        now = datetime.now(timezone.utc)
        for i in range(3):
            sub = FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/order{i}.jpg",
                captured_date=date.today(),
                uploaded_at=now - timedelta(seconds=i),
            )
            db_session.add(sub)
        db_session.commit()

        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        items = response.json()["items"]
        timestamps = [item["uploaded_at"] for item in items]
        assert timestamps == sorted(timestamps, reverse=True)

    def test_get_submissions_tie_breaking(
        self, client, auth_headers, test_user, test_bike, db_session
    ):
        """Items with same uploaded_at should all be returned via tie-breaking on submission_id."""
        now = datetime.now(timezone.utc)
        for i in range(3):
            sub = FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/tie{i}.jpg",
                captured_date=date.today(),
                uploaded_at=now,
            )
            db_session.add(sub)
        db_session.commit()

        all_ids = []
        cursor = None
        while True:
            url = "/submissions?limit=1"
            if cursor:
                url += f"&cursor={cursor}"
            response = client.get(url, headers=auth_headers)
            assert response.status_code == 200
            data = response.json()
            all_ids.extend(item["submission_id"] for item in data["items"])
            if not data["has_more"]:
                break
            cursor = data["next_cursor"]

        assert len(all_ids) == len(set(all_ids)) == 3


class TestGetSubmission:
    """Tests for GET /submissions/{submission_id} endpoint."""

    def test_get_submission_success(self, client, test_submission):
        response = client.get(f"/submissions/{test_submission.submission_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["submission_id"] == str(test_submission.submission_id)
        assert data["username"] == "testuser"

    def test_get_submission_not_found(self, client):
        response = client.get("/submissions/00000000-0000-0000-0000-000000000000")
        assert response.status_code == 404

    def test_get_submission_invalid_id(self, client):
        response = client.get("/submissions/not-a-uuid")
        assert response.status_code == 422


class TestDeleteSubmission:
    """Tests for DELETE /submissions/{submission_id} endpoint."""

    def test_delete_own_submission(
        self, client, auth_headers, test_submission, db_session
    ):
        """Owner should be able to delete their own submission."""
        response = client.delete(
            f"/submissions/{test_submission.submission_id}",
            headers=auth_headers,
        )
        assert response.status_code == 200
        assert response.json()["msg"] == "Submission deleted"

        # Verify submission is gone
        get_response = client.get(f"/submissions/{test_submission.submission_id}")
        assert get_response.status_code == 404

    def test_delete_other_users_submission(
        self, client, test_submission, db_session
    ):
        """Non-owner should get 403 when trying to delete another user's submission."""
        other_user = User(
            username="otheruser",
            email="other@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add(other_user)
        db_session.commit()

        other_token = create_access_token(subject=str(other_user.user_id))
        other_headers = {"Authorization": f"Bearer {other_token}"}

        response = client.delete(
            f"/submissions/{test_submission.submission_id}",
            headers=other_headers,
        )
        assert response.status_code == 403

    def test_delete_nonexistent_submission(self, client, auth_headers):
        """Deleting a non-existent submission should return 404."""
        response = client.delete(
            "/submissions/00000000-0000-0000-0000-000000000000",
            headers=auth_headers,
        )
        assert response.status_code == 404

    def test_delete_submission_no_auth(self, client, test_submission):
        """Request without auth should return 401."""
        response = client.delete(
            f"/submissions/{test_submission.submission_id}"
        )
        assert response.status_code == 401

    def test_delete_submission_removes_stored_images(
        self, client, auth_headers, test_user, test_bike, db_session
    ):
        """Deleting a submission should remove its image files from storage."""
        # Upload a real image to create files on disk
        image = create_test_image()
        upload_resp = client.post(
            "/uploads/images",
            files={"image": ("photo.jpg", image, "image/jpeg")},
            headers=auth_headers,
        )
        assert upload_resp.status_code == 201
        upload_data = upload_resp.json()
        original_url = upload_data["url"]
        thumbnail_url = upload_data["thumbnail_url"]

        # Verify the files are accessible
        assert client.get(original_url).status_code == 200
        assert client.get(thumbnail_url).status_code == 200

        # Create a submission referencing those uploaded images
        submission = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url=original_url,
            image_url_thumbnail=thumbnail_url,
            captured_date=date.today(),
        )
        db_session.add(submission)
        db_session.commit()
        db_session.refresh(submission)
        submission_id = submission.submission_id
        db_session.expunge(submission)

        # Delete the submission
        delete_resp = client.delete(
            f"/submissions/{submission_id}",
            headers=auth_headers,
        )
        assert delete_resp.status_code == 200

        # Verify the image files are gone
        assert client.get(original_url).status_code == 404
        assert client.get(thumbnail_url).status_code == 404


class TestCreateSubmission:
    """Tests for POST /submissions endpoint."""

    @pytest.fixture(autouse=True)
    def setup_upload_dir(self):
        """Create a temporary upload directory for tests."""
        original_dir = UPLOAD_DIR
        temp_dir = tempfile.mkdtemp()
        os.makedirs(os.path.join(temp_dir, "images"), exist_ok=True)
        import app.services.storage as storage_module
        storage_module.UPLOAD_DIR = temp_dir
        self._temp_dir = temp_dir
        yield
        shutil.rmtree(temp_dir, ignore_errors=True)
        storage_module.UPLOAD_DIR = original_dir

    def _post_submission(self, client, auth_headers, bike_qr_id, image=None, **extra_data):
        """Helper to post a multipart submission."""
        if image is None:
            image = create_test_image()
        data = {"bike_qr_id": bike_qr_id, "captured_date": date.today().isoformat()}
        data.update(extra_data)
        return client.post(
            "/submissions",
            data=data,
            files={"image": ("photo.jpg", image, "image/jpeg")},
            headers=auth_headers,
        )

    def test_create_submission_new_bike(self, client, auth_headers, test_user, db_session):
        """Creating submission for new bike should create the bike."""
        response = self._post_submission(
            client, auth_headers, "https://lime.bike/bc/v1/NEW-BIKE-001",
            user_caption="My new bike submission",
        )
        assert response.status_code == 201
        data = response.json()
        assert data["bike_qr_id"] == "NEW-BIKE-001"
        assert data["user_id"] == str(test_user.user_id)
        assert data["image_url"].startswith("/uploads/images/")
        assert data["user_caption"] == "My new bike submission"
        assert data["username"] == "testuser"

        # Verify bike was created
        bike = db_session.query(Bike).filter(Bike.bike_qr_id == "NEW-BIKE-001").first()
        assert bike is not None
        assert bike.first_seen_at is not None
        assert bike.last_seen_at is not None

    def test_create_submission_existing_bike(
        self, client, auth_headers, test_bike, test_user, db_session
    ):
        """Creating submission for existing bike should update last_seen_at."""
        original_last_seen = test_bike.last_seen_at

        response = self._post_submission(
            client, auth_headers, f"https://lime.bike/bc/v1/{test_bike.bike_qr_id}"
        )
        assert response.status_code == 201

        # Query for updated bike to verify last_seen_at was updated
        updated_bike = db_session.query(Bike).filter(
            Bike.bike_qr_id == test_bike.bike_qr_id
        ).first()
        assert updated_bike.last_seen_at >= original_last_seen

    def test_create_submission_required_fields_only(
        self, client, auth_headers, test_user
    ):
        """Submission with only required fields should succeed."""
        response = self._post_submission(
            client, auth_headers, "https://lime.bike/bc/v1/MINIMAL-BIKE"
        )
        assert response.status_code == 201
        data = response.json()
        assert data["user_caption"] is None

    def test_create_submission_missing_bike_qr_id(self, client, auth_headers):
        """Submission without bike_qr_id should return 422."""
        image = create_test_image()
        response = client.post(
            "/submissions",
            data={"captured_date": date.today().isoformat()},
            files={"image": ("photo.jpg", image, "image/jpeg")},
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_create_submission_missing_captured_date(self, client, auth_headers):
        """Submission without captured_date should return 422."""
        image = create_test_image()
        response = client.post(
            "/submissions",
            data={"bike_qr_id": "TEST-BIKE"},
            files={"image": ("photo.jpg", image, "image/jpeg")},
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_create_submission_lime_url(self, client, auth_headers, test_user, db_session):
        """Submitting a Lime URL should parse provider and bike ID."""
        response = self._post_submission(
            client, auth_headers, "https://lime.bike/bc/v1/G5EZAYI="
        )
        assert response.status_code == 201
        data = response.json()
        assert data["bike_qr_id"] == "G5EZAYI"
        assert data["provider"] == "lime"

        bike = db_session.query(Bike).filter(Bike.bike_qr_id == "G5EZAYI").first()
        assert bike is not None
        assert bike.provider == "lime"

    def test_create_submission_bird_url(self, client, auth_headers, test_user, db_session):
        """Submitting a Bird URL should parse provider and bike ID."""
        response = self._post_submission(
            client, auth_headers, "https://ride.bird.co/bc/v1/abc123"
        )
        assert response.status_code == 201
        data = response.json()
        assert data["bike_qr_id"] == "abc123"
        assert data["provider"] == "bird"

    def test_create_submission_unknown_url(self, client, auth_headers, test_user, db_session):
        """Submitting an unknown URL should be rejected."""
        response = self._post_submission(
            client, auth_headers, "https://unknown.com/bikes/XYZ"
        )
        assert response.status_code == 422

    def test_create_submission_plain_string_rejected(
        self, client, auth_headers, test_user
    ):
        """Submitting a plain string should be rejected."""
        response = self._post_submission(client, auth_headers, "PLAIN-BIKE-ID")
        assert response.status_code == 422

    def test_create_submission_concurrent_bike_creation(
        self, client, auth_headers, test_user, db_session
    ):
        """IntegrityError on bike insert falls back to fetching the existing bike."""
        db_session.add(Bike(
            bike_qr_id="RACE-BIKE",
            first_seen_at=datetime.now(timezone.utc),
            last_seen_at=datetime.now(timezone.utc),
        ))
        db_session.commit()

        # First Bike query returns None to simulate the race window
        original_first = Query.first
        skip_once = True

        def patched_first(self):
            nonlocal skip_once
            if skip_once and Bike in (d["entity"] for d in self.column_descriptions):
                skip_once = False
                return None
            return original_first(self)

        with patch.object(Query, "first", patched_first):
            response = self._post_submission(
                client, auth_headers, "https://lime.bike/bc/v1/RACE-BIKE"
            )

        assert response.status_code == 201
        assert response.json()["bike_qr_id"] == "RACE-BIKE"

    def test_create_submission_generates_thumbnail(
        self, client, auth_headers, test_user
    ):
        """Server should generate a thumbnail for the submission."""
        response = self._post_submission(
            client, auth_headers, "https://lime.bike/bc/v1/THUMB-BIKE"
        )
        assert response.status_code == 201
        data = response.json()
        assert data["image_url_thumbnail"] is not None
        assert data["image_url_thumbnail"].startswith("/uploads/images/thumb_")

    def test_create_submission_missing_image(self, client, auth_headers):
        """Submission without image file should return 422."""
        response = client.post(
            "/submissions",
            data={
                "bike_qr_id": "TEST-BIKE",
                "captured_date": date.today().isoformat(),
            },
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_create_submission_no_auth(self, client):
        """Request without auth should return 401."""
        image = create_test_image()
        response = client.post(
            "/submissions",
            data={
                "bike_qr_id": "TEST-BIKE",
                "captured_date": date.today().isoformat(),
            },
            files={"image": ("photo.jpg", image, "image/jpeg")},
        )
        assert response.status_code == 401

    def test_create_submission_invalid_image_returns_400(self, client, auth_headers):
        """Sending corrupt bytes as image should return 400."""
        import io
        corrupt_image = io.BytesIO(b"not-an-image-at-all")
        response = client.post(
            "/submissions",
            data={
                "bike_qr_id": "https://lime.bike/bc/v1/TEST-BIKE",
                "captured_date": date.today().isoformat(),
            },
            files={"image": ("photo.jpg", corrupt_image, "image/jpeg")},
            headers=auth_headers,
        )
        assert response.status_code == 400

    def test_create_submission_atomicity_db_failure_cleans_images(
        self, client, auth_headers, test_user
    ):
        """If db.commit raises, stored image files should be cleaned up."""
        from sqlalchemy.orm import Session as _Session

        def failing_commit(self):
            raise RuntimeError("simulated DB failure")

        with patch.object(_Session, "commit", failing_commit):
            with pytest.raises(RuntimeError, match="simulated DB failure"):
                self._post_submission(
                    client, auth_headers, "https://lime.bike/bc/v1/ATOMIC-BIKE"
                )

        # Verify image files were cleaned up
        images_dir = os.path.join(self._temp_dir, "images")
        remaining_files = os.listdir(images_dir)
        assert len(remaining_files) == 0, f"Expected no files but found: {remaining_files}"

    def test_create_submission_stored_image_is_retrievable(
        self, client, auth_headers, test_user
    ):
        """Created submission's image and thumbnail should be retrievable via GET."""
        response = self._post_submission(
            client, auth_headers, "https://lime.bike/bc/v1/RETRIEVE-BIKE"
        )
        assert response.status_code == 201
        data = response.json()

        # GET the image
        img_response = client.get(data["image_url"])
        assert img_response.status_code == 200

        # GET the thumbnail
        thumb_response = client.get(data["image_url_thumbnail"])
        assert thumb_response.status_code == 200

from datetime import date, datetime, timezone
from unittest.mock import patch

from sqlalchemy.orm import Query

from app.dependencies import create_access_token, get_password_hash
from app.models import User, Bike, FenderSubmission
from tests.conftest import create_test_image


class TestGetSubmissions:
    """Tests for GET /submissions endpoint."""

    def test_get_submissions_empty(self, client, auth_headers):
        """Empty database should return paginated response with no items."""
        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["total"] == 0
        assert data["limit"] == 20
        assert data["offset"] == 0

    def test_get_submissions_with_data(
        self, client, auth_headers, test_submission
    ):
        """Should return all submissions in paginated wrapper."""
        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 1
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)
        assert data["items"][0]["username"] == "testuser"

    def test_get_submissions_includes_all_users(
        self, client, auth_headers, test_submission, test_user, db_session
    ):
        """Should include submissions from all users."""
        # Create another user with a submission
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

        # Should see both submissions
        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 2
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

    def test_get_submissions_custom_limit_offset(
        self, client, auth_headers, test_user, test_bike, db_session
    ):
        """Should respect custom limit and offset parameters."""
        # Create 5 submissions
        for i in range(5):
            sub = FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/img{i}.jpg",
                captured_date=date.today(),
            )
            db_session.add(sub)
        db_session.commit()

        # Get first 2
        response = client.get("/submissions?limit=2&offset=0", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["limit"] == 2
        assert data["offset"] == 0

        # Get next 2
        response = client.get("/submissions?limit=2&offset=2", headers=auth_headers)
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["offset"] == 2

    def test_get_submissions_offset_beyond_total(
        self, client, auth_headers, test_submission
    ):
        """Offset beyond total should return empty items with correct total."""
        response = client.get("/submissions?offset=100", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 0
        assert data["offset"] == 100

    def test_get_submissions_invalid_limit_zero(self, client, auth_headers):
        """Limit of 0 should return 422."""
        response = client.get("/submissions?limit=0", headers=auth_headers)
        assert response.status_code == 422

    def test_get_submissions_invalid_limit_too_large(self, client, auth_headers):
        """Limit greater than 100 should return 422."""
        response = client.get("/submissions?limit=101", headers=auth_headers)
        assert response.status_code == 422

    def test_get_submissions_invalid_negative_offset(self, client, auth_headers):
        """Negative offset should return 422."""
        response = client.get("/submissions?offset=-1", headers=auth_headers)
        assert response.status_code == 422


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

    def test_create_submission_new_bike(self, client, auth_headers, test_user, db_session):
        """Creating submission for new bike should create the bike."""
        submission_data = {
            "bike_qr_id": "NEW-BIKE-001",
            "image_url": "https://example.com/original.jpg",
            "captured_date": date.today().isoformat(),
            "user_caption": "My new bike submission",
        }

        response = client.post(
            "/submissions",
            json=submission_data,
            headers=auth_headers,
        )
        assert response.status_code == 201
        data = response.json()
        assert data["bike_qr_id"] == "NEW-BIKE-001"
        assert data["user_id"] == str(test_user.user_id)
        assert data["image_url"] == submission_data["image_url"]
        assert data["user_caption"] == submission_data["user_caption"]
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

        submission_data = {
            "bike_qr_id": test_bike.bike_qr_id,
            "image_url": "https://example.com/new.jpg",
            "captured_date": date.today().isoformat(),
        }

        response = client.post(
            "/submissions",
            json=submission_data,
            headers=auth_headers,
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
        submission_data = {
            "bike_qr_id": "MINIMAL-BIKE",
            "image_url": "https://example.com/original.jpg",
            "captured_date": date.today().isoformat(),
        }

        response = client.post(
            "/submissions",
            json=submission_data,
            headers=auth_headers,
        )
        assert response.status_code == 201
        data = response.json()
        assert data["user_caption"] is None

    def test_create_submission_missing_bike_qr_id(self, client, auth_headers):
        """Submission without bike_qr_id should return 422."""
        submission_data = {
            "image_url": "https://example.com/original.jpg",
            "captured_date": date.today().isoformat(),
        }

        response = client.post(
            "/submissions",
            json=submission_data,
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_create_submission_missing_captured_date(self, client, auth_headers):
        """Submission without captured_date should return 422."""
        submission_data = {
            "bike_qr_id": "TEST-BIKE",
            "image_url": "https://example.com/original.jpg",
        }

        response = client.post(
            "/submissions",
            json=submission_data,
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_create_submission_lime_url(self, client, auth_headers, test_user, db_session):
        """Submitting a Lime URL should parse provider and bike ID."""
        submission_data = {
            "bike_qr_id": "https://lime.bike/bc/v1/G5EZAYI=",
            "image_url": "https://example.com/original.jpg",
            "captured_date": date.today().isoformat(),
        }

        response = client.post("/submissions", json=submission_data, headers=auth_headers)
        assert response.status_code == 201
        data = response.json()
        assert data["bike_qr_id"] == "G5EZAYI"
        assert data["provider"] == "lime"

        bike = db_session.query(Bike).filter(Bike.bike_qr_id == "G5EZAYI").first()
        assert bike is not None
        assert bike.provider == "lime"

    def test_create_submission_bird_url(self, client, auth_headers, test_user, db_session):
        """Submitting a Bird URL should parse provider and bike ID."""
        submission_data = {
            "bike_qr_id": "https://ride.bird.co/bc/v1/abc123",
            "image_url": "https://example.com/original.jpg",
            "captured_date": date.today().isoformat(),
        }

        response = client.post("/submissions", json=submission_data, headers=auth_headers)
        assert response.status_code == 201
        data = response.json()
        assert data["bike_qr_id"] == "abc123"
        assert data["provider"] == "bird"

    def test_create_submission_unknown_url(self, client, auth_headers, test_user, db_session):
        """Submitting an unknown URL should store it as-is with no provider."""
        submission_data = {
            "bike_qr_id": "https://unknown.com/bikes/XYZ",
            "image_url": "https://example.com/original.jpg",
            "captured_date": date.today().isoformat(),
        }

        response = client.post("/submissions", json=submission_data, headers=auth_headers)
        assert response.status_code == 201
        data = response.json()
        assert data["bike_qr_id"] == "https://unknown.com/bikes/XYZ"
        assert data["provider"] is None

    def test_create_submission_plain_string_provider_null(
        self, client, auth_headers, test_user
    ):
        """Submitting a plain string should have provider=None."""
        submission_data = {
            "bike_qr_id": "PLAIN-BIKE-ID",
            "image_url": "https://example.com/original.jpg",
            "captured_date": date.today().isoformat(),
        }

        response = client.post("/submissions", json=submission_data, headers=auth_headers)
        assert response.status_code == 201
        data = response.json()
        assert data["bike_qr_id"] == "PLAIN-BIKE-ID"
        assert data["provider"] is None

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
            response = client.post(
                "/submissions",
                json={
                    "bike_qr_id": "RACE-BIKE",
                    "image_url": "https://example.com/original.jpg",
                            "captured_date": date.today().isoformat(),
                },
                headers=auth_headers,
            )

        assert response.status_code == 201
        assert response.json()["bike_qr_id"] == "RACE-BIKE"

    def test_create_submission_saves_thumbnail_url(
        self, client, auth_headers, test_user
    ):
        """Thumbnail URL should be saved to the submission."""
        submission_data = {
            "bike_qr_id": "THUMB-BIKE",
            "image_url": "https://example.com/original.jpg",
            "image_url_thumbnail": "https://example.com/thumb.jpg",
            "captured_date": date.today().isoformat(),
        }

        response = client.post(
            "/submissions",
            json=submission_data,
            headers=auth_headers,
        )
        assert response.status_code == 201
        data = response.json()
        assert data["image_url_thumbnail"] == "https://example.com/thumb.jpg"

    def test_create_submission_no_auth(self, client):
        """Request without auth should return 401."""
        submission_data = {
            "bike_qr_id": "TEST-BIKE",
            "image_url": "https://example.com/original.jpg",
            "captured_date": date.today().isoformat(),
        }

        response = client.post("/submissions", json=submission_data)
        assert response.status_code == 401

from datetime import datetime, timezone

from app.dependencies import get_password_hash, create_access_token
from app.models import User, Bike, FenderSubmission


class TestGetSubmissions:
    """Tests for GET /submissions endpoint."""

    def test_get_submissions_empty(self, client, auth_headers):
        """Empty database should return empty list."""
        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        assert response.json() == []

    def test_get_submissions_with_data(
        self, client, auth_headers, test_submission
    ):
        """Should return all submissions."""
        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert len(data) == 1
        assert data[0]["submission_id"] == test_submission.submission_id

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
            bike_qr_id=other_bike.bike_qr_id,
            image_url_original="https://example.com/other.jpg",
            image_url_processed="https://example.com/other-processed.jpg",
            captured_at=datetime.now(timezone.utc),
        )
        db_session.add(other_submission)
        db_session.commit()

        # Should see both submissions
        response = client.get("/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert len(data) == 2

        user_ids = {sub["user_id"] for sub in data}
        assert test_user.user_id in user_ids
        assert other_user.user_id in user_ids

    def test_get_submissions_no_auth(self, client):
        """Request without auth should return 200 (public endpoint)."""
        response = client.get("/submissions")
        assert response.status_code == 200


class TestCreateSubmission:
    """Tests for POST /submissions endpoint."""

    def test_create_submission_new_bike(self, client, auth_headers, test_user, db_session):
        """Creating submission for new bike should create the bike."""
        submission_data = {
            "bike_qr_id": "NEW-BIKE-001",
            "image_url_original": "https://example.com/original.jpg",
            "image_url_processed": "https://example.com/processed.jpg",
            "captured_at": datetime.now(timezone.utc).isoformat(),
            "latitude": 45.5231,
            "longitude": -122.6765,
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
        assert data["user_id"] == test_user.user_id
        assert data["image_url_original"] == submission_data["image_url_original"]
        assert data["user_caption"] == submission_data["user_caption"]

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
            "image_url_original": "https://example.com/new.jpg",
            "image_url_processed": "https://example.com/new-processed.jpg",
            "captured_at": datetime.now(timezone.utc).isoformat(),
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
            "image_url_original": "https://example.com/original.jpg",
            "image_url_processed": "https://example.com/processed.jpg",
            "captured_at": datetime.now(timezone.utc).isoformat(),
        }

        response = client.post(
            "/submissions",
            json=submission_data,
            headers=auth_headers,
        )
        assert response.status_code == 201
        data = response.json()
        assert data["latitude"] is None
        assert data["longitude"] is None
        assert data["user_caption"] is None

    def test_create_submission_missing_bike_qr_id(self, client, auth_headers):
        """Submission without bike_qr_id should return 422."""
        submission_data = {
            "image_url_original": "https://example.com/original.jpg",
            "image_url_processed": "https://example.com/processed.jpg",
            "captured_at": datetime.now(timezone.utc).isoformat(),
        }

        response = client.post(
            "/submissions",
            json=submission_data,
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_create_submission_missing_captured_at(self, client, auth_headers):
        """Submission without captured_at should return 422."""
        submission_data = {
            "bike_qr_id": "TEST-BIKE",
            "image_url_original": "https://example.com/original.jpg",
            "image_url_processed": "https://example.com/processed.jpg",
        }

        response = client.post(
            "/submissions",
            json=submission_data,
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_create_submission_no_auth(self, client):
        """Request without auth should return 401."""
        submission_data = {
            "bike_qr_id": "TEST-BIKE",
            "image_url_original": "https://example.com/original.jpg",
            "image_url_processed": "https://example.com/processed.jpg",
            "captured_at": datetime.now(timezone.utc).isoformat(),
        }

        response = client.post("/submissions", json=submission_data)
        assert response.status_code == 401

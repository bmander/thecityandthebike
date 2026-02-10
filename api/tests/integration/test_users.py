from datetime import date, datetime, timezone

from app.dependencies import get_password_hash
from app.models import User, FenderSubmission, Bike


class TestGetProfile:
    """Tests for GET /users/me endpoint."""

    def test_get_profile_success(self, client, auth_headers, test_user):
        """Authenticated user should receive their profile."""
        response = client.get("/users/me", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["user_id"] == test_user.user_id
        assert data["username"] == test_user.username
        assert data["email"] == test_user.email

    def test_get_profile_no_auth(self, client):
        """Request without auth should return 401."""
        response = client.get("/users/me")
        assert response.status_code == 401

    def test_get_profile_invalid_token(self, client):
        """Request with invalid token should return 401."""
        response = client.get(
            "/users/me",
            headers={"Authorization": "Bearer invalidtoken"},
        )
        assert response.status_code == 401

    def test_get_profile_malformed_auth_header(self, client):
        """Request with malformed auth header should return 401."""
        response = client.get(
            "/users/me",
            headers={"Authorization": "NotBearer token"},
        )
        assert response.status_code == 401


class TestGetMySubmissions:
    """Tests for GET /users/me/submissions endpoint."""

    def test_get_my_submissions_empty(self, client, auth_headers):
        """User with no submissions should receive paginated response with no items."""
        response = client.get("/users/me/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["total"] == 0
        assert data["limit"] == 20
        assert data["offset"] == 0

    def test_get_my_submissions_with_data(
        self, client, auth_headers, test_submission, test_user
    ):
        """User should see their own submissions in paginated wrapper."""
        response = client.get("/users/me/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 1
        assert data["items"][0]["submission_id"] == test_submission.submission_id
        assert data["items"][0]["user_id"] == test_user.user_id

    def test_get_my_submissions_excludes_other_users(
        self, client, auth_headers, test_submission, test_user, db_session
    ):
        """User should not see submissions from other users."""
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
            image_url_original="https://example.com/other.jpg",
            image_url_processed="https://example.com/other-processed.jpg",
            captured_date=date.today(),
        )
        db_session.add(other_submission)
        db_session.commit()

        # Test user should only see their own submission
        response = client.get("/users/me/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 1
        assert data["items"][0]["user_id"] == test_user.user_id
        assert data["items"][0]["submission_id"] == test_submission.submission_id

    def test_get_my_submissions_no_auth(self, client):
        """Request without auth should return 401."""
        response = client.get("/users/me/submissions")
        assert response.status_code == 401

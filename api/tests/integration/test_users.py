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
        assert data["user_id"] == str(test_user.user_id)
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
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)
        assert data["items"][0]["user_id"] == str(test_user.user_id)

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
        assert data["items"][0]["user_id"] == str(test_user.user_id)
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)

    def test_get_my_submissions_no_auth(self, client):
        """Request without auth should return 401."""
        response = client.get("/users/me/submissions")
        assert response.status_code == 401

    def test_get_my_submissions_custom_limit_offset(
        self, client, auth_headers, test_user, test_bike, db_session
    ):
        """Should respect custom limit and offset parameters."""
        for i in range(5):
            sub = FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url_original=f"https://example.com/img{i}.jpg",
                image_url_processed=f"https://example.com/img{i}-proc.jpg",
                captured_date=date.today(),
            )
            db_session.add(sub)
        db_session.commit()

        response = client.get(
            "/users/me/submissions?limit=2&offset=0", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["limit"] == 2
        assert data["offset"] == 0

        response = client.get(
            "/users/me/submissions?limit=2&offset=2", headers=auth_headers
        )
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["offset"] == 2

    def test_get_my_submissions_offset_beyond_total(
        self, client, auth_headers, test_submission
    ):
        """Offset beyond total should return empty items with correct total."""
        response = client.get(
            "/users/me/submissions?offset=100", headers=auth_headers
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 0
        assert data["offset"] == 100

    def test_get_my_submissions_invalid_limit_zero(self, client, auth_headers):
        """Limit of 0 should return 422."""
        response = client.get(
            "/users/me/submissions?limit=0", headers=auth_headers
        )
        assert response.status_code == 422

    def test_get_my_submissions_invalid_limit_too_large(self, client, auth_headers):
        """Limit greater than 100 should return 422."""
        response = client.get(
            "/users/me/submissions?limit=101", headers=auth_headers
        )
        assert response.status_code == 422

    def test_get_my_submissions_invalid_negative_offset(self, client, auth_headers):
        """Negative offset should return 422."""
        response = client.get(
            "/users/me/submissions?offset=-1", headers=auth_headers
        )
        assert response.status_code == 422


class TestGetUserDetail:
    """Tests for GET /users/{user_id} endpoint."""

    def test_get_user_detail_success(self, client, test_user):
        """Should return user details with submission count."""
        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["user_id"] == str(test_user.user_id)
        assert data["username"] == test_user.username
        assert data["submission_count"] == 0

    def test_get_user_detail_not_found(self, client):
        """Request for nonexistent user should return 404."""
        response = client.get("/users/00000000-0000-0000-0000-000000000000")
        assert response.status_code == 404
        assert "User not found" in response.json()["detail"]["msg"]

    def test_get_user_detail_submission_count(
        self, client, test_user, test_bike, db_session
    ):
        """Should return correct submission count and first/last seen dates."""
        for i in range(3):
            sub = FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url_original=f"https://example.com/img{i}.jpg",
                image_url_processed=f"https://example.com/img{i}-proc.jpg",
                captured_date=date.today(),
            )
            db_session.add(sub)
        db_session.commit()

        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["submission_count"] == 3
        assert data["first_seen_at"] is not None
        assert data["last_seen_at"] is not None

    def test_get_user_detail_no_submissions_nulls(self, client, test_user):
        """User with no submissions should have null first/last seen dates."""
        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["first_seen_at"] is None
        assert data["last_seen_at"] is None

    def test_get_user_detail_omits_email(self, client, test_user):
        """Response should not contain the user's email for privacy."""
        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        assert "email" not in data


class TestGetUserSubmissions:
    """Tests for GET /users/{user_id}/submissions endpoint."""

    def test_get_user_submissions_success(
        self, client, auth_headers, test_user, test_submission
    ):
        """Should return submissions for the specified user in paginated wrapper."""
        response = client.get(
            f"/users/{test_user.user_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 1
        assert data["items"][0]["user_id"] == str(test_user.user_id)
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)

    def test_get_user_submissions_not_found(self, client, auth_headers):
        """Request for nonexistent user should return 404."""
        response = client.get(
            "/users/00000000-0000-0000-0000-000000000000/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 404
        assert "User not found" in response.json()["detail"]["msg"]

    def test_get_user_submissions_multiple(
        self, client, auth_headers, test_user, test_bike, db_session
    ):
        """Should return all submissions for a user."""
        submission1 = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url_original="https://example.com/img1.jpg",
            image_url_processed="https://example.com/img1-proc.jpg",
            captured_date=date.today(),
        )
        submission2 = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url_original="https://example.com/img2.jpg",
            image_url_processed="https://example.com/img2-proc.jpg",
            captured_date=date.today(),
        )
        db_session.add_all([submission1, submission2])
        db_session.commit()

        response = client.get(
            f"/users/{test_user.user_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 2
        assert len(data["items"]) == 2
        for sub in data["items"]:
            assert sub["user_id"] == str(test_user.user_id)

    def test_get_user_submissions_empty(
        self, client, auth_headers, test_user
    ):
        """User with no submissions should return paginated response with no items."""
        response = client.get(
            f"/users/{test_user.user_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["total"] == 0
        assert data["limit"] == 20
        assert data["offset"] == 0

    def test_get_user_submissions_no_auth(self, client, test_user):
        """Request without auth should return 200 (public endpoint)."""
        response = client.get(f"/users/{test_user.user_id}/submissions")
        assert response.status_code == 200

    def test_get_user_submissions_custom_limit_offset(
        self, client, auth_headers, test_user, test_bike, db_session
    ):
        """Should respect custom limit and offset parameters."""
        for i in range(5):
            sub = FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url_original=f"https://example.com/img{i}.jpg",
                image_url_processed=f"https://example.com/img{i}-proc.jpg",
                captured_date=date.today(),
            )
            db_session.add(sub)
        db_session.commit()

        response = client.get(
            f"/users/{test_user.user_id}/submissions?limit=2&offset=0",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["limit"] == 2
        assert data["offset"] == 0

        response = client.get(
            f"/users/{test_user.user_id}/submissions?limit=2&offset=2",
            headers=auth_headers,
        )
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["offset"] == 2

    def test_get_user_submissions_offset_beyond_total(
        self, client, auth_headers, test_user, test_submission
    ):
        """Offset beyond total should return empty items with correct total."""
        response = client.get(
            f"/users/{test_user.user_id}/submissions?offset=100",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 0
        assert data["offset"] == 100

    def test_get_user_submissions_invalid_limit_zero(
        self, client, auth_headers, test_user
    ):
        """Limit of 0 should return 422."""
        response = client.get(
            f"/users/{test_user.user_id}/submissions?limit=0",
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_get_user_submissions_invalid_limit_too_large(
        self, client, auth_headers, test_user
    ):
        """Limit greater than 100 should return 422."""
        response = client.get(
            f"/users/{test_user.user_id}/submissions?limit=101",
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_get_user_submissions_invalid_negative_offset(
        self, client, auth_headers, test_user
    ):
        """Negative offset should return 422."""
        response = client.get(
            f"/users/{test_user.user_id}/submissions?offset=-1",
            headers=auth_headers,
        )
        assert response.status_code == 422

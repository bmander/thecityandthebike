from datetime import date, datetime, timezone, timedelta

from app.dependencies import get_password_hash
from app.models import FenderSubmission, User


class TestGetBikeDetail:
    """Tests for GET /bikes/{bike_qr_id} endpoint."""

    def test_get_bike_detail_success(self, client, test_bike):
        """Should return bike details with submission count."""
        response = client.get(f"/bikes/{test_bike.bike_qr_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["bike_qr_id"] == test_bike.bike_qr_id
        assert data["submission_count"] == 0

    def test_get_bike_detail_not_found(self, client):
        """Request for nonexistent bike should return 404."""
        response = client.get("/bikes/NONEXISTENT-BIKE")
        assert response.status_code == 404
        assert "Bike not found" in response.json()["detail"]["msg"]

    def test_get_bike_detail_no_submissions_captured_by_null(self, client, test_bike):
        """Bike with no submissions should return null for both captured_by fields."""
        response = client.get(f"/bikes/{test_bike.bike_qr_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["first_captured_by"] is None
        assert data["last_captured_by"] is None

    def test_get_bike_detail_one_submission_captured_by_same_user(
        self, client, test_bike, test_user, db_session
    ):
        """Single submission should return same user for both captured_by fields."""
        sub = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=date.today(),
        )
        db_session.add(sub)
        db_session.commit()

        response = client.get(f"/bikes/{test_bike.bike_qr_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["first_captured_by"]["name"] == test_user.username
        assert data["first_captured_by"]["id"] == str(test_user.user_id)
        assert data["last_captured_by"]["name"] == test_user.username
        assert data["last_captured_by"]["id"] == str(test_user.user_id)

    def test_get_bike_detail_multiple_submissions_captured_by(
        self, client, test_bike, test_user, db_session
    ):
        """Multiple submissions from different users should return correct first/last."""
        second_user = User(
            username="seconduser",
            email="second@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add(second_user)
        db_session.commit()
        db_session.refresh(second_user)

        now = datetime.now(timezone.utc)
        early_sub = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/first.jpg",
            captured_date=date.today(),
            uploaded_at=now - timedelta(days=10),
        )
        late_sub = FenderSubmission(
            user_id=second_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/last.jpg",
            captured_date=date.today(),
            uploaded_at=now,
        )
        db_session.add_all([early_sub, late_sub])
        db_session.commit()

        response = client.get(f"/bikes/{test_bike.bike_qr_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["first_captured_by"]["name"] == test_user.username
        assert data["first_captured_by"]["id"] == str(test_user.user_id)
        assert data["last_captured_by"]["name"] == "seconduser"
        assert data["last_captured_by"]["id"] == str(second_user.user_id)

    def test_get_bike_detail_captured_by_user_object_format(
        self, client, test_bike, test_user, db_session
    ):
        """Captured-by fields should have exactly 'name' and 'id' keys."""
        sub = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=date.today(),
        )
        db_session.add(sub)
        db_session.commit()

        response = client.get(f"/bikes/{test_bike.bike_qr_id}")
        assert response.status_code == 200
        data = response.json()
        assert set(data["first_captured_by"].keys()) == {"name", "id"}
        assert set(data["last_captured_by"].keys()) == {"name", "id"}

    def test_get_bike_detail_submission_count(
        self, client, test_bike, test_user, db_session
    ):
        """Should return correct submission count."""
        for i in range(3):
            sub = FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/img{i}.jpg",
                captured_date=date.today(),
            )
            db_session.add(sub)
        db_session.commit()

        response = client.get(f"/bikes/{test_bike.bike_qr_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["submission_count"] == 3


class TestGetBikeSubmissions:
    """Tests for GET /bikes/{bike_qr_id}/submissions endpoint."""

    def test_get_bike_submissions_success(
        self, client, auth_headers, test_bike, test_submission
    ):
        """Should return submissions for the specified bike in paginated wrapper."""
        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 1
        assert data["items"][0]["bike_qr_id"] == test_bike.bike_qr_id
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)

    def test_get_bike_submissions_not_found(self, client, auth_headers):
        """Request for nonexistent bike should return 404."""
        response = client.get(
            "/bikes/NONEXISTENT-BIKE/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 404
        assert "Bike not found" in response.json()["detail"]["msg"]

    def test_get_bike_submissions_multiple(
        self, client, auth_headers, test_bike, test_user, db_session
    ):
        """Should return all submissions for a bike."""
        # Create additional submissions for the same bike
        submission1 = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img1.jpg",
            captured_date=date.today(),
        )
        submission2 = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img2.jpg",
            captured_date=date.today(),
        )
        db_session.add_all([submission1, submission2])
        db_session.commit()

        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 2
        assert len(data["items"]) == 2
        for sub in data["items"]:
            assert sub["bike_qr_id"] == test_bike.bike_qr_id

    def test_get_bike_submissions_empty(
        self, client, auth_headers, test_bike
    ):
        """Bike with no submissions should return paginated response with no items."""
        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["total"] == 0
        assert data["limit"] == 20
        assert data["offset"] == 0

    def test_get_bike_submissions_no_auth(self, client, test_bike):
        """Request without auth should return 200 (public endpoint)."""
        response = client.get(f"/bikes/{test_bike.bike_qr_id}/submissions")
        assert response.status_code == 200

    def test_get_bike_submissions_custom_limit_offset(
        self, client, auth_headers, test_bike, test_user, db_session
    ):
        """Should respect custom limit and offset parameters."""
        for i in range(5):
            sub = FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/img{i}.jpg",
                captured_date=date.today(),
            )
            db_session.add(sub)
        db_session.commit()

        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions?limit=2&offset=0",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["limit"] == 2
        assert data["offset"] == 0

        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions?limit=2&offset=2",
            headers=auth_headers,
        )
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["offset"] == 2

    def test_get_bike_submissions_offset_beyond_total(
        self, client, auth_headers, test_bike, test_submission
    ):
        """Offset beyond total should return empty items with correct total."""
        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions?offset=100",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 0
        assert data["offset"] == 100

    def test_get_bike_submissions_invalid_limit_zero(
        self, client, auth_headers, test_bike
    ):
        """Limit of 0 should return 422."""
        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions?limit=0",
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_get_bike_submissions_invalid_limit_too_large(
        self, client, auth_headers, test_bike
    ):
        """Limit greater than 100 should return 422."""
        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions?limit=101",
            headers=auth_headers,
        )
        assert response.status_code == 422

    def test_get_bike_submissions_invalid_negative_offset(
        self, client, auth_headers, test_bike
    ):
        """Negative offset should return 422."""
        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions?offset=-1",
            headers=auth_headers,
        )
        assert response.status_code == 422

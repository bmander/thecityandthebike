from datetime import date, datetime, timezone

from app.models import Bike, FenderSubmission


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
        assert data["items"][0]["submission_id"] == test_submission.submission_id

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
        """Request without auth should return 401."""
        response = client.get(f"/bikes/{test_bike.bike_qr_id}/submissions")
        assert response.status_code == 401

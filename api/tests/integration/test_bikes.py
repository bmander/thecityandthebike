from datetime import date, datetime, timezone, timedelta

from app.dependencies import get_password_hash
from app.models import FenderSubmission, User


class TestListBikes:
    """Tests for GET /bikes endpoint."""

    def test_list_bikes_empty(self, client):
        """Should return empty list when no bikes exist."""
        response = client.get("/bikes")
        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["total"] == 0
        assert data["limit"] == 20
        assert data["offset"] == 0

    def test_list_bikes_single_bike_no_submissions(self, client, test_bike):
        """Single bike with no submissions should have count 0 and no owner."""
        response = client.get("/bikes")
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 1
        assert len(data["items"]) == 1
        item = data["items"][0]
        assert item["bike_qr_id"] == test_bike.bike_qr_id
        assert item["submission_count"] == 0
        assert item["owner"] is None

    def test_list_bikes_with_submissions_and_owner(
        self, client, test_bike, test_user, db_session
    ):
        """Bike with submissions should show count and owner."""
        for i in range(3):
            db_session.add(FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/img{i}.jpg",
                captured_date=date.today(),
            ))
        db_session.commit()

        response = client.get("/bikes")
        assert response.status_code == 200
        data = response.json()
        item = data["items"][0]
        assert item["submission_count"] == 3
        assert item["owner"]["name"] == test_user.username
        assert item["owner"]["id"] == str(test_user.user_id)

    def test_list_bikes_ordered_by_submission_count_desc(
        self, client, test_user, db_session
    ):
        """Bikes should be ordered by submission count descending."""
        from app.models import Bike
        bike_a = Bike(bike_qr_id="BIKE-AAA", first_seen_at=datetime.now(timezone.utc), last_seen_at=datetime.now(timezone.utc))
        bike_b = Bike(bike_qr_id="BIKE-BBB", first_seen_at=datetime.now(timezone.utc), last_seen_at=datetime.now(timezone.utc))
        db_session.add_all([bike_a, bike_b])
        db_session.commit()
        db_session.refresh(bike_a)
        db_session.refresh(bike_b)

        # bike_b gets 3 submissions, bike_a gets 1
        db_session.add(FenderSubmission(
            user_id=test_user.user_id,
            bike_id=bike_a.id,
            image_url="https://example.com/a1.jpg",
            captured_date=date.today(),
        ))
        for i in range(3):
            db_session.add(FenderSubmission(
                user_id=test_user.user_id,
                bike_id=bike_b.id,
                image_url=f"https://example.com/b{i}.jpg",
                captured_date=date.today(),
            ))
        db_session.commit()

        response = client.get("/bikes")
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 2
        assert data["items"][0]["bike_qr_id"] == "BIKE-BBB"
        assert data["items"][0]["submission_count"] == 3
        assert data["items"][1]["bike_qr_id"] == "BIKE-AAA"
        assert data["items"][1]["submission_count"] == 1

    def test_list_bikes_pagination(self, client, db_session):
        """Should respect limit and offset parameters."""
        from app.models import Bike
        for i in range(5):
            db_session.add(Bike(
                bike_qr_id=f"BIKE-{i:03d}",
                first_seen_at=datetime.now(timezone.utc),
                last_seen_at=datetime.now(timezone.utc),
            ))
        db_session.commit()

        response = client.get("/bikes?limit=2&offset=0")
        assert response.status_code == 200
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["limit"] == 2
        assert data["offset"] == 0

        response = client.get("/bikes?limit=2&offset=2")
        data = response.json()
        assert data["total"] == 5
        assert len(data["items"]) == 2
        assert data["offset"] == 2

    def test_list_bikes_owner_is_user_with_most_submissions(
        self, client, test_bike, test_user, db_session
    ):
        """Owner should be the user with the most submissions for that bike."""
        second_user = User(
            username="seconduser",
            email="second@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add(second_user)
        db_session.commit()
        db_session.refresh(second_user)

        # test_user gets 1 submission, second_user gets 2
        db_session.add(FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/u1.jpg",
            captured_date=date.today(),
        ))
        for i in range(2):
            db_session.add(FenderSubmission(
                user_id=second_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/u2_{i}.jpg",
                captured_date=date.today(),
            ))
        db_session.commit()

        response = client.get("/bikes")
        assert response.status_code == 200
        data = response.json()
        item = data["items"][0]
        assert item["owner"]["name"] == "seconduser"
        assert item["owner"]["id"] == str(second_user.user_id)

    def test_list_bikes_provider_included(self, client, db_session):
        """Provider field should be included in the response."""
        from app.models import Bike
        bike = Bike(
            bike_qr_id="BIKE-PROV",
            provider="citibike",
            first_seen_at=datetime.now(timezone.utc),
            last_seen_at=datetime.now(timezone.utc),
        )
        db_session.add(bike)
        db_session.commit()

        response = client.get("/bikes")
        assert response.status_code == 200
        data = response.json()
        assert data["items"][0]["provider"] == "citibike"


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

    def test_get_bike_detail_user_summary_object_format(
        self, client, test_bike, test_user, db_session
    ):
        """UserSummary fields should have exactly 'name' and 'id' keys."""
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
        assert set(data["owners"][0]["user"].keys()) == {"name", "id"}

    def test_get_bike_detail_no_submissions_owners_empty(self, client, test_bike):
        """Bike with no submissions should return empty owners list."""
        response = client.get(f"/bikes/{test_bike.bike_qr_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["owners"] == []

    def test_get_bike_detail_single_submission_owner(
        self, client, test_bike, test_user, db_session
    ):
        """Single submission should make that user the sole owner."""
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
        assert len(data["owners"]) == 1
        assert data["owners"][0]["user"]["name"] == test_user.username
        assert data["owners"][0]["user"]["id"] == str(test_user.user_id)
        assert data["owners"][0]["submission_count"] == 1

    def test_get_bike_detail_single_owner_multiple_submissions(
        self, client, test_bike, test_user, db_session
    ):
        """One user with multiple submissions should be the sole owner."""
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
        assert len(data["owners"]) == 1
        assert data["owners"][0]["user"]["name"] == test_user.username
        assert data["owners"][0]["submission_count"] == 3

    def test_get_bike_detail_shared_ownership(
        self, client, test_bike, test_user, db_session
    ):
        """Two users tied for most submissions should both be owners."""
        second_user = User(
            username="seconduser",
            email="second@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add(second_user)
        db_session.commit()
        db_session.refresh(second_user)

        for i in range(2):
            db_session.add(FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/u1_{i}.jpg",
                captured_date=date.today(),
            ))
            db_session.add(FenderSubmission(
                user_id=second_user.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/u2_{i}.jpg",
                captured_date=date.today(),
            ))
        db_session.commit()

        response = client.get(f"/bikes/{test_bike.bike_qr_id}")
        assert response.status_code == 200
        data = response.json()
        assert len(data["owners"]) == 2
        owner_names = {o["user"]["name"] for o in data["owners"]}
        assert owner_names == {test_user.username, "seconduser"}
        for owner in data["owners"]:
            assert owner["submission_count"] == 2

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
    """Tests for GET /bikes/{bike_qr_id}/submissions endpoint (cursor-paginated)."""

    def test_get_bike_submissions_success(
        self, client, auth_headers, test_bike, test_submission
    ):
        """Should return submissions for the specified bike in cursor-paginated wrapper."""
        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 1
        assert data["items"][0]["bike_qr_id"] == test_bike.bike_qr_id
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)
        assert data["has_more"] is False

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
        assert len(data["items"]) == 2
        for sub in data["items"]:
            assert sub["bike_qr_id"] == test_bike.bike_qr_id

    def test_get_bike_submissions_empty(
        self, client, auth_headers, test_bike
    ):
        """Bike with no submissions should return cursor-paginated response with no items."""
        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["has_more"] is False
        assert data["next_cursor"] is None

    def test_get_bike_submissions_no_auth(self, client, test_bike):
        """Request without auth should return 200 (public endpoint)."""
        response = client.get(f"/bikes/{test_bike.bike_qr_id}/submissions")
        assert response.status_code == 200

    def test_get_bike_submissions_cursor_traversal(
        self, client, auth_headers, test_bike, test_user, db_session
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

        all_ids = []
        cursor = None
        while True:
            url = f"/bikes/{test_bike.bike_qr_id}/submissions?limit=2"
            if cursor:
                url += f"&cursor={cursor}"
            response = client.get(url, headers=auth_headers)
            assert response.status_code == 200
            data = response.json()
            all_ids.extend(item["submission_id"] for item in data["items"])
            if not data["has_more"]:
                break
            cursor = data["next_cursor"]

        assert len(all_ids) == len(set(all_ids)) == 5

    def test_get_bike_submissions_invalid_cursor(
        self, client, auth_headers, test_bike
    ):
        """Invalid cursor should return 422."""
        response = client.get(
            f"/bikes/{test_bike.bike_qr_id}/submissions?cursor=not-valid",
            headers=auth_headers,
        )
        assert response.status_code == 422

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

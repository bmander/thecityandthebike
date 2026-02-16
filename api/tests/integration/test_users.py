from datetime import date, datetime, timedelta, timezone

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
    """Tests for GET /users/me/submissions endpoint (cursor-paginated)."""

    def test_get_my_submissions_empty(self, client, auth_headers):
        """User with no submissions should receive cursor-paginated response with no items."""
        response = client.get("/users/me/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["has_more"] is False
        assert data["next_cursor"] is None

    def test_get_my_submissions_with_data(
        self, client, auth_headers, test_submission, test_user
    ):
        """User should see their own submissions in cursor-paginated wrapper."""
        response = client.get("/users/me/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 1
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)
        assert data["items"][0]["user_id"] == str(test_user.user_id)
        assert data["has_more"] is False

    def test_get_my_submissions_excludes_other_users(
        self, client, auth_headers, test_submission, test_user, db_session
    ):
        """User should not see submissions from other users."""
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

        response = client.get("/users/me/submissions", headers=auth_headers)
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 1
        assert data["items"][0]["user_id"] == str(test_user.user_id)
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)

    def test_get_my_submissions_no_auth(self, client):
        """Request without auth should return 401."""
        response = client.get("/users/me/submissions")
        assert response.status_code == 401

    def test_get_my_submissions_descending_order(
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

        response = client.get("/users/me/submissions", headers=auth_headers)
        assert response.status_code == 200
        items = response.json()["items"]
        timestamps = [item["uploaded_at"] for item in items]
        assert timestamps == sorted(timestamps, reverse=True)

    def test_get_my_submissions_cursor_traversal(
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

        all_ids = []
        cursor = None
        while True:
            url = "/users/me/submissions?limit=2"
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

    def test_get_my_submissions_invalid_cursor(self, client, auth_headers):
        """Invalid cursor should return 422."""
        response = client.get(
            "/users/me/submissions?cursor=not-valid", headers=auth_headers
        )
        assert response.status_code == 422

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
                image_url=f"https://example.com/img{i}.jpg",
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

    def test_get_user_detail_owned_bike_count(
        self, client, test_user, db_session
    ):
        """User should own bikes where they have the most submissions."""
        bike_a = Bike(
            bike_qr_id="BIKE-A",
            first_seen_at=datetime.now(timezone.utc),
            last_seen_at=datetime.now(timezone.utc),
        )
        bike_b = Bike(
            bike_qr_id="BIKE-B",
            first_seen_at=datetime.now(timezone.utc),
            last_seen_at=datetime.now(timezone.utc),
        )
        db_session.add_all([bike_a, bike_b])
        db_session.commit()

        other_user = User(
            username="otheruser",
            email="other@example.com",
            password_hash="fakehash",
        )
        db_session.add(other_user)
        db_session.commit()

        # test_user has 3 submissions for bike_a, other_user has 1
        for _ in range(3):
            db_session.add(FenderSubmission(
                user_id=test_user.user_id,
                bike_id=bike_a.id,
                image_url="https://example.com/a.jpg",
                captured_date=date.today(),
            ))
        db_session.add(FenderSubmission(
            user_id=other_user.user_id,
            bike_id=bike_a.id,
            image_url="https://example.com/a2.jpg",
            captured_date=date.today(),
        ))

        # other_user has 2 submissions for bike_b, test_user has 1
        for _ in range(2):
            db_session.add(FenderSubmission(
                user_id=other_user.user_id,
                bike_id=bike_b.id,
                image_url="https://example.com/b.jpg",
                captured_date=date.today(),
            ))
        db_session.add(FenderSubmission(
            user_id=test_user.user_id,
            bike_id=bike_b.id,
            image_url="https://example.com/b2.jpg",
            captured_date=date.today(),
        ))
        db_session.commit()

        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["owned_bike_count"] == 1  # only bike_a

    def test_get_user_detail_owned_bike_count_zero(self, client, test_user):
        """User with no submissions should own zero bikes."""
        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["owned_bike_count"] == 0

    def test_get_user_detail_owned_bike_count_tied(
        self, client, test_user, db_session
    ):
        """User tied for most submissions should still count as owner."""
        bike = Bike(
            bike_qr_id="BIKE-TIE",
            first_seen_at=datetime.now(timezone.utc),
            last_seen_at=datetime.now(timezone.utc),
        )
        db_session.add(bike)
        db_session.commit()

        other_user = User(
            username="tieuser",
            email="tie@example.com",
            password_hash="fakehash",
        )
        db_session.add(other_user)
        db_session.commit()

        # Both users have 2 submissions for the same bike
        for _ in range(2):
            db_session.add(FenderSubmission(
                user_id=test_user.user_id,
                bike_id=bike.id,
                image_url="https://example.com/t1.jpg",
                captured_date=date.today(),
            ))
            db_session.add(FenderSubmission(
                user_id=other_user.user_id,
                bike_id=bike.id,
                image_url="https://example.com/t2.jpg",
                captured_date=date.today(),
            ))
        db_session.commit()

        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        assert data["owned_bike_count"] == 1

    def test_get_user_detail_leaderboard_ranks(
        self, client, test_user, test_bike, db_session
    ):
        """User with recent submissions should appear ranked on leaderboards."""
        now = datetime.now(timezone.utc)
        for _ in range(3):
            db_session.add(FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url="https://example.com/lb.jpg",
                captured_date=date.today(),
                uploaded_at=now,
            ))
        db_session.commit()

        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        ranks = {r["period"]: r["rank"] for r in data["leaderboard_ranks"]}
        assert ranks["daily"] == 1
        assert ranks["weekly"] == 1
        assert ranks["monthly"] == 1

    def test_get_user_detail_leaderboard_ranks_no_submissions(
        self, client, test_user
    ):
        """User with no submissions should have null ranks."""
        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        ranks = {r["period"]: r["rank"] for r in data["leaderboard_ranks"]}
        assert ranks["daily"] is None
        assert ranks["weekly"] is None
        assert ranks["monthly"] is None

    def test_get_user_detail_leaderboard_ranks_not_first(
        self, client, test_user, test_bike, db_session
    ):
        """User with fewer submissions than another should rank lower."""
        now = datetime.now(timezone.utc)
        other_user = User(
            username="leaderuser",
            email="leader@example.com",
            password_hash="fakehash",
        )
        db_session.add(other_user)
        db_session.commit()

        # other_user has 5 submissions, test_user has 2
        for _ in range(5):
            db_session.add(FenderSubmission(
                user_id=other_user.user_id,
                bike_id=test_bike.id,
                image_url="https://example.com/o.jpg",
                captured_date=date.today(),
                uploaded_at=now,
            ))
        for _ in range(2):
            db_session.add(FenderSubmission(
                user_id=test_user.user_id,
                bike_id=test_bike.id,
                image_url="https://example.com/t.jpg",
                captured_date=date.today(),
                uploaded_at=now,
            ))
        db_session.commit()

        response = client.get(f"/users/{test_user.user_id}")
        assert response.status_code == 200
        data = response.json()
        ranks = {r["period"]: r["rank"] for r in data["leaderboard_ranks"]}
        assert ranks["daily"] == 2
        assert ranks["weekly"] == 2
        assert ranks["monthly"] == 2


class TestGetUserSubmissions:
    """Tests for GET /users/{user_id}/submissions endpoint (cursor-paginated)."""

    def test_get_user_submissions_success(
        self, client, auth_headers, test_user, test_submission
    ):
        """Should return submissions for the specified user in cursor-paginated wrapper."""
        response = client.get(
            f"/users/{test_user.user_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 1
        assert data["items"][0]["user_id"] == str(test_user.user_id)
        assert data["items"][0]["submission_id"] == str(test_submission.submission_id)
        assert data["has_more"] is False

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
            f"/users/{test_user.user_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert len(data["items"]) == 2
        for sub in data["items"]:
            assert sub["user_id"] == str(test_user.user_id)

    def test_get_user_submissions_empty(
        self, client, auth_headers, test_user
    ):
        """User with no submissions should return cursor-paginated response with no items."""
        response = client.get(
            f"/users/{test_user.user_id}/submissions",
            headers=auth_headers,
        )
        assert response.status_code == 200
        data = response.json()
        assert data["items"] == []
        assert data["has_more"] is False
        assert data["next_cursor"] is None

    def test_get_user_submissions_no_auth(self, client, test_user):
        """Request without auth should return 200 (public endpoint)."""
        response = client.get(f"/users/{test_user.user_id}/submissions")
        assert response.status_code == 200

    def test_get_user_submissions_descending_order(
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

        response = client.get(
            f"/users/{test_user.user_id}/submissions", headers=auth_headers
        )
        assert response.status_code == 200
        items = response.json()["items"]
        timestamps = [item["uploaded_at"] for item in items]
        assert timestamps == sorted(timestamps, reverse=True)

    def test_get_user_submissions_cursor_traversal(
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

        all_ids = []
        cursor = None
        while True:
            url = f"/users/{test_user.user_id}/submissions?limit=2"
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

    def test_get_user_submissions_invalid_cursor(
        self, client, auth_headers, test_user
    ):
        """Invalid cursor should return 422."""
        response = client.get(
            f"/users/{test_user.user_id}/submissions?cursor=not-valid",
            headers=auth_headers,
        )
        assert response.status_code == 422

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

from datetime import datetime, timedelta, timezone

from app.dependencies import get_password_hash
from app.models import User, FenderSubmission


class TestGetLeaderboard:
    """Tests for GET /leaderboard endpoint."""

    def test_empty_leaderboard(self, client):
        """No submissions in period returns empty entries."""
        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        data = response.json()
        assert data["period"] == "daily"
        assert data["entries"] == []

    def test_default_period_is_weekly(self, client):
        """Default period should be weekly when not specified."""
        response = client.get("/leaderboard")
        assert response.status_code == 200
        data = response.json()
        assert data["period"] == "weekly"

    def test_invalid_period(self, client):
        """Invalid period value should return 422."""
        response = client.get("/leaderboard?period=yearly")
        assert response.status_code == 422

    def test_daily_leaderboard(self, client, db_session, test_user, test_bike):
        """Daily leaderboard returns submissions from today only."""
        now = datetime.now(timezone.utc)
        # Submission today
        sub = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        )
        db_session.add(sub)
        # Submission yesterday (should not appear)
        sub_old = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img2.jpg",
            captured_date=(now - timedelta(days=1)).date(),
            uploaded_at=now - timedelta(days=1),
        )
        db_session.add(sub_old)
        db_session.commit()

        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        data = response.json()
        assert data["period"] == "daily"
        assert data["start_date"] == now.date().isoformat()
        assert data["end_date"] == now.date().isoformat()
        assert len(data["entries"]) == 1
        assert data["entries"][0]["submission_count"] == 1
        assert data["entries"][0]["username"] == "testuser"
        assert data["entries"][0]["rank"] == 1

    def test_weekly_leaderboard(self, client, db_session, test_user, test_bike):
        """Weekly leaderboard returns submissions from current Monday-Sunday."""
        now = datetime.now(timezone.utc)
        monday = now - timedelta(days=now.weekday())
        # Submission on Monday of this week
        sub = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=monday.date(),
            uploaded_at=monday.replace(hour=12, minute=0, second=0),
        )
        db_session.add(sub)
        db_session.commit()

        response = client.get("/leaderboard?period=weekly")
        assert response.status_code == 200
        data = response.json()
        assert data["period"] == "weekly"
        assert len(data["entries"]) == 1
        assert data["entries"][0]["submission_count"] == 1

    def test_monthly_leaderboard(self, client, db_session, test_user, test_bike):
        """Monthly leaderboard returns submissions from current calendar month."""
        now = datetime.now(timezone.utc)
        first_of_month = now.replace(day=1, hour=12, minute=0, second=0)
        sub = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=first_of_month.date(),
            uploaded_at=first_of_month,
        )
        db_session.add(sub)
        db_session.commit()

        response = client.get("/leaderboard?period=monthly")
        assert response.status_code == 200
        data = response.json()
        assert data["period"] == "monthly"
        assert data["start_date"] == first_of_month.date().isoformat()
        assert len(data["entries"]) == 1

    def test_multiple_users_ranked_by_count(self, client, db_session, test_bike):
        """Users are ranked by submission count descending."""
        now = datetime.now(timezone.utc)

        # Create two users
        user_alice = User(
            username="alice",
            email="alice@example.com",
            password_hash=get_password_hash("password123"),
        )
        user_bob = User(
            username="bob",
            email="bob@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add_all([user_alice, user_bob])
        db_session.commit()

        # Alice gets 3 submissions, Bob gets 1
        for i in range(3):
            db_session.add(FenderSubmission(
                user_id=user_alice.user_id,
                bike_id=test_bike.id,
                image_url=f"https://example.com/alice{i}.jpg",
                captured_date=now.date(),
                uploaded_at=now,
            ))
        db_session.add(FenderSubmission(
            user_id=user_bob.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/bob.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        ))
        db_session.commit()

        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        data = response.json()
        entries = data["entries"]
        assert len(entries) == 2
        assert entries[0]["rank"] == 1
        assert entries[0]["username"] == "alice"
        assert entries[0]["submission_count"] == 3
        assert entries[1]["rank"] == 2
        assert entries[1]["username"] == "bob"
        assert entries[1]["submission_count"] == 1

    def test_response_includes_user_id(self, client, db_session, test_user, test_bike):
        """Each entry should include the user_id."""
        now = datetime.now(timezone.utc)
        db_session.add(FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        ))
        db_session.commit()

        response = client.get("/leaderboard?period=daily")
        data = response.json()
        assert data["entries"][0]["user_id"] == str(test_user.user_id)

    def test_no_auth_required(self, client, db_session, test_user, test_bike):
        """Leaderboard endpoint should not require authentication."""
        now = datetime.now(timezone.utc)
        db_session.add(FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        ))
        db_session.commit()

        # No auth headers
        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        assert len(response.json()["entries"]) == 1

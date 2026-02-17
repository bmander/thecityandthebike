from datetime import datetime, timedelta, timezone

from app.dependencies import get_password_hash
from app.models import User, Bike, FenderSubmission, ScoringEvent
from app.services.scoring import award_submission_points


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
        """Daily leaderboard returns scores from today only."""
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
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, sub, bike)
        db_session.commit()

        # Submission yesterday (scoring events should not appear in daily)
        sub_old = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img2.jpg",
            captured_date=(now - timedelta(days=1)).date(),
            uploaded_at=now - timedelta(days=1),
        )
        db_session.add(sub_old)
        db_session.commit()
        award_submission_points(db_session, test_user.user_id, sub_old, bike)
        db_session.commit()
        # Backdate the scoring events for the old submission
        db_session.query(ScoringEvent).filter(
            ScoringEvent.submission_id == sub_old.submission_id
        ).update({"created_at": now - timedelta(days=1)})
        db_session.commit()

        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        data = response.json()
        assert data["period"] == "daily"
        assert data["start_date"] == now.date().isoformat()
        assert data["end_date"] == now.date().isoformat()
        assert len(data["entries"]) == 1
        assert data["entries"][0]["score"] == 47  # first bike ever: 25+15+5+2
        assert data["entries"][0]["username"] == "testuser"
        assert data["entries"][0]["rank"] == 1

    def test_weekly_leaderboard(self, client, db_session, test_user, test_bike):
        """Weekly leaderboard returns scores from current Monday-Sunday."""
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

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, sub, bike)
        db_session.commit()

        response = client.get("/leaderboard?period=weekly")
        assert response.status_code == 200
        data = response.json()
        assert data["period"] == "weekly"
        assert len(data["entries"]) == 1
        assert data["entries"][0]["score"] == 47

    def test_monthly_leaderboard(self, client, db_session, test_user, test_bike):
        """Monthly leaderboard returns scores from current calendar month."""
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

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, sub, bike)
        db_session.commit()

        response = client.get("/leaderboard?period=monthly")
        assert response.status_code == 200
        data = response.json()
        assert data["period"] == "monthly"
        assert data["start_date"] == first_of_month.date().isoformat()
        assert len(data["entries"]) == 1

    def test_multiple_users_ranked_by_score(self, client, db_session, test_bike):
        """Users are ranked by score descending."""
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

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()

        # Alice: first-ever bike = 47 points
        sub_alice = FenderSubmission(
            user_id=user_alice.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/alice.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        )
        db_session.add(sub_alice)
        db_session.commit()
        award_submission_points(db_session, user_alice.user_id, sub_alice, bike)
        db_session.commit()

        # Bob: same bike same day = 15 + 2 = 17 points
        sub_bob = FenderSubmission(
            user_id=user_bob.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/bob.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        )
        db_session.add(sub_bob)
        db_session.commit()
        award_submission_points(db_session, user_bob.user_id, sub_bob, bike)
        db_session.commit()

        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        data = response.json()
        entries = data["entries"]
        assert len(entries) == 2
        assert entries[0]["rank"] == 1
        assert entries[0]["username"] == "alice"
        assert entries[0]["score"] == 47
        assert entries[1]["rank"] == 2
        assert entries[1]["username"] == "bob"
        assert entries[1]["score"] == 17

    def test_response_includes_user_id(self, client, db_session, test_user, test_bike):
        """Each entry should include the user_id."""
        now = datetime.now(timezone.utc)
        sub = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        )
        db_session.add(sub)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, sub, bike)
        db_session.commit()

        response = client.get("/leaderboard?period=daily")
        data = response.json()
        assert data["entries"][0]["user_id"] == str(test_user.user_id)

    def test_no_auth_required(self, client, db_session, test_user, test_bike):
        """Leaderboard endpoint should not require authentication."""
        now = datetime.now(timezone.utc)
        sub = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        )
        db_session.add(sub)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, sub, bike)
        db_session.commit()

        # No auth headers
        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        assert len(response.json()["entries"]) == 1

    def test_boundary_late_night_vs_next_day(self, client, db_session, test_bike):
        """Scoring events at 23:59:59 should count, but 00:00:00 next day should not."""
        now = datetime.now(timezone.utc)
        today = now.date()
        tomorrow = today + timedelta(days=1)

        user = User(
            username="boundary_user",
            email="boundary@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add(user)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()

        # Submission at 23:59:59 today
        late_sub = FenderSubmission(
            user_id=user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/late.jpg",
            captured_date=today,
            uploaded_at=datetime(today.year, today.month, today.day, 23, 59, 59, tzinfo=timezone.utc),
        )
        db_session.add(late_sub)
        db_session.commit()
        award_submission_points(db_session, user.user_id, late_sub, bike)
        db_session.commit()
        # Set scoring events to 23:59:59
        db_session.query(ScoringEvent).filter(
            ScoringEvent.submission_id == late_sub.submission_id
        ).update({"created_at": datetime(today.year, today.month, today.day, 23, 59, 59, tzinfo=timezone.utc)})
        db_session.commit()

        # Submission at exactly midnight tomorrow
        midnight_sub = FenderSubmission(
            user_id=user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/midnight.jpg",
            captured_date=tomorrow,
            uploaded_at=datetime(tomorrow.year, tomorrow.month, tomorrow.day, 0, 0, 0, tzinfo=timezone.utc),
        )
        db_session.add(midnight_sub)
        db_session.commit()
        award_submission_points(db_session, user.user_id, midnight_sub, bike)
        db_session.commit()
        # Set scoring events to midnight tomorrow
        db_session.query(ScoringEvent).filter(
            ScoringEvent.submission_id == midnight_sub.submission_id
        ).update({"created_at": datetime(tomorrow.year, tomorrow.month, tomorrow.day, 0, 0, 0, tzinfo=timezone.utc)})
        db_session.commit()

        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        data = response.json()
        # Only the 23:59:59 scoring events should count for today's daily leaderboard
        assert len(data["entries"]) == 1

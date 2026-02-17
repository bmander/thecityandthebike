from datetime import date, datetime, timedelta, timezone

from app.dependencies import get_password_hash
from app.models import User, Bike, FenderSubmission, Tag, ScoringEvent
from app.services.scoring import (
    award_submission_points,
    award_tag_points,
    revoke_submission_points,
    revoke_tag_points,
)


class TestSubmissionPointAwards:
    """Tests for submission point awards."""

    def test_first_bike_ever_gets_all_bonuses(self, db_session, test_user, test_bike):
        """First-ever bike capture = 10 + 5 + 5 + 2 = 22 points."""
        submission = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=date.today(),
        )
        db_session.add(submission)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        breakdown = award_submission_points(db_session, test_user.user_id, submission, bike)
        db_session.commit()

        breakdown_dict = dict(breakdown)
        assert sum(pts for _, pts in breakdown) == 22
        assert breakdown_dict["first_bike_ever"] == 10
        assert breakdown_dict["first_bike_for_user"] == 5
        assert breakdown_dict["first_bike_today"] == 5
        assert breakdown_dict["add_image"] == 2

        events = db_session.query(ScoringEvent).filter(
            ScoringEvent.submission_id == submission.submission_id
        ).all()
        event_types = {e.event_type: e.points for e in events}
        assert event_types["first_bike_ever"] == 10
        assert event_types["first_bike_for_user"] == 5
        assert event_types["first_bike_today"] == 5
        assert event_types["add_image"] == 2

    def test_same_bike_different_day_by_same_user(self, db_session, test_user, test_bike):
        """Second submission of same bike by same user, different day = 5 + 2 = 7."""
        yesterday = date.today() - timedelta(days=1)
        sub1 = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img1.jpg",
            captured_date=yesterday,
        )
        db_session.add(sub1)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, sub1, bike)
        db_session.commit()

        sub2 = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img2.jpg",
            captured_date=date.today(),
        )
        db_session.add(sub2)
        db_session.commit()

        breakdown = award_submission_points(db_session, test_user.user_id, sub2, bike)
        db_session.commit()

        assert sum(pts for _, pts in breakdown) == 7  # first_bike_today (5) + add_image (2)

    def test_same_bike_same_day_again(self, db_session, test_user, test_bike):
        """Third submission of same bike, same day = 2 (add_image only)."""
        today = date.today()
        sub1 = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img1.jpg",
            captured_date=today,
        )
        db_session.add(sub1)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, sub1, bike)
        db_session.commit()

        sub2 = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img2.jpg",
            captured_date=today,
        )
        db_session.add(sub2)
        db_session.commit()

        breakdown = award_submission_points(db_session, test_user.user_id, sub2, bike)
        db_session.commit()

        assert sum(pts for _, pts in breakdown) == 2  # add_image only

    def test_different_user_same_bike(self, db_session, test_user, test_bike):
        """Different user capturing already-seen bike = 5 + 5 + 2 = 12 (or 5 + 2 = 7 if same day)."""
        today = date.today()
        sub1 = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img1.jpg",
            captured_date=today,
        )
        db_session.add(sub1)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, sub1, bike)
        db_session.commit()

        user2 = User(
            username="user2",
            email="user2@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add(user2)
        db_session.commit()

        sub2 = FenderSubmission(
            user_id=user2.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img2.jpg",
            captured_date=today,
        )
        db_session.add(sub2)
        db_session.commit()

        breakdown = award_submission_points(db_session, user2.user_id, sub2, bike)
        db_session.commit()

        # Not first_bike_ever, is first_bike_for_user, not first_bike_today (same day), add_image
        assert sum(pts for _, pts in breakdown) == 7  # 5 + 2


class TestTagPointAwards:
    """Tests for tag point awards."""

    def test_tag_points_decreasing(self, db_session, test_user, test_submission):
        """1st tag=2, 2nd tag=1, 3rd tag=1, 4th tag=0."""
        results = []
        for i in range(4):
            tag = Tag(
                submission_id=test_submission.submission_id,
                user_id=test_user.user_id,
                image_url=f"/uploads/tag{i}.png",
            )
            db_session.add(tag)
            db_session.commit()
            db_session.refresh(tag)

            breakdown = award_tag_points(
                db_session, test_user.user_id,
                test_submission.submission_id, tag
            )
            db_session.commit()
            total = sum(pts for _, pts in breakdown)
            results.append((tag, total, breakdown))

        assert results[0][1] == 2  # 1st tag
        assert results[1][1] == 1  # 2nd tag
        assert results[2][1] == 1  # 3rd tag
        assert results[3][1] == 0  # 4th tag
        # Verify breakdown structure
        assert results[0][2] == [("add_tag", 2)]
        assert results[3][2] == []  # No points, empty breakdown


class TestPointRevocation:
    """Tests for point revocation."""

    def test_revoke_submission_points(self, db_session, test_user, test_bike):
        """Revoking submission points sets revoked_at on all events."""
        submission = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=date.today(),
        )
        db_session.add(submission)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, submission, bike)
        db_session.commit()

        events_before = db_session.query(ScoringEvent).filter(
            ScoringEvent.submission_id == submission.submission_id,
            ScoringEvent.revoked_at.is_(None),
        ).count()
        assert events_before == 4  # first_bike_ever, first_bike_for_user, first_bike_today, add_image

        revoke_submission_points(db_session, submission.submission_id)
        db_session.commit()

        events_after = db_session.query(ScoringEvent).filter(
            ScoringEvent.submission_id == submission.submission_id,
            ScoringEvent.revoked_at.is_(None),
        ).count()
        assert events_after == 0

    def test_revoke_tag_points(self, db_session, test_user, test_submission):
        """Revoking tag points sets revoked_at on the tag's event."""
        tag = Tag(
            submission_id=test_submission.submission_id,
            user_id=test_user.user_id,
            image_url="/uploads/tag.png",
        )
        db_session.add(tag)
        db_session.commit()
        db_session.refresh(tag)

        award_tag_points(
            db_session, test_user.user_id,
            test_submission.submission_id, tag
        )
        db_session.commit()

        events_before = db_session.query(ScoringEvent).filter(
            ScoringEvent.tag_id == tag.tag_id,
            ScoringEvent.revoked_at.is_(None),
        ).count()
        assert events_before == 1

        revoke_tag_points(db_session, tag.tag_id)
        db_session.commit()

        events_after = db_session.query(ScoringEvent).filter(
            ScoringEvent.tag_id == tag.tag_id,
            ScoringEvent.revoked_at.is_(None),
        ).count()
        assert events_after == 0


class TestLeaderboardWithScoring:
    """Tests that leaderboard reflects points, not submission counts."""

    def test_leaderboard_shows_points(self, client, db_session, test_user, test_bike):
        """Leaderboard entries show score instead of submission_count."""
        now = datetime.now(timezone.utc)
        submission = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        )
        db_session.add(submission)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, submission, bike)
        db_session.commit()

        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        data = response.json()
        assert len(data["entries"]) == 1
        assert data["entries"][0]["score"] == 22
        assert "submission_count" not in data["entries"][0]

    def test_leaderboard_ranked_by_score(self, client, db_session, test_bike):
        """Users are ranked by total score descending."""
        now = datetime.now(timezone.utc)

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

        # Alice: first-ever submission for this bike = 22 points
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

        # Bob: same bike same day = 5 + 2 = 7 points (first for user + image)
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
        entries = response.json()["entries"]
        assert len(entries) == 2
        assert entries[0]["username"] == "alice"
        assert entries[0]["score"] == 22
        assert entries[0]["rank"] == 1
        assert entries[1]["username"] == "bob"
        assert entries[1]["score"] == 7
        assert entries[1]["rank"] == 2

    def test_all_time_leaderboard(self, client, db_session, test_user, test_bike):
        """All-time leaderboard period works and has no date filter."""
        # Create a submission in the past
        old_date = date.today() - timedelta(days=365)
        old_time = datetime.now(timezone.utc) - timedelta(days=365)
        submission = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=old_date,
            uploaded_at=old_time,
        )
        db_session.add(submission)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        # Manually set scoring event created_at to the past
        award_submission_points(db_session, test_user.user_id, submission, bike)
        db_session.commit()

        # Update scoring events to have old created_at
        db_session.query(ScoringEvent).filter(
            ScoringEvent.submission_id == submission.submission_id
        ).update({"created_at": old_time})
        db_session.commit()

        # Should NOT appear in daily leaderboard
        response = client.get("/leaderboard?period=daily")
        assert response.status_code == 200
        assert len(response.json()["entries"]) == 0

        # Should appear in all_time leaderboard
        response = client.get("/leaderboard?period=all_time")
        assert response.status_code == 200
        data = response.json()
        assert data["period"] == "all_time"
        assert data["start_date"] is None
        assert data["end_date"] is None
        assert len(data["entries"]) == 1
        assert data["entries"][0]["score"] == 22

    def test_revoked_points_excluded_from_leaderboard(self, client, db_session, test_user, test_bike):
        """Revoked scoring events should not count toward leaderboard."""
        now = datetime.now(timezone.utc)
        submission = FenderSubmission(
            user_id=test_user.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/img.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        )
        db_session.add(submission)
        db_session.commit()

        bike = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, test_user.user_id, submission, bike)
        db_session.commit()

        # Verify points exist
        response = client.get("/leaderboard?period=daily")
        assert len(response.json()["entries"]) == 1

        # Revoke
        revoke_submission_points(db_session, submission.submission_id)
        db_session.commit()

        response = client.get("/leaderboard?period=daily")
        assert len(response.json()["entries"]) == 0

    def test_deleting_submission_preserves_other_users_points(
        self, client, db_session, test_bike
    ):
        """Deleting one user's submission should not affect another user's points."""
        now = datetime.now(timezone.utc)

        alice = User(
            username="alice_del",
            email="alice_del@example.com",
            password_hash=get_password_hash("password123"),
        )
        bob = User(
            username="bob_del",
            email="bob_del@example.com",
            password_hash=get_password_hash("password123"),
        )
        db_session.add_all([alice, bob])
        db_session.commit()

        bike2 = Bike(
            bike_qr_id="DEL-TEST-002",
            bike_brand="Brand B",
            first_seen_at=now,
            last_seen_at=now,
        )
        db_session.add(bike2)
        db_session.commit()
        db_session.refresh(bike2)

        # Alice captures bike 1
        sub_alice = FenderSubmission(
            user_id=alice.user_id,
            bike_id=test_bike.id,
            image_url="https://example.com/alice.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        )
        db_session.add(sub_alice)
        db_session.commit()
        bike1_obj = db_session.query(Bike).filter(Bike.id == test_bike.id).first()
        award_submission_points(db_session, alice.user_id, sub_alice, bike1_obj)
        db_session.commit()

        # Bob captures a different bike
        sub_bob = FenderSubmission(
            user_id=bob.user_id,
            bike_id=bike2.id,
            image_url="https://example.com/bob.jpg",
            captured_date=now.date(),
            uploaded_at=now,
        )
        db_session.add(sub_bob)
        db_session.commit()
        bike2_obj = db_session.query(Bike).filter(Bike.id == bike2.id).first()
        award_submission_points(db_session, bob.user_id, sub_bob, bike2_obj)
        db_session.commit()

        # Both users appear on leaderboard
        response = client.get("/leaderboard?period=daily")
        entries = response.json()["entries"]
        assert len(entries) == 2

        # Delete Alice's submission via the API
        from app.dependencies import create_access_token

        alice_token = create_access_token(subject=str(alice.user_id))
        del_resp = client.delete(
            f"/submissions/{sub_alice.submission_id}",
            headers={"Authorization": f"Bearer {alice_token}"},
        )
        assert del_resp.status_code == 200

        # Bob's points should still be on the leaderboard
        response = client.get("/leaderboard?period=daily")
        entries = response.json()["entries"]
        assert len(entries) == 1
        assert entries[0]["username"] == "bob_del"
        assert entries[0]["score"] == 22

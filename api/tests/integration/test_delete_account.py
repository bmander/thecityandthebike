from datetime import date
from unittest.mock import patch

from app.dependencies import create_access_token, get_password_hash
from app.models import Bike, FenderSubmission, Tag, User
from app.models.orm import LoginAttempt, RefreshToken, ScoringEvent


def _snapshot(obj, *attrs):
    """Return a simple namespace capturing the given attributes."""
    return type("Snapshot", (), {a: getattr(obj, a) for a in attrs})()


def _make_user(db, username, email):
    user = User(
        username=username,
        email=email,
        password_hash=get_password_hash("password123"),
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    snap = _snapshot(user, "user_id", "username", "email")
    db.expunge(user)
    return snap


def _make_bike(db, qr_id):
    bike = Bike(bike_qr_id=qr_id)
    db.add(bike)
    db.commit()
    db.refresh(bike)
    snap = _snapshot(bike, "id", "bike_qr_id")
    db.expunge(bike)
    return snap


def _make_submission(db, user, bike, image_url="/uploads/images/sub.jpg"):
    sub = FenderSubmission(
        user_id=user.user_id,
        bike_id=bike.id,
        image_url=image_url,
        image_url_thumbnail=image_url.replace(".jpg", "_thumb.jpg"),
        captured_date=date.today(),
    )
    db.add(sub)
    db.commit()
    db.refresh(sub)
    snap = _snapshot(
        sub, "submission_id", "user_id", "image_url", "image_url_thumbnail"
    )
    db.expunge(sub)
    return snap


def _make_tag(db, user, submission, image_url="/uploads/images/tag.png"):
    tag = Tag(
        submission_id=submission.submission_id,
        user_id=user.user_id,
        image_url=image_url,
    )
    db.add(tag)
    db.commit()
    db.refresh(tag)
    snap = _snapshot(tag, "tag_id", "submission_id", "user_id", "image_url")
    db.expunge(tag)
    return snap


def _make_scoring_event(db, user, submission=None, tag=None):
    ev = ScoringEvent(
        user_id=user.user_id,
        event_type="add_image",
        points=2,
        submission_id=submission.submission_id if submission else None,
        tag_id=tag.tag_id if tag else None,
    )
    db.add(ev)
    db.commit()
    db.refresh(ev)
    snap = _snapshot(ev, "event_id", "user_id", "submission_id", "tag_id")
    db.expunge(ev)
    return snap


class TestDeleteAccount:
    def test_success_with_submissions_tags_and_scoring(self, client, db_session):
        """Full deletion: user with submissions, tags, and scoring events."""
        user = _make_user(db_session, "alice", "alice@example.com")
        bike = _make_bike(db_session, "BIKE-A")
        sub = _make_submission(db_session, user, bike)
        tag = _make_tag(db_session, user, sub)
        _make_scoring_event(db_session, user, submission=sub)
        _make_scoring_event(db_session, user, tag=tag)

        token = create_access_token(subject=str(user.user_id))
        headers = {"Authorization": f"Bearer {token}"}

        with patch("app.routers.users.delete_image") as mock_delete:
            resp = client.delete("/users/me", headers=headers)

        assert resp.status_code == 200
        assert resp.json()["msg"] == "Account deleted"

        # Verify all data is gone
        assert db_session.query(User).filter_by(user_id=user.user_id).first() is None
        assert (
            db_session.query(FenderSubmission)
            .filter_by(user_id=user.user_id)
            .first()
            is None
        )
        assert db_session.query(Tag).filter_by(user_id=user.user_id).first() is None
        assert (
            db_session.query(ScoringEvent).filter_by(user_id=user.user_id).first()
            is None
        )

        # delete_image called for sub image, thumb, and tag image
        deleted_urls = [call.args[0] for call in mock_delete.call_args_list]
        assert sub.image_url in deleted_urls
        assert sub.image_url_thumbnail in deleted_urls
        assert tag.image_url in deleted_urls

    def test_no_auth_returns_401(self, client):
        resp = client.delete("/users/me")
        assert resp.status_code == 401

    def test_image_files_cleaned_up(self, client, db_session):
        """Image deletion is called for every image URL."""
        user = _make_user(db_session, "bob", "bob@example.com")
        bike = _make_bike(db_session, "BIKE-B")
        _make_submission(db_session, user, bike, image_url="/uploads/images/s1.jpg")
        sub2 = _make_submission(
            db_session, user, bike, image_url="/uploads/images/s2.jpg"
        )
        _make_tag(db_session, user, sub2, image_url="/uploads/images/t1.png")

        token = create_access_token(subject=str(user.user_id))
        headers = {"Authorization": f"Bearer {token}"}

        with patch("app.routers.users.delete_image") as mock_delete:
            resp = client.delete("/users/me", headers=headers)

        assert resp.status_code == 200
        deleted_urls = {call.args[0] for call in mock_delete.call_args_list}
        assert "/uploads/images/s1.jpg" in deleted_urls
        assert "/uploads/images/s1_thumb.jpg" in deleted_urls
        assert "/uploads/images/s2.jpg" in deleted_urls
        assert "/uploads/images/s2_thumb.jpg" in deleted_urls
        assert "/uploads/images/t1.png" in deleted_urls

    def test_cross_user_tags_on_my_submissions(self, client, db_session):
        """Tags by other users on my submissions are deleted too."""
        alice = _make_user(db_session, "alice", "alice@example.com")
        bob = _make_user(db_session, "bob", "bob@example.com")
        bike = _make_bike(db_session, "BIKE-X")

        alice_sub = _make_submission(db_session, alice, bike)
        bob_tag = _make_tag(
            db_session,
            bob,
            alice_sub,
            image_url="/uploads/images/bob-tag.png",
        )
        bob_scoring = _make_scoring_event(db_session, bob, tag=bob_tag)

        token = create_access_token(subject=str(alice.user_id))
        headers = {"Authorization": f"Bearer {token}"}

        with patch("app.routers.users.delete_image") as mock_delete:
            resp = client.delete("/users/me", headers=headers)

        assert resp.status_code == 200

        # Bob's tag on alice's submission should be gone
        assert db_session.query(Tag).filter_by(tag_id=bob_tag.tag_id).first() is None

        # Bob's scoring event should be detached and revoked, not deleted
        ev = (
            db_session.query(ScoringEvent)
            .filter_by(event_id=bob_scoring.event_id)
            .first()
        )
        assert ev is not None
        assert ev.tag_id is None
        assert ev.revoked_at is not None

        # Bob still exists
        assert (
            db_session.query(User).filter_by(user_id=bob.user_id).first() is not None
        )

        # Bob's tag image was included in cleanup
        deleted_urls = {call.args[0] for call in mock_delete.call_args_list}
        assert "/uploads/images/bob-tag.png" in deleted_urls

    def test_my_tags_on_other_users_submissions(self, client, db_session):
        """Tags I created on other users' submissions are deleted."""
        alice = _make_user(db_session, "alice", "alice@example.com")
        bob = _make_user(db_session, "bob", "bob@example.com")
        bike = _make_bike(db_session, "BIKE-Y")

        bob_sub = _make_submission(db_session, bob, bike)
        alice_tag = _make_tag(
            db_session,
            alice,
            bob_sub,
            image_url="/uploads/images/alice-tag.png",
        )

        token = create_access_token(subject=str(alice.user_id))
        headers = {"Authorization": f"Bearer {token}"}

        with patch("app.routers.users.delete_image"):
            resp = client.delete("/users/me", headers=headers)

        assert resp.status_code == 200

        # Alice's tag on bob's submission should be gone
        assert (
            db_session.query(Tag).filter_by(tag_id=alice_tag.tag_id).first() is None
        )

        # Bob's submission should still exist
        assert (
            db_session.query(FenderSubmission)
            .filter_by(submission_id=bob_sub.submission_id)
            .first()
            is not None
        )

    def test_empty_user_can_delete(self, client, db_session):
        """A user with no data can still delete their account."""
        user = _make_user(db_session, "empty", "empty@example.com")
        token = create_access_token(subject=str(user.user_id))
        headers = {"Authorization": f"Bearer {token}"}

        resp = client.delete("/users/me", headers=headers)
        assert resp.status_code == 200
        assert db_session.query(User).filter_by(user_id=user.user_id).first() is None

    def test_token_invalid_after_deletion(self, client, db_session):
        """After deletion, the same token should return 401."""
        user = _make_user(db_session, "gone", "gone@example.com")
        token = create_access_token(subject=str(user.user_id))
        headers = {"Authorization": f"Bearer {token}"}

        resp = client.delete("/users/me", headers=headers)
        assert resp.status_code == 200

        resp = client.get("/users/me", headers=headers)
        assert resp.status_code == 401

    def test_refresh_tokens_deleted(self, client, db_session):
        """RefreshTokens for the user are deleted."""
        user = _make_user(db_session, "tokuser", "tok@example.com")
        rt = RefreshToken(
            token="abc123",
            user_id=user.user_id,
            expires_at=date.today(),
        )
        db_session.add(rt)
        db_session.commit()

        token = create_access_token(subject=str(user.user_id))
        headers = {"Authorization": f"Bearer {token}"}

        resp = client.delete("/users/me", headers=headers)
        assert resp.status_code == 200
        assert (
            db_session.query(RefreshToken).filter_by(user_id=user.user_id).first()
            is None
        )

    def test_login_attempts_deleted(self, client, db_session):
        """LoginAttempts for the user's username are deleted."""
        user = _make_user(db_session, "attuser", "att@example.com")
        la = LoginAttempt(username=user.username, ip_address="127.0.0.1")
        db_session.add(la)
        db_session.commit()

        token = create_access_token(subject=str(user.user_id))
        headers = {"Authorization": f"Bearer {token}"}

        resp = client.delete("/users/me", headers=headers)
        assert resp.status_code == 200
        assert (
            db_session.query(LoginAttempt)
            .filter_by(username="attuser")
            .first()
            is None
        )

    def test_other_users_scoring_on_my_submissions_detached(
        self, client, db_session
    ):
        """Other users' scoring events referencing my submissions are detached."""
        alice = _make_user(db_session, "alice", "alice@example.com")
        bob = _make_user(db_session, "bob", "bob@example.com")
        bike = _make_bike(db_session, "BIKE-Z")

        alice_sub = _make_submission(db_session, alice, bike)
        bob_scoring = _make_scoring_event(db_session, bob, submission=alice_sub)

        token = create_access_token(subject=str(alice.user_id))
        headers = {"Authorization": f"Bearer {token}"}

        with patch("app.routers.users.delete_image"):
            resp = client.delete("/users/me", headers=headers)

        assert resp.status_code == 200

        # Bob's scoring event should be detached and revoked
        ev = (
            db_session.query(ScoringEvent)
            .filter_by(event_id=bob_scoring.event_id)
            .first()
        )
        assert ev is not None
        assert ev.submission_id is None
        assert ev.revoked_at is not None
        # But bob's scoring event still exists (not deleted)
        assert ev.user_id == bob.user_id

import io
from datetime import date, datetime, timezone
from unittest.mock import patch

import pytest
from PIL import Image
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.database import Base
from app.models import User, Bike, FenderSubmission
from app.dependencies import get_password_hash


def _create_jpeg_bytes(width=200, height=150):
    """Create valid JPEG bytes for testing."""
    img = Image.new("RGB", (width, height), color="red")
    buf = io.BytesIO()
    img.save(buf, format="JPEG")
    return buf.getvalue()


@pytest.fixture
def backfill_db():
    """Create a standalone in-memory database for backfill tests."""
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(bind=engine)
    SessionLocal = sessionmaker(bind=engine)
    session = SessionLocal()

    # Seed a user and a bike
    user = User(
        username="backfilluser",
        email="backfill@example.com",
        password_hash=get_password_hash("password"),
    )
    session.add(user)
    session.commit()
    session.refresh(user)

    bike = Bike(
        bike_qr_id="BIKE-BF-001",
        bike_brand="Test",
        first_seen_at=datetime.now(timezone.utc),
        last_seen_at=datetime.now(timezone.utc),
    )
    session.add(bike)
    session.commit()
    session.refresh(bike)

    yield {
        "engine": engine,
        "SessionLocal": SessionLocal,
        "session": session,
        "user_id": user.user_id,
        "bike_id": bike.id,
    }

    session.close()


def _add_submission(session, user_id, bike_id, image_url, thumbnail_url=None):
    """Helper to insert a submission row."""
    sub = FenderSubmission(
        user_id=user_id,
        bike_id=bike_id,
        image_url=image_url,
        image_url_thumbnail=thumbnail_url,
        captured_date=date.today(),
    )
    session.add(sub)
    session.commit()
    session.refresh(sub)
    return sub.submission_id


def _get_thumbnail(SessionLocal, sid):
    """Query the thumbnail URL for a submission using a fresh session."""
    check = SessionLocal()
    try:
        sub = check.get(FenderSubmission, sid)
        return sub.image_url_thumbnail if sub else None
    finally:
        check.close()


class TestBackfill:
    """Tests for backfill_thumbnails.backfill()."""

    def test_finds_submissions_needing_backfill(self, backfill_db, monkeypatch, tmp_path):
        """Should find submissions with image_url but no thumbnail."""
        session = backfill_db["session"]
        uid = backfill_db["user_id"]
        bid = backfill_db["bike_id"]

        # One submission needs backfill
        sid = _add_submission(session, uid, bid, "/uploads/images/photo.jpg")
        # One already has a thumbnail — should be skipped
        _add_submission(session, uid, bid, "/uploads/images/other.jpg", "/uploads/images/thumb_other.jpg")

        # Set up local file
        images_dir = tmp_path / "images"
        images_dir.mkdir()
        (images_dir / "photo.jpg").write_bytes(_create_jpeg_bytes())

        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))
        monkeypatch.setattr("app.config.settings.STORAGE_BUCKET", None)

        from backfill_thumbnails import backfill
        monkeypatch.setattr(
            "backfill_thumbnails.create_engine",
            lambda *a, **kw: backfill_db["engine"],
        )

        backfill()

        thumb = _get_thumbnail(backfill_db["SessionLocal"], sid)
        assert thumb is not None
        assert "thumb_" in thumb

    def test_handles_missing_local_file(self, backfill_db, monkeypatch, tmp_path):
        """Should skip submissions whose source file is missing."""
        session = backfill_db["session"]
        uid = backfill_db["user_id"]
        bid = backfill_db["bike_id"]

        sid = _add_submission(session, uid, bid, "/uploads/images/gone.jpg")

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        # Deliberately do NOT create gone.jpg

        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))
        monkeypatch.setattr("app.config.settings.STORAGE_BUCKET", None)

        from backfill_thumbnails import backfill
        monkeypatch.setattr(
            "backfill_thumbnails.create_engine",
            lambda *a, **kw: backfill_db["engine"],
        )

        backfill()  # should not raise

        thumb = _get_thumbnail(backfill_db["SessionLocal"], sid)
        assert thumb is None  # not updated

    def test_handles_thumbnail_generation_failure(self, backfill_db, monkeypatch, tmp_path):
        """Should skip submissions when thumbnail generation returns None."""
        session = backfill_db["session"]
        uid = backfill_db["user_id"]
        bid = backfill_db["bike_id"]

        sid = _add_submission(session, uid, bid, "/uploads/images/bad.jpg")

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        # Write non-image bytes so _generate_thumbnail returns None
        (images_dir / "bad.jpg").write_bytes(b"not a real image")

        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))
        monkeypatch.setattr("app.config.settings.STORAGE_BUCKET", None)

        from backfill_thumbnails import backfill
        monkeypatch.setattr(
            "backfill_thumbnails.create_engine",
            lambda *a, **kw: backfill_db["engine"],
        )

        backfill()  # should not raise

        thumb = _get_thumbnail(backfill_db["SessionLocal"], sid)
        assert thumb is None  # not updated

    def test_rolls_back_on_error(self, backfill_db, monkeypatch, tmp_path):
        """Errors during processing should roll back that submission and continue."""
        session = backfill_db["session"]
        uid = backfill_db["user_id"]
        bid = backfill_db["bike_id"]

        sid = _add_submission(session, uid, bid, "/uploads/images/err.jpg")

        images_dir = tmp_path / "images"
        images_dir.mkdir()
        (images_dir / "err.jpg").write_bytes(_create_jpeg_bytes())

        import app.services.storage as storage_module
        monkeypatch.setattr(storage_module, "UPLOAD_DIR", str(tmp_path))
        monkeypatch.setattr("app.config.settings.STORAGE_BUCKET", None)

        from backfill_thumbnails import backfill

        monkeypatch.setattr(
            "backfill_thumbnails.create_engine",
            lambda *a, **kw: backfill_db["engine"],
        )

        # Force an error when trying to write the thumbnail
        original_open = open

        def failing_open(path, *args, **kwargs):
            if "thumb_" in str(path):
                raise IOError("disk full")
            return original_open(path, *args, **kwargs)

        with patch("builtins.open", side_effect=failing_open):
            backfill()  # should not raise

        thumb = _get_thumbnail(backfill_db["SessionLocal"], sid)
        assert thumb is None  # rolled back

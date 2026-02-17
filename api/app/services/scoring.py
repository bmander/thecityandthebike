from datetime import datetime, timezone

from sqlalchemy.orm import Session

from ..models.orm import FenderSubmission, ScoringEvent

# --- Scoring rules ---------------------------------------------------------
# Submission bonuses: awarded when no prior submission matches the condition.
# "add_image" is unconditional (always awarded).
SUBMISSION_POINTS = {
    "first_bike_ever": 10,
    "first_bike_for_user": 5,
    "first_bike_today": 5,
    "add_image": 2,
}

# Tag points by position (0-indexed). Tags beyond this list earn 0.
TAG_POINTS = [2, 1, 1]
# ---------------------------------------------------------------------------


def award_submission_points(db: Session, user_id, submission, bike) -> int:
    """Award points for a new submission. Returns total points awarded."""
    total = 0

    # Check if any prior submissions exist for this bike (first bike ever)
    prior_any = (
        db.query(FenderSubmission.submission_id)
        .filter(
            FenderSubmission.bike_id == bike.id,
            FenderSubmission.submission_id != submission.submission_id,
        )
        .first()
    )
    if prior_any is None:
        pts = SUBMISSION_POINTS["first_bike_ever"]
        db.add(ScoringEvent(
            user_id=user_id,
            event_type="first_bike_ever",
            points=pts,
            submission_id=submission.submission_id,
        ))
        total += pts

    # Check if any prior submissions by this user for this bike (first bike for user)
    prior_user = (
        db.query(FenderSubmission.submission_id)
        .filter(
            FenderSubmission.bike_id == bike.id,
            FenderSubmission.user_id == user_id,
            FenderSubmission.submission_id != submission.submission_id,
        )
        .first()
    )
    if prior_user is None:
        pts = SUBMISSION_POINTS["first_bike_for_user"]
        db.add(ScoringEvent(
            user_id=user_id,
            event_type="first_bike_for_user",
            points=pts,
            submission_id=submission.submission_id,
        ))
        total += pts

    # Check if any prior submissions for this bike with today's captured_date (first bike today)
    prior_today = (
        db.query(FenderSubmission.submission_id)
        .filter(
            FenderSubmission.bike_id == bike.id,
            FenderSubmission.captured_date == submission.captured_date,
            FenderSubmission.submission_id != submission.submission_id,
        )
        .first()
    )
    if prior_today is None:
        pts = SUBMISSION_POINTS["first_bike_today"]
        db.add(ScoringEvent(
            user_id=user_id,
            event_type="first_bike_today",
            points=pts,
            submission_id=submission.submission_id,
        ))
        total += pts

    # Always award add_image
    pts = SUBMISSION_POINTS["add_image"]
    db.add(ScoringEvent(
        user_id=user_id,
        event_type="add_image",
        points=pts,
        submission_id=submission.submission_id,
    ))
    total += pts

    return total


def award_tag_points(db: Session, user_id, submission_id, tag) -> int:
    """Award points for a new tag. Returns points awarded."""
    prior_count = (
        db.query(ScoringEvent)
        .filter(
            ScoringEvent.submission_id == submission_id,
            ScoringEvent.event_type == "add_tag",
            ScoringEvent.revoked_at.is_(None),
        )
        .count()
    )

    points = TAG_POINTS[prior_count] if prior_count < len(TAG_POINTS) else 0

    if points > 0:
        db.add(ScoringEvent(
            user_id=user_id,
            event_type="add_tag",
            points=points,
            submission_id=submission_id,
            tag_id=tag.tag_id,
        ))

    return points


def revoke_submission_points(db: Session, submission_id) -> None:
    """Revoke all scoring events for a submission and detach FK references."""
    now = datetime.now(timezone.utc)
    db.query(ScoringEvent).filter(
        ScoringEvent.submission_id == submission_id,
        ScoringEvent.revoked_at.is_(None),
    ).update({"revoked_at": now})
    # Detach FK references so the submission and its tags can be deleted
    db.query(ScoringEvent).filter(
        ScoringEvent.submission_id == submission_id,
    ).update({"submission_id": None, "tag_id": None})


def revoke_tag_points(db: Session, tag_id) -> None:
    """Revoke scoring events for a tag and detach FK reference."""
    now = datetime.now(timezone.utc)
    db.query(ScoringEvent).filter(
        ScoringEvent.tag_id == tag_id,
        ScoringEvent.revoked_at.is_(None),
    ).update({"revoked_at": now})
    # Detach FK reference so the tag can be deleted
    db.query(ScoringEvent).filter(
        ScoringEvent.tag_id == tag_id,
    ).update({"tag_id": None})

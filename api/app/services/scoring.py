from datetime import datetime, timezone

from sqlalchemy.orm import Session

from ..models.orm import FenderSubmission, ScoringEvent


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
        db.add(ScoringEvent(
            user_id=user_id,
            event_type="first_bike_ever",
            points=25,
            submission_id=submission.submission_id,
        ))
        total += 25

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
        db.add(ScoringEvent(
            user_id=user_id,
            event_type="first_bike_for_user",
            points=15,
            submission_id=submission.submission_id,
        ))
        total += 15

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
        db.add(ScoringEvent(
            user_id=user_id,
            event_type="first_bike_today",
            points=5,
            submission_id=submission.submission_id,
        ))
        total += 5

    # Always award add_image
    db.add(ScoringEvent(
        user_id=user_id,
        event_type="add_image",
        points=2,
        submission_id=submission.submission_id,
    ))
    total += 2

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

    if prior_count == 0:
        points = 2
    elif prior_count <= 2:
        points = 1
    else:
        points = 0

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
    """Revoke all scoring events for a submission."""
    now = datetime.now(timezone.utc)
    db.query(ScoringEvent).filter(
        ScoringEvent.submission_id == submission_id,
        ScoringEvent.revoked_at.is_(None),
    ).update({"revoked_at": now})


def revoke_tag_points(db: Session, tag_id) -> None:
    """Revoke scoring events for a tag."""
    now = datetime.now(timezone.utc)
    db.query(ScoringEvent).filter(
        ScoringEvent.tag_id == tag_id,
        ScoringEvent.revoked_at.is_(None),
    ).update({"revoked_at": now})

from datetime import datetime, timezone

from sqlalchemy.orm import Session

from ..models.orm import (
    FenderSubmission,
    LoginAttempt,
    RefreshToken,
    ScoringEvent,
    Tag,
    User,
)


def delete_user_account(db: Session, user: User) -> list[str]:
    """Delete a user account and all associated data.

    Returns a list of image URLs to clean up after commit.
    """
    user_id = user.user_id
    username = user.username
    now = datetime.now(timezone.utc)

    # 1. Collect image URLs for cleanup
    image_urls: list[str] = []

    # From user's submissions
    for sub in (
        db.query(FenderSubmission)
        .filter(FenderSubmission.user_id == user_id)
        .all()
    ):
        if sub.image_url:
            image_urls.append(sub.image_url)
        if sub.image_url_thumbnail:
            image_urls.append(sub.image_url_thumbnail)

    # From tags created by this user
    for tag in db.query(Tag).filter(Tag.user_id == user_id).all():
        if tag.image_url:
            image_urls.append(tag.image_url)

    # From tags by other users on this user's submissions (will cascade-delete)
    submission_ids = [
        sid
        for (sid,) in db.query(FenderSubmission.submission_id)
        .filter(FenderSubmission.user_id == user_id)
        .all()
    ]
    if submission_ids:
        for tag in (
            db.query(Tag)
            .filter(Tag.submission_id.in_(submission_ids), Tag.user_id != user_id)
            .all()
        ):
            if tag.image_url:
                image_urls.append(tag.image_url)

    # 2. Delete user's own ScoringEvents (user_id FK is NOT nullable)
    db.query(ScoringEvent).filter(ScoringEvent.user_id == user_id).delete(
        synchronize_session=False
    )

    # 3. Detach other users' ScoringEvents that reference user's submissions or tags
    if submission_ids:
        db.query(ScoringEvent).filter(
            ScoringEvent.submission_id.in_(submission_ids),
            ScoringEvent.user_id != user_id,
        ).update(
            {"submission_id": None, "tag_id": None, "revoked_at": now},
            synchronize_session=False,
        )

    # Also detach scoring events referencing ANY tags that will be deleted:
    # - tags created by user (on other users' submissions)
    # - tags on user's submissions (by any user, since those submissions are deleted)
    all_doomed_tag_ids = [
        tid
        for (tid,) in db.query(Tag.tag_id).filter(Tag.user_id == user_id).all()
    ]
    if submission_ids:
        all_doomed_tag_ids += [
            tid
            for (tid,) in db.query(Tag.tag_id)
            .filter(Tag.submission_id.in_(submission_ids))
            .all()
        ]
    if all_doomed_tag_ids:
        db.query(ScoringEvent).filter(
            ScoringEvent.tag_id.in_(all_doomed_tag_ids),
            ScoringEvent.user_id != user_id,
        ).update(
            {"tag_id": None, "revoked_at": now},
            synchronize_session=False,
        )

    # 4. Delete tags created by user on other users' submissions
    db.query(Tag).filter(
        Tag.user_id == user_id,
        ~Tag.submission_id.in_(submission_ids) if submission_ids else True,
    ).delete(synchronize_session=False)

    # 5. Delete all tags on user's submissions (bulk delete doesn't trigger ORM cascade)
    if submission_ids:
        db.query(Tag).filter(Tag.submission_id.in_(submission_ids)).delete(
            synchronize_session=False
        )

    # 6. Delete user's submissions
    db.query(FenderSubmission).filter(
        FenderSubmission.user_id == user_id
    ).delete(synchronize_session=False)

    # 7. Delete RefreshTokens
    db.query(RefreshToken).filter(RefreshToken.user_id == user_id).delete(
        synchronize_session=False
    )

    # 8. Delete LoginAttempts
    db.query(LoginAttempt).filter(LoginAttempt.username == username).delete(
        synchronize_session=False
    )

    # 9. Delete the User row
    db.delete(user)

    return image_urls

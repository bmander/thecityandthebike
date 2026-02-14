from datetime import date, datetime, timezone
from typing import Annotated, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from sqlalchemy import or_, and_, tuple_
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session, joinedload

from ..bike_url_parser import parse_bike_url
from ..cursor import decode_cursor, encode_cursor
from ..database import get_db
from ..dependencies import get_current_user, get_current_user_optional
from ..models import User, Bike, FenderSubmission
from ..schemas import CursorPaginatedResponse, SubmissionResponse
from ..schemas.auth import MessageResponse
from ..services.storage import delete_image
from .uploads import process_and_store_image

router = APIRouter(prefix="/submissions", tags=["submissions"])


@router.get("", response_model=CursorPaginatedResponse[SubmissionResponse])
def get_global_submissions(
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[Optional[User], Depends(get_current_user_optional)] = None,
    limit: int = Query(default=20, ge=1, le=100),
    cursor: Optional[str] = Query(default=None),
):
    query = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
    )
    if cursor is not None:
        try:
            c = decode_cursor(cursor)
        except ValueError:
            raise HTTPException(status_code=422, detail="Invalid cursor")
        query = query.filter(
            or_(
                FenderSubmission.uploaded_at < c.uploaded_at,
                and_(
                    FenderSubmission.uploaded_at == c.uploaded_at,
                    FenderSubmission.submission_id < c.submission_id,
                ),
            )
        )
    submissions = (
        query.order_by(FenderSubmission.uploaded_at.desc(), FenderSubmission.submission_id.desc())
        .limit(limit + 1)
        .all()
    )
    has_more = len(submissions) > limit
    items = submissions[:limit]
    next_cursor = encode_cursor(items[-1].uploaded_at, items[-1].submission_id) if has_more else None
    return CursorPaginatedResponse(items=items, next_cursor=next_cursor, has_more=has_more)


@router.get("/{submission_id}", response_model=SubmissionResponse)
def get_submission(submission_id: UUID, db: Annotated[Session, Depends(get_db)]):
    submission = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .filter(FenderSubmission.submission_id == submission_id)
        .first()
    )
    if submission is None:
        raise HTTPException(status_code=404, detail="Submission not found")
    return submission


@router.delete("/{submission_id}", response_model=MessageResponse)
def delete_submission(
    submission_id: UUID,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
):
    submission = (
        db.query(FenderSubmission)
        .filter(FenderSubmission.submission_id == submission_id)
        .first()
    )
    if submission is None:
        raise HTTPException(status_code=404, detail="Submission not found")
    if submission.user_id != current_user.user_id:
        raise HTTPException(status_code=403, detail="Not authorized to delete this submission")
    image_urls = [
        submission.image_url,
        submission.image_url_thumbnail,
    ]
    db.delete(submission)
    db.commit()
    for url in image_urls:
        delete_image(url)
    return MessageResponse(msg="Submission deleted")


@router.post("", response_model=SubmissionResponse, status_code=status.HTTP_201_CREATED)
async def create_submission(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    image: UploadFile = File(...),
    bike_qr_id: str = Form(...),
    captured_date: date = Form(...),
    user_caption: Optional[str] = Form(None),
):
    image_url, thumbnail_url = await process_and_store_image(image)

    parsed = parse_bike_url(bike_qr_id)
    bike = db.query(Bike).filter(Bike.bike_qr_id == parsed.bike_id).first()
    now = datetime.now(timezone.utc)

    if bike:
        bike.last_seen_at = now
    else:
        try:
            with db.begin_nested():
                bike = Bike(
                    bike_qr_id=parsed.bike_id,
                    provider=parsed.provider,
                    first_seen_at=now,
                    last_seen_at=now,
                )
                db.add(bike)
        except IntegrityError:
            bike = db.query(Bike).filter(Bike.bike_qr_id == parsed.bike_id).first()
            bike.last_seen_at = now

    submission = FenderSubmission(
        user_id=current_user.user_id,
        bike_id=bike.id,
        image_url=image_url,
        image_url_thumbnail=thumbnail_url,
        captured_date=captured_date,
        user_caption=user_caption,
    )
    db.add(submission)
    try:
        db.commit()
    except Exception:
        delete_image(image_url)
        delete_image(thumbnail_url)
        raise
    db.refresh(submission)
    # Force load relationships for the username and provider properties
    _ = submission.user
    _ = submission.bike
    return submission

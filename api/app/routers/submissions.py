from datetime import datetime, timezone
from typing import Annotated, Optional

from fastapi import APIRouter, Depends, Query, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload

from ..bike_url_parser import parse_bike_url
from ..database import get_db
from ..dependencies import get_current_user, get_current_user_optional
from ..models import User, Bike, FenderSubmission
from ..schemas import PaginatedResponse, SubmissionCreate, SubmissionResponse

router = APIRouter(prefix="/submissions", tags=["submissions"])


@router.get("", response_model=PaginatedResponse[SubmissionResponse])
def get_global_submissions(
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[Optional[User], Depends(get_current_user_optional)] = None,
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
):
    total = db.execute(
        select(func.count()).select_from(FenderSubmission)
    ).scalar()
    submissions = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .order_by(FenderSubmission.uploaded_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    return PaginatedResponse(items=submissions, total=total, limit=limit, offset=offset)


@router.post("", response_model=SubmissionResponse, status_code=status.HTTP_201_CREATED)
def create_submission(
    data: SubmissionCreate,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
):
    parsed = parse_bike_url(data.bike_qr_id)
    bike = db.query(Bike).filter(Bike.bike_qr_id == parsed.bike_id).first()
    now = datetime.now(timezone.utc)

    if bike:
        bike.last_seen_at = now
    else:
        bike = Bike(
            bike_qr_id=parsed.bike_id,
            provider=parsed.provider,
            first_seen_at=now,
            last_seen_at=now,
        )
        db.add(bike)
        db.flush()

    submission = FenderSubmission(
        user_id=current_user.user_id,
        bike_id=bike.id,
        image_url_original=data.image_url_original,
        image_url_processed=data.image_url_processed,
        captured_date=data.captured_date,
        user_caption=data.user_caption,
    )
    db.add(submission)
    db.commit()
    db.refresh(submission)
    # Force load relationships for the username and provider properties
    _ = submission.user
    _ = submission.bike
    return submission

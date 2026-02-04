from datetime import datetime
from typing import Annotated, List

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from ..database import get_db
from ..dependencies import get_current_user
from ..models import User, Bike, FenderSubmission
from ..schemas import SubmissionCreate, SubmissionResponse

router = APIRouter(prefix="/submissions", tags=["submissions"])


@router.get("", response_model=List[SubmissionResponse])
def get_global_submissions(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
):
    submissions = (
        db.query(FenderSubmission)
        .order_by(FenderSubmission.uploaded_at.desc())
        .all()
    )
    return submissions


@router.post("", response_model=SubmissionResponse, status_code=status.HTTP_201_CREATED)
def create_submission(
    data: SubmissionCreate,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
):
    bike = db.query(Bike).filter(Bike.bike_qr_id == data.bike_qr_id).first()
    now = datetime.utcnow()

    if bike:
        bike.last_seen_at = now
    else:
        bike = Bike(bike_qr_id=data.bike_qr_id, first_seen_at=now, last_seen_at=now)
        db.add(bike)

    submission = FenderSubmission(
        user_id=current_user.user_id,
        bike_qr_id=data.bike_qr_id,
        image_url_original=data.image_url_original,
        image_url_processed=data.image_url_processed,
        latitude=data.latitude,
        longitude=data.longitude,
        captured_at=data.captured_at,
        user_caption=data.user_caption,
    )
    db.add(submission)
    db.commit()
    db.refresh(submission)
    return submission

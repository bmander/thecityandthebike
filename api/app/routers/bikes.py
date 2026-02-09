from typing import Annotated, List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload

from ..database import get_db
from ..dependencies import get_current_user
from ..models import User, Bike, FenderSubmission
from ..schemas import SubmissionResponse

router = APIRouter(prefix="/bikes", tags=["bikes"])


@router.get("/{bike_qr_id}/submissions", response_model=List[SubmissionResponse])
def get_bike_submissions(
    bike_qr_id: str,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
):
    bike = db.query(Bike).filter(Bike.bike_qr_id == bike_qr_id).first()
    if not bike:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"msg": "Bike not found"},
        )

    submissions = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .filter(FenderSubmission.bike_qr_id == bike_qr_id)
        .order_by(FenderSubmission.uploaded_at.desc())
        .all()
    )
    return submissions

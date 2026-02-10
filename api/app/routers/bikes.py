from typing import Annotated, List

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload

from ..database import get_db
from ..dependencies import get_current_user
from ..models import User, Bike, FenderSubmission
from ..schemas import PaginatedResponse, SubmissionResponse

router = APIRouter(prefix="/bikes", tags=["bikes"])


@router.get("/{bike_qr_id}/submissions", response_model=PaginatedResponse[SubmissionResponse])
def get_bike_submissions(
    bike_qr_id: str,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
):
    bike = db.query(Bike).filter(Bike.bike_qr_id == bike_qr_id).first()
    if not bike:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"msg": "Bike not found"},
        )

    total = db.execute(
        select(func.count())
        .select_from(FenderSubmission)
        .where(FenderSubmission.bike_id == bike.id)
    ).scalar()
    submissions = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .filter(FenderSubmission.bike_id == bike.id)
        .order_by(FenderSubmission.uploaded_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    return PaginatedResponse(items=submissions, total=total, limit=limit, offset=offset)

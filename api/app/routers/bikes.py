from typing import Annotated, Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload

from ..database import get_db
from ..dependencies import get_current_user_optional
from ..models import User, Bike, FenderSubmission
from ..schemas import BikeDetailResponse, Owner, PaginatedResponse, SubmissionResponse, UserSummary

router = APIRouter(prefix="/bikes", tags=["bikes"])


@router.get("/{bike_qr_id}", response_model=BikeDetailResponse)
def get_bike_detail(
    bike_qr_id: str,
    db: Annotated[Session, Depends(get_db)],
):
    bike = db.query(Bike).filter(Bike.bike_qr_id == bike_qr_id).first()
    if not bike:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"msg": "Bike not found"},
        )

    submission_count = db.execute(
        select(func.count())
        .select_from(FenderSubmission)
        .where(FenderSubmission.bike_id == bike.id)
    ).scalar()

    first_submission = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user))
        .filter(FenderSubmission.bike_id == bike.id)
        .order_by(FenderSubmission.uploaded_at.asc())
        .first()
    )

    last_submission = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user))
        .filter(FenderSubmission.bike_id == bike.id)
        .order_by(FenderSubmission.uploaded_at.desc())
        .first()
    )

    first_captured_by = (
        UserSummary(name=first_submission.user.username, id=first_submission.user.user_id)
        if first_submission else None
    )
    last_captured_by = (
        UserSummary(name=last_submission.user.username, id=last_submission.user.user_id)
        if last_submission else None
    )

    # Compute owners: users with the most submissions for this bike
    owners = []
    if submission_count > 0:
        user_counts = (
            db.query(User, func.count(FenderSubmission.submission_id).label("cnt"))
            .join(FenderSubmission, FenderSubmission.user_id == User.user_id)
            .filter(FenderSubmission.bike_id == bike.id)
            .group_by(User.user_id)
            .all()
        )
        max_count = max(cnt for _, cnt in user_counts)
        owners = [
            Owner(
                user=UserSummary(name=user.username, id=user.user_id),
                submission_count=cnt,
            )
            for user, cnt in user_counts
            if cnt == max_count
        ]

    return BikeDetailResponse(
        bike_qr_id=bike.bike_qr_id,
        provider=bike.provider,
        bike_brand=bike.bike_brand,
        first_seen_at=bike.first_seen_at,
        last_seen_at=bike.last_seen_at,
        notes=bike.notes,
        submission_count=submission_count,
        first_captured_by=first_captured_by,
        last_captured_by=last_captured_by,
        owners=owners,
    )


@router.get("/{bike_qr_id}/submissions", response_model=PaginatedResponse[SubmissionResponse])
def get_bike_submissions(
    bike_qr_id: str,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[Optional[User], Depends(get_current_user_optional)] = None,
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

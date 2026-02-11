from typing import Annotated, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload

from ..database import get_db
from ..dependencies import get_current_user, get_current_user_optional
from ..models import User, FenderSubmission
from ..schemas import PaginatedResponse, UserDetailResponse, UserResponse, SubmissionResponse

router = APIRouter(prefix="/users", tags=["users"])


@router.get("/me", response_model=UserResponse)
def get_profile(current_user: Annotated[User, Depends(get_current_user)]):
    return current_user


@router.get("/me/submissions", response_model=PaginatedResponse[SubmissionResponse])
def get_my_submissions(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
):
    total = db.execute(
        select(func.count())
        .select_from(FenderSubmission)
        .where(FenderSubmission.user_id == current_user.user_id)
    ).scalar()
    submissions = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .filter(FenderSubmission.user_id == current_user.user_id)
        .order_by(FenderSubmission.uploaded_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    return PaginatedResponse(items=submissions, total=total, limit=limit, offset=offset)


@router.get("/{user_id}", response_model=UserDetailResponse)
def get_user_detail(
    user_id: UUID,
    db: Annotated[Session, Depends(get_db)],
):
    user = db.query(User).filter(User.user_id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"msg": "User not found"},
        )

    submission_count = db.execute(
        select(func.count())
        .select_from(FenderSubmission)
        .where(FenderSubmission.user_id == user.user_id)
    ).scalar()

    first_seen = db.execute(
        select(func.min(FenderSubmission.uploaded_at))
        .where(FenderSubmission.user_id == user.user_id)
    ).scalar()

    last_seen = db.execute(
        select(func.max(FenderSubmission.uploaded_at))
        .where(FenderSubmission.user_id == user.user_id)
    ).scalar()

    return UserDetailResponse(
        user_id=user.user_id,
        username=user.username,
        submission_count=submission_count,
        first_seen_at=first_seen,
        last_seen_at=last_seen,
    )


@router.get("/{user_id}/submissions", response_model=PaginatedResponse[SubmissionResponse])
def get_user_submissions(
    user_id: UUID,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[Optional[User], Depends(get_current_user_optional)] = None,
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
):
    user = db.query(User).filter(User.user_id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"msg": "User not found"},
        )

    total = db.execute(
        select(func.count())
        .select_from(FenderSubmission)
        .where(FenderSubmission.user_id == user.user_id)
    ).scalar()
    submissions = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .filter(FenderSubmission.user_id == user.user_id)
        .order_by(FenderSubmission.uploaded_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )
    return PaginatedResponse(items=submissions, total=total, limit=limit, offset=offset)

from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func, select
from sqlalchemy.orm import Session, joinedload

from ..database import get_db
from ..dependencies import get_current_user
from ..models import User, FenderSubmission
from ..schemas import PaginatedResponse, UserResponse, SubmissionResponse

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

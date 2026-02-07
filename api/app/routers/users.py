from typing import Annotated, List

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session, joinedload

from ..database import get_db
from ..dependencies import get_current_user
from ..models import User, FenderSubmission
from ..schemas import UserResponse, SubmissionResponse

router = APIRouter(prefix="/users", tags=["users"])


@router.get("/me", response_model=UserResponse)
def get_profile(current_user: Annotated[User, Depends(get_current_user)]):
    return current_user


@router.get("/me/submissions", response_model=List[SubmissionResponse])
def get_my_submissions(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
):
    submissions = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user))
        .filter(FenderSubmission.user_id == current_user.user_id)
        .order_by(FenderSubmission.uploaded_at.desc())
        .all()
    )
    return submissions

from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from ..database import get_db
from ..dependencies import get_current_admin_user, get_current_user
from ..models import User, FenderSubmission, Flag
from ..schemas.flag import FlagStatusResponse

router = APIRouter(tags=["flags"])


@router.post(
    "/submissions/{submission_id}/flags",
    response_model=FlagStatusResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_flag(
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

    flag = Flag(
        submission_id=submission_id,
        user_id=current_user.user_id,
    )
    db.add(flag)
    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="Already flagged",
        )

    flag_count = db.query(Flag).filter(Flag.submission_id == submission_id).count()
    return FlagStatusResponse(flagged=True, flag_count=flag_count)


@router.get(
    "/submissions/{submission_id}/flags/me",
    response_model=FlagStatusResponse,
)
def get_my_flag_status(
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

    my_flag = (
        db.query(Flag)
        .filter(Flag.submission_id == submission_id, Flag.user_id == current_user.user_id)
        .first()
    )
    flag_count = db.query(Flag).filter(Flag.submission_id == submission_id).count()
    return FlagStatusResponse(flagged=my_flag is not None, flag_count=flag_count)


@router.delete(
    "/submissions/{submission_id}/flags",
    response_model=FlagStatusResponse,
)
def clear_flags(
    submission_id: UUID,
    current_user: Annotated[User, Depends(get_current_admin_user)],
    db: Annotated[Session, Depends(get_db)],
):
    submission = (
        db.query(FenderSubmission)
        .filter(FenderSubmission.submission_id == submission_id)
        .first()
    )
    if submission is None:
        raise HTTPException(status_code=404, detail="Submission not found")

    db.query(Flag).filter(Flag.submission_id == submission_id).delete()
    db.commit()

    return FlagStatusResponse(flagged=False, flag_count=0)

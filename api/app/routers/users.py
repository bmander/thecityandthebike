from typing import Annotated, Optional
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import and_, func, or_, select
from sqlalchemy.orm import Session, joinedload

from ..cursor import decode_cursor, encode_cursor
from ..database import get_db
from ..dependencies import get_current_user
from ..models import User, Bike, FenderSubmission
from ..models.orm import ScoringEvent
from ..schemas import CursorPaginatedResponse, MessageResponse, PaginatedResponse, UserDetailResponse, UserResponse, SubmissionResponse
from ..schemas.leaderboard import LeaderboardPeriod
from ..schemas.user import LeaderboardRank
from ..services.account import delete_user_account
from ..services.storage import delete_image
from .leaderboard import _get_date_range

router = APIRouter(prefix="/users", tags=["users"])


@router.get("/me", response_model=UserResponse)
def get_profile(current_user: Annotated[User, Depends(get_current_user)]):
    return current_user


@router.delete("/me", response_model=MessageResponse)
def delete_account(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
):
    image_urls = delete_user_account(db, current_user)
    db.commit()
    for url in image_urls:
        delete_image(url)
    return MessageResponse(msg="Account deleted")


@router.get("/me/submissions", response_model=CursorPaginatedResponse[SubmissionResponse])
def get_my_submissions(
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    limit: int = Query(default=20, ge=1, le=100),
    cursor: Optional[str] = Query(default=None),
):
    query = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .filter(FenderSubmission.user_id == current_user.user_id)
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

    stats = db.execute(
        select(
            func.count(),
            func.min(FenderSubmission.uploaded_at),
            func.max(FenderSubmission.uploaded_at),
        )
        .select_from(FenderSubmission)
        .where(FenderSubmission.user_id == user.user_id)
    ).one()

    # Owned bike count: bikes where this user has the most submissions (or tied)
    user_bike_counts = (
        db.query(
            FenderSubmission.bike_id,
            FenderSubmission.user_id,
            func.count().label("cnt"),
        )
        .group_by(FenderSubmission.bike_id, FenderSubmission.user_id)
        .all()
    )
    # Group by bike_id to find max count per bike
    bike_max: dict[int, int] = {}
    for bike_id, _, cnt in user_bike_counts:
        if bike_id not in bike_max or cnt > bike_max[bike_id]:
            bike_max[bike_id] = cnt
    owned_bike_count = sum(
        1 for bike_id, uid, cnt in user_bike_counts
        if uid == user.user_id and cnt == bike_max.get(bike_id)
    )

    # Leaderboard ranks
    leaderboard_ranks = []
    for period in LeaderboardPeriod:
        start_dt, end_dt = _get_date_range(period)
        query = (
            db.query(
                ScoringEvent.user_id,
                func.sum(ScoringEvent.points).label("score"),
            )
            .filter(ScoringEvent.revoked_at.is_(None))
        )
        if start_dt is not None:
            query = query.filter(ScoringEvent.created_at >= start_dt)
        if end_dt is not None:
            query = query.filter(ScoringEvent.created_at < end_dt)
        rows = (
            query
            .group_by(ScoringEvent.user_id)
            .order_by(func.sum(ScoringEvent.points).desc())
            .all()
        )
        rank = None
        for i, row in enumerate(rows):
            if row.user_id == user.user_id:
                rank = i + 1
                break
        leaderboard_ranks.append(LeaderboardRank(period=period.value, rank=rank))

    return UserDetailResponse(
        user_id=user.user_id,
        username=user.username,
        submission_count=stats[0],
        first_seen_at=stats[1],
        last_seen_at=stats[2],
        owned_bike_count=owned_bike_count,
        leaderboard_ranks=leaderboard_ranks,
    )


@router.get("/{user_id}/submissions", response_model=CursorPaginatedResponse[SubmissionResponse])
def get_user_submissions(
    user_id: UUID,
    db: Annotated[Session, Depends(get_db)],
    limit: int = Query(default=20, ge=1, le=100),
    cursor: Optional[str] = Query(default=None),
):
    user = db.query(User).filter(User.user_id == user_id).first()
    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"msg": "User not found"},
        )

    query = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .filter(FenderSubmission.user_id == user.user_id)
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

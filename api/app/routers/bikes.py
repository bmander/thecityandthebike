from typing import Annotated, Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import and_, func, or_, select
from sqlalchemy.orm import Session, joinedload

from ..cursor import decode_cursor, encode_cursor
from ..database import get_db
from ..dependencies import get_current_user_optional
from ..models import User, Bike, FenderSubmission
from ..schemas import BikeDetailResponse, BikeListItem, CursorPaginatedResponse, Owner, PaginatedResponse, SubmissionResponse, UserSummary

router = APIRouter(prefix="/bikes", tags=["bikes"])


@router.get("", response_model=PaginatedResponse[BikeListItem])
def list_bikes(
    db: Annotated[Session, Depends(get_db)],
    limit: int = Query(default=20, ge=1, le=100),
    offset: int = Query(default=0, ge=0),
):
    total = db.execute(select(func.count()).select_from(Bike)).scalar()

    # Subquery: count submissions per bike
    sub_count = (
        select(
            FenderSubmission.bike_id,
            func.count().label("cnt"),
        )
        .group_by(FenderSubmission.bike_id)
        .subquery()
    )

    bikes = (
        db.query(Bike, sub_count.c.cnt)
        .outerjoin(sub_count, Bike.id == sub_count.c.bike_id)
        .order_by(func.coalesce(sub_count.c.cnt, 0).desc(), Bike.bike_qr_id.asc())
        .offset(offset)
        .limit(limit)
        .all()
    )

    # For each bike, find the owner (user with most submissions)
    bike_ids = [bike.id for bike, _ in bikes]
    owners_map: dict[int, UserSummary] = {}
    if bike_ids:
        # Subquery: max submission count per user per bike
        user_counts = (
            db.query(
                FenderSubmission.bike_id,
                User.user_id,
                User.username,
                func.count().label("cnt"),
            )
            .join(User, FenderSubmission.user_id == User.user_id)
            .filter(FenderSubmission.bike_id.in_(bike_ids))
            .group_by(FenderSubmission.bike_id, User.user_id, User.username)
            .all()
        )

        # Find the user with max count per bike
        bike_max: dict[int, tuple] = {}
        for bike_id, user_id, username, cnt in user_counts:
            if bike_id not in bike_max or cnt > bike_max[bike_id][2]:
                bike_max[bike_id] = (user_id, username, cnt)

        for bike_id, (user_id, username, _) in bike_max.items():
            owners_map[bike_id] = UserSummary(name=username, id=user_id)

    items = [
        BikeListItem(
            bike_qr_id=bike.bike_qr_id,
            provider=bike.provider,
            submission_count=cnt or 0,
            owner=owners_map.get(bike.id),
        )
        for bike, cnt in bikes
    ]

    return PaginatedResponse(items=items, total=total, limit=limit, offset=offset)


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


@router.get("/{bike_qr_id}/submissions", response_model=CursorPaginatedResponse[SubmissionResponse])
def get_bike_submissions(
    bike_qr_id: str,
    db: Annotated[Session, Depends(get_db)],
    current_user: Annotated[Optional[User], Depends(get_current_user_optional)] = None,
    limit: int = Query(default=20, ge=1, le=100),
    cursor: Optional[str] = Query(default=None),
):
    bike = db.query(Bike).filter(Bike.bike_qr_id == bike_qr_id).first()
    if not bike:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"msg": "Bike not found"},
        )

    query = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .filter(FenderSubmission.bike_id == bike.id)
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

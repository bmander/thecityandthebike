import calendar
from datetime import date, datetime, timedelta, timezone
from typing import Annotated

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import User, FenderSubmission
from ..schemas.leaderboard import LeaderboardEntry, LeaderboardPeriod, LeaderboardResponse

router = APIRouter(prefix="/leaderboard", tags=["leaderboard"])


def _get_date_range(period: LeaderboardPeriod) -> tuple[date, date]:
    """Return (start_date, end_date) for the given period in UTC."""
    today = datetime.now(timezone.utc).date()
    if period == LeaderboardPeriod.daily:
        return today, today
    elif period == LeaderboardPeriod.weekly:
        start = today - timedelta(days=today.weekday())
        end = start + timedelta(days=6)
        return start, end
    else:  # monthly
        last_day = calendar.monthrange(today.year, today.month)[1]
        return date(today.year, today.month, 1), date(today.year, today.month, last_day)


@router.get("", response_model=LeaderboardResponse)
def get_leaderboard(
    db: Annotated[Session, Depends(get_db)],
    period: LeaderboardPeriod = Query(default=LeaderboardPeriod.weekly),
):
    start_date, end_date = _get_date_range(period)

    rows = (
        db.query(
            FenderSubmission.user_id,
            User.username,
            func.count().label("submission_count"),
        )
        .join(User, FenderSubmission.user_id == User.user_id)
        .filter(func.date(FenderSubmission.uploaded_at) >= start_date)
        .filter(func.date(FenderSubmission.uploaded_at) <= end_date)
        .group_by(FenderSubmission.user_id, User.username)
        .order_by(func.count().desc())
        .all()
    )

    entries = [
        LeaderboardEntry(
            rank=i + 1,
            user_id=row.user_id,
            username=row.username,
            submission_count=row.submission_count,
        )
        for i, row in enumerate(rows)
    ]

    return LeaderboardResponse(
        period=period,
        start_date=start_date,
        end_date=end_date,
        entries=entries,
    )

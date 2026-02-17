import calendar
from datetime import date, datetime, timedelta, timezone
from typing import Annotated, Optional

from fastapi import APIRouter, Depends, Query
from sqlalchemy import func
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import User
from ..models.orm import ScoringEvent
from ..schemas.leaderboard import LeaderboardEntry, LeaderboardPeriod, LeaderboardResponse

router = APIRouter(prefix="/leaderboard", tags=["leaderboard"])


def _get_date_range(period: LeaderboardPeriod) -> tuple[Optional[datetime], Optional[datetime]]:
    """Return (start_dt, end_dt) as UTC datetimes using a half-open interval.

    end_dt is midnight of the day AFTER the period ends.
    Returns (None, None) for all_time.
    """
    if period == LeaderboardPeriod.all_time:
        return None, None
    today = datetime.now(timezone.utc).date()
    if period == LeaderboardPeriod.daily:
        start = today
        end_exclusive = today + timedelta(days=1)
    elif period == LeaderboardPeriod.weekly:
        start = today - timedelta(days=today.weekday())
        end_exclusive = start + timedelta(days=7)
    else:  # monthly
        last_day = calendar.monthrange(today.year, today.month)[1]
        start = date(today.year, today.month, 1)
        end_exclusive = start + timedelta(days=last_day)
    start_dt = datetime(start.year, start.month, start.day, tzinfo=timezone.utc)
    end_dt = datetime(end_exclusive.year, end_exclusive.month, end_exclusive.day, tzinfo=timezone.utc)
    return start_dt, end_dt


@router.get("", response_model=LeaderboardResponse)
def get_leaderboard(
    db: Annotated[Session, Depends(get_db)],
    period: LeaderboardPeriod = Query(default=LeaderboardPeriod.weekly),
):
    start_dt, end_dt = _get_date_range(period)

    query = (
        db.query(
            ScoringEvent.user_id,
            User.username,
            func.sum(ScoringEvent.points).label("score"),
        )
        .join(User, ScoringEvent.user_id == User.user_id)
        .filter(ScoringEvent.revoked_at.is_(None))
    )

    if start_dt is not None:
        query = query.filter(ScoringEvent.created_at >= start_dt)
    if end_dt is not None:
        query = query.filter(ScoringEvent.created_at < end_dt)

    rows = (
        query
        .group_by(ScoringEvent.user_id, User.username)
        .order_by(func.sum(ScoringEvent.points).desc())
        .all()
    )

    entries = [
        LeaderboardEntry(
            rank=i + 1,
            user_id=row.user_id,
            username=row.username,
            score=row.score,
        )
        for i, row in enumerate(rows)
    ]

    return LeaderboardResponse(
        period=period,
        start_date=start_dt.date() if start_dt else None,
        end_date=(end_dt - timedelta(days=1)).date() if end_dt else None,
        entries=entries,
    )

from datetime import date
from enum import Enum
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel


class LeaderboardPeriod(str, Enum):
    daily = "daily"
    weekly = "weekly"
    monthly = "monthly"
    all_time = "all_time"


class LeaderboardEntry(BaseModel):
    rank: int
    user_id: UUID
    username: str
    score: int


class LeaderboardResponse(BaseModel):
    period: LeaderboardPeriod
    start_date: Optional[date] = None
    end_date: Optional[date] = None
    entries: List[LeaderboardEntry]

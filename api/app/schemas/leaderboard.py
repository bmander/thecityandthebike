from datetime import date
from enum import Enum
from typing import List
from uuid import UUID

from pydantic import BaseModel


class LeaderboardPeriod(str, Enum):
    daily = "daily"
    weekly = "weekly"
    monthly = "monthly"


class LeaderboardEntry(BaseModel):
    rank: int
    user_id: UUID
    username: str
    submission_count: int


class LeaderboardResponse(BaseModel):
    period: LeaderboardPeriod
    start_date: date
    end_date: date
    entries: List[LeaderboardEntry]

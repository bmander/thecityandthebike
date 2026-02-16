from datetime import datetime
from typing import List, Optional
from uuid import UUID

from pydantic import BaseModel, ConfigDict


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    user_id: UUID
    username: str
    email: str
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


class LeaderboardRank(BaseModel):
    period: str
    rank: Optional[int] = None


class UserDetailResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    user_id: UUID
    username: str
    submission_count: int
    first_seen_at: Optional[datetime] = None
    last_seen_at: Optional[datetime] = None
    owned_bike_count: int
    leaderboard_ranks: List[LeaderboardRank]

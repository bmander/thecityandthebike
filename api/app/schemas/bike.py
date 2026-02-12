from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, ConfigDict


class UserSummary(BaseModel):
    name: str
    id: UUID


class Owner(BaseModel):
    user: UserSummary
    submission_count: int


class BikeResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    bike_qr_id: str
    provider: Optional[str] = None
    bike_brand: Optional[str] = None
    first_seen_at: Optional[datetime] = None
    last_seen_at: Optional[datetime] = None
    notes: Optional[str] = None


class BikeDetailResponse(BikeResponse):
    submission_count: int
    first_captured_by: Optional[UserSummary] = None
    last_captured_by: Optional[UserSummary] = None
    owners: list[Owner] = []


class BikeListItem(BaseModel):
    bike_qr_id: str
    provider: Optional[str] = None
    submission_count: int
    owner: Optional[UserSummary] = None

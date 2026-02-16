from datetime import datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, ConfigDict

from .bike import UserSummary


class TagResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    tag_id: UUID
    submission_id: UUID
    user_id: UUID
    image_url: str
    ring: list[list[float]] | None = None
    ring_width: int | None = None
    ring_height: int | None = None
    created_at: datetime


class TagDetailResponse(BaseModel):
    tag_id: UUID
    image_url: str
    created_at: datetime
    submission_count: int
    first_captured_at: Optional[datetime] = None
    last_captured_at: Optional[datetime] = None
    first_captured_by: Optional[UserSummary] = None
    last_captured_by: Optional[UserSummary] = None

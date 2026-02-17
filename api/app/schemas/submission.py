from datetime import date, datetime
from typing import Generic, List, Optional, TypeVar
from uuid import UUID

from pydantic import BaseModel, ConfigDict

T = TypeVar("T")


class PaginatedResponse(BaseModel, Generic[T]):
    items: List[T]
    total: int
    limit: int
    offset: int


class CursorPaginatedResponse(BaseModel, Generic[T]):
    items: List[T]
    next_cursor: Optional[str] = None
    has_more: bool


class SubmissionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    submission_id: UUID
    user_id: UUID
    bike_qr_id: str
    provider: Optional[str] = None
    image_url: Optional[str] = None
    image_url_thumbnail: Optional[str] = None
    captured_date: Optional[date] = None
    uploaded_at: Optional[datetime] = None
    user_caption: Optional[str] = None
    side: Optional[str] = None
    username: Optional[str] = None
    points_awarded: Optional[int] = None

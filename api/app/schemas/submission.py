from datetime import date, datetime
from typing import Optional
from uuid import UUID

from pydantic import BaseModel, ConfigDict


class SubmissionCreate(BaseModel):
    bike_qr_id: str
    image_url_original: str
    image_url_thumbnail: Optional[str] = None
    image_url_processed: str
    captured_date: date
    user_caption: Optional[str] = None


class SubmissionResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    submission_id: UUID
    user_id: UUID
    bike_qr_id: str
    provider: Optional[str] = None
    image_url_original: Optional[str] = None
    image_url_thumbnail: Optional[str] = None
    image_url_processed: Optional[str] = None
    captured_date: Optional[date] = None
    uploaded_at: Optional[datetime] = None
    user_caption: Optional[str] = None
    username: Optional[str] = None

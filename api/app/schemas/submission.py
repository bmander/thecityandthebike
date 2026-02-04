from datetime import datetime
from typing import Optional

from pydantic import BaseModel


class SubmissionCreate(BaseModel):
    bike_qr_id: str
    image_url_original: str
    image_url_processed: str
    captured_at: datetime
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    user_caption: Optional[str] = None


class SubmissionResponse(BaseModel):
    submission_id: str
    user_id: str
    bike_qr_id: str
    image_url_original: Optional[str] = None
    image_url_processed: Optional[str] = None
    latitude: Optional[float] = None
    longitude: Optional[float] = None
    captured_at: Optional[datetime] = None
    uploaded_at: Optional[datetime] = None
    user_caption: Optional[str] = None

    class Config:
        from_attributes = True

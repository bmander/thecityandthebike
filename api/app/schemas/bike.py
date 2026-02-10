from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict


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

from datetime import datetime
from typing import Optional

from pydantic import BaseModel


class BikeResponse(BaseModel):
    bike_qr_id: str
    bike_brand: Optional[str] = None
    first_seen_at: Optional[datetime] = None
    last_seen_at: Optional[datetime] = None
    notes: Optional[str] = None

    class Config:
        from_attributes = True

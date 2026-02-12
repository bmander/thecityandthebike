from datetime import datetime
from uuid import UUID

from pydantic import BaseModel, ConfigDict


class TagResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    tag_id: UUID
    submission_id: UUID
    user_id: UUID
    image_url: str
    created_at: datetime

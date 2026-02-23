from typing import Optional

from pydantic import BaseModel


class BanRequest(BaseModel):
    reason: Optional[str] = None


class BanResponse(BaseModel):
    msg: str
    is_banned: bool
    device_banned: bool = False

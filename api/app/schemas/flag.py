from pydantic import BaseModel


class FlagStatusResponse(BaseModel):
    flagged: bool
    flag_count: int

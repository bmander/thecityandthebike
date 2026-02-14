from pydantic import BaseModel


class ProcessedMaskResponse(BaseModel):
    ring: list[list[float]]
    width: int
    height: int

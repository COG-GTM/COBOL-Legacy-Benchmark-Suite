from datetime import datetime

from pydantic import BaseModel, ConfigDict


class ErrorDetail(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    timestamp: datetime
    program_id: str | None = None
    category: str | None = None
    error_code: str | None = None
    severity: int | None = None
    error_text: str | None = None
    error_details: str | None = None

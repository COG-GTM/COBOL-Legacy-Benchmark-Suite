from datetime import datetime

from pydantic import BaseModel, ConfigDict


class AuditEntry(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    timestamp: datetime
    user_id: str | None = None
    program_id: str | None = None
    action: str | None = None
    entity_type: str | None = None
    entity_id: str | None = None
    before_image: str | None = None
    after_image: str | None = None
    message: str | None = None

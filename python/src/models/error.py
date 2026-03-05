"""
Error log models translated from COBOL copybook ERRLOG.cpy / ERRHAND.cpy.
"""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field

from src.common.constants import ErrorCategory, ReturnCode


class ErrorLogRecord(BaseModel):
    """Translates COBOL ERR-MESSAGE from ERRHAND.cpy."""

    id: Optional[int] = Field(default=None)
    timestamp: datetime = Field(description="PIC X(26)")
    program: str = Field(max_length=8, description="PIC X(08)")
    category: ErrorCategory = Field(description="PIC X(02)")
    error_code: str = Field(max_length=4, description="PIC X(04)")
    severity: ReturnCode = Field(description="PIC S9(4) COMP")
    error_text: str = Field(max_length=80, description="PIC X(80)")
    error_details: str = Field(max_length=256, default="", description="PIC X(256)")

    model_config = {"from_attributes": True}

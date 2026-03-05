"""
Security models translated from COBOL copybooks:
- SECPARM.cpy (Security Parameters)
- USRDATA.cpy (User Data)
"""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field

from src.common.constants import SecurityRequestType


class SecurityParameters(BaseModel):
    """Translates COBOL security parameter record from SECPARM.cpy."""

    request_type: SecurityRequestType = Field(description="PIC X(01)")
    user_id: str = Field(max_length=8, description="PIC X(08)")
    resource_name: str = Field(max_length=8, default="", description="PIC X(08)")
    access_type: str = Field(max_length=8, default="", description="PIC X(08)")
    response_code: int = Field(default=0, description="PIC S9(8) COMP")
    error_info: str = Field(max_length=80, default="", description="PIC X(80)")

    model_config = {"from_attributes": True}


class UserData(BaseModel):
    """Translates COBOL user data record from USRDATA.cpy."""

    user_id: str = Field(max_length=8, description="PIC X(08)")
    user_name: str = Field(max_length=30, default="")
    role: str = Field(max_length=10, default="USER")
    department: str = Field(max_length=20, default="")
    terminal_id: str = Field(max_length=4, default="", description="PIC X(04)")
    last_login: Optional[datetime] = None
    login_count: int = 0
    failed_attempts: int = 0
    locked: bool = False
    api_key: Optional[str] = Field(default=None, max_length=64)

    model_config = {"from_attributes": True}

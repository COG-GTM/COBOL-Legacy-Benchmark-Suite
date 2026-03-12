"""
Security models translated from COBOL copybooks SECPARM.cpy and USRDATA.cpy.

These models support authentication, authorization, and user management
replacing the CICS security manager (SECMGR.cbl).
"""

from datetime import datetime

from pydantic import BaseModel, Field


class SecurityParameters(BaseModel):
    """
    Security parameters from SECPARM.cpy.

    Controls authentication and authorization for CICS transactions,
    now mapped to API security middleware.
    """

    user_id: str = Field(max_length=8, description="User identifier")
    password_hash: str = Field(default="", max_length=64, description="Hashed password")
    security_level: int = Field(default=0, ge=0, le=9, description="Security clearance level")
    access_portfolio: bool = Field(default=False, description="Can access portfolio functions")
    access_transaction: bool = Field(default=False, description="Can process transactions")
    access_inquiry: bool = Field(default=False, description="Can perform inquiries")
    access_admin: bool = Field(default=False, description="Can perform admin functions")
    access_batch: bool = Field(default=False, description="Can trigger batch jobs")
    session_token: str = Field(default="", max_length=64, description="Active session token")
    last_login: datetime | None = Field(default=None, description="Last successful login")
    failed_attempts: int = Field(default=0, ge=0, description="Consecutive failed login attempts")
    locked: bool = Field(default=False, description="Account locked flag")


class UserData(BaseModel):
    """
    User data from USRDATA.cpy.

    Stores user profile information.
    """

    user_id: str = Field(max_length=8, description="User identifier")
    user_name: str = Field(default="", max_length=30, description="User full name")
    department: str = Field(default="", max_length=20, description="Department code")
    branch_id: str = Field(default="", max_length=2, description="Branch identifier")
    role: str = Field(default="", max_length=10, description="User role")
    email: str = Field(default="", max_length=50, description="Email address")
    phone: str = Field(default="", max_length=15, description="Phone number")
    active: bool = Field(default=True, description="Account active flag")
    created_date: datetime = Field(default_factory=datetime.now, description="Account creation date")
    last_modified: datetime = Field(default_factory=datetime.now, description="Last modification date")

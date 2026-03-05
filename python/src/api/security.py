"""
Security module translated from COBOL program SECMGR.cbl.

Replaces CICS security with API key authentication.
Translates:
- P100-VALIDATE-USER (validate)
- P200-CHECK-AUTH (authorize)
- P300-LOG-ACCESS (audit)
"""

import logging
import os
from typing import Optional

from fastapi import HTTPException, Security, status
from fastapi.security import APIKeyHeader
from sqlalchemy.orm import Session

from src.common.audit import write_audit_record
from src.common.constants import AuditAction, AuditStatus, AuditType

logger = logging.getLogger(__name__)

API_KEY_HEADER = APIKeyHeader(name="X-API-Key", auto_error=False)

# Default API key for development (from SECMGR.cbl WS-USER-ID default)
_DEFAULT_API_KEY = "dev-api-key-portfolio-system"


def _get_api_key() -> str:
    """Get the configured API key, reading env var at call time."""
    return os.environ.get("API_KEY", _DEFAULT_API_KEY)


def validate_api_key(
    api_key: Optional[str] = Security(API_KEY_HEADER),
) -> str:
    """
    Validate API key. Translates SECMGR.cbl P100-VALIDATE-USER.
    """
    if api_key is None:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="API key required",
        )

    # In production, validate against database or external auth service
    # For dev, accept the configured key
    if api_key == _get_api_key():
        return "DEVUSER"

    # Check against UserAuth table
    # This would be expanded in production
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Invalid API key",
    )


def check_authorization(
    user_id: str,
    resource: str,
    access_type: str,
    session: Session,
) -> bool:
    """
    Check user authorization. Translates SECMGR.cbl P200-CHECK-AUTH.
    """
    from src.db.repository import UserAuthRepository

    auth_repo = UserAuthRepository(session)
    return auth_repo.check_access(user_id, resource, access_type)


def log_access(
    user_id: str,
    action: str,
    resource: str,
    session: Session,
    status_val: AuditStatus = AuditStatus.SUCCESS,
) -> None:
    """
    Log access for audit. Translates SECMGR.cbl P300-LOG-ACCESS.
    """
    write_audit_record(
        session=session,
        audit_type=AuditType.USER,
        action=AuditAction.INQUIRY,
        user_id=user_id,
        program="SECMGR",
        key_info=resource,
        message=f"Access: {action} on {resource}",
        status=status_val,
    )

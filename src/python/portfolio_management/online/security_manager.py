"""Security Manager for Online Programs - migrated from SECMGR.cbl.

Validates user credentials, manages authorization, implements access
control, and maintains security audit trail.
"""

import logging
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "SECMGR"


@dataclass
class SecurityAuditEntry:
    timestamp: str = ""
    user_id: str = ""
    program: str = ""
    action: str = ""
    result: str = ""
    details: str = ""


class SecurityManager:
    def __init__(self):
        self._authorized_users: set[str] = set()
        self._user_roles: dict[str, list[str]] = {}
        self._audit_trail: list[SecurityAuditEntry] = []

    def validate_user(self, user_id: str) -> int:
        if not user_id or not user_id.strip():
            logger.warning("Empty user ID provided")
            return ReturnCode.ERROR

        entry = SecurityAuditEntry(
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            user_id=user_id,
            program=PROGRAM_ID,
            action="VALIDATE",
        )

        if self._authorized_users and user_id not in self._authorized_users:
            entry.result = "DENIED"
            entry.details = "User not in authorized list"
            self._audit_trail.append(entry)
            logger.warning("User %s validation failed", user_id)
            return ReturnCode.ERROR

        entry.result = "GRANTED"
        self._audit_trail.append(entry)
        logger.debug("User %s validated", user_id)
        return ReturnCode.SUCCESS

    def check_authorization(self, user_id: str, resource: str) -> int:
        roles = self._user_roles.get(user_id, [])

        entry = SecurityAuditEntry(
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            user_id=user_id,
            program=PROGRAM_ID,
            action="AUTHORIZE",
            details=f"Resource: {resource}",
        )

        if not self._user_roles:
            entry.result = "GRANTED"
            self._audit_trail.append(entry)
            return ReturnCode.SUCCESS

        if "ADMIN" in roles or resource in roles:
            entry.result = "GRANTED"
            self._audit_trail.append(entry)
            return ReturnCode.SUCCESS

        entry.result = "DENIED"
        self._audit_trail.append(entry)
        logger.warning("Authorization denied for %s on %s", user_id, resource)
        return ReturnCode.ERROR

    def log_access(self, user_id: str, program: str, action: str) -> int:
        entry = SecurityAuditEntry(
            timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            user_id=user_id,
            program=program,
            action=action,
            result="LOGGED",
        )
        self._audit_trail.append(entry)
        return ReturnCode.SUCCESS

    def add_authorized_user(self, user_id: str, roles: Optional[list[str]] = None) -> None:
        self._authorized_users.add(user_id)
        if roles:
            self._user_roles[user_id] = roles

    def get_audit_trail(self, user_id: Optional[str] = None) -> list[SecurityAuditEntry]:
        if user_id is None:
            return list(self._audit_trail)
        return [e for e in self._audit_trail if e.user_id == user_id]

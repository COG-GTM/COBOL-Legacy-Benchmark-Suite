"""
Structured response objects for the Security Manager service.

Replaces the COBOL SECURITY-REQUEST-AREA fields:
  SEC-RESPONSE-CODE  PIC S9(8) COMP  ->  SecurityResponse.code
  SEC-ERROR-INFO     PIC X(80)       ->  SecurityResponse.error_info
"""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import IntEnum
from typing import Optional


class ResponseCode(IntEnum):
    """Maps directly to the COBOL SEC-RESPONSE-CODE values."""

    SUCCESS = 0
    DENIED = 8
    ERROR = 12


@dataclass
class SecurityResponse:
    """Structured response returned by each security operation.

    Attributes:
        code: Numeric response code matching COBOL convention (0/8/12).
        error_info: Human-readable error message (maps to SEC-ERROR-INFO).
        request_type: The operation performed ('V', 'A', or 'L').
        user_id: The user ID involved in the operation.
        resource_name: The resource being accessed (for authorization).
        access_type: The type of access requested.
    """

    code: ResponseCode = ResponseCode.SUCCESS
    error_info: Optional[str] = field(default=None)
    request_type: str = ""
    user_id: str = ""
    resource_name: str = ""
    access_type: str = ""

    @property
    def success(self) -> bool:
        return self.code == ResponseCode.SUCCESS

    def to_dict(self) -> dict[str, object]:
        return {
            "code": int(self.code),
            "success": self.success,
            "error_info": self.error_info,
            "request_type": self.request_type,
            "user_id": self.user_id,
            "resource_name": self.resource_name,
            "access_type": self.access_type,
        }

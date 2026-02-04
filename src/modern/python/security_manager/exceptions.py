"""
Custom exceptions for the Security Manager.

Maps to COBOL response codes:
- 0: Success (no exception)
- 8: Denied (ValidationException, AuthorizationException)
- 12: Error (SecurityException base)
"""

from typing import Optional


class SecurityException(Exception):
    """
    Base exception for security-related errors.
    
    Maps to COBOL SEC-RESPONSE-CODE = 12 (error condition).
    """
    
    def __init__(
        self,
        message: str,
        response_code: int = 12,
        error_info: Optional[str] = None
    ):
        self.message = message
        self.response_code = response_code
        self.error_info = error_info or message
        super().__init__(self.message)
    
    def to_dict(self) -> dict:
        """Convert exception to dictionary for API responses."""
        return {
            "success": False,
            "response_code": self.response_code,
            "error": self.message,
            "error_info": self.error_info
        }


class ValidationException(SecurityException):
    """
    Exception raised when user validation fails.
    
    Maps to COBOL P100-VALIDATE-USER:
    - SEC-RESPONSE-CODE = 8: User validation failed
    - SEC-RESPONSE-CODE = 12: Unable to obtain user credentials
    """
    
    def __init__(
        self,
        message: str = "User validation failed",
        response_code: int = 8,
        error_info: Optional[str] = None
    ):
        super().__init__(message, response_code, error_info)


class AuthorizationException(SecurityException):
    """
    Exception raised when authorization check fails.
    
    Maps to COBOL P200-CHECK-AUTH:
    - SEC-RESPONSE-CODE = 8: Access denied
    - SEC-RESPONSE-CODE = 12: Authorization check failed (DB error)
    """
    
    def __init__(
        self,
        message: str = "Access denied",
        response_code: int = 8,
        error_info: Optional[str] = None,
        user_id: Optional[str] = None,
        resource: Optional[str] = None,
        access_type: Optional[str] = None
    ):
        self.user_id = user_id
        self.resource = resource
        self.access_type = access_type
        super().__init__(message, response_code, error_info)
    
    def to_dict(self) -> dict:
        """Convert exception to dictionary with authorization details."""
        result = super().to_dict()
        if self.user_id:
            result["user_id"] = self.user_id
        if self.resource:
            result["resource"] = self.resource
        if self.access_type:
            result["access_type"] = self.access_type
        return result


class AuditException(SecurityException):
    """
    Exception raised when audit logging fails.
    
    Maps to COBOL P300-LOG-ACCESS:
    - SEC-RESPONSE-CODE = 12: Audit logging failed
    
    Note: In the original COBOL, audit failures block the transaction.
    The modern implementation can optionally use async logging to avoid blocking.
    """
    
    def __init__(
        self,
        message: str = "Audit logging failed",
        response_code: int = 12,
        error_info: Optional[str] = None
    ):
        super().__init__(message, response_code, error_info)


class ContextException(SecurityException):
    """
    Exception raised when security context is invalid or missing.
    
    Maps to COBOL EXEC CICS ASSIGN failures when obtaining
    USERID, TERMID, or TRANSID.
    """
    
    def __init__(
        self,
        message: str = "Unable to obtain security context",
        response_code: int = 12,
        error_info: Optional[str] = None
    ):
        super().__init__(message, response_code, error_info)

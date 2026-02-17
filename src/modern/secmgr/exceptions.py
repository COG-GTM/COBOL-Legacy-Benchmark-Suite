"""
Custom exceptions for the Security Manager service.

Replaces the COBOL numeric response codes:
  0  = Success (no exception)
  8  = Denied  (ValidationException, AuthorizationException)
  12 = Error   (SecurityError, AuditException)
"""


class SecurityError(Exception):
    """Base exception for all security-related errors.

    Corresponds to COBOL SEC-RESPONSE-CODE = 12 (system error).
    """

    def __init__(self, message: str, code: int = 12) -> None:
        self.message = message
        self.code = code
        super().__init__(self.message)


class ValidationException(SecurityError):
    """Raised when user credential validation fails.

    Corresponds to COBOL P100-VALIDATE-USER setting
    SEC-RESPONSE-CODE = 8 with SEC-ERROR-INFO = 'User validation failed'.
    """

    def __init__(self, message: str = "User validation failed") -> None:
        super().__init__(message, code=8)


class AuthorizationException(SecurityError):
    """Raised when a user lacks permission for the requested resource.

    Corresponds to COBOL P200-CHECK-AUTH setting
    SEC-RESPONSE-CODE = 8 with SEC-ERROR-INFO = 'Access denied'.
    """

    def __init__(self, message: str = "Access denied") -> None:
        super().__init__(message, code=8)


class AuditException(SecurityError):
    """Raised when audit logging fails.

    Corresponds to COBOL P300-LOG-ACCESS setting
    SEC-RESPONSE-CODE = 12 with SEC-ERROR-INFO = 'Audit logging failed'.
    """

    def __init__(self, message: str = "Audit logging failed") -> None:
        super().__init__(message, code=12)

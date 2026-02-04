"""
REST API interface for the Security Manager.

Provides HTTP endpoints for security operations, replacing the CICS LINK
commands used in the original COBOL implementation.

Endpoints:
- POST /security/validate: Validate user credentials
- POST /security/authorize: Check resource authorization
- POST /security/audit: Log access attempt
- POST /security/check: Full security check (validate -> authorize -> audit)
- POST /admin/authorization: Add authorization rule
- DELETE /admin/authorization: Remove authorization rule
- GET /admin/audit-log: Retrieve audit log entries

HTTP Status Code Mapping:
- 200 OK: Success (COBOL response code 0)
- 401 Unauthorized: Validation failed (COBOL response code 8 for validation)
- 403 Forbidden: Authorization denied (COBOL response code 8 for authorization)
- 500 Internal Server Error: System error (COBOL response code 12)
"""

from datetime import datetime
from typing import Optional
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException, Header, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

from .models import get_engine, get_session_factory, init_db
from .context import SecurityContext, SecurityContextManager
from .security_manager import SecurityManager, ResponseCode


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initialize database on startup."""
    engine = get_engine()
    init_db(engine)
    app.state.session_factory = get_session_factory(engine)
    app.state.security_manager = SecurityManager(app.state.session_factory)
    yield


app = FastAPI(
    title="Security Manager API",
    description="Modern Python implementation of COBOL SECMGR security manager",
    version="1.0.0",
    lifespan=lifespan
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


class ValidationRequest(BaseModel):
    """Request model for user validation."""
    user_id: str = Field(..., max_length=8, description="User identifier")
    expected_user_id: Optional[str] = Field(None, max_length=8, description="Expected user ID for comparison")


class AuthorizationRequest(BaseModel):
    """Request model for authorization check."""
    user_id: str = Field(..., max_length=8, description="User identifier")
    resource: str = Field(..., max_length=8, description="Resource name (e.g., 'INQONLN')")
    access_type: str = Field(..., max_length=8, description="Access type (e.g., 'READ', 'WRITE')")


class AuditRequest(BaseModel):
    """Request model for audit logging."""
    user_id: str = Field(..., max_length=8, description="User identifier")
    program: str = Field(..., max_length=8, description="Program name")
    access_type: str = Field(..., max_length=8, description="Access type")
    terminal_id: Optional[str] = Field("WEB", max_length=4, description="Terminal identifier")
    transaction_id: Optional[str] = Field(None, max_length=4, description="Transaction identifier")


class SecurityCheckRequest(BaseModel):
    """Request model for full security check."""
    user_id: str = Field(..., max_length=8, description="User identifier")
    resource: str = Field(..., max_length=8, description="Resource name")
    access_type: str = Field(..., max_length=8, description="Access type")
    expected_user_id: Optional[str] = Field(None, max_length=8, description="Expected user ID for validation")


class AuthorizationRuleRequest(BaseModel):
    """Request model for authorization rule management."""
    user_id: str = Field(..., max_length=8, description="User identifier")
    resource: str = Field(..., max_length=8, description="Resource name")
    access_type: str = Field(..., max_length=8, description="Access type")


class SecurityResponse(BaseModel):
    """Response model for security operations."""
    success: bool
    response_code: int = Field(..., description="0=success, 8=denied, 12=error")
    error_info: Optional[str] = None


def get_security_manager(request: Request) -> SecurityManager:
    """Dependency to get the security manager instance."""
    return request.app.state.security_manager


def get_context_from_headers(
    x_user_id: Optional[str] = Header(None),
    x_terminal_id: Optional[str] = Header("WEB"),
    x_transaction_id: Optional[str] = Header(None),
    x_request_id: Optional[str] = Header(None),
    x_correlation_id: Optional[str] = Header(None),
    x_forwarded_for: Optional[str] = Header(None),
    user_agent: Optional[str] = Header(None)
) -> dict:
    """Extract context information from HTTP headers."""
    return {
        "user_id": x_user_id,
        "terminal_id": x_terminal_id or "WEB",
        "transaction_id": x_transaction_id,
        "request_id": x_request_id,
        "correlation_id": x_correlation_id,
        "source_ip": x_forwarded_for,
        "user_agent": user_agent
    }


def response_to_http_status(response_code: int, is_validation: bool = False) -> int:
    """
    Map COBOL response codes to HTTP status codes.
    
    - 0 (SUCCESS) -> 200 OK
    - 8 (DENIED) -> 401 Unauthorized (validation) or 403 Forbidden (authorization)
    - 12 (ERROR) -> 500 Internal Server Error
    """
    if response_code == ResponseCode.SUCCESS.value:
        return 200
    elif response_code == ResponseCode.DENIED.value:
        return 401 if is_validation else 403
    else:
        return 500


@app.post("/security/validate", response_model=SecurityResponse)
async def validate_user(
    request: ValidationRequest,
    manager: SecurityManager = Depends(get_security_manager),
    headers: dict = Depends(get_context_from_headers)
):
    """
    Validate user credentials.
    
    Maps to COBOL SEC-REQUEST-TYPE = 'V' (P100-VALIDATE-USER).
    
    Returns:
    - 200: Validation successful
    - 401: User validation failed
    - 500: Unable to obtain user credentials
    """
    context = SecurityContext(
        user_id=request.user_id,
        terminal_id=headers.get("terminal_id", "WEB"),
        transaction_id=headers.get("transaction_id"),
        request_id=headers.get("request_id"),
        correlation_id=headers.get("correlation_id"),
        source_ip=headers.get("source_ip"),
        user_agent=headers.get("user_agent")
    )
    
    response = manager.validate_user(context, request.expected_user_id)
    
    if not response.success:
        raise HTTPException(
            status_code=response_to_http_status(response.response_code, is_validation=True),
            detail=response.to_dict()
        )
    
    return response.to_dict()


@app.post("/security/authorize", response_model=SecurityResponse)
async def authorize_access(
    request: AuthorizationRequest,
    manager: SecurityManager = Depends(get_security_manager),
    headers: dict = Depends(get_context_from_headers)
):
    """
    Check user authorization for a resource.
    
    Maps to COBOL SEC-REQUEST-TYPE = 'A' (P200-CHECK-AUTH).
    
    Returns:
    - 200: Authorization granted
    - 403: Access denied
    - 500: Authorization check failed
    """
    context = SecurityContext(
        user_id=request.user_id,
        terminal_id=headers.get("terminal_id", "WEB"),
        transaction_id=headers.get("transaction_id"),
        request_id=headers.get("request_id"),
        correlation_id=headers.get("correlation_id"),
        source_ip=headers.get("source_ip"),
        user_agent=headers.get("user_agent")
    )
    
    response = manager.authorize_access(context, request.resource, request.access_type)
    
    if not response.success:
        raise HTTPException(
            status_code=response_to_http_status(response.response_code),
            detail=response.to_dict()
        )
    
    return response.to_dict()


@app.post("/security/audit", response_model=SecurityResponse)
async def audit_access(
    request: AuditRequest,
    manager: SecurityManager = Depends(get_security_manager),
    headers: dict = Depends(get_context_from_headers)
):
    """
    Log access attempt to audit trail.
    
    Maps to COBOL SEC-REQUEST-TYPE = 'L' (P300-LOG-ACCESS).
    
    Returns:
    - 200: Audit logging successful
    - 500: Audit logging failed
    """
    context = SecurityContext(
        user_id=request.user_id,
        terminal_id=request.terminal_id or headers.get("terminal_id", "WEB"),
        transaction_id=request.transaction_id or headers.get("transaction_id"),
        program_name=request.program,
        access_type=request.access_type,
        request_id=headers.get("request_id"),
        correlation_id=headers.get("correlation_id"),
        source_ip=headers.get("source_ip"),
        user_agent=headers.get("user_agent")
    )
    
    response = manager.audit_access(context)
    
    if not response.success:
        raise HTTPException(
            status_code=response_to_http_status(response.response_code),
            detail=response.to_dict()
        )
    
    return response.to_dict()


@app.post("/security/check", response_model=SecurityResponse)
async def check_security(
    request: SecurityCheckRequest,
    manager: SecurityManager = Depends(get_security_manager),
    headers: dict = Depends(get_context_from_headers)
):
    """
    Perform full security check: validation -> authorization -> audit.
    
    Maps to COBOL P050-SECURITY-CHECK in INQONLN.cbl which calls SECMGR
    three times with SEC-REQUEST-TYPE values 'V', 'A', and 'L'.
    
    Returns:
    - 200: All phases successful
    - 401: Validation failed
    - 403: Authorization denied
    - 500: System error
    """
    context = SecurityContext(
        user_id=request.user_id,
        terminal_id=headers.get("terminal_id", "WEB"),
        transaction_id=headers.get("transaction_id"),
        request_id=headers.get("request_id"),
        correlation_id=headers.get("correlation_id"),
        source_ip=headers.get("source_ip"),
        user_agent=headers.get("user_agent")
    )
    
    response = manager.check_security(
        context,
        request.resource,
        request.access_type,
        request.expected_user_id
    )
    
    if not response.success:
        is_validation = response.error_info and "validation" in response.error_info.lower()
        raise HTTPException(
            status_code=response_to_http_status(response.response_code, is_validation=is_validation),
            detail=response.to_dict()
        )
    
    return response.to_dict()


@app.post("/admin/authorization", response_model=dict)
async def add_authorization(
    request: AuthorizationRuleRequest,
    manager: SecurityManager = Depends(get_security_manager)
):
    """
    Add an authorization rule to the AUTHFILE table.
    
    Administrative endpoint for managing authorization rules.
    """
    success = manager.add_authorization(
        request.user_id,
        request.resource,
        request.access_type
    )
    
    if not success:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "error": "Failed to add authorization rule"}
        )
    
    return {
        "success": True,
        "message": f"Authorization added: {request.user_id}/{request.resource}/{request.access_type}"
    }


@app.delete("/admin/authorization", response_model=dict)
async def remove_authorization(
    request: AuthorizationRuleRequest,
    manager: SecurityManager = Depends(get_security_manager)
):
    """
    Remove an authorization rule from the AUTHFILE table.
    
    Administrative endpoint for managing authorization rules.
    """
    success = manager.remove_authorization(
        request.user_id,
        request.resource,
        request.access_type
    )
    
    if not success:
        raise HTTPException(
            status_code=500,
            detail={"success": False, "error": "Failed to remove authorization rule"}
        )
    
    return {
        "success": True,
        "message": f"Authorization removed: {request.user_id}/{request.resource}/{request.access_type}"
    }


@app.get("/admin/audit-log", response_model=list)
async def get_audit_log(
    user_id: Optional[str] = None,
    start_time: Optional[datetime] = None,
    end_time: Optional[datetime] = None,
    limit: int = 100,
    manager: SecurityManager = Depends(get_security_manager)
):
    """
    Retrieve audit log entries.
    
    Query parameters:
    - user_id: Filter by user ID
    - start_time: Filter by start time (ISO format)
    - end_time: Filter by end time (ISO format)
    - limit: Maximum number of entries (default 100)
    """
    return manager.get_audit_log(
        user_id=user_id,
        start_time=start_time,
        end_time=end_time,
        limit=limit
    )


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "service": "security-manager"}

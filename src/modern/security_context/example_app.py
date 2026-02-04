"""
Example FastAPI Application demonstrating modern security context propagation.

This example shows how to modernize the COBOL SECMGR/INQONLN pattern using:
- JWT-based authentication (replacing CICS ASSIGN USERID)
- Request correlation IDs (replacing CICS TRANSID)
- Client identification from HTTP headers (replacing CICS TERMID)
- Dependency injection for security context
- Three-phase security model (Validate -> Authorize -> Audit)

To run this example:
    pip install fastapi uvicorn pyjwt
    uvicorn example_app:app --reload

Example requests:
    # Get a test JWT token
    curl http://localhost:8000/auth/token?user_id=user123

    # Access portfolio (requires authentication)
    curl -H "Authorization: Bearer <token>" http://localhost:8000/portfolio/P001
"""

from datetime import datetime, timezone, timedelta
from typing import Optional
import json
import base64
import hashlib
import hmac

from fastapi import FastAPI, Depends, HTTPException, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from .context import SecurityContext, SecurityContextBuilder
from .middleware import SecurityContextMiddleware, get_security_context
from .models import AccessType, AuditStatus, AuthorizationRecord, UserIdentity
from .security_manager import (
    SecurityManager,
    DefaultTokenValidator,
    InMemoryAuthorizationStore,
    InMemoryAuditLogger,
)
from .extractors import JWTExtractor, HeaderExtractor

SECRET_KEY = "your-secret-key-for-demo-only"

app = FastAPI(
    title="Portfolio Inquiry System",
    description="Modern implementation of COBOL INQONLN/SECMGR pattern",
    version="1.0.0",
)

auth_store = InMemoryAuthorizationStore()
auth_store.add_record(
    AuthorizationRecord(
        user_id="user123",
        resource_pattern="portfolio/*",
        access_types=[AccessType.READ],
    )
)
auth_store.add_record(
    AuthorizationRecord(
        user_id="admin",
        resource_pattern="*",
        access_types=[AccessType.READ, AccessType.WRITE, AccessType.DELETE, AccessType.ADMIN],
    )
)
auth_store.add_record(
    AuthorizationRecord(
        role="analyst",
        resource_pattern="portfolio/*",
        access_types=[AccessType.READ],
    )
)
auth_store.add_record(
    AuthorizationRecord(
        role="analyst",
        resource_pattern="history/*",
        access_types=[AccessType.READ],
    )
)

audit_logger = InMemoryAuditLogger()

security_manager = SecurityManager(
    token_validator=DefaultTokenValidator(verify_expiration=True),
    authorization_store=auth_store,
    audit_logger=audit_logger,
)

app.add_middleware(
    SecurityContextMiddleware,
    extractors=[
        JWTExtractor(secret_key=SECRET_KEY, verify_signature=False),
        HeaderExtractor(),
    ],
    require_auth=False,
    anonymous_user_id="anonymous",
)


class TokenResponse(BaseModel):
    """Response model for token endpoint."""
    access_token: str
    token_type: str = "bearer"
    expires_in: int


class PortfolioResponse(BaseModel):
    """Response model for portfolio inquiry."""
    portfolio_id: str
    account_name: str
    balance: float
    currency: str
    last_updated: str


class HistoryResponse(BaseModel):
    """Response model for transaction history."""
    portfolio_id: str
    transactions: list


class AuditResponse(BaseModel):
    """Response model for audit log query."""
    entries: list
    total: int


def create_jwt_token(user_id: str, roles: list = None, expires_delta: timedelta = None) -> str:
    """
    Create a simple JWT token for demonstration.
    
    In production, use a proper JWT library like PyJWT.
    """
    if expires_delta is None:
        expires_delta = timedelta(hours=1)
    
    now = datetime.now(timezone.utc)
    exp = now + expires_delta
    
    header = {"alg": "HS256", "typ": "JWT"}
    payload = {
        "sub": user_id,
        "iat": int(now.timestamp()),
        "exp": int(exp.timestamp()),
        "roles": roles or [],
    }
    
    def b64_encode(data: dict) -> str:
        json_bytes = json.dumps(data).encode()
        return base64.urlsafe_b64encode(json_bytes).rstrip(b"=").decode()
    
    header_b64 = b64_encode(header)
    payload_b64 = b64_encode(payload)
    
    signature_input = f"{header_b64}.{payload_b64}".encode()
    signature = hmac.new(SECRET_KEY.encode(), signature_input, hashlib.sha256).digest()
    signature_b64 = base64.urlsafe_b64encode(signature).rstrip(b"=").decode()
    
    return f"{header_b64}.{payload_b64}.{signature_b64}"


MOCK_PORTFOLIOS = {
    "P001": {
        "portfolio_id": "P001",
        "account_name": "Growth Fund",
        "balance": 125000.50,
        "currency": "USD",
        "last_updated": "2024-01-15T10:30:00Z",
    },
    "P002": {
        "portfolio_id": "P002",
        "account_name": "Income Fund",
        "balance": 75000.25,
        "currency": "USD",
        "last_updated": "2024-01-15T09:45:00Z",
    },
}

MOCK_HISTORY = {
    "P001": [
        {"date": "2024-01-15", "type": "BUY", "symbol": "AAPL", "quantity": 100, "price": 185.50},
        {"date": "2024-01-14", "type": "SELL", "symbol": "GOOGL", "quantity": 50, "price": 142.30},
    ],
    "P002": [
        {"date": "2024-01-15", "type": "DIVIDEND", "symbol": "VTI", "amount": 125.00},
    ],
}


@app.get("/auth/token", response_model=TokenResponse)
async def get_token(user_id: str, roles: str = ""):
    """
    Get a test JWT token.
    
    This endpoint is for demonstration only. In production, use proper
    authentication (OAuth2, OIDC, etc.).
    
    Query parameters:
        user_id: The user ID to include in the token
        roles: Comma-separated list of roles (e.g., "analyst,viewer")
    """
    role_list = [r.strip() for r in roles.split(",") if r.strip()]
    token = create_jwt_token(user_id, role_list)
    
    return TokenResponse(
        access_token=token,
        token_type="bearer",
        expires_in=3600,
    )


@app.get("/portfolio/{portfolio_id}", response_model=PortfolioResponse)
async def get_portfolio(
    portfolio_id: str,
    context: SecurityContext = Depends(get_security_context),
):
    """
    Get portfolio information.
    
    This endpoint demonstrates the modern equivalent of INQONLN calling INQPORT.
    
    The three-phase security check is performed:
    1. Validate user (JWT signature and claims)
    2. Authorize access (check permission for portfolio/*)
    3. Audit the access attempt
    
    Original COBOL flow (INQONLN.cbl):
        MOVE 'INQP' TO WS-COMMAREA-FUNCTION
        PERFORM P300-PORTFOLIO-INQUIRY
           THRU P300-EXIT
    """
    resource = f"portfolio/{portfolio_id}"
    
    security_result = await security_manager.check_security(
        context=context,
        resource=resource,
        access_type=AccessType.READ,
    )
    
    if not security_result.success:
        raise HTTPException(
            status_code=403 if security_result.response_code == 8 else 401,
            detail=security_result.error_info,
        )
    
    portfolio = MOCK_PORTFOLIOS.get(portfolio_id)
    if not portfolio:
        await security_manager.log_access(
            context=context,
            resource=resource,
            access_type=AccessType.READ,
            status=AuditStatus.FAILURE,
            response_code=404,
            error_message="Portfolio not found",
        )
        raise HTTPException(status_code=404, detail="Portfolio not found")
    
    return PortfolioResponse(**portfolio)


@app.get("/history/{portfolio_id}", response_model=HistoryResponse)
async def get_history(
    portfolio_id: str,
    context: SecurityContext = Depends(get_security_context),
):
    """
    Get transaction history for a portfolio.
    
    This endpoint demonstrates the modern equivalent of INQONLN calling INQHIST.
    
    Original COBOL flow (INQONLN.cbl):
        MOVE 'INQH' TO WS-COMMAREA-FUNCTION
        PERFORM P400-HISTORY-INQUIRY
           THRU P400-EXIT
    """
    resource = f"history/{portfolio_id}"
    
    security_result = await security_manager.check_security(
        context=context,
        resource=resource,
        access_type=AccessType.READ,
    )
    
    if not security_result.success:
        raise HTTPException(
            status_code=403 if security_result.response_code == 8 else 401,
            detail=security_result.error_info,
        )
    
    history = MOCK_HISTORY.get(portfolio_id, [])
    
    return HistoryResponse(
        portfolio_id=portfolio_id,
        transactions=history,
    )


@app.get("/audit", response_model=AuditResponse)
async def get_audit_log(
    user_id: Optional[str] = None,
    resource: Optional[str] = None,
    context: SecurityContext = Depends(get_security_context),
):
    """
    Query the audit log.
    
    This endpoint demonstrates querying the modern AUDITLOG table.
    Only admin users can access this endpoint.
    """
    security_result = await security_manager.check_security(
        context=context,
        resource="audit",
        access_type=AccessType.ADMIN,
    )
    
    if not security_result.success:
        raise HTTPException(
            status_code=403,
            detail="Admin access required",
        )
    
    entries = audit_logger.get_entries(user_id=user_id, resource=resource)
    
    return AuditResponse(
        entries=[e.to_dict() for e in entries],
        total=len(entries),
    )


@app.get("/context")
async def get_current_context(
    context: SecurityContext = Depends(get_security_context),
):
    """
    Debug endpoint to view the current security context.
    
    This shows what information is extracted from the request,
    equivalent to what CICS ASSIGN would have captured.
    """
    return {
        "user_id": context.user_id,
        "client_id": context.client_id,
        "request_id": context.request_id,
        "trace_id": context.trace_id,
        "session_id": context.session_id,
        "user_agent": context.user_agent,
        "timestamp": context.timestamp.isoformat(),
        "claims": context.claims,
        "comparison_to_cobol": {
            "USERID": f"{context.user_id} (was 8 chars, now up to 255)",
            "TERMID": f"{context.client_id} (was 4 chars, now IP/device ID)",
            "TRANSID": f"{context.request_id} (was 4 chars, now UUID)",
        },
    }


@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "timestamp": datetime.now(timezone.utc).isoformat()}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

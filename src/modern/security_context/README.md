# Modern Security Context Propagation

This module provides a modernized implementation of the SECMGR security manager from the COBOL Legacy Benchmark Suite. It replaces CICS-based context capture with a modern approach using JWT tokens, request correlation IDs, and dependency injection.

## Overview

The original COBOL `SECMGR` program (in `src/programs/online/SECMGR.cbl`) used CICS commands to capture execution context:

```cobol
EXEC CICS ASSIGN
          USERID(WS-USER-ID)
          TERMID(WS-TERMINAL-ID)
          TRANSID(WS-TRANSACTION-ID)
END-EXEC.
```

This modern implementation replaces these implicit context captures with explicit, typed context objects that are passed via dependency injection.

## Key Components

### SecurityContext

The `SecurityContext` class holds all authentication and tracing information:

| Field | Replaces | Description |
|-------|----------|-------------|
| `user_id` | CICS USERID (8 chars) | User identifier - email, username, etc. (up to 255 chars) |
| `client_id` | CICS TERMID (4 chars) | Client identifier - IP address, device ID (up to 255 chars) |
| `request_id` | CICS TRANSID (4 chars) | Request correlation ID - UUID format (36 chars) |
| `trace_id` | N/A | OpenTelemetry trace ID for distributed tracing |
| `session_id` | N/A | Session identifier for stateful interactions |
| `user_agent` | N/A | HTTP User-Agent for audit purposes |

### Context Extractors

Multiple extractors can pull identity and context from HTTP requests:

- **JWTExtractor**: Extracts user identity from JWT tokens (Authorization: Bearer header)
- **OAuth2Extractor**: Supports OAuth2 token introspection and userinfo endpoints
- **SessionExtractor**: Extracts identity from session cookies
- **HeaderExtractor**: Captures request IDs, client IPs, trace IDs from HTTP headers

### Three-Phase Security Model

The original COBOL implemented a three-phase security check (see `INQONLN.cbl` lines 139-169):

1. **Validation** (SEC-REQUEST-TYPE = 'V'): Verify user identity
2. **Authorization** (SEC-REQUEST-TYPE = 'A'): Check user permissions
3. **Audit** (SEC-REQUEST-TYPE = 'L'): Log the access attempt

This is preserved in the modern `SecurityManager`:

```python
result = await security_manager.check_security(
    context=security_context,
    resource="INQONLN",
    access_type=AccessType.READ,
)
```

### Expanded Audit Logging

The original AUDITLOG table had limited field sizes:

| Original Field | Size | Modern Field | Size |
|---------------|------|--------------|------|
| TIMESTAMP | 26 chars | timestamp | ISO 8601 datetime |
| USER_ID | 8 chars | user_id | 255 chars |
| TERMINAL_ID | 4 chars | client_id | 255 chars |
| TRANS_ID | 4 chars | request_id | 36 chars (UUID) |
| PROGRAM | 8 chars | resource_name | 255 chars |
| ACCESS_TYPE | 8 chars | access_type | enum |

Additional modern fields:
- `trace_id`: Distributed tracing correlation
- `session_id`: User session tracking
- `response_code`: HTTP status or application response
- `duration_ms`: Request processing time
- `ip_address`: Client IP for security analysis
- `error_message`: Details if operation failed
- `metadata`: Extensible JSON for additional context

## Usage

### FastAPI Integration

```python
from fastapi import FastAPI, Depends
from security_context import (
    SecurityContextMiddleware,
    get_security_context,
    SecurityContext,
    SecurityManager,
    JWTExtractor,
    HeaderExtractor,
)

app = FastAPI()

# Add middleware to extract context from requests
app.add_middleware(
    SecurityContextMiddleware,
    extractors=[
        JWTExtractor(secret_key="your-secret"),
        HeaderExtractor(),
    ],
)

# Create security manager
security_manager = SecurityManager()

@app.get("/portfolio/{portfolio_id}")
async def get_portfolio(
    portfolio_id: str,
    context: SecurityContext = Depends(get_security_context),
):
    # Three-phase security check
    result = await security_manager.check_security(
        context=context,
        resource=f"portfolio/{portfolio_id}",
        access_type=AccessType.READ,
    )
    
    if not result.success:
        raise HTTPException(status_code=403, detail=result.error_info)
    
    # Proceed with business logic...
```

### Building Context Manually

```python
from security_context import SecurityContextBuilder

context = (
    SecurityContextBuilder()
    .with_user_id("user@example.com")
    .with_client_id("192.168.1.1")
    .with_request_id()  # Auto-generates UUID
    .with_trace_id("trace-123")
    .with_claims({"roles": ["analyst"]})
    .build()
)
```

### Authorization Records

```python
from security_context import AuthorizationRecord, AccessType

# User-based authorization
auth_store.add_record(
    AuthorizationRecord(
        user_id="user123",
        resource_pattern="portfolio/*",
        access_types=[AccessType.READ],
    )
)

# Role-based authorization
auth_store.add_record(
    AuthorizationRecord(
        role="admin",
        resource_pattern="*",
        access_types=[AccessType.READ, AccessType.WRITE, AccessType.DELETE],
    )
)
```

## Running the Example

```bash
# Install dependencies
pip install fastapi uvicorn

# Run the example application
cd src/modern/security_context
python -m uvicorn example_app:app --reload

# Get a test token
curl "http://localhost:8000/auth/token?user_id=user123&roles=analyst"

# Access portfolio with token
curl -H "Authorization: Bearer <token>" http://localhost:8000/portfolio/P001

# View current context
curl -H "Authorization: Bearer <token>" http://localhost:8000/context
```

## Running Tests

```bash
# Install test dependencies
pip install pytest pytest-asyncio

# Run tests
pytest src/modern/security_context/tests/
```

## Migration Guide

### From CICS ASSIGN to SecurityContext

**Before (COBOL):**
```cobol
EXEC CICS ASSIGN
          USERID(WS-USER-ID)
          TERMID(WS-TERMINAL-ID)
          TRANSID(WS-TRANSACTION-ID)
END-EXEC.
```

**After (Python):**
```python
# Context is automatically extracted by middleware
# and injected via dependency injection
context: SecurityContext = Depends(get_security_context)
user_id = context.user_id
client_id = context.client_id
request_id = context.request_id
```

### From CICS LINK to Dependency Injection

**Before (COBOL):**
```cobol
MOVE 'V' TO SEC-REQUEST-TYPE.
EXEC CICS LINK PROGRAM('SECMGR')
          COMMAREA(WS-SECURITY-REQUEST)
          LENGTH(LENGTH OF WS-SECURITY-REQUEST)
END-EXEC.
```

**After (Python):**
```python
result = await security_manager.validate_user(context)
```

### From DB2 AUTHFILE to AuthorizationStore

**Before (COBOL):**
```cobol
EXEC SQL
     SELECT COUNT(*)
     FROM AUTHFILE
     WHERE USER_ID = :SEC-USER-ID
       AND RESOURCE = :SEC-RESOURCE-NAME
       AND ACCESS_TYPE = :SEC-ACCESS-TYPE
END-EXEC.
```

**After (Python):**
```python
has_permission = authorization_store.check_permission(
    user_id=context.user_id,
    resource="INQONLN",
    access_type=AccessType.READ,
)
```

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        HTTP Request                              │
│  Headers: Authorization, X-Request-ID, X-Forwarded-For, etc.    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  SecurityContextMiddleware                       │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │JWTExtractor │  │HeaderExtract│  │ OAuth2/SessionExtractor │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SecurityContext                             │
│  user_id, client_id, request_id, trace_id, claims, etc.         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      SecurityManager                             │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐   │
│  │ 1. Validate  │─▶│ 2. Authorize │─▶│ 3. Audit             │   │
│  │ (JWT/Token)  │  │ (Permissions)│  │ (Log to AUDITLOG)    │   │
│  └──────────────┘  └──────────────┘  └──────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

## Files

- `context.py` - SecurityContext and SecurityContextBuilder
- `models.py` - Data models (AuditLogEntry, AuthorizationRecord, UserIdentity)
- `extractors.py` - Context extractors (JWT, OAuth2, Session, Header)
- `middleware.py` - FastAPI middleware for automatic context extraction
- `security_manager.py` - SecurityManager implementing three-phase security
- `example_app.py` - Example FastAPI application
- `tests/` - Unit tests for all components

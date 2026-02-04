# Security Manager - Python Implementation

Modern Python implementation of the COBOL SECMGR security manager program from the COBOL Legacy Benchmark Suite.

## Overview

This module provides a modernized version of the `SECMGR.cbl` program, which handles user validation, authorization, and audit logging for online transaction processing. The implementation preserves the original three-phase security model while leveraging modern Python patterns and frameworks.

## Original COBOL Architecture

The original SECMGR program (`src/programs/online/SECMGR.cbl`) implements a request-response pattern with three operations controlled by `SEC-REQUEST-TYPE`:

| Request Type | Operation | COBOL Paragraph | Description |
|--------------|-----------|-----------------|-------------|
| 'V' | Validation | P100-VALIDATE-USER | Verifies CICS user credentials |
| 'A' | Authorization | P200-CHECK-AUTH | Queries AUTHFILE table for permissions |
| 'L' | Audit | P300-LOG-ACCESS | Logs access attempts to AUDITLOG table |

The calling program (`INQONLN.cbl`) invokes SECMGR via CICS LINK commands in sequence: validation → authorization → audit. Each phase must succeed before proceeding to the next.

## Mapping to Modern Implementation

### Response Codes

| COBOL Code | Meaning | Python Exception/Response | HTTP Status |
|------------|---------|---------------------------|-------------|
| 0 | Success | `SecurityResponse.ok()` | 200 OK |
| 8 | Denied | `ValidationException` / `AuthorizationException` | 401 / 403 |
| 12 | Error | `SecurityException` | 500 |

### Database Tables

**AUTHFILE** (Authorization Rules)
```
COBOL:                          Python (SQLAlchemy):
USER_ID    PIC X(8)     →      user_id: String(8), PK
RESOURCE   PIC X(8)     →      resource: String(8), PK
ACCESS_TYPE PIC X(8)    →      access_type: String(8), PK
```

**AUDITLOG** (Audit Trail)
```
COBOL:                          Python (SQLAlchemy):
TIMESTAMP   PIC X(26)   →      timestamp: DateTime
USER_ID     PIC X(8)    →      user_id: String(8)
TERMINAL_ID PIC X(4)    →      terminal_id: String(4)
TRANS_ID    PIC X(4)    →      trans_id: String(4)
PROGRAM     PIC X(8)    →      program: String(8)
ACCESS_TYPE PIC X(8)    →      access_type: String(8)
```

### Context Propagation

| COBOL (CICS ASSIGN) | Modern Equivalent |
|---------------------|-------------------|
| USERID | HTTP Header `X-User-ID` or JWT `sub` claim |
| TERMID | HTTP Header `X-Terminal-ID` (defaults to "WEB") |
| TRANSID | HTTP Header `X-Transaction-ID` or auto-generated |

### Chain of Responsibility Pattern

The original COBOL uses sequential PERFORM statements. The Python implementation uses a chain-of-responsibility pattern:

```
COBOL (INQONLN P050-SECURITY-CHECK):          Python (SecurityManager.check_security):
MOVE 'V' TO SEC-REQUEST-TYPE                  ValidationHandler
EXEC CICS LINK PROGRAM('SECMGR')...                  ↓
IF SEC-RESPONSE-CODE = 0                      AuthorizationHandler
   MOVE 'A' TO SEC-REQUEST-TYPE                      ↓
   EXEC CICS LINK PROGRAM('SECMGR')...        AuditHandler
   IF SEC-RESPONSE-CODE = 0
      MOVE 'L' TO SEC-REQUEST-TYPE
      EXEC CICS LINK PROGRAM('SECMGR')...
```

## Installation

```bash
pip install -r requirements.txt
```

## Usage

### Direct Python Usage

```python
from security_manager import SecurityManager, SecurityContext
from security_manager.models import get_engine, get_session_factory, init_db

# Initialize database
engine = get_engine("sqlite:///security.db")
init_db(engine)
session_factory = get_session_factory(engine)

# Create security manager
manager = SecurityManager(session_factory)

# Add authorization rule
manager.add_authorization("TESTUSER", "INQONLN", "READ")

# Create security context
context = SecurityContext(user_id="TESTUSER")

# Perform full security check (validation → authorization → audit)
response = manager.check_security(context, "INQONLN", "READ")

if response.success:
    print("Access granted")
else:
    print(f"Access denied: {response.error_info}")
```

### REST API Usage

Start the server:
```bash
uvicorn security_manager.api:app --reload --port 8000
```

API Endpoints:

| Endpoint | Method | Description | COBOL Equivalent |
|----------|--------|-------------|------------------|
| `/security/validate` | POST | Validate user | SEC-REQUEST-TYPE = 'V' |
| `/security/authorize` | POST | Check authorization | SEC-REQUEST-TYPE = 'A' |
| `/security/audit` | POST | Log access | SEC-REQUEST-TYPE = 'L' |
| `/security/check` | POST | Full security check | P050-SECURITY-CHECK |
| `/admin/authorization` | POST | Add auth rule | N/A (admin) |
| `/admin/authorization` | DELETE | Remove auth rule | N/A (admin) |
| `/admin/audit-log` | GET | Query audit log | N/A (admin) |

Example API call:
```bash
# Full security check
curl -X POST http://localhost:8000/security/check \
  -H "Content-Type: application/json" \
  -d '{"user_id": "TESTUSER", "resource": "INQONLN", "access_type": "READ"}'
```

## Async Audit Logging

The original COBOL implementation uses synchronous audit logging that blocks the transaction if it fails. The Python implementation supports optional async logging:

```python
# Enable async audit logging
manager = SecurityManager(session_factory, async_audit=True)
```

**Trade-off**: Async logging improves performance but may lose audit records if the system crashes before persistence. For compliance-critical applications, use synchronous logging (the default).

## File Structure

```
security_manager/
├── __init__.py          # Package exports
├── models.py            # SQLAlchemy ORM models (AUTHFILE, AUDITLOG)
├── context.py           # Security context (replaces CICS ASSIGN)
├── exceptions.py        # Custom exceptions (maps to response codes)
├── security_manager.py  # Core security logic (chain-of-responsibility)
├── api.py               # FastAPI REST interface
├── requirements.txt     # Python dependencies
└── README.md            # This documentation
```

## Comparison with Original COBOL

| Feature | COBOL SECMGR | Python SecurityManager |
|---------|--------------|------------------------|
| User validation | CICS ASSIGN USERID | SecurityContext from headers/JWT |
| Authorization query | DB2 SQL | SQLAlchemy ORM |
| Audit logging | DB2 INSERT (sync) | SQLAlchemy (sync or async) |
| Error handling | Response codes (0/8/12) | Exceptions + HTTP status codes |
| Inter-program call | CICS LINK | REST API or direct method call |
| Context propagation | COMMAREA | Thread-local / ContextVar |

## Known Limitations

The current implementation maintains feature parity with the original COBOL but does not address these gaps (which also exist in the original):

1. No password validation (original only checks user ID match)
2. No rate limiting
3. No encryption of sensitive data
4. No session management

These security enhancements can be added in future iterations.

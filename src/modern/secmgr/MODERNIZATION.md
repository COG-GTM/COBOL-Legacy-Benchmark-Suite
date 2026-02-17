# SECMGR Modernization: COBOL to Python

## Overview

This document describes the modernization of the `SECMGR` COBOL program
(`src/programs/online/SECMGR.cbl`) into a Python-based REST microservice.
The original program is the core security component for online transaction
processing in a CICS/DB2 mainframe environment.

## Architecture Mapping

### COBOL → Python Component Map

| COBOL Component | Python Equivalent | File |
|---|---|---|
| `SECMGR.cbl` (full program) | `SecurityManager` class | `service.py` |
| `P100-VALIDATE-USER` | `SecurityManager.validate_user()` | `service.py` |
| `P200-CHECK-AUTH` | `SecurityManager.authorize_access()` | `service.py` |
| `P300-LOG-ACCESS` | `SecurityManager.audit_access()` | `service.py` |
| `PROCEDURE DIVISION EVALUATE` | `SecurityManager.process_request()` | `service.py` |
| `INQONLN P050-SECURITY-CHECK` | `SecurityManager.execute_security_chain()` | `service.py` |
| `SECURITY-REQUEST-AREA` (LINKAGE) | `SecurityRequest` / `SecurityResponse` | `api.py`, `responses.py` |
| `SEC-RESPONSE-CODE` (0/8/12) | `ResponseCode` enum + HTTP status codes | `responses.py`, `exceptions.py` |
| `SEC-ERROR-INFO` | Exception messages + `SecurityResponse.error_info` | `exceptions.py` |
| `EXEC CICS ASSIGN` | `SecurityContext` from HTTP headers | `context.py` |
| `EXEC SQL` (AUTHFILE) | SQLAlchemy ORM query on `AuthFile` model | `models.py` |
| `EXEC SQL` (AUDITLOG) | SQLAlchemy ORM insert on `AuditLog` model | `models.py` |
| `EXEC CICS LINK` (caller) | HTTP POST to REST endpoints | `api.py` |
| `COPY ERRHND` | Python exceptions hierarchy | `exceptions.py` |

### Three-Phase Security Model

The original COBOL program implements a sequential security pattern where
each phase must succeed before proceeding to the next. This is preserved
exactly in the Python implementation:

```
COBOL (INQONLN P050-SECURITY-CHECK)    Python (execute_security_chain)
─────────────────────────────────────   ──────────────────────────────
MOVE 'V' TO SEC-REQUEST-TYPE            validate_user(context)
LINK PROGRAM('SECMGR')                    │
IF SEC-RESPONSE-CODE = 0                  ▼ (success?)
                                        authorize_access(context, resource, access)
  MOVE 'A' TO SEC-REQUEST-TYPE              │
  LINK PROGRAM('SECMGR')                   ▼ (success?)
  IF SEC-RESPONSE-CODE = 0             audit_access(context, resource, access)
                                            │
    MOVE 'L' TO SEC-REQUEST-TYPE            ▼
    LINK PROGRAM('SECMGR')             SecurityResponse (final result)
```

## Response Code Mapping

| COBOL Code | Meaning | Python Exception | HTTP Status |
|---|---|---|---|
| `SEC-RESPONSE-CODE = 0` | Success | No exception | 200 OK |
| `SEC-RESPONSE-CODE = 8` | Denied | `ValidationException` / `AuthorizationException` | 401 / 403 |
| `SEC-RESPONSE-CODE = 12` | System error | `SecurityError` / `AuditException` | 500 |

## Context Propagation

### COBOL (CICS Context)
```cobol
EXEC CICS ASSIGN
    USERID(WS-USER-ID)
    TERMID(WS-TERMINAL-ID)
    TRANSID(WS-TRANSACTION-ID)
END-EXEC.
```

### Python (HTTP Headers)
```python
SecurityContext(
    user_id=request.headers["X-User-ID"],        # from JWT or auth header
    terminal_id=request.headers["X-Terminal-ID"], # client IP or device ID
    transaction_id=request.headers["X-Transaction-ID"],  # correlation ID
    program_name=request.headers["X-Program-Name"],
    trace_id=request.headers.get("traceparent"),  # OpenTelemetry
)
```

The context is stored in thread-local storage (`threading.local()`) so it
is available throughout the request lifecycle without explicit parameter
passing, similar to how CICS maintains context for the transaction.

## Database Integration

### AUTHFILE Table

The authorization query is preserved exactly:

**COBOL:**
```sql
SELECT COUNT(*) INTO :WS-DB2-AREA
FROM AUTHFILE
WHERE USER_ID = :SEC-USER-ID
  AND RESOURCE = :SEC-RESOURCE-NAME
  AND ACCESS_TYPE = :SEC-ACCESS-TYPE
```

**Python (SQLAlchemy):**
```python
stmt = select(AuthFile).where(
    AuthFile.USER_ID == context.user_id,
    AuthFile.RESOURCE == resource_name,
    AuthFile.ACCESS_TYPE == access_type,
)
result = session.execute(stmt).first()
```

The composite index on `(USER_ID, RESOURCE, ACCESS_TYPE)` is maintained
for query performance.

### AUDITLOG Table

The audit insert maps directly:

**COBOL:**
```sql
INSERT INTO AUDITLOG
  (TIMESTAMP, USER_ID, TERMINAL_ID, TRANS_ID, PROGRAM, ACCESS_TYPE)
VALUES
  (:WS-TIMESTAMP, :WS-USER-ID, :WS-TERMINAL-ID,
   :WS-TRANSACTION-ID, :WS-PROGRAM-NAME, :WS-ACCESS-TYPE)
```

**Python (SQLAlchemy):**
```python
audit_record = AuditLog(
    id=str(uuid.uuid4()),
    TIMESTAMP=datetime.now(timezone.utc),
    USER_ID=context.user_id,
    TERMINAL_ID=context.terminal_id,
    TRANS_ID=context.transaction_id,
    PROGRAM=resource_name,
    ACCESS_TYPE=access_type,
)
session.add(audit_record)
session.commit()
```

## Audit Logging Strategy

The COBOL implementation uses **synchronous** audit logging that blocks
the transaction if the INSERT fails (SEC-RESPONSE-CODE = 12). The Python
implementation provides both options:

| Method | Behavior | Trade-off |
|---|---|---|
| `audit_access()` | Synchronous, blocks caller | Guarantees persistence; matches COBOL behavior |
| `audit_access_async()` | Background thread pool with retries | Non-blocking; may lose records on crash |

The async method includes retry logic (up to 3 attempts) for transient
database failures. For strict compliance requirements, use the synchronous
method.

## REST API Endpoints

| Endpoint | Method | COBOL Equivalent | Description |
|---|---|---|---|
| `/api/v1/security/process` | POST | `PROCEDURE DIVISION EVALUATE` | Dispatch by request type |
| `/api/v1/security/validate` | POST | `P100-VALIDATE-USER` | Validate credentials |
| `/api/v1/security/authorize` | POST | `P200-CHECK-AUTH` | Check authorization |
| `/api/v1/security/audit` | POST | `P300-LOG-ACCESS` | Log access attempt |
| `/api/v1/security/check` | POST | `INQONLN P050-SECURITY-CHECK` | Full V→A→L chain |
| `/api/v1/security/health` | GET | N/A | Health check |

### Example: Full Security Chain

This replaces the INQONLN `P050-SECURITY-CHECK` three-step CICS LINK sequence:

```bash
curl -X POST http://localhost:8000/api/v1/security/check \
  -H "Content-Type: application/json" \
  -H "X-Terminal-ID: WEB1" \
  -H "X-Transaction-ID: INQ1" \
  -H "X-Program-Name: INQONLN" \
  -d '{
    "user_id": "JSMITH",
    "resource_name": "INQONLN",
    "access_type": "READ"
  }'
```

**Success Response (200):**
```json
{
  "code": 0,
  "success": true,
  "error_info": null,
  "request_type": "L",
  "user_id": "JSMITH",
  "resource_name": "INQONLN",
  "access_type": "READ"
}
```

**Denied Response (403):**
```json
{
  "detail": "Access denied"
}
```

## Error Handling Comparison

### COBOL
```cobol
IF SEC-RESPONSE-CODE NOT = 0
   MOVE SEC-ERROR-INFO TO WS-ERROR-MESSAGE
   PERFORM P900-ERROR-ROUTINE THRU P900-EXIT
   EXEC CICS RETURN END-EXEC
END-IF.
```

### Python (caller pattern)
```python
try:
    response = security_manager.execute_security_chain(
        context=ctx,
        resource_name="INQONLN",
        access_type="READ",
    )
except ValidationException:
    # SEC-RESPONSE-CODE = 8, user validation failed
    return error_response(401, "User validation failed")
except AuthorizationException:
    # SEC-RESPONSE-CODE = 8, access denied
    return error_response(403, "Access denied")
except SecurityError:
    # SEC-RESPONSE-CODE = 12, system error
    return error_response(500, "Security check failed")
```

## Running the Service

```bash
pip install -r requirements.txt
uvicorn secmgr.app:app --reload
```

The API documentation is available at `http://localhost:8000/docs` (Swagger UI).

## File Structure

```
src/modern/secmgr/
├── __init__.py          # Package marker
├── app.py               # FastAPI application entry point
├── api.py               # REST API endpoints (replaces CICS LINK interface)
├── context.py           # Security context propagation (replaces CICS ASSIGN)
├── database.py          # SQLAlchemy engine/session management (replaces DB2)
├── exceptions.py        # Exception hierarchy (replaces SEC-RESPONSE-CODE)
├── models.py            # ORM models for AUTHFILE and AUDITLOG tables
├── responses.py         # Structured response objects
├── service.py           # Core SecurityManager class (replaces SECMGR.cbl)
├── requirements.txt     # Python dependencies
└── MODERNIZATION.md     # This document
```

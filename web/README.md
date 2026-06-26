# CLBS Web Frontend

Modern web frontend for the Investment Portfolio Management System, replacing
the legacy COBOL/CICS green-screen application. Built with React, TypeScript and
Vite.

This is the first slice of the frontend modernization epic (MBA-1424). It
delivers **US-1: Authentication & Session Management** (MBA-1425).

## Scope (MBA-1425)

Mirrors the behaviour of the legacy `SECMGR` security manager program
(`src/programs/online/SECMGR.cbl`) and the `AUDITLOG` copybook
(`src/copybook/common/AUDITLOG.cpy`):

- Login page with user ID / password fields
- Configurable session timeout on inactivity (with a pre-timeout warning)
- Role-based access gating (admin vs. read-only)
- Logout that clears session state
- Failed login attempts logged (mirrors `AUD-FAILURE`)
- Mock user fixture with roles and credentials

The implementation is frontend-only; authentication runs against a mock
credential fixture (`src/mocks/users.ts`). The async `authService` interface
(`src/services/authService.ts`) is the integration point for a future backend.

## Mock users

| User ID    | Password      | Role      |
| ---------- | ------------- | --------- |
| `ADMIN001` | `admin123`    | ADMIN     |
| `PMGR0001` | `manage123`   | ADMIN     |
| `READ0001` | `readonly123` | READONLY  |
| `ANALYST1` | `analyst123`  | READONLY  |

## Getting started

```bash
cd web
npm install
npm run dev        # start the dev server
npm run build      # type-check + production build
npm run lint       # eslint
npm test           # vitest
```

## Configuration

Session timing is configurable via Vite env vars (see `.env.example`):

- `VITE_SESSION_TIMEOUT_MS` — inactivity timeout (default 15 min)
- `VITE_SESSION_WARNING_MS` — warning lead time before timeout (default 1 min)

## Legacy mapping

| Web concept                         | Legacy COBOL                                  |
| ----------------------------------- | --------------------------------------------- |
| `authService.authenticate`          | `SECMGR` `P100-VALIDATE-USER`                 |
| `ProtectedRoute` role gating        | `SECMGR` `P200-CHECK-AUTH` (AUTHFILE)         |
| `auditService.recordAuditEvent`     | `SECMGR` `P300-LOG-ACCESS` (AUDITLOG insert)  |
| `AuditRecord`                       | `AUDIT-RECORD` in `AUDITLOG.cpy`              |
| Session inactivity timeout          | CICS idle-terminal timeout                     |

# S5 — auth-service / security library: Modernization Specification

Phase 0 component analysis for the COBOL → Java (Spring Boot) modernization
(see decomposition plan, row **S5**).

Sources analyzed (every line read):

| Source | Lines | Role |
| --- | --- | --- |
| `src/programs/online/SECMGR.cbl` | 134 | Security Manager (validate / authorize / audit) |
| `src/copybook/online/ERRHND.cpy` | 20 | Online error-handling commarea |
| `src/programs/online/INQONLN.cbl` (P050-SECURITY-CHECK, lines 27-33, 79-88, 139-171) | — | Only caller of SECMGR |
| `src/cics/PORTDFN.csd` lines 56-61 | — | CICS PROGRAM definition for SECMGR |
| `documentation/technical/system-architecture.md` §8 (lines 792-881), §1 (225-230), §2.5 (348-386), §5.1.4 (523-534) | — | Documented security behaviour |
| `documentation/technical/data-dictionary.md` | — | Context only; contains no security entities |

Legend: **[CODE]** = behaviour observed in COBOL source; **INFERRED FROM DOCS** = described in documentation only, not implemented in any source file.

---

## 1. Purpose & scope

SECMGR is the single security gate for the online (CICS) side of the investment-portfolio system. It is a stateless, CICS-LINKed subroutine that offers three request types to the online transaction `PINQ` (program `INQONLN`):

1. **Validate user** (`V`) — confirm that the user ID supplied by the calling program is the user ID CICS has signed on for this task.
2. **Authorize** (`A`) — check whether a (user, resource, access-type) triple is granted in the `AUTHFILE` DB2 table.
3. **Audit** (`L`) — write an access record (timestamp, user, terminal, transaction, program, access type) to the `AUDITLOG` DB2 table.

Each call returns a 3-state return code (`0` success, `8` business denial, `12` technical failure) plus an 80-byte message. The caller is responsible for reacting (INQONLN routes non-zero codes to `ERRHNDL` and ends the transaction).

**In scope for S5:** user identity verification, resource/permission authorization, security audit logging, the shared online error record (`ERRHND`) as it relates to security failures.

**Out of scope (owned by other components):** the inquiry dispatch loop in `INQONLN` (S4), `ERRHNDL` error-logging program (S4/S8), the batch audit trail `AUDITLOG` copybook / `AUDPROC` / `RPTAUD00` (S8/S7), RACF/CICS system-level security (replaced by the platform's IdP).

There is **no session management, password handling, token issuance, role model, or security alerting** anywhere in the code. Those appear in documentation only (see §5).

---

## 2. Business rules

Numbering is stable and should be referenced by Phase-2 unit tests (`BR-S5-nn`). Return codes are `SEC-RESPONSE-CODE` (`PIC S9(8) COMP`).

### 2.1 Request dispatch — `PROCEDURE DIVISION` (SECMGR.cbl:41-54)

| # | Rule | Ref |
| --- | --- | --- |
| BR-S5-01 | A security request carries exactly one request type in `SEC-REQUEST-TYPE` (`PIC X`): `V` = validate user, `A` = authorize, `L` = audit-log. Exactly one paragraph is executed per LINK. | SECMGR.cbl:31-34, 42-52 |
| BR-S5-02 | **Unknown request type is silently ignored.** The `EVALUATE` has no `WHEN OTHER`; for any value other than `V`/`A`/`L` the program performs nothing and returns with `SEC-RESPONSE-CODE` and `SEC-ERROR-INFO` **unchanged** (whatever the caller passed in). No error is signalled. [CODE] | SECMGR.cbl:42-52 |
| BR-S5-03 | The program always ends with `EXEC CICS RETURN`; control goes back to the LINKing program with the (possibly updated) commarea. There is no `XCTL`, no abend path and no `HANDLE CONDITION` inside SECMGR. | SECMGR.cbl:54 |
| BR-S5-04 | `SEC-RESPONSE-CODE` / `SEC-ERROR-INFO` are **never initialised** by SECMGR; the caller must clear them. On success paths only the code is set to `0`; `SEC-ERROR-INFO` retains stale text from any previous call. [CODE] | SECMGR.cbl:64, 91, 128 |

### 2.2 Validate user — `P100-VALIDATE-USER` (SECMGR.cbl:56-76)

| # | Rule | Ref |
| --- | --- | --- |
| BR-S5-05 | The authoritative identity is obtained from the CICS task context via `EXEC CICS ASSIGN USERID(...)`, never from the request. | SECMGR.cbl:57-60 |
| BR-S5-06 | If `ASSIGN` fails (`RESP` ≠ `DFHRESP(NORMAL)`): message `'Unable to obtain user credentials'`, return code **12**. | SECMGR.cbl:70-74 |
| BR-S5-07 | If `ASSIGN` succeeds and the caller-supplied `SEC-USER-ID` (`PIC X(8)`) is byte-for-byte equal (COBOL alphanumeric compare, space-padded) to the CICS user ID: return code **0**. | SECMGR.cbl:62-64 |
| BR-S5-08 | If they differ: message `'User validation failed'`, return code **8**. | SECMGR.cbl:65-69 |
| BR-S5-09 | Note on effective semantics [CODE]: the only caller (`INQONLN` P050) populates `SEC-USER-ID` from the very same `EXEC CICS ASSIGN USERID` immediately before LINKing (INQONLN.cbl:142-149). Therefore in the shipped system the `V` request can only fail with code 12 (ASSIGN failure); the code-8 path is unreachable from `PINQ`. It does *not* check passwords, sign-on state, user status, or existence in any table. | INQONLN.cbl:139-149 |
| BR-S5-10 | The `ASSIGN` response is written **directly into `SEC-RESPONSE-CODE`** (RESP option). If `ASSIGN` fails, the code is overwritten by 12 (BR-S5-06); if it succeeds, `DFHRESP(NORMAL)` = 0 is then replaced by 0 or 8. The raw CICS RESP value never leaks to the caller. | SECMGR.cbl:59, 62 |

### 2.3 Authorize — `P200-CHECK-AUTH` (SECMGR.cbl:78-103)

| # | Rule | Ref |
| --- | --- | --- |
| BR-S5-11 | Authorization is a positive-list lookup: `SELECT COUNT(*) FROM AUTHFILE WHERE USER_ID = :SEC-USER-ID AND RESOURCE = :SEC-RESOURCE-NAME AND ACCESS_TYPE = :SEC-ACCESS-TYPE`. All three predicates are exact equality on `CHAR(8)` host variables (DB2 pads/ignores trailing blanks for CHAR comparison). No wildcards, no role hierarchy, no deny rows, no effective-date columns. | SECMGR.cbl:79-86 |
| BR-S5-12 | `SQLCODE = 0` and count `> 0` → return code **0** (granted). More than one matching row is treated the same as one. | SECMGR.cbl:88-91 |
| BR-S5-13 | `SQLCODE = 0` and count `= 0` → message `'Access denied'`, return code **8**. | SECMGR.cbl:92-96 |
| BR-S5-14 | Any other `SQLCODE` (including `+100`, negative codes, and warnings such as `+802`) → message `'Authorization check failed'`, return code **12**. Denial and technical failure are therefore distinguishable to the caller (8 vs 12). | SECMGR.cbl:97-100 |
| BR-S5-15 | **Defect to preserve or fix (decision needed, see §7 Q2):** the `INTO` target is `:WS-DB2-AREA`, which is the 01-level group item that *contains the SQLCA* (SECMGR.cbl:15-16), not a numeric host variable. `IF WS-DB2-AREA > 0` (line 90) is then an alphanumeric group comparison. On a real DB2 precompiler this is either a precompile error or an overlay of the SQLCA by the COUNT result; the program as written cannot have executed this path correctly. The *intended* rule is BR-S5-12/13 (count into an `S9(9) COMP` host variable). [CODE — DEFECT] | SECMGR.cbl:15-16, 79-81, 90 |
| BR-S5-16 | The authorize request does **not** re-verify identity: `SEC-USER-ID` is taken from the request as-is. Callers are expected to run `V` first (INQONLN does: INQONLN.cbl:151-159). | SECMGR.cbl:83 |
| BR-S5-17 | Resource and access-type vocabulary in use: `INQONLN` requests `SEC-RESOURCE-NAME = 'INQONLN'` (the program name) and `SEC-ACCESS-TYPE = 'READ'`. No other values appear in any source. `AUTHFILE` seed data is not in the repo. | INQONLN.cbl:153-154 |

### 2.4 Audit log — `P300-LOG-ACCESS` (SECMGR.cbl:105-135)

| # | Rule | Ref |
| --- | --- | --- |
| BR-S5-18 | The audit timestamp is `FUNCTION CURRENT-DATE` (21 chars: `YYYYMMDDhhmmsshh±hhmm`) moved into `PIC X(26)` → right-padded with 5 spaces. It is **not** in DB2 `TIMESTAMP` format (`YYYY-MM-DD-hh.mm.ss.ffffff`); if the `TIMESTAMP` column is a DB2 TIMESTAMP the INSERT would fail with `-180`; if it is `CHAR(26)` the stored value is the raw CURRENT-DATE string. [CODE] | SECMGR.cbl:24, 106 |
| BR-S5-19 | User, terminal and transaction IDs for the audit row are taken from the CICS task context (`ASSIGN USERID/TERMID/TRANSID`), **not** from the request. This `ASSIGN` has no `RESP`; a failure would raise the default CICS condition (task abend) rather than a return code. | SECMGR.cbl:108-112 |
| BR-S5-20 | The audited program is the request's `SEC-RESOURCE-NAME`; the audited access type is the request's `SEC-ACCESS-TYPE` (each `X(8)`, copied 1:1). | SECMGR.cbl:114-115 |
| BR-S5-21 | One row is inserted into `AUDITLOG (TIMESTAMP, USER_ID, TERMINAL_ID, TRANS_ID, PROGRAM, ACCESS_TYPE)`. No outcome/status column is written — the row records *that access was requested*, not whether it was granted. | SECMGR.cbl:117-125 |
| BR-S5-22 | `SQLCODE = 0` → return code **0**; any other SQLCODE → message `'Audit logging failed'`, return code **12**. There is no code-8 path for audit. | SECMGR.cbl:127-133 |
| BR-S5-23 | No `COMMIT`/`SYNCPOINT` is issued; the audit insert commits with the CICS task's unit of work (i.e. at `EXEC CICS RETURN` of `INQONLN`, or is rolled back if the task abends). | SECMGR.cbl (absence), INQONLN.cbl:50, 87 |
| BR-S5-24 | Caller-side sequencing [CODE, INQONLN]: audit (`L`) is requested **only when** validate (`V`) *and* authorize (`A`) both returned 0. Denied or failed attempts are **not** audited. This contradicts the documented "Log Violation" branch (see §5 D-04). | INQONLN.cbl:151-168 |

### 2.5 Return-code contract (summary)

| `SEC-RESPONSE-CODE` | Meaning | Set by | `SEC-ERROR-INFO` |
| --- | --- | --- | --- |
| `0` | Success (validated / granted / logged) | P100, P200, P300 | unchanged |
| `8` | Business denial | P100 (user mismatch), P200 (no AUTHFILE row) | `'User validation failed'` / `'Access denied'` |
| `12` | Technical failure | P100 (ASSIGN failed), P200 (SQL error), P300 (SQL error) | `'Unable to obtain user credentials'` / `'Authorization check failed'` / `'Audit logging failed'` |
| unchanged | Unknown request type | dispatcher | unchanged |

Caller behaviour on non-zero (INQONLN.cbl:82-88): the message is copied to the screen error field, `ERRHNDL` is LINKed with `ERR-WARNING` severity, and the transaction ends via `EXEC CICS RETURN`. The caller does not distinguish 8 from 12.

### 2.6 ERRHND copybook rules (ERRHND.cpy:1-21)

| # | Rule | Ref |
| --- | --- | --- |
| BR-S5-25 | Severity is one of `F` (fatal), `W` (warning), `I` (info); action is one of `R` (return), `C` (continue), `A` (abend). The consuming program (`ERRHNDL`) decides the action; callers abend with `ABCODE('IERR')` when `ERR-ABEND` is returned (INQONLN.cbl:131-133). | ERRHND.cpy:10-18, INQONLN.cbl:131-133 |
| BR-S5-26 | SECMGR copies `ERRHND` into `WS-ERROR-AREA` (SECMGR.cbl:26-27) but **never references any of its fields** and never LINKs `ERRHNDL`. Security failures reach the error handler only through the caller. [CODE] | SECMGR.cbl:26-27 |

**Business rule count: 26** (BR-S5-01 … BR-S5-26). No arithmetic, currency, or date calculations exist in this component; the only computation is a `COUNT(*)` comparison against zero.

---

## 3. Data contracts

### 3.1 `SECURITY-REQUEST-AREA` — SECMGR LINKAGE (SECMGR.cbl:30-39); identical layout in INQONLN `WS-SECURITY-REQUEST` (INQONLN.cbl:27-33)

Total length 109 bytes (1 + 8 + 8 + 8 + 4 + 80; `SEC-RESPONSE-CODE` is 4-byte binary). This is the `LENGTH OF WS-SECURITY-REQUEST` passed on the LINK (INQONLN.cbl:148).

| Field | PIC | Bytes | Semantics | Java type | Persistence mapping |
| --- | --- | --- | --- | --- | --- |
| `SEC-REQUEST-TYPE` | `X` | 1 | `V`/`A`/`L` (Level-88 `SEC-VALIDATE`, `SEC-AUTHORIZE`, `SEC-AUDIT`) | `enum SecurityRequestType { VALIDATE('V'), AUTHORIZE('A'), AUDIT('L') }` | none (dispatch only) |
| `SEC-USER-ID` | `X(8)` | 8 | CICS user ID (RACF ID), uppercase, space-padded | `String` (max 8; principal name from JWT `preferred_username`) | `AUTHFILE.USER_ID`, `AUDITLOG.USER_ID` |
| `SEC-RESOURCE-NAME` | `X(8)` | 8 | Protected resource; in practice the program name (`'INQONLN'`) | `String` (max 8) → becomes permission resource key | `AUTHFILE.RESOURCE`, `AUDITLOG.PROGRAM` |
| `SEC-ACCESS-TYPE` | `X(8)` | 8 | Access mode; only `'READ'` observed | `enum AccessType { READ, ... }` — only `READ` is evidenced; keep as `String` until vocabulary is confirmed (Q4) | `AUTHFILE.ACCESS_TYPE`, `AUDITLOG.ACCESS_TYPE` |
| `SEC-RESPONSE-CODE` | `S9(8) COMP` | 4 | 0 / 8 / 12 (see §2.5); also receives raw CICS RESP transiently | `enum SecurityResult { OK(0), DENIED(8), FAILED(12) }` (`int code`) | none |
| `SEC-ERROR-INFO` | `X(80)` | 80 | Human-readable failure text | `String` (max 80) | none |

### 3.2 `WS-SECURITY-AREA` — SECMGR working storage (SECMGR.cbl:18-24) → audit row

| Field | PIC | Semantics | Java type | Persistence mapping |
| --- | --- | --- | --- | --- |
| `WS-USER-ID` | `X(8)` | CICS `ASSIGN USERID` | `String` | `AUDITLOG.USER_ID` |
| `WS-TERMINAL-ID` | `X(4)` | CICS `ASSIGN TERMID` | `String` (max 4) → client/device identifier (IP or session id in Java) | `AUDITLOG.TERMINAL_ID` |
| `WS-TRANSACTION-ID` | `X(4)` | CICS `ASSIGN TRANSID` (`PINQ`) | `String` (max 4) → HTTP route / operation id | `AUDITLOG.TRANS_ID` |
| `WS-PROGRAM-NAME` | `X(8)` | copy of `SEC-RESOURCE-NAME` | `String` | `AUDITLOG.PROGRAM` |
| `WS-ACCESS-TYPE` | `X(8)` | copy of `SEC-ACCESS-TYPE` | `String` | `AUDITLOG.ACCESS_TYPE` |
| `WS-TIMESTAMP` | `X(26)` | `FUNCTION CURRENT-DATE` (21 chars + 5 spaces), local time with UTC offset | `OffsetDateTime` (store as `TIMESTAMP WITH TIME ZONE`) | `AUDITLOG.TIMESTAMP` |
| `WS-DB2-AREA` | group (SQLCA) | misused as `COUNT(*)` target (BR-S5-15) | `long` / `boolean exists` | — |

### 3.3 `ERRHND.cpy` — `ERROR-HANDLING` (ERRHND.cpy:4-21), 173 bytes

| Field | PIC | Semantics | Java type | Persistence mapping |
| --- | --- | --- | --- | --- |
| `ERR-PROGRAM` | `X(8)` | Reporting program | `String` | `ERRLOG.PROGRAM` (via ERRHNDL, S4/S8) |
| `ERR-PARAGRAPH` | `X(30)` | Reporting paragraph | `String` (→ method/class name) | `ERRLOG.PARAGRAPH` |
| `ERR-SQLCODE` | `S9(9) COMP` | DB2 SQLCODE | `Integer` (nullable) | `ERRLOG.SQLCODE` |
| `ERR-CICS-RESP` | `S9(8) COMP` | EIBRESP | `Integer` (nullable) — no Java analogue; retire | `ERRLOG.CICS_RESP` |
| `ERR-CICS-RESP2` | `S9(8) COMP` | EIBRESP2 | `Integer` (nullable) — retire | — |
| `ERR-SEVERITY` | `X` | `F`/`W`/`I` | `enum ErrorSeverity { FATAL('F'), WARNING('W'), INFO('I') }` | `ERRLOG.SEVERITY` |
| `ERR-MESSAGE` | `X(80)` | Message text | `String` (max 80) | `ERRLOG.MESSAGE` |
| `ERR-ACTION` | `X` | `R`/`C`/`A` | `enum ErrorAction { RETURN('R'), CONTINUE('C'), ABEND('A') }` | — |
| `ERR-TRACE-ID` | `X(16)` | Correlation id (ERRHNDL fills with `FUNCTION RANDOM` if blank) | `String` → W3C `traceparent` / Micrometer trace id | `ERRLOG.TRACE_ID` |
| `ERR-TIMESTAMP` | `X(26)` | `FUNCTION CURRENT-DATE` | `OffsetDateTime` | — |

`ERRHND` is owned by S4 (inquiry-api) / S8 (platform-common) per the plan; S5 only *consumes* the `ErrorSeverity`/`ErrorAction` enums and the 80-char message contract when it raises `SecurityException`s.

### 3.4 DB2 tables referenced (no DDL exists in repo — see §5 D-01)

**`AUTHFILE`** (read by P200) — columns inferred solely from the SQL predicates:

| Column | Inferred type | Java entity field |
| --- | --- | --- |
| `USER_ID` | `CHAR(8)` | `Permission.userId : String` |
| `RESOURCE` | `CHAR(8)` | `Permission.resource : String` |
| `ACCESS_TYPE` | `CHAR(8)` | `Permission.accessType : String` |

**`AUDITLOG`** (inserted by P300) — columns from the INSERT list:

| Column | Inferred type | Java entity field |
| --- | --- | --- |
| `TIMESTAMP` | `CHAR(26)` or `TIMESTAMP` (ambiguous, BR-S5-18) | `SecurityAuditEvent.occurredAt : OffsetDateTime` |
| `USER_ID` | `CHAR(8)` | `userId : String` |
| `TERMINAL_ID` | `CHAR(4)` | `clientId : String` |
| `TRANS_ID` | `CHAR(4)` | `operation : String` |
| `PROGRAM` | `CHAR(8)` | `resource : String` |
| `ACCESS_TYPE` | `CHAR(8)` | `accessType : String` |

The DB2 table `AUDITLOG` is **not** the same structure as the batch `AUDITLOG` copybook (`src/copybook/common/AUDITLOG.cpy`, 402-byte record with before/after images, used by `AUDPROC`/`RPTAUD00` against sequential dataset `PROD.AUDIT.LOG`). They share a name only.

**`SESSION`** — the plan lists "`SESSION` usage". In the repo, `SESSION` appears only as (a) the DB2 qualifier of a declared global temporary table `SESSION.DBSTATS` in `src/programs/common/DB2STAT.cbl` (statistics, unrelated to security) and (b) the `SESSION-ACTIVE`/`SESSION-TERMINATED` loop flag in `INQONLN.cbl:19-21`. **There is no session table, session record, or session logic in SECMGR.** Nothing to map.

---

## 4. Interfaces

### 4.1 Inbound

| Interface | Detail | Java target |
| --- | --- | --- |
| `EXEC CICS LINK PROGRAM('SECMGR') COMMAREA(WS-SECURITY-REQUEST) LENGTH(109)` from `INQONLN` P050, called up to 3× per screen interaction (`V` → `A` → `L`) | INQONLN.cbl:146-167 | **Library call** into `security-lib` from S4 inquiry-api. Recommended shape: a Spring Security filter chain on S4 performs *validate* (JWT verification) + *authorize* (`@PreAuthorize` / `AuthorizationManager` backed by `PermissionRepository`) + *audit* (`AuthorizationEvent` listener) automatically; the three-call protocol collapses into one request interception. Optionally also exposed as REST `POST /authz/check` for non-JVM consumers. |
| CICS `DEFINE PROGRAM(SECMGR) … EXECKEY(USER) RESIDENT(NO) GROUP(PORTGRP)` | PORTDFN.csd:56-61 | Retired; replaced by Spring bean registration / starter auto-configuration. |

### 4.2 Outbound

| Interface | Detail | Java target |
| --- | --- | --- |
| `EXEC CICS ASSIGN USERID` (P100, P300), `TERMID`, `TRANSID` (P300) | SECMGR.cbl:57-60, 108-112 | Read from `SecurityContextHolder` (authenticated `Jwt` principal), `HttpServletRequest` remote address / `X-Forwarded-For`, and the matched route id. Provided by **S5 security-lib** itself. |
| `SELECT COUNT(*) FROM AUTHFILE WHERE USER_ID=? AND RESOURCE=? AND ACCESS_TYPE=?` | SECMGR.cbl:79-86 | **DB read** via `PermissionRepository.existsByUserIdAndResourceAndAccessType(...)` on the S5-owned `authz.permission` table (Postgres, Flyway). Alternative: map to Keycloak realm roles and drop the table (Q1). |
| `INSERT INTO AUDITLOG (…)` | SECMGR.cbl:117-125 | **Event** — publish `SecurityAuditEvent` on the S8 platform-common audit publisher (`AuditEventPublisher`), persisted by S8 into its `AUDITLOG` store; S5 does not own the table. Synchronous fallback (`SecurityAuditRepository.save`) if S8's publisher is unavailable, to keep BR-S5-22 semantics (failure is surfaced to the caller). |
| `EXEC CICS RETURN` | SECMGR.cbl:54 | Method return / HTTP response. |
| Files (DDNAMEs), JCL jobs, `CALL` statements | none | — SECMGR has no file I/O, no batch JCL, no static/dynamic `CALL`s. |
| `COPY ERRHND` | SECMGR.cbl:27 | Library dependency on S8 `platform-common` (`ErrorSeverity`, `ErrorAction`, `ApplicationError` DTO) — used only by the caller today (BR-S5-26). |

### 4.3 Documented-but-absent interfaces (INFERRED FROM DOCS)

| Documented | Source | Reality |
| --- | --- | --- |
| `SECMGR` depends on `DB2ONLN` | system-architecture.md:531 | SECMGR issues embedded SQL directly; it never LINKs `DB2ONLN`. |
| `SECMGR → AUD: Log Violation` on denial | system-architecture.md:856-860 | No denial logging anywhere (BR-S5-24). |
| "Session Control", "Security Alerts", "Audit Reports" | system-architecture.md:802, 816-817 | Not implemented. |
| `/documentation/technical/security-guide.md` | system-architecture.md:965 | File does not exist. |

---

## 5. Discrepancies (code vs. documentation vs. plan)

| # | Discrepancy | Evidence |
| --- | --- | --- |
| D-01 | **No DDL for `AUTHFILE` or `AUDITLOG` (DB2)**. `src/database/db2/` defines only `PORTFOLIO_MASTER`, `INVESTMENT_POSITIONS`, `TRANSACTION_HISTORY`, `POSHIST`, `ERRLOG`, `RTNCODES`; `PORTPLAN.sql` grants nothing on security tables. Column types in §3.4 are inferred. The plan's "6 DB2 tables" count excludes these two. | `src/database/db2/*.sql`, SECMGR.cbl:82, 118 |
| D-02 | **`SESSION` is not a security artifact.** The plan lists "`SESSION`, `AUTHFILE` usage" for S5; `SESSION` in the repo is only the DB2 temp-table schema qualifier in `DB2STAT.cbl` and a loop flag in `INQONLN`. No session store exists. | DB2STAT.cbl:90-196, INQONLN.cbl:19-21 |
| D-03 | Docs list "Manages session security" / "Session Control" as SECMGR functions. Not implemented. | system-architecture.md:229, 802 vs SECMGR.cbl (whole file) |
| D-04 | Docs show violations being audited (`Log Violation`). Code audits only successful, authorized accesses, and only when the caller asks. | system-architecture.md:856-860 vs INQONLN.cbl:161-168 |
| D-05 | Docs: `SECMGR` depends on `DB2ONLN` ("Security Tables"). Code: direct embedded SQL, no LINK. | system-architecture.md:531 vs SECMGR.cbl:79-86, 117-125 |
| D-06 | Docs: "Manages DB2 authorization" (program header) — nothing in the code grants/revokes DB2 privileges; it only reads an application table. | SECMGR.cbl:6 |
| D-07 | Docs list "Security Alerts" and "Audit Reports" under SECMGR audit. Neither exists; the only audit report program (`RPTAUD00`) reads the *batch* `AUDITLOG` dataset, not the DB2 table. | system-architecture.md:814-818; RPTAUD00.cbl:17 |
| D-08 | Referenced `security-guide.md` does not exist. | system-architecture.md:965 |
| D-09 | **Name collision `AUDITLOG`**: DB2 table with 6 columns (SECMGR) vs. 402-byte sequential record copybook (`AUDITLOG.cpy`, `AUDPROC`, `RPTAUD00`, JCL `RPTAUD` DD `AUDITLOG`). Different layouts, different stores. | SECMGR.cbl:118-124 vs AUDITLOG.cpy:7-38, RPTAUD.jcl:8 |
| D-10 | **Host-variable defect**: `SELECT COUNT(*) INTO :WS-DB2-AREA` targets the SQLCA group item; `IF WS-DB2-AREA > 0` is an alphanumeric compare. The authorize path cannot have worked as written (BR-S5-15). No `EXEC SQL BEGIN DECLARE SECTION` for host variables either (other online programs such as `ERRHNDL` do declare one). | SECMGR.cbl:15-16, 79-81, 90 |
| D-11 | **Commarea addressability**: SECMGR declares `PROCEDURE DIVISION USING SECURITY-REQUEST-AREA` with a LINKAGE item that is not named `DFHCOMMAREA`. Under the CICS translator the commarea is exposed as `DFHCOMMAREA`; a separately-named USING parameter receives no storage from `EXEC CICS LINK`. All other online programs (`INQONLN`, `ERRHNDL`) use `DFHCOMMAREA`. Likely a latent addressing bug; the *intent* (BR-S5-01…24) is unambiguous. | SECMGR.cbl:30, 41 vs INQONLN.cbl:36, ERRHNDL.cbl LINKAGE |
| D-12 | Timestamp format mismatch: `FUNCTION CURRENT-DATE` string is inserted into a column named `TIMESTAMP` (BR-S5-18). Either the column is `CHAR(26)` (non-standard) or the insert fails at runtime. | SECMGR.cbl:106, 118-122 |
| D-13 | `ERRHND` is copied into SECMGR but never used; SECMGR never calls `ERRHNDL`, although §6 of the architecture doc describes centralized online error handling for all online programs. | SECMGR.cbl:26-27 |
| D-14 | Documented "User Validation" implies credential checking; actual `V` request only compares the caller-supplied ID with `ASSIGN USERID`, and the caller fills it from the same source — a tautology (BR-S5-09). | system-architecture.md:808 vs SECMGR.cbl:62-69, INQONLN.cbl:142-149 |
| D-15 | Caller ordering (S4 concern, recorded here because it changes the security posture): `INQONLN` performs the requested function (`INQP`/`INQH` LINK) **before** `P050-SECURITY-CHECK`. Authorization is post-hoc; data is already returned before the check runs. Docs (§2.5, §8.2.2) show validation first. | INQONLN.cbl:62-80 vs system-architecture.md:361-363, 848-853 |
| D-16 | `INQONLN` moves `SEC-ERROR-INFO` to `WS-ERROR-MESSAGE`, a field that does not exist in `ERRHND.cpy` (`ERR-MESSAGE`) nor in `INQCOM`. Would not compile; S4 owns the fix but it affects how S5 error text surfaces. | INQONLN.cbl:83-84, 135 vs ERRHND.cpy:14 |
| D-17 | `data-dictionary.md` §4.1 shows an `INQUERY-COMMAREA` for online, with no security fields; the security commarea (`SECURITY-REQUEST-AREA`) is documented nowhere and has no copybook — it is duplicated inline in both SECMGR and INQONLN. | data-dictionary.md:222-233; SECMGR.cbl:30-39; INQONLN.cbl:27-33 |
| D-18 | Architecture §8.3.2 lists `Maintenance / Admin` transaction security; no maintenance/admin transaction or access type exists in code (only `PINQ`/`READ`). | system-architecture.md:881; PORTDFN.csd:6-11 |

Missing or stub programs relevant to S5: none of SECMGR's own paragraphs are empty. Missing *artifacts*: `AUTHFILE` DDL, DB2 `AUDITLOG` DDL, any session component, `security-guide.md`.

---

## 6. Proposed Java design

Per the plan, S5 is primarily a **library** (`libs/security-lib`) consumed by S4 (and any future update services), backed by **Keycloak (OIDC)** for authentication and a small **auth-service** only if a central permission store/admin API is wanted (Q1). Recommended: start library-only; the Postgres `permission` table lives in the shared schema owned by S5's Flyway module.

### 6.1 Modules & packages

```
libs/security-lib
  com.portfolio.security
    SecurityLibAutoConfiguration        // Spring Boot starter; registers filter chain + beans
    config/
      ResourceServerConfig              // OAuth2 resource server, JWT decoder (Keycloak issuer)
      SecurityProperties                // issuer-uri, audit.mode=EVENT|SYNC, legacy-user-claim
    identity/
      CallerIdentity                    // record(userId, clientId, operation)  ← ASSIGN USERID/TERMID/TRANSID
      CallerIdentityResolver            // BR-S5-05/07/19: from SecurityContext + HttpServletRequest
      IdentityValidator                 // BR-S5-06..10 → SecurityResult
    authz/
      AccessType                        // enum, seeded with READ (BR-S5-17); extensible via Q4
      Permission                        // @Entity → authz.permission (USER_ID, RESOURCE, ACCESS_TYPE) BR-S5-11
      PermissionRepository              // JpaRepository; existsByUserIdAndResourceAndAccessType
      PermissionAuthorizationManager    // implements AuthorizationManager<RequestAuthorizationContext>; BR-S5-12..14
      RequiresAccess                    // @interface (resource, accessType) for @PreAuthorize-style method security
    audit/
      SecurityAuditEvent                // record: occurredAt, userId, clientId, operation, resource, accessType, outcome
      SecurityAuditPublisher            // BR-S5-18..23; delegates to S8 AuditEventPublisher; SYNC fallback → SecurityAuditRepository
      AuthorizationAuditListener        // Spring Security AuthorizationGrantedEvent / DeniedEvent → publish (Q3 decides whether denials are logged)
    result/
      SecurityResult                    // enum OK(0), DENIED(8), FAILED(12)  BR-S5 §2.5
      SecurityRequestType               // enum V/A/L (kept for parity tests & legacy adapter)
      SecurityException                 // maps to 401 (FAILED on identity), 403 (DENIED), 500/503 (FAILED on infra)
    legacy/
      SecmgrFacade                      // validate()/authorize()/audit() 1:1 with P100/P200/P300 for S10 parity tests
  db/migration/V5__authz_permission.sql // Flyway: authz.permission (user_id CHAR(8), resource CHAR(8), access_type CHAR(8), PK on all three)

services/auth-service   (OPTIONAL, only if Q1 = "own permission store")
  com.portfolio.auth
    api/PermissionAdminController       // CRUD on permissions (admin role), GET /authz/check?user&resource&access
    api/AuthzCheckController            // POST /authz/check → SecurityResult   (REST form of P200 for non-JVM callers)
```

### 6.2 Spring components

| COBOL | Spring component | Notes |
| --- | --- | --- |
| Dispatcher (`EVALUATE SEC-REQUEST-TYPE`) | `SecurityFilterChain` (auto-config) | One HTTP request triggers validate→authorize→audit in order; unknown type (BR-S5-02) has no analogue — reject at compile time via enum. |
| P100 `V` | `JwtAuthenticationProvider` + `IdentityValidator` | JWT signature/expiry/issuer check replaces the tautological ASSIGN compare (D-14). Invalid/missing token → `SecurityResult.FAILED` → HTTP 401. |
| P200 `A` | `PermissionAuthorizationManager` + `PermissionRepository` | `exists(...)` replaces `COUNT(*) > 0` (fixes D-10). No row → `DENIED` → 403. `DataAccessException` → `FAILED` → 503. |
| P300 `L` | `AuthorizationAuditListener` + `SecurityAuditPublisher` | Emits `SecurityAuditEvent` (S8 audit publisher). Add `outcome` (GRANTED/DENIED/FAILED) column — new vs COBOL (Q3). `occurredAt` = `OffsetDateTime.now(clock)` fixes D-12. |
| `ERRHND` usage | `ProblemDetail` responses built from S8 `ApplicationError` (`ErrorSeverity`, `ErrorAction`) | Security failures use `WARNING`/`RETURN` to mirror INQONLN.cbl:124. |

### 6.3 Entities

- `Permission` (`authz.permission`): `userId`, `resource`, `accessType` — composite PK. Loaded by exact match; consider `@Cacheable("permissions")` with short TTL.
- `SecurityAuditRecord` (only for SYNC fallback): `id`, `occurredAt`, `userId`, `clientId`, `operation`, `resource`, `accessType`, `outcome`. Otherwise S8 owns storage.

### 6.4 Events

- `SecurityAuditEvent` (outbound, S5 → S8 `platform-common` audit topic): `{occurredAt, userId, clientId, operation, resource, accessType, outcome}`. Key = `userId`. Replaces `INSERT INTO AUDITLOG`.
- `PermissionChanged` (outbound, only if auth-service exists): invalidates permission cache in consumers.

### 6.5 Config

```
portfolio.security.issuer-uri          = https://keycloak/realms/portfolio
portfolio.security.user-id-claim       = preferred_username        # 8-char RACF id parity (Q5)
portfolio.security.audit.mode          = EVENT | SYNC
portfolio.security.audit.log-denials   = false                     # COBOL parity default (BR-S5-24); true per docs (Q3)
portfolio.security.legacy-resource-name= INQONLN                   # resource key S4 asserts for inquiry routes (BR-S5-17)
```

### 6.6 ARB note

Introducing Keycloak (new external dependency), a new `authz.permission` data store, and changing auth boundaries are ARB triggers; the plan assigns the ADR to Phase-1 F3. This spec only documents; no infrastructure is added here.

---

## 7. Open questions

1. **Permission store vs. IdP roles.** Keep a `Permission(user, resource, accessType)` table (1:1 with `AUTHFILE`) or replace it with Keycloak realm/client roles (`INQONLN_READ`) and drop the DB read? Affects whether an `auth-service` exists at all.
2. **Authorize-path defect (D-10/BR-S5-15).** The COBOL `COUNT(*)` host variable is invalid. Confirm the intended rule is "grant iff at least one exact-match row exists" (as specified), since the shipped program could not have executed this correctly.
3. **Audit denials?** Code audits only granted accesses (BR-S5-24); documentation shows violations logged. Which is the requirement for Java? Also: should the audit row gain an `outcome` column (not present in COBOL)?
4. **Access-type vocabulary.** Only `READ` exists in code; docs mention Update/Admin levels. Provide the full list of `ACCESS_TYPE` values and `RESOURCE` keys (or the `AUTHFILE` seed data / DDL).
5. **User-ID identity model.** `USER_ID` is an 8-char RACF id. Will Keycloak users carry the same id (claim mapping), or must permissions be re-keyed to OIDC `sub`?
6. **Terminal / transaction analogues.** What should populate `TERMINAL_ID` (X(4)) and `TRANS_ID` (X(4)) in the Java audit record — client IP, session id, route id? Can column widths be relaxed?
7. **Pre- vs. post-dispatch check (D-15).** `INQONLN` runs the security check *after* performing the inquiry. Confirm the Java target must enforce authorization *before* data access (standard) rather than preserving legacy ordering.
8. **Session semantics.** Docs and plan mention "Session Control"/`SESSION`, but no code exists. Is stateless JWT sufficient, or is server-side session/inactivity timeout a requirement?
9. **AUDITLOG naming.** Two unrelated `AUDITLOG` artifacts exist (DB2 table vs. batch dataset). Should the Java security audit feed the same S8 audit store as `AUDPROC`, or remain a separate security log?
10. **Unknown request type behaviour (BR-S5-02).** Legacy silently ignores unknown types and returns the caller's stale code. Confirm Java should reject (400) rather than emulate.

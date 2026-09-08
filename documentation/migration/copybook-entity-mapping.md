# Copybook-to-Entity Mapping

All 20 copybooks under `src/copybook/`, classified by what they represent in a
target domain model (entity, value object, enum, service contract, or platform
glue), with field-level mapping notes and the programs that depend on each.

Classification key:

| Class | Meaning in target design |
|---|---|
| **Entity** | Persisted record with identity; becomes a table / aggregate root |
| **Value object** | Immutable structured value embedded in an entity or message |
| **Enum / constants** | Code tables and literals; becomes enums or configuration |
| **Contract** | Request/response area passed between programs; becomes a DTO / service interface |
| **Platform** | DB2 / CICS plumbing with no business meaning; replaced by the target platform |

## 1. Summary

| # | Copybook | Dir | Class | Target entity / type | Copied by |
|---|---|---|---|---|---|
| C-01 | `AUDITLOG.cpy` | common | Entity | `AuditEvent` | AUDPROC (FD), RPTAUD00, PORTTRAN |
| C-02 | `COMMON.cpy` | common | Enum / constants | `ReturnCode`, `RecordStatus`, `TransactionType`, `CurrencyCode` | *(none)* |
| C-03 | `ERRHAND.cpy` | common | Contract + Enum | `ErrorMessage`, `ErrorCategory`, `Severity`, `VsamStatus` | BCHCTL00, HISTLD00, PRCSEQ00, RCVPRC00, RPTAUD00, RPTPOS00, RPTSTA00, DB2CMT, DB2CONN, DB2ERR, DB2STAT, ERRPROC, PORTTEST, PORTTRAN, TSTGEN00, TSTVAL00, UTLMNT00, UTLMON00, UTLVAL00 |
| C-04 | `HISTREC.cpy` | common | Entity | `ChangeHistory` (before/after image log) | HISTLD00 (FD, but fields unused) |
| C-05 | `PORTFLIO.cpy` | common | Entity | `Portfolio` | PORTADD, PORTDEL, PORTREAD, PORTTEST, PORTUPDT, TSTGEN00 |
| C-06 | `PORTVAL.cpy` | common | Enum / constants | `PortfolioValidationRule`, `ValidationResult` | PORTVALD |
| C-07 | `POSREC.cpy` | common | Entity | `Position` | RPTPOS00, INQPORT, UTLVAL00 |
| C-08 | `RETHND.cpy` | common | Contract | `OperationResult` (rich return status) | CKPRST |
| C-09 | `RTNCODE.cpy` | common | Contract | `ReturnCodeRequest` (service DTO for RTNCDE00) | RPTAUD00, RPTPOS00, RPTSTA00, RTNCDE00, TSTGEN00, TSTVAL00, UTLMNT00, UTLMON00, UTLVAL00 |
| C-10 | `TRNREC.cpy` | common | Entity | `Transaction` | RPTPOS00, PORTTRAN (FD), TSTGEN00, UTLVAL00 |
| C-11 | `BCHCON.cpy` | batch | Enum / constants | `BatchStatus`, `ProcessType`, `DependencyType`, batch limits | BCHCTL00, HISTLD00, PRCSEQ00, RCVPRC00 |
| C-12 | `BCHCTL.cpy` | batch | Entity | `BatchJobRun` (control record) | BCHCTL00, HISTLD00, PRCSEQ00, RCVPRC00, RPTSTA00 |
| C-13 | `CKPRST.cpy` | batch | Entity + Contract | `Checkpoint` (state) and `CheckpointRecord` (persisted) | CKPRST |
| C-14 | `PRCSEQ.cpy` | batch | Entity + constants | `ProcessDefinition` (schedule/dependency metadata) | PRCSEQ00, RCVPRC00 |
| C-15 | `DBPROC.cpy` | db2 | Platform | connection / error paragraphs → data-access layer | HISTLD00, DB2CMT, DB2CONN, DB2ERR, DB2STAT |
| C-16 | `DBTBLS.cpy` | db2 | Entity (host structs) | `PositionHistory` (`POSHIST`), `ErrorLogEntry` (`ERRLOG`) | HISTLD00, DB2ERR |
| C-17 | `SQLCA.cpy` | db2 | Platform | SQLSTATE constants → exception mapping | HISTLD00, DB2CMT, DB2CONN, DB2ERR, DB2STAT |
| C-18 | `DB2REQ.cpy` | online | Contract | `Db2ConnectionRequest` | DB2RECV (DB2ONLN and INQHIST redeclare it inline) |
| C-19 | `ERRHND.cpy` | online | Contract | `OnlineError` | DB2ONLN, DB2RECV, ERRHNDL, INQONLN, SECMGR |
| C-20 | `INQCOM.cpy` | online | Contract | `InquiryRequest` (CICS COMMAREA) | INQHIST, INQONLN, INQPORT |

Two record layouts used by programs have **no copybook**: the `TH-*` transaction
history record read by `HISTLD00`, and the 100-byte `PORTFOLIO-RECORD` inlined in
`PORTMSTR`. Three copybook names are referenced but absent from the repo:
`DB2STAT` (RPTSTA00, UTLMON00), `PORTREC` (PORTTRAN), `SQLPOS` (INQPORT).

## 2. Entities

### C-05 `PORTFLIO.cpy` → `Portfolio`

Record length 148 bytes (18 key + 31 client + 17 portfolio + 16 financial + 16 audit
+ 50 filler). Data dictionary and `PORTDEF.jcl` say 200; `vsam-definitions.txt`
says 400.

| COBOL field | PIC | Target attribute | Type | Notes |
|---|---|---|---|---|
| `PORT-ID` | X(8) | `portfolioId` | string(8), **PK part** | BR-P01: `'PORT'` + 4 digits (PORTMSTR/PORTVALD) |
| `PORT-ACCOUNT-NO` | X(10) | `accountNumber` | string(10), **PK part** | data dictionary says `ACCOUNT-NO` is 9(9) |
| `PORT-CLIENT-NAME` | X(30) | `clientName` | string(30) | required (BR-P02) |
| `PORT-CLIENT-TYPE` | X(1) | `clientType` | enum `I` Individual / `C` Corporate / `T` Trust | |
| `PORT-CREATE-DATE` | 9(8) | `createdOn` | date (YYYYMMDD) | set by PORTADD |
| `PORT-LAST-MAINT` | 9(8) | `lastMaintainedOn` | date | set by PORTADD / PORTMSTR |
| `PORT-STATUS` | X(1) | `status` | enum `A` Active / `C` Closed / `S` Suspended | PORTMSTR accepts `A`/`I`/`C` instead (BR-P03 conflict) |
| `PORT-TOTAL-VALUE` | S9(13)V99 COMP-3 | `totalValue` | decimal(15,2) | |
| `PORT-CASH-BALANCE` | S9(13)V99 COMP-3 | `cashBalance` | decimal(15,2) | |
| `PORT-LAST-USER` | X(8) | `lastMaintainedBy` | string(8) | |
| `PORT-LAST-TRANS` | 9(8) | `lastTransactionDate` | date | |
| `PORT-FILLER` | X(50) | — | drop | |

The `PORTMSTR` inline variant is 100 bytes: `PORT-ID X(10)` key, `PORT-NAME X(50)`,
`PORT-CREATE-DATE X(10)`, `PORT-STATUS X(1)`, `PORT-TOTAL-VALUE`, filler. `PORTTRAN` expects `PORT-TOTAL-UNITS` and
`PORT-TOTAL-COST`. Union of the three gives the candidate `Portfolio` aggregate;
the relational `PORTFOLIO_MASTER` in `db2-definitions.sql` (`CURRENCY_CODE`,
`RISK_LEVEL`, `OPEN_DATE`, `CLOSE_DATE`, `BRANCH_ID`, `ACCOUNT_TYPE`) is a fourth
shape.

### C-07 `POSREC.cpy` → `Position`

| COBOL field | PIC | Target attribute | Notes |
|---|---|---|---|
| `POS-PORTFOLIO-ID` | X(8) | `portfolioId` FK → Portfolio | key part 1 |
| `POS-DATE` | X(8) | `positionDate` | key part 2 (YYYYMMDD) |
| `POS-INVESTMENT-ID` | X(10) | `investmentId` | key part 3 |
| `POS-QUANTITY` | S9(11)V9(4) COMP-3 | `quantity` decimal(15,4) | data dict says 3 decimals |
| `POS-COST-BASIS` | S9(13)V9(2) COMP-3 | `costBasis` decimal(15,2) | |
| `POS-MARKET-VALUE` | S9(13)V9(2) COMP-3 | `marketValue` decimal(15,2) | |
| `POS-CURRENCY` | X(3) | `currency` (enum from `COMMON.cpy`) | |
| `POS-STATUS` | X(1) | `status` enum `A`/`C`/`P` (Active/Closed/Pending) | |
| `POS-LAST-MAINT-DATE` | X(26) | `lastMaintainedAt` timestamp | |
| `POS-LAST-MAINT-USER` | X(8) | `lastMaintainedBy` | |

Maps 1:1 onto DB2 `INVESTMENT_POSITIONS` (PK `PORTFOLIO_ID, INVESTMENT_ID,
POSITION_DATE`). The data dictionary's `POSMSTRE` (key `POS-ACCOUNT-NO` 9(9) + `POS-FUND-ID` X(6);
fields `POS-CUSIP`, `POS-SHARE-BAL`, `POS-AVG-COST`, `POS-COST-BASIS`,
`POS-LAST-DATE`, `POS-LAST-TRANS`, `POS-STATUS`) and `RPTPOS00`'s
`POS-CURRENT-VALUE` / `POS-PREVIOUS-VALUE` describe a different, account-centric position; both
concepts need to be reconciled into one `Position` entity or split into
`Position` and `PositionSnapshot`.

### C-10 `TRNREC.cpy` → `Transaction`

| COBOL field | PIC | Target attribute | Notes |
|---|---|---|---|
| `TRN-DATE` X(8), `TRN-TIME` X(6) | | `transactionAt` timestamp | key parts 1–2 |
| `TRN-PORTFOLIO-ID` | X(8) | `portfolioId` FK | key part 3 |
| `TRN-SEQUENCE-NO` | X(6) | `sequenceNumber` | key part 4 → together a natural `transactionId` (28 chars; DB2 `TRANSACTION_HISTORY.TRANSACTION_ID` is CHAR(20), data dictionary `TRANS-ID` is 12) |
| `TRN-INVESTMENT-ID` | X(10) | `investmentId` | |
| `TRN-TYPE` | X(2) | `type` enum `BU` Buy / `SL` Sell / `TR` Transfer / `FE` Fee | |
| `TRN-QUANTITY` | S9(11)V9(4) COMP-3 | `quantity` | |
| `TRN-PRICE` | S9(11)V9(4) COMP-3 | `price` | |
| `TRN-AMOUNT` | S9(13)V9(2) COMP-3 | `amount` | |
| `TRN-CURRENCY` | X(3) | `currency` | |
| `TRN-STATUS` | X(1) | `status` enum `P` Pending / `D` Done / `F` Failed / `R` Reversed | |
| `TRN-PROCESS-DATE` X(26), `TRN-PROCESS-USER` X(8) | | `processedAt`, `processedBy` | |

Maps onto DB2 `TRANSACTION_HISTORY` (adds `TRANSACTION_ID CHAR(20)`, drops
sequence). The `TH-*` record consumed by `HISTLD00` (account, security id, fees,
total amount, cost basis, gain/loss) is a richer *settled* transaction; it maps to
`POSHIST` and should be modelled as `PositionHistory` (C-16) rather than as
`Transaction`.

### C-04 `HISTREC.cpy` → `ChangeHistory`

Audit-image record, 917 bytes: key (portfolio 8, date 8, time 6, seq 4) +
`HIST-RECORD-TYPE` (`PT` portfolio / `PS` position / `TR` transaction) +
`HIST-ACTION-CODE` (`A`/`C`/`D`) + 400-byte before image + 400-byte after image +
`HIST-REASON-CODE` + process date/user. Target: generic `ChangeHistory` table
(entityType, action, beforeJson, afterJson, reason, changedAt, changedBy). Although
`HISTLD00` copies it as the `TRANHIST` FD, it never references any `HIST-*` field.

### C-01 `AUDITLOG.cpy` → `AuditEvent`

| COBOL field | Target attribute |
|---|---|
| `AUD-TIMESTAMP` X(26) | `occurredAt` |
| `AUD-SYSTEM-ID`, `AUD-USER-ID`, `AUD-PROGRAM`, `AUD-TERMINAL` X(8) each | `systemId`, `userId`, `program`, `terminal` |
| `AUD-TYPE` X(4) | enum `TRAN` / `USER` / `SYST` |
| `AUD-ACTION` X(8) | enum `CREATE` / `UPDATE` / `DELETE` / `INQUIRE` / `LOGIN` / `LOGOUT` / `STARTUP` / `SHUTDOWN` |
| `AUD-STATUS` X(4) | enum `SUCC` / `FAIL` / `WARN` |
| `AUD-PORTFOLIO-ID` X(8), `AUD-ACCOUNT-NO` X(10) | `portfolioId`, `accountNumber` (nullable FKs) |
| `AUD-BEFORE-IMAGE`, `AUD-AFTER-IMAGE` X(100) | `before`, `after` (JSON) |
| `AUD-MESSAGE` X(100) | `message` |

Two other audit shapes exist (PORTDEL's 80-byte line; SECMGR's DB2 `AUDITLOG`
insert with `TERMINAL_ID`, `TRANS_ID`, `ACCESS_TYPE`); `AuditEvent` should be the
superset.

### C-12 `BCHCTL.cpy` → `BatchJobRun`

| COBOL field | Target attribute |
|---|---|
| `BCT-JOB-NAME` X(8), `BCT-PROCESS-DATE` X(8), `BCT-SEQUENCE-NO` 9(4) | composite id (`jobName`, `businessDate`, `sequence`) |
| `BCT-STATUS` X(1) | enum `R` Ready / `A` Active / `W` Waiting / `D` Done / `E` Error |
| `BCT-STEP-NAME`, `BCT-PROGRAM-NAME` X(8) | `stepName`, `programName` |
| `BCT-START-TIME`, `BCT-END-TIME` X(8) | `startedAt`, `endedAt` |
| `BCT-PREREQ-COUNT` + `BCT-PREREQ-JOBS OCCURS 10` (`NAME`, `SEQ`, `RC`) | child collection `BatchPrerequisite` |
| `BCT-RETURN-CODE` S9(4), `BCT-ERROR-DESC` X(80) | `returnCode`, `errorDescription` |
| `BCT-RESTART-COUNT` 9(2), `BCT-ATTEMPT-TS`, `BCT-COMPLETE-TS` X(26) | `restartCount`, `lastAttemptAt`, `completedAt` |

`HISTLD00` also moves its checkpoint counter to `BCT-RECORDS-READ`, a field that
`BCHCTL.cpy` does not define; a `recordsProcessed` attribute should be added.

### C-14 `PRCSEQ.cpy` → `ProcessDefinition`

| COBOL field | Target attribute |
|---|---|
| `PSR-PROCESS-ID` X(8), `PSR-VERSION` 9(2) | id + version |
| `PSR-DESCRIPTION` X(30), `PSR-TYPE` X(3) (`INI`/`UPD`/`RPT`/`CLN` from `BCHCON`) | `description`, `processType` |
| `PSR-FREQ` X(1), `PSR-START-TIME` 9(4), `PSR-MAX-TIME` 9(4) | `frequency`, `earliestStart`, `maxRunMinutes` |
| `PSR-DEP-ENTRY OCCURS 10` (`ID`, `TYPE` H/S, `RC`) | child collection `ProcessDependency` (hard / soft, max acceptable RC) |
| `PSR-PROGRAM` X(8), `PSR-PARM` X(50), `PSR-MAX-RC` S9(4), `PSR-RESTART` X(1) | `program`, `parameters`, `maxReturnCode`, `restartable` |
| `PSR-ACTIVE-DAYS` X(7), `PSR-MONTH-END`, `PSR-HOLIDAY-RUN` X(1) | `schedule` value object |
| `PSR-RECOVERY-PGM` X(8), `PSR-RECOVERY-PARM` X(50), `PSR-ERROR-LIMIT` 9(4) | `recovery` value object |
| `PSR-CREATE-*`, `PSR-UPDATE-*` | audit columns |

`STANDARD-SEQUENCES` constants (`INITDAY`, `CKPCLR`, `DATEVAL` → `TRNVAL00`,
`POSUPD00`, `HISTLD00` → `RPTGEN00`, `BCKLOD00`, `ENDDAY`) are the seed data for the
scheduler and name six processes that have no program in the repo.

### C-13 `CKPRST.cpy` → `Checkpoint` / `CheckpointRecord`

`CHECKPOINT-CONTROL` (in-memory state): program id, run date/time, status;
counters read/processed/error/restarts; `CK-LAST-KEY` X(50), `CK-LAST-TIME`,
`CK-PHASE`; up to 5 `CK-FILE-STATUS` entries (file name, position, status);
`CK-COMMIT-FREQ 1000`, `CK-MAX-ERRORS 100`, `CK-MAX-RESTARTS 3`, `CK-RESTART-MODE`.
`CHECKPOINT-RECORD` (persisted): key (program id 8 + run date 8) + 400-byte opaque
data. Target: `Checkpoint` table keyed by (program, runDate) with the control
block serialised as JSON; commit frequency / limits become configuration.

### C-16 `DBTBLS.cpy` → `PositionHistory`, `ErrorLogEntry`

`POSHIST-RECORD` (18 fields) is an exact host-variable image of the `POSHIST`
DDL. `ERRLOG-RECORD` (10 fields) matches `ERRLOG` DDL; `EL-ERROR-TYPE` enum
`S` System / `A` Application / `D` Data, `EL-ERROR-SEVERITY` 1 Info / 2 Warn /
3 Error / 4 Severe. Both become straight table mappings; `PositionHistory`
should also absorb the `TH-*` layout so that `TRANHIST` → `POSHIST` becomes an
identity mapping.

## 3. Contracts (service DTOs)

| Copybook | Fields | Target interface |
|---|---|---|
| C-03 `ERRHAND.cpy` `ERR-MESSAGE` | `ERR-DATE` X(10), `ERR-TIME` X(8), `ERR-PROGRAM` X(8), `ERR-CATEGORY` X(2) (`VS` VSAM / `VL` Validation / `PR` Processing / `SY` System), `ERR-CODE` X(4), `ERR-SEVERITY` S9(4) (0/4/8/12/16), `ERR-TEXT` X(80), `ERR-DETAILS` X(256) | `ErrorMessage` – payload of `ErrorService.log()` (replaces `CALL 'ERRPROC'`). Also carries `ERR-VSAM-STATUSES` `00`/`22`/`23`/`10` → `VsamStatus` enum mapped to exceptions |
| C-08 `RETHND.cpy` | `RETURN-CODE` (88s 0/4/8/12/16), `REASON-CODE`, `MODULE-ID`, `FUNCTION-ID`, `ERROR-LOCATION` (program/paragraph/routine), `ERROR-TYPE` `V`/`P`/`D`/`F`/`S`, `ERROR-CODE`, `ERROR-TEXT`, `SYSTEM-CODE/MSG`, `ACTION-FLAG` `C` Continue / `A` Abort / `R` Retry, `RETRY-COUNT`, `MAX-RETRIES 3`; `STD-ERROR-CODES` `E001`–`E010` | `OperationResult<T>` with `Severity`, `ErrorType`, `RecommendedAction`; `E001`–`E010` become an `ErrorCode` enum. Note `E001`–`E004` here (invalid data / not found / duplicate / file error) differ from the data dictionary's `E001`–`E004` (invalid account / invalid fund / invalid type / insufficient balance) |
| C-09 `RTNCODE.cpy` | `RC-REQUEST-TYPE` `I`/`S`/`G`/`L`/`A`, `RC-PROGRAM-ID`, `RC-CURRENT-CODE`, `RC-HIGHEST-CODE`, `RC-NEW-CODE`, `RC-STATUS` `S`/`W`/`E`/`F`, `RC-MESSAGE` X(80), `RC-RESPONSE-CODE`, analysis window + stats, return data | `ReturnCodeService` (init / set / get / log / analyze) backed by `RTNCODES` table |
| C-18 `DB2REQ.cpy` | `DB2-REQUEST-TYPE` `C`/`D`/`S`, `DB2-RESPONSE-CODE`, `DB2-CONNECTION-TOKEN` X(16), `DB2-SQLCODE`, `DB2-ERROR-MSG` X(80) | disappears – replaced by connection pool / DataSource |
| C-19 `ERRHND.cpy` | `ERR-PROGRAM` X(8), `ERR-PARAGRAPH` X(30), `ERR-SQLCODE`, `ERR-CICS-RESP`, `ERR-CICS-RESP2`, `ERR-SEVERITY` `F`/`W`/`I`, `ERR-MESSAGE` X(80), `ERR-ACTION` `R`/`C`/`A`, `ERR-TRACE-ID` X(16), `ERR-TIMESTAMP` X(26) | `OnlineError` – exception payload with trace id; `ERR-ACTION` maps to exception type (recoverable / fatal). Shares the group name `ERROR-HANDLING` with `COMMON.cpy` and field names with `ERRHAND.cpy` – the two cannot be copied into one program |
| C-20 `INQCOM.cpy` | `INQCOM-FUNCTION` X(4) (`MENU`/`INQP`/`INQH`/`EXIT`), `INQCOM-ACCOUNT-NO` X(10), `INQCOM-RESPONSE-CODE`, `INQCOM-ERROR-MSG` X(80) | `InquiryRequest` / `InquiryResponse` for the inquiry API; functions become endpoints `GET /portfolios/{account}/position`, `GET /portfolios/{account}/history` |

## 4. Enums and constants

| Copybook | Constant group | Target |
|---|---|---|
| C-02 `COMMON.cpy` | `RETURN-CODES` 0/4/8/12/16 | `ReturnCode` enum (SUCCESS, WARNING, ERROR, SEVERE, CRITICAL) – the canonical severity scale; identical values repeated in `ERRHAND`, `RETHND`, `BCHCON` |
| | `STATUS-CODES` A/C/P/S/F/R | `RecordStatus` enum |
| | `TRANSACTION-TYPES` BU/SL/TR/FE | `TransactionType` enum |
| | `COMMON-DATETIME`, `ERROR-HANDLING`, `AUDIT-FIELDS` | replace with platform date/time, error, audit types |
| | `CURRENCY-CODES` USD/EUR/GBP/JPY/CAD | `Currency` enum / ISO-4217 lookup |
| C-06 `PORTVAL.cpy` | `VAL-RETURN-CODES` 0–4, `VAL-ERROR-MESSAGES`, `VAL-MIN-AMOUNT` −9 999 999 999 999.99, `VAL-MAX-AMOUNT` +9 999 999 999 999.99, `VAL-ID-PREFIX 'PORT'` | validation rule constants (BR-V01…V04) |
| C-11 `BCHCON.cpy` | `BCT-STAT-VALUES` R/A/W/D/E; `BCT-RC-THRESHOLDS`; `BCT-MAX-PREREQ 10`, `BCT-MAX-RESTARTS 3`, `BCT-WAIT-INTERVAL 300`, `BCT-MAX-WAIT-TIME 3600`; `BCT-PROC-TYPES` INI/UPD/RPT/CLN; `BCT-DEP-TYPES` R/O/X; `BCT-PROC-NAMES` STARTDAY/ENDDAY/EMERGENCY; `BCT-REC-TYPES` C/P/D/H; messages | `BatchStatus`, `ProcessType`, `DependencyType` enums + scheduler configuration. Note `PRCSEQ.cpy` defines `PSR-DEP-TYPE` as `H` hard / `S` soft (used by `PRCSEQ00`) while `BCHCON` defines dependency types `R`/`O`/`X` – two vocabularies for one concept |
| C-17 `SQLCA.cpy` | SQLSTATE `00000`, `02000`, `23505`, `40001`, `40003`, `08001`, `58004` | exception mapping table |

## 5. Platform glue

| Copybook | Content | Migration treatment |
|---|---|---|
| C-15 `DBPROC.cpy` | `DB2-ERROR-HANDLING` work area (`DB2-MAX-RETRIES 3`, `DB2-RETRY-WAIT 100`); paragraphs `CONNECT-TO-DB2` (`CONNECT TO POSMVP`), `DISCONNECT-FROM-DB2` (`COMMIT WORK` + `CONNECT RESET`), `DB2-ERROR-ROUTINE` (`ROLLBACK WORK`, `CALL 'ERRPROC'`), `CHECK-SQL-STATUS` | Replaced by connection management and a SQL-exception → `ErrorService` adapter. Because the copybook contains a `CALL 'ERRPROC'`, every program that copies it (HISTLD00, DB2CMT, DB2CONN, DB2ERR, DB2STAT) has an implicit dependency on `ERRPROC` even where it is not called directly |
| C-17 `SQLCA.cpy` | `EXEC SQL INCLUDE SQLCA` + status constants | precompiler artefact |
| C-18 `DB2REQ.cpy` | see §3 | connection pool |

## 6. Field-type conventions for conversion

| COBOL pattern | Occurrences | Target type |
|---|---|---|
| `PIC X(8)` date `YYYYMMDD` (`PORT-CREATE-DATE` 9(8), `POS-DATE`, `TRN-DATE`, `BCT-PROCESS-DATE`) | 9 | `LocalDate` |
| `PIC X(6)` time `HHMMSS` | 3 | `LocalTime` |
| `PIC X(26)` timestamp (`YYYY-MM-DD-HH.MM.SS.NNNNNN`) | 12 | `Instant` / `TIMESTAMP(6)` |
| `PIC X(10)` ISO date, `PIC X(8)` ISO time (`DBTBLS`) | 4 | `LocalDate` / `LocalTime` (DB2 `DATE`/`TIME`) |
| `S9(13)V99 COMP-3` money | 12 | `decimal(15,2)` |
| `S9(11)V9(4) COMP-3` quantity / price | 4 | `decimal(15,4)` |
| `S9(12)V9(3) COMP-3` (`PH-QUANTITY`, `PH-PRICE`) | 2 | `decimal(15,3)` – precision differs from `TRNREC` 4 dp |
| `S9(4) COMP` return code | many | `ReturnCode` enum / `short` |
| `88`-level groups | 15 | enums (listed above) |
| `OCCURS 10 TIMES` (`BCT-PREREQ-JOBS`, `PSR-DEP-ENTRY`), `OCCURS 5` (`CK-FILE-STATUS`) | 3 | child collections |
| `PIC X(50)` `FILLER` | 7 | dropped |

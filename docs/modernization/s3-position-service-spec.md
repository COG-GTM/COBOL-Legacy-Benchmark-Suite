# S3 — position-service: Modernization Specification (Phase 0)

Component: **S3 position-service** (per `cobol-java-modernization-plan.md` §2, row S3)
Target: Spring Boot microservice owning `INVESTMENT_POSITIONS` and `POSHIST`
(replacing VSAM `POSMSTRE` / `POSHIST` KSDS), consuming `TransactionPosted` events from
S2 transaction-service and publishing `PositionChanged` events to S7 reporting-service.

Sources analysed (all read in full):

| Source | Lines | Role |
|---|---|---|
| `src/programs/batch/HISTLD00.cbl` | 232 | Position-history DB2 load (the only implemented S3 program) |
| `src/programs/batch/POSUPDT.cbl` | **0 — EMPTY FILE** | Intended position-update batch program |
| `src/programs/portfolio/PORTTRAN.cbl` (paragraphs 2200–2310 and their callers) | 316 | Only place where position/holding arithmetic exists in code |
| `src/copybook/common/POSREC.cpy` | 32 | Position master record |
| `src/copybook/common/HISTREC.cpy` | 38 | History (before/after-image) record |
| `src/copybook/db2/DBTBLS.cpy` (`POSHIST-RECORD` host structure) | — | Host variables used by `HISTLD00` |
| `src/database/db2/POSHIST.sql` | 98 | `POSHIST` DDL |
| `src/database/db2/db2-definitions.sql` (`INVESTMENT_POSITIONS`) | 104 | Position table DDL |
| `src/database/vsam/vsam-definitions.txt` | 99 | VSAM cluster definitions |
| `documentation/technical/data-dictionary.md` §2.2, §2.3, §3.1 (+ §5.2, §8) | — | Documented layouts / rules |
| `documentation/technical/system-architecture.md` §1.2.2, §2.2, §4, §5.1 | — | Context |

Legend used throughout: **[CODE]** = rule observed in COBOL source; **INFERRED FROM DOCS** = rule
exists only in documentation (no implementing code exists); **[GAP]** = behaviour that must exist for
the service to work but is neither coded nor documented.

---

## 1. Purpose & scope

The position-service is the system of record for **what each portfolio holds**: for every
(portfolio, investment, position-date) it keeps the quantity held, the total cost basis, the current
market value, currency and a status (active / closed / pending). It also owns the **position history**
(`POSHIST`) — the immutable, per-transaction ledger that explains how a position got to its current
state and that downstream reporting and the online inquiry (`INQHIST`) read.

In the legacy batch day (data-dictionary §8.1/§9.1, system-architecture §4.1) the component is
realised by two steps that run after transaction validation:

1. **POSUPD00 / POSUPDT — Position Update** (1815–1900): apply validated buy/sell/transfer/fee
   transactions to the position master, maintain cost basis, write history records.
   **The program file is empty; no code exists.** The only executable position arithmetic in the repo is
   in `PORTTRAN.cbl` paragraphs `2200-UPDATE-POSITIONS` … `2240-PROCESS-FEE`, which update
   *portfolio-level* totals (units / cost), not per-investment positions, and which are never invoked
   (see §5).
2. **HISTLD00 — History Load to DB2** (1900–1930): copy the day's transaction-history VSAM records into
   the DB2 `POSHIST` table with commit/checkpoint control and duplicate suppression. Fully implemented.

Out of scope for S3 (owned elsewhere): transaction validation (`PORTTRAN` 2100–2130 → S2),
portfolio master CRUD (S1), the inquiry screens that read positions (S4), batch orchestration /
checkpoint framework (S6), error & audit libraries `ERRPROC`/`AUDPROC` (S8), position reports (S7).

---

## 2. Business rules

Numbering is continuous across sub-sections; `business_rule_count` = **41**.

### 2.1 HISTLD00 — history load (all [CODE])

Program-level structure and control flow

| # | Rule | Ref |
|---|---|---|
| BR-01 | Input is the transaction-history file (DD `TRANHIST`), an indexed (KSDS) file read **sequentially** from the first record; record key is `TH-KEY`. There is no positioning to a restart point — every run processes the whole file. | `HISTLD00.cbl:17-22`, `:134-141` |
| BR-02 | The batch-control file (DD `BCHCTL`) is opened I-O with dynamic access, keyed on `BCT-KEY`. | `HISTLD00.cbl:24-29`, `:109` |
| BR-03 | Processing sequence is fixed: initialise (open files → connect DB2 → init checkpoint) → loop → terminate (final commit → close → disconnect → stats). | `HISTLD00.cbl:67-100` |
| BR-04 | The read/load loop terminates when end-of-file is reached **or** when `WS-ERROR-COUNT > 100` (i.e. the 101st error stops the run). | `HISTLD00.cbl:70-72` |
| BR-05 | The job **return code equals the error count** (`MOVE WS-ERROR-COUNT TO RETURN-CODE`). It does not follow the 0/4/8/12/16 convention of `BCHCON.cpy` / data-dictionary §8.2; any non-zero count yields RC = count (e.g. 3 errors → RC 3, 101 errors → RC 101). | `HISTLD00.cbl:76` |
| BR-06 | An `OPEN` failure on either file (status ≠ `'00'`) is reported through `9000-ERROR-ROUTINE` but **does not stop the program**; execution continues into DB2 connect and the loop (which will then fail on the first READ/REWRITE). | `HISTLD00.cbl:102-114` |
| BR-07 | DB2 connection is `CONNECT TO POSMVP`; a non-zero SQLCODE goes to `DB2-ERROR-ROUTINE` (rollback + `ERRPROC`) but does not stop the program. | `HISTLD00.cbl:116-118`, `DBPROC.cpy:26-34` |

Checkpoint initialisation

| # | Rule | Ref |
|---|---|---|
| BR-08 | The batch-control record is located by key = spaces with `BCT-JOB-NAME = 'HISTLD00'` (process-date and sequence-no components of `BCT-KEY` are left as spaces). If not found (`INVALID KEY`) → error routine; processing still continues. | `HISTLD00.cbl:120-128` |
| BR-09 | On start the control record status is set to `BCT-STAT-ACTIVE` (`'A'`) and rewritten. The status is **never** set to DONE/ERROR at end of job — the record stays `'A'` after a successful run. | `HISTLD00.cbl:130-131`, `BCHCON.cpy:10`; absence of any later status MOVE in `:202-224` |

Per-record load

| # | Rule | Ref |
|---|---|---|
| BR-10 | Each successful `READ` increments `WS-RECORDS-READ`; `AT END` sets the EOF switch. A read error other than EOF is not checked (`WS-TH-STATUS` is never inspected after open). | `HISTLD00.cbl:134-141` |
| BR-11 | The DB2 host structure `POSHIST-RECORD` is `INITIALIZE`d (alphanumerics → spaces, numerics → zero) before mapping. | `HISTLD00.cbl:144` |
| BR-12 | Exactly 13 fields are mapped from the history record to the host structure by simple `MOVE` (no conversion, no rounding): ACCOUNT-NO, PORTFOLIO-ID, TRANS-DATE, TRANS-TIME, TRANS-TYPE, SECURITY-ID, QUANTITY, PRICE, AMOUNT, FEES, TOTAL-AMOUNT, COST-BASIS, GAIN-LOSS. | `HISTLD00.cbl:146-158` |
| BR-13 | The five remaining `POSHIST` columns — `PROCESS_DATE`, `PROCESS_TIME`, `PROGRAM_ID`, `USER_ID`, `AUDIT_TIMESTAMP` — are **not populated**; they carry the `INITIALIZE` value (spaces). Because `PROCESS_DATE`/`PROCESS_TIME` are `DATE NOT NULL`/`TIME NOT NULL` without defaults, a real DB2 would reject every row with a conversion error (SQLCODE −180/−181); only `AUDIT_TIMESTAMP` has `WITH DEFAULT`. | `HISTLD00.cbl:144-158`, `DBTBLS.cpy:24-28`, `POSHIST.sql:39-43` |
| BR-14 | Insert is a single-row `INSERT INTO POSHIST VALUES (:POSHIST-RECORD)` using the whole host structure positionally (18 host fields ↔ 18 columns, column order of `POSHIST.sql:26-43`). | `HISTLD00.cbl:160-163` |
| BR-15 | `SQLCODE = 0` → `WS-RECORDS-WRITTEN` + 1. | `HISTLD00.cbl:165-166` |
| BR-16 | `SQLCODE = −803` (duplicate primary key `ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME`) is **silently skipped**: not counted as written, not counted as an error, not logged. This is the "duplicate checking" of the backlog and the idempotent re-run mechanism. | `HISTLD00.cbl:168-169`, `POSHIST.sql:55-57` |
| BR-17 | Any other non-zero `SQLCODE` → `WS-ERROR-COUNT` + 1 and `DB2-ERROR-ROUTINE`, which performs `ROLLBACK WORK` and calls `ERRPROC` with program `'DB2ERROR'`. The rollback discards **all uncommitted inserts since the last commit** (up to 999 rows) but `WS-RECORDS-WRITTEN` and `WS-COMMIT-COUNT` are not adjusted, so statistics and checkpoint counts overstate the rows actually persisted. | `HISTLD00.cbl:170-173`, `DBPROC.cpy:46-57` |

Commit and checkpoint

| # | Rule | Ref |
|---|---|---|
| BR-18 | `WS-COMMIT-COUNT` is incremented for **every record read** (including duplicates and failed inserts); when it reaches `WS-COMMIT-THRESHOLD = 1000` a `COMMIT WORK` is issued, the counter reset to 0 and the checkpoint updated. Matches data-dictionary §8.3 "History load: every 1000 records". | `HISTLD00.cbl:59`, `:177-189` |
| BR-19 | A checkpoint update copies `WS-RECORDS-READ` → `BCT-RECORDS-READ` and `WS-RECORDS-WRITTEN` → `BCT-RECORDS-WRITTEN` and rewrites the control record; `INVALID KEY` → error routine. No last-key / restart position is saved. (These two `BCT-` fields do not exist in `BCHCTL.cpy` — see §5.) | `HISTLD00.cbl:191-200` |
| BR-20 | At termination a final `COMMIT WORK` and a final checkpoint update are performed unconditionally, even if the loop ended by the error threshold. | `HISTLD00.cbl:202-208` |
| BR-21 | Disconnect = `COMMIT WORK` (a second commit) then `CONNECT RESET`. | `HISTLD00.cbl:215-217`, `DBPROC.cpy:36-44` |
| BR-22 | Statistics `Records Read / Records Written / Errors` are written to SYSOUT via `DISPLAY`. | `HISTLD00.cbl:219-224` |
| BR-23 | `9000-ERROR-ROUTINE` sets `ERR-PROGRAM = 'HISTLD00'`, calls `ERRPROC` with `ERR-MESSAGE`, then issues `ROLLBACK WORK`. It does **not** increment `WS-ERROR-COUNT`, set a return code or terminate; VSAM/file errors therefore never affect RC (BR-05) and never trip the error threshold (BR-04). | `HISTLD00.cbl:226-232` |

### 2.2 PORTTRAN — position-update paragraphs ([CODE], but **dead code**, see BR-24)

These paragraphs are the only implemented position arithmetic and are the closest proxy for the
missing `POSUPDT`. They operate on the *portfolio* record (`PORT-RECORD`), not on `POSITION-RECORD`.

| # | Rule | Ref |
|---|---|---|
| BR-24 | `2200-UPDATE-POSITIONS` is **never PERFORMed**: the main loop only calls `2100-VALIDATE-TRANSACTION`. As written, `PORTTRAN` validates transactions and counts them but never changes any balance. | `PORTTRAN.cbl:92-100`, `:102-118`, `:167` |
| BR-25 | Precondition (validation, S2 scope, listed for completeness): a transaction reaches position update only if portfolio-id ≠ spaces and exists on `PORTFILE`; `TRN-TYPE ∈ {BU, SL, TR, FE}`; `TRN-QUANTITY > 0`; `TRN-PRICE > 0` unless type `TR`; `TRN-AMOUNT > 0` unless type `TR`. Validation stops at the first failure. | `PORTTRAN.cbl:102-165` |
| BR-26 | Dispatch on `TRN-TYPE`: `BU`→buy, `SL`→sell, `TR`→transfer, `FE`→fee, then always write an audit record (even after a failed update). | `PORTTRAN.cbl:167-180` |
| BR-27 | **Buy**: re-read portfolio by `TRN-PORTFOLIO-ID`; not found → `'Portfolio not found for update'`, error routine, exit. Otherwise `PORT-TOTAL-UNITS := PORT-TOTAL-UNITS + TRN-QUANTITY` and `PORT-TOTAL-COST := PORT-TOTAL-COST + TRN-AMOUNT`, then `REWRITE`; rewrite failure → `'Error updating portfolio'`. | `PORTTRAN.cbl:182-199` |
| BR-28 | **Sell**: re-read portfolio; not found → error/exit. **Insufficient-units check**: if `PORT-TOTAL-UNITS < TRN-QUANTITY` → `'Insufficient units for sale'`, error routine, exit (no partial fill). Otherwise `PORT-TOTAL-UNITS -= TRN-QUANTITY` and `PORT-TOTAL-COST -= TRN-AMOUNT`, then `REWRITE`. | `PORTTRAN.cbl:201-224` |
| BR-29 | Sell reduces cost by the **sale proceeds (`TRN-AMOUNT`)**, not by a proportional/average cost basis; no realised gain/loss is computed and the cost total can go negative. (Contradicts data-dictionary §5.2 — see BR-36/37.) | `PORTTRAN.cbl:216-217` |
| BR-30 | **Transfer** (`TR`) is a hard stop: `'Transfer processing not implemented'` → error routine. No balance changes. | `PORTTRAN.cbl:226-229` |
| BR-31 | **Fee**: re-read portfolio; not found → `'Portfolio not found for fee'`, exit. `PORT-TOTAL-COST -= TRN-AMOUNT`; units unchanged; `REWRITE`. | `PORTTRAN.cbl:231-247` |
| BR-32 | Arithmetic semantics: all updates use `ADD`/`SUBTRACT` **without `ROUNDED` and without `ON SIZE ERROR`**. Operands are `TRN-QUANTITY PIC S9(11)V9(4) COMP-3` (scale 4) and `TRN-AMOUNT PIC S9(13)V9(2) COMP-3` (scale 2). Result scale is that of the receiving field; excess low-order digits are truncated (never rounded); high-order overflow is silently truncated (Enterprise COBOL, no SIZE ERROR clause). Receiving fields `PORT-TOTAL-UNITS` / `PORT-TOTAL-COST` are undefined in the repo (see §5), so their scale/precision cannot be verified. | `PORTTRAN.cbl:191-192`, `:216-217`, `:240`; `TRNREC.cpy:19-21` |
| BR-33 | **Audit trail** after every update: `AUD-TIMESTAMP = CURRENT-DATE`, program `PORTTRAN`, `AUD-USER-ID = FUNCTION USER-ID`, type `TRAN`; action mapping `BU→CREATE`, `SL→DELETE`, `TR→UPDATE`, `FE→UPDATE`; status `SUCC` if `WS-PORT-STATUS = '00'` else `FAIL`; keys = portfolio-id / account-no; `AUD-BEFORE-IMAGE` receives `PORT-RECORD` **after** the update (truncated to 100 bytes) — there is no true before-image and `AUD-AFTER-IMAGE` is never set; message = `'Transaction: ' type ' Amount: ' amount ' Units: ' quantity` (raw COMP-3 bytes are STRINGed, not edited). | `PORTTRAN.cbl:249-290` |
| BR-34 | The audit record is written via `CALL 'AUDPROC' USING AUDIT-RECORD`; a non-zero `RETURN-CODE` → `'Error writing audit record'` error routine. | `PORTTRAN.cbl:292-300` |
| BR-35 | `PORTTRAN` error routine: `WS-ERROR-COUNT + 1`, category `ERR-CAT-PROC ('PR')`, program `PORTTRAN`, `CALL 'ERRPROC'`. Main loop stops when `WS-ERROR-COUNT > 100`. No `RETURN-CODE` is set (RC 0 regardless of errors). | `PORTTRAN.cbl:60-72`, `:311-316` |

### 2.3 Position-update rules that exist only in documentation — **INFERRED FROM DOCS**

No code implements these. They define what `POSUPD00` was meant to do and are the basis for the
open questions in §7.

| # | Rule | Source |
|---|---|---|
| BR-36 | **INFERRED FROM DOCS** — Share balance must not go negative (a sell exceeding the position is rejected; cf. error `E004 Insufficient Position Balance`). Coded analogue: BR-28 at portfolio level. | data-dictionary §5.2, §6 |
| BR-37 | **INFERRED FROM DOCS** — Cost basis must be updated for every buy/sell; **average cost must be recalculated for buys** (`POS-AVG-COST PIC 9(5)V9999`, `AVG_COST DECIMAL(9,4)` exist only in the documented layouts). The formula (weighted average `cost_basis / share_bal`, rounding mode) is not specified anywhere. | data-dictionary §2.2, §3.1, §5.2 |
| BR-38 | **INFERRED FROM DOCS** — Position status must be **Active** (`'A'`) for a transaction to be applied; `POSREC.cpy` additionally defines `'C'` closed and `'P'` pending but no transition logic exists. | data-dictionary §5.2; `POSREC.cpy:16-19` |
| BR-39 | **INFERRED FROM DOCS** — Position update checkpoints every **500** updates (vs 1000 for history load); minimum checkpoint interval 2 minutes. | data-dictionary §8.3 |
| BR-40 | **INFERRED FROM DOCS** — Job gating: `POSUPD00` runs only if `TRNVAL00` ended with RC ≤ 4; `HISTLD00` runs only if `POSUPD00` RC ≤ 4; `RPTGEN00` follows `HISTLD00` unconditionally. Both S3 steps are restartable. | data-dictionary §8.1, §9.1; system-architecture §4.1; `PRCSEQ.cpy:68-71` |
| BR-41 | **INFERRED FROM DOCS** — `POSUPD00` reads the validated transaction file, writes the Position Master (VSAM, read/write) and appends to Transaction History; `HISTLD00` reads Transaction History and writes DB2 only. Documented `HISTREC` also carries `HIST-BEFORE-BAL` / `HIST-AFTER-BAL` share balances and a `HIST-RESULT-CODE`, implying the history row records the balance before and after each posting. | system-architecture §1.2.2, §5.1.1, §5.2.1; data-dictionary §2.3 |

---

## 3. Data contracts

Java type conventions (from the plan): `BigDecimal` with explicit scale for `V9…`; `LocalDate` for
`9(8)`/`X(8)` `YYYYMMDD`; `LocalTime` for `HHMMSS`; `LocalDateTime`/`Instant` for `X(26)` DB2 timestamps;
enums for Level-88 groups; `String` for `X` (trimmed, max length preserved as a constraint).

### 3.1 `POSREC.cpy` — `POSITION-RECORD` (position master; 138 bytes computed)

Used by `RPTPOS00`, `UTLVAL00`, `INQPORT` (FD on DD `POSMSTRE`); **not used by any S3 program**.

| Field | PIC | Bytes | Semantics | Java type | Maps to |
|---|---|---|---|---|---|
| `POS-PORTFOLIO-ID` (key 1) | `X(08)` | 8 | Portfolio identifier | `String` (len 8) | `INVESTMENT_POSITIONS.PORTFOLIO_ID CHAR(8)` |
| `POS-DATE` (key 2) | `X(08)` | 8 | Position date `YYYYMMDD` | `LocalDate` | `INVESTMENT_POSITIONS.POSITION_DATE DATE` |
| `POS-INVESTMENT-ID` (key 3) | `X(10)` | 10 | Investment / security identifier | `String` (len 10) | `INVESTMENT_POSITIONS.INVESTMENT_ID CHAR(10)` |
| `POS-QUANTITY` | `S9(11)V9(4) COMP-3` | 8 | Holding quantity (units) | `BigDecimal` scale 4, precision 15 | `QUANTITY DECIMAL(18,4)` |
| `POS-COST-BASIS` | `S9(13)V9(2) COMP-3` | 8 | Total cost basis | `BigDecimal` scale 2, precision 15 | `COST_BASIS DECIMAL(18,2)` |
| `POS-MARKET-VALUE` | `S9(13)V9(2) COMP-3` | 8 | Current market value | `BigDecimal` scale 2, precision 15 | `MARKET_VALUE DECIMAL(18,2)` |
| `POS-CURRENCY` | `X(03)` | 3 | ISO currency | `String` (len 3) / `java.util.Currency` | `CURRENCY_CODE CHAR(3)` |
| `POS-STATUS` | `X(01)` 88s `A`/`C`/`P` | 1 | Active / Closed / Pending | `enum PositionStatus { ACTIVE('A'), CLOSED('C'), PENDING('P') }` | **no column** in `INVESTMENT_POSITIONS` (gap) |
| `POS-LAST-MAINT-DATE` | `X(26)` | 26 | DB2-style timestamp | `LocalDateTime` | `LAST_MAINT_DATE TIMESTAMP` |
| `POS-LAST-MAINT-USER` | `X(08)` | 8 | Last maintaining user | `String` (len 8) | `LAST_MAINT_USER VARCHAR(8)` |
| `POS-FILLER` | `X(50)` | 50 | unused | — | — |

Key = 26 bytes (`vsam-definitions.txt:55` says 18 — see §5). Documented `POSMSTRE` fields with **no
code counterpart**: `POS-ACCOUNT-NO`, `POS-FUND-ID`, `POS-CUSIP`, `POS-SHARE-BAL`, `POS-AVG-COST`,
`POS-LAST-DATE`, `POS-LAST-TRANS`.

### 3.2 `HISTREC.cpy` — `HISTORY-RECORD` (917 bytes computed)

Copied into `HISTLD00` FD for DD `TRANHIST`, **but `HISTLD00` never references any `HIST-*` field**
(it references `TH-*` names, see §5).

| Field | PIC | Bytes | Semantics | Java type | Maps to |
|---|---|---|---|---|---|
| `HIST-PORTFOLIO-ID` (key) | `X(08)` | 8 | Portfolio | `String` (8) | `POSHIST.PORTFOLIO_ID` (CHAR(10) — width mismatch) |
| `HIST-DATE` (key) | `X(08)` | 8 | `YYYYMMDD` | `LocalDate` | `POSHIST.TRANS_DATE` |
| `HIST-TIME` (key) | `X(06)` | 6 | `HHMMSS` | `LocalTime` | `POSHIST.TRANS_TIME` |
| `HIST-SEQ-NO` (key) | `X(04)` | 4 | Sequence within second | `String`/`int` | none (POSHIST PK has no sequence → BR-16 collisions) |
| `HIST-RECORD-TYPE` | `X(02)` 88s `PT`/`PS`/`TR` | 2 | Portfolio / Position / Transaction image | `enum HistoryRecordType { PORTFOLIO("PT"), POSITION("PS"), TRANSACTION("TR") }` | none |
| `HIST-ACTION-CODE` | `X(01)` 88s `A`/`C`/`D` | 1 | Add / Change / Delete | `enum HistoryAction { ADD, CHANGE, DELETE }` | none |
| `HIST-BEFORE-IMAGE` | `X(400)` | 400 | Raw record image before | `byte[]`/`String` (or typed snapshot) | none |
| `HIST-AFTER-IMAGE` | `X(400)` | 400 | Raw record image after | `byte[]`/`String` | none |
| `HIST-REASON-CODE` | `X(04)` | 4 | Reason for change | `String` (4) | none |
| `HIST-PROCESS-DATE` | `X(26)` | 26 | Timestamp | `LocalDateTime` | `POSHIST.PROCESS_DATE`+`PROCESS_TIME` |
| `HIST-PROCESS-USER` | `X(08)` | 8 | User | `String` (8) | `POSHIST.USER_ID` |
| `HIST-FILLER` | `X(50)` | 50 | unused | — | — |

### 3.3 `DBTBLS.cpy` `POSHIST-RECORD` ↔ `POSHIST.sql` (the contract `HISTLD00` actually writes)

| Host field | PIC | Column | DDL type | Set by HISTLD00? | Java type |
|---|---|---|---|---|---|
| `PH-ACCOUNT-NO` | `X(8)` | `ACCOUNT_NO` (PK1) | `CHAR(8)` | yes | `String` (8) |
| `PH-PORTFOLIO-ID` | `X(10)` | `PORTFOLIO_ID` (PK2) | `CHAR(10)` | yes | `String` (10) |
| `PH-TRANS-DATE` | `X(10)` | `TRANS_DATE` (PK3, partition key) | `DATE` | yes | `LocalDate` |
| `PH-TRANS-TIME` | `X(8)` | `TRANS_TIME` (PK4) | `TIME` | yes | `LocalTime` |
| `PH-TRANS-TYPE` | `X(2)` | `TRANS_TYPE` | `CHAR(2)` | yes | `enum TransactionType { BU, SL, TR, FE }` (DDL comment lists only BU/SL/TR) |
| `PH-SECURITY-ID` | `X(12)` | `SECURITY_ID` | `CHAR(12)` | yes | `String` (12) |
| `PH-QUANTITY` | `S9(12)V9(3) COMP-3` | `QUANTITY` | `DECIMAL(15,3)` | yes | `BigDecimal` scale 3 |
| `PH-PRICE` | `S9(12)V9(3) COMP-3` | `PRICE` | `DECIMAL(15,3)` | yes | `BigDecimal` scale 3 |
| `PH-AMOUNT` | `S9(13)V9(2) COMP-3` | `AMOUNT` | `DECIMAL(15,2)` | yes | `BigDecimal` scale 2 |
| `PH-FEES` | `S9(13)V9(2) COMP-3` | `FEES` | `DECIMAL(15,2) DEFAULT 0` | yes | `BigDecimal` scale 2 |
| `PH-TOTAL-AMOUNT` | `S9(13)V9(2) COMP-3` | `TOTAL_AMOUNT` | `DECIMAL(15,2)` | yes | `BigDecimal` scale 2 |
| `PH-COST-BASIS` | `S9(13)V9(2) COMP-3` | `COST_BASIS` | `DECIMAL(15,2)` | yes | `BigDecimal` scale 2 |
| `PH-GAIN-LOSS` | `S9(13)V9(2) COMP-3` | `GAIN_LOSS` | `DECIMAL(15,2)` | yes | `BigDecimal` scale 2 |
| `PH-PROCESS-DATE` | `X(10)` | `PROCESS_DATE` | `DATE NOT NULL` | **no** (BR-13) | `LocalDate` |
| `PH-PROCESS-TIME` | `X(8)` | `PROCESS_TIME` | `TIME NOT NULL` | **no** | `LocalTime` |
| `PH-PROGRAM-ID` | `X(8)` | `PROGRAM_ID` | `CHAR(8)` | **no** | `String` |
| `PH-USER-ID` | `X(8)` | `USER_ID` | `CHAR(8)` | **no** | `String` |
| `PH-AUDIT-TIMESTAMP` | `X(26)` | `AUDIT_TIMESTAMP` | `TIMESTAMP WITH DEFAULT` | **no** | `Instant` |

Indexes: `POSHIST_PK` (clustered, PK), `POSHIST_IX1 (SECURITY_ID, TRANS_DATE)`,
`POSHIST_IX2 (PROCESS_DATE, PROGRAM_ID)`. Range-partitioned quarterly on `TRANS_DATE` for 2024 only
(`POSHIST.sql:19-23`). Grants: `POSAPP` SELECT/INSERT (no UPDATE/DELETE → append-only ledger), `POSRPT` SELECT.

### 3.4 `INVESTMENT_POSITIONS` (`db2-definitions.sql:29-41`)

| Column | Type | Java | Notes |
|---|---|---|---|
| `PORTFOLIO_ID` | `CHAR(8)` PK, FK→`PORTFOLIO_MASTER` | `String` | |
| `INVESTMENT_ID` | `CHAR(10)` PK | `String` | |
| `POSITION_DATE` | `DATE` PK | `LocalDate` | daily snapshot key; view `CURRENT_POSITIONS` selects `POSITION_DATE = CURRENT DATE − 1 DAY` |
| `QUANTITY` | `DECIMAL(18,4)` | `BigDecimal` scale 4 | |
| `COST_BASIS` | `DECIMAL(18,2)` | `BigDecimal` scale 2 | |
| `MARKET_VALUE` | `DECIMAL(18,2)` | `BigDecimal` scale 2 | |
| `CURRENCY_CODE` | `CHAR(3)` | `String` | |
| `LAST_MAINT_DATE` | `TIMESTAMP` | `LocalDateTime` | |
| `LAST_MAINT_USER` | `VARCHAR(8)` | `String` | |

Index `IDX_POSITIONS_DATE (POSITION_DATE, PORTFOLIO_ID)`. No `STATUS`, `AVG_COST`, `ACCOUNT_NO` columns.

### 3.5 VSAM clusters (`vsam-definitions.txt`)

| File | Org | RECLEN | Key | Documented key structure |
|---|---|---|---|---|
| `TRANHIST` | KSDS | 300 | 20 @1 | date(8)+time(6)+portfolio(8)+seq(6) = **28** (≠ 20) |
| `POSHIST` (VSAM) | KSDS | 350 | 18 @1 | portfolio(8)+date(8)+investment(10) = **26** (≠ 18) |

Only `PORTMSTR` has an `IDCAMS DEFINE CLUSTER`; `TRANHIST`/`POSHIST` are "not shown". Data-dictionary
calls the position master `POSMSTRE` (KSDS, key `ACCOUNT-NO+FUND-ID`, 250 bytes) and `TRANHIST` an
**ESDS**; neither appears in `vsam-definitions.txt`.

### 3.6 Batch-control fields touched by HISTLD00 (`BCHCTL.cpy`, owned by S6)

`BCT-KEY` = `BCT-JOB-NAME X(8)` + `BCT-PROCESS-DATE X(8)` + `BCT-SEQUENCE-NO 9(4)`; `BCT-STATUS X(1)`
(`R/A/W/D/E`). `BCT-RECORDS-READ` / `BCT-RECORDS-WRITTEN` referenced by `HISTLD00.cbl:192-193` **do not
exist** in the copybook.

---

## 4. Interfaces

### 4.1 Inbound

| Legacy interface | Direction / mechanism | Detail | Java target (per plan) |
|---|---|---|---|
| JCL job step `HISTLD00` (no JCL in repo; sequence `TRNVAL00→POSUPD00→HISTLD00` in `PRCSEQ.cpy:68-71`) | Scheduler → program | Nightly 1900–1930, gated on `POSUPD00` RC ≤ 4 | **S6 batch-orchestrator** launches Spring Batch job `positionHistoryLoadJob` (REST trigger or Spring Cloud Task) |
| JCL job step `POSUPD00`/`POSUPDT` (program empty) | Scheduler → program | Nightly 1815–1900, gated on `TRNVAL00` RC ≤ 4 | Replaced by **event consumption**: S2 `TransactionPosted` → S3 `PositionUpdateListener`; a batch replay job remains for the S6 DAG |
| DD `TRANHIST` (KSDS, `HISTREC`) | File read, sequential | Whole file every run | Retired: content arrives as `TransactionPosted` events / `TRANSACTION_HISTORY` read (S2) |
| DD `TRANFILE` (`TRNREC`) — input of the intended `POSUPDT` | File read | Validated transactions | S2 owns ingestion; S3 receives events |
| DD `PORTFILE` (`PORTREC`/`PORTFLIO`) — read by `PORTTRAN` 2210–2240 | KSDS random read + REWRITE | Portfolio existence check and totals update | Existence check → **REST call to S1 portfolio-service** (`GET /portfolios/{id}`) or cached replica; totals update becomes a `PositionChanged` event S1 may consume |
| DD `BCHCTL` (`BCHCTL.cpy`) | KSDS read/REWRITE | Status → `A`, read/written counters | **S6** Spring Batch `JobRepository` / `StepExecution` counters (library call) |
| Online read of positions (`INQPORT` FD `POSMSTRE`, `INQHIST` SQL on `POSHIST`) | S4 reads S3 data directly | `SELECT … FROM POSHIST WHERE ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC` (`INQHIST.cbl:130-134`) | **REST** exposed by S3: `GET /positions?portfolioId=…&asOf=…`, `GET /positions/{portfolioId}/history?accountNo=…` (paginated) |

### 4.2 Outbound

| Legacy interface | Mechanism | Detail | Java target |
|---|---|---|---|
| `EXEC SQL CONNECT TO POSMVP` / `CONNECT RESET` (`DBPROC`) | DB2 session | | Spring `DataSource` + `@Transactional`; programs `DB2CONN/DB2CMT` retired (S8) |
| `INSERT INTO POSHIST VALUES (:POSHIST-RECORD)` | SQL write | 18 columns, PK dedupe on −803 | **DB write** via `PositionHistoryRepository.save` with `ON CONFLICT DO NOTHING` semantics |
| `COMMIT WORK` every 1000 / `ROLLBACK WORK` on error | SQL tx control | | Spring Batch chunk size 1000 (`commit-interval`), skip policy for duplicate-key |
| `CALL 'ERRPROC' USING ERR-MESSAGE` (`HISTLD00.cbl:228`, `PORTTRAN.cbl:316`, `DBPROC.cpy:56`) | Static CALL | Error logging to `ERRLOG` | **Library call** — S8 `platform-common` exception hierarchy / `ErrorLogger` |
| `CALL 'AUDPROC' USING AUDIT-RECORD` (`PORTTRAN.cbl:294`) | Static CALL | Audit trail | **Library call** — S8 audit event publisher (`AuditEvent`) |
| Position master writes (intended `POSUPDT` → `POSMSTRE`; coded proxy = `REWRITE PORTFOLIO-RECORD`) | VSAM REWRITE | | **DB write** to `INVESTMENT_POSITIONS` + **event** `PositionChanged` to S7 (and S1 if it keeps portfolio totals) |
| `DISPLAY` statistics | SYSOUT | | Spring Batch `StepExecution` metrics + Micrometer counters (`records.read`, `records.written`, `records.duplicate`, `records.error`) |
| `RETURN-CODE` = error count | JCL condition code | | Job `ExitStatus` mapped to the S6 RC convention (see OQ-6) |

### 4.3 CICS
No `EXEC CICS` statements exist in any S3 source. S3 is batch-only in the legacy system; the online
read side is S4.

---

## 5. Discrepancies (code vs documentation vs schema)

| # | Discrepancy | Evidence |
|---|---|---|
| D-01 | **`POSUPDT.cbl` is an empty file (0 bytes).** Docs describe it (as `POSUPDT`/`POSUPD00`) as the core position-update program: "Updates position records, maintains cost basis, records transaction history". | `src/programs/batch/POSUPDT.cbl`; system-architecture §1.2.2, §5.1.1; data-dictionary §8.1 |
| D-02 | Program naming: docs use `POSUPD00`, `TRNVAL00`, `TRNMAIN`, `RPTGEN00`; files are `POSUPDT.cbl`, `PORTTRAN.cbl`, `RPTPOS00.cbl`; `TRNVAL00`/`RPTGEN00` do not exist as programs. `PRCSEQ.cpy` hard-codes `TRNVAL00/POSUPD00/HISTLD00`. | `PRCSEQ.cpy:68-71`; README:89 |
| D-03 | **`HISTLD00` does not compile against its own copybook**: FD copies `HISTREC` (fields `HIST-*`) but the program references `TH-KEY` (record key) and 13 `TH-*` fields that are defined nowhere in the repo. `HISTREC` has no account-no, security-id, quantity, price, amount, fees, total, cost-basis or gain-loss fields. | `HISTLD00.cbl:21`, `:34`, `:146-158`; `HISTREC.cpy` |
| D-04 | `HISTLD00` writes `BCT-RECORDS-READ` / `BCT-RECORDS-WRITTEN`, which do not exist in `BCHCTL.cpy`. | `HISTLD00.cbl:192-193`; `BCHCTL.cpy:9-39` |
| D-05 | `HISTLD00` leaves `PROCESS_DATE`, `PROCESS_TIME`, `PROGRAM_ID`, `USER_ID` unpopulated although they are `NOT NULL` without default; on real DB2 every insert would fail (BR-13). | `HISTLD00.cbl:144-158`; `POSHIST.sql:39-42` |
| D-06 | `POSHIST` schema differs between data-dictionary §3.1 (`ACCOUNT_NO DECIMAL(9,0)`, `FUND_ID CHAR(6)`, `SHARE_BAL DECIMAL(14,3)`, `COST_BASIS DECIMAL(13,2)`, `AVG_COST DECIMAL(9,4)`, `PROC_TIMESTAMP`; PK `ACCOUNT_NO, FUND_ID, TRANS_DATE`) and the actual DDL (18 columns, `ACCOUNT_NO CHAR(8)`, `PORTFOLIO_ID CHAR(10)`, `SECURITY_ID CHAR(12)`, per-transaction amounts; PK adds `TRANS_TIME`). The dictionary describes a *balance snapshot* table; the DDL is a *transaction ledger*. | data-dictionary §3.1; `POSHIST.sql:25-57` |
| D-07 | `INQHIST` (S4) selects columns `TRANS_UNITS`, `TRANS_PRICE`, `TRANS_AMOUNT` from `POSHIST`; the DDL has `QUANTITY`, `PRICE`, `AMOUNT`. Queries by `ACCOUNT_NO` only, which is not a leading-column-only index. | `INQHIST.cbl:130-134`; `POSHIST.sql:26-43` |
| D-08 | Position master layout: data-dictionary §2.2 (`POSMSTRE`, key `ACCOUNT-NO 9(9)+FUND-ID X(6)`, 250 bytes, fields `CUSIP`, `SHARE-BAL S9(11)V999`, `AVG-COST 9(5)V9999`, `COST-BASIS S9(11)V99`, `LAST-DATE`, `LAST-TRANS`, status A/C) vs `POSREC.cpy` (key `PORTFOLIO-ID+DATE+INVESTMENT-ID`, 138 bytes, `QUANTITY S9(11)V9(4)`, `COST-BASIS S9(13)V99`, `MARKET-VALUE`, `CURRENCY`, status A/C/P, `LAST-MAINT-DATE/USER`). No average-cost field exists in code. | data-dictionary §2.2; `POSREC.cpy` |
| D-09 | `POSREC` vs `INVESTMENT_POSITIONS`: table lacks `STATUS`; precision differs (`S9(11)V9(4)` = 15 digits vs `DECIMAL(18,4)`; `S9(13)V99` vs `DECIMAL(18,2)`). | `POSREC.cpy:12-16`; `db2-definitions.sql:33-35` |
| D-10 | History record layout: data-dictionary §2.3 (`TRANHIST` **ESDS**, 300 bytes, timestamp/account/fund/trans-id/type/qty/price/amount/result-code/before-bal/after-bal) vs `HISTREC.cpy` (keyed 917-byte before/after **raw image** record). `HISTLD00` declares `TRANHIST` as **INDEXED** (KSDS), contradicting the dictionary's ESDS. | data-dictionary §2.3; `HISTREC.cpy`; `HISTLD00.cbl:19` |
| D-11 | VSAM definitions: `POSHIST` KSDS key length 18 vs documented 26-byte key structure; `TRANHIST` key length 20 vs documented 28-byte structure; record lengths 350/300 vs copybook sizes 138 (`POSREC`) / 917 (`HISTREC`). `POSMSTRE` (the DD used by `RPTPOS00`, `UTLVAL00`, `INQPORT`, JCL) is not defined at all; a VSAM file named `POSHIST` is defined although `POSHIST` is a DB2 table in code. | `vsam-definitions.txt:28-66`; `RPTPOS.jcl:8-9` |
| D-12 | `PORTTRAN` copies `PORTREC`, which does not exist (`PORTFLIO.cpy` is the portfolio copybook) and uses `PORT-TOTAL-UNITS`/`PORT-TOTAL-COST`, absent from `PORTFLIO` (which has `PORT-TOTAL-VALUE`, `PORT-CASH-BALANCE`). | `PORTTRAN.cbl:40`, `:191-192`; `PORTFLIO.cpy` |
| D-13 | `PORTTRAN` never performs `2200-UPDATE-POSITIONS` (dead code) — the "position update" of the transaction flow is not executed anywhere. | `PORTTRAN.cbl:92-118`, `:167` |
| D-14 | Return-code convention: docs/`BCHCON` define RC 0/4/8/12/16 and gate `HISTLD00` on RC ≤ 4; `HISTLD00` returns the raw error count and `PORTTRAN` never sets RC. `BCT-STATUS` is never moved to `D`/`E` by `HISTLD00`. | `HISTLD00.cbl:76`; `BCHCON.cpy:17-21`; data-dictionary §8.2 |
| D-15 | Checkpoint/restart: docs describe restart from last checkpoint (`CKPRST`, "Restart: Yes"); `HISTLD00` stores only counters, never a key, and always restarts from record 1 — idempotency relies solely on the −803 skip. | system-architecture §4.2; `HISTLD00.cbl:191-200` |
| D-16 | Transaction-type domains: `TRNREC`/`db2-definitions` = `BU/SL/TR/FE`; data-dictionary §2.1 = `BY/SL/FE`; `POSHIST.sql` column comment = `BU/SL/TR` (no `FE`). | `TRNREC.cpy:15-18`; data-dictionary:57-59; `POSHIST.sql:80-81` |
| D-17 | Development backlog marks `HISTLD00` "Deadlock/timeout handling", "Bulk insert processing", "Logging to ERRLOG" as complete; the code has single-row inserts, no −911/−913 retry (the `DB2-RETRY-*` fields of `DBPROC` are never used), and error logging only indirectly via `ERRPROC`. | development-backlog.md:248-272; `HISTLD00.cbl:160-174` |
| D-18 | Audit before/after image: `PORTTRAN` stores the post-update record in `AUD-BEFORE-IMAGE` and never sets `AUD-AFTER-IMAGE`; `HISTREC` before/after images are never populated by any program. | `PORTTRAN.cbl:277-278`; `AUDITLOG.cpy:34-35` |

Missing or stub programs relevant to S3: **`POSUPDT.cbl` (empty)**, **`POSUPD00` (documented name, no file)**,
**`TRNVAL00` (upstream dependency, no file)**, **`RPTGEN00` (downstream dependency, no file)**; missing copybook
**`PORTREC`**; undefined host fields **`TH-*`** used by `HISTLD00`.

---

## 6. Proposed Java design

Module `services/position-service` (Gradle), depends on `libs/domain-model` (F1) and
`libs/platform-common` (F2). Persistence: Postgres via Spring Data JPA + Flyway. Messaging: Spring Cloud
Stream (Kafka or RabbitMQ per F3). Batch: Spring Batch.

### 6.1 Package layout — `com.cobolbench.position`

```
com.cobolbench.position
├── api                     REST controllers + DTOs (read side for S4)
│   ├── PositionController          GET /positions, GET /positions/{portfolioId}/{investmentId}
│   ├── PositionHistoryController   GET /positions/{portfolioId}/history  (paginated, replaces CURSMGR)
│   └── dto                         PositionDto, PositionHistoryDto, PageResponse
├── domain                  pure domain model + rules (no Spring)
│   ├── Position                    (portfolioId, investmentId, positionDate, quantity, costBasis,
│   │                                marketValue, currency, status, lastMaint*)
│   ├── PositionStatus              enum A/C/P  (POSREC 88s)
│   ├── PositionHistoryEntry        18 fields of POSHIST
│   ├── TransactionType             enum BU/SL/TR/FE
│   ├── PositionArithmetic          BR-27/28/29/31/32 (BigDecimal, RoundingMode.DOWN, scales 4/2,
│   │                                overflow -> exception instead of COBOL silent truncation)
│   └── InsufficientUnitsException, TransferNotSupportedException, PortfolioNotFoundException
├── application             use-cases / transactional services
│   ├── PositionUpdateService       apply(TransactionPosted) -> PositionChanged
│   ├── PositionHistoryService      record(entry) with duplicate-key = no-op (BR-16)
│   └── PositionQueryService
├── infrastructure
│   ├── persistence
│   │   ├── PositionEntity, PositionHistoryEntity (JPA)
│   │   ├── PositionRepository, PositionHistoryRepository (Spring Data)
│   │   └── V1__investment_positions.sql, V2__poshist.sql (Flyway; from F1)
│   ├── messaging
│   │   ├── TransactionPostedListener   (S2 -> S3 inbound event)
│   │   └── PositionChangedPublisher    (S3 -> S7/S1 outbound event)
│   ├── client
│   │   └── PortfolioClient             (REST to S1, replaces PORTFILE READ; BR-27/28/31 existence check)
│   └── batch
│       ├── PositionHistoryLoadJobConfig  (replaces HISTLD00; chunk=1000)
│       ├── PositionUpdateReplayJobConfig (replaces POSUPD00 for S6 DAG; chunk=500, BR-39)
│       ├── HistoryLoadSkipPolicy         (DuplicateKeyException -> skip, count `records.duplicate`)
│       └── ErrorThresholdListener        (stop step after 101 errors, BR-04)
└── config
    ├── PositionServiceProperties   (@ConfigurationProperties: commit intervals, error threshold,
    │                                s1 base-url, topic names)
    └── ObservabilityConfig         (Micrometer counters replacing DISPLAY stats / DB2STAT)
```

### 6.2 Spring components and how they realise the rules

| Component | Legacy analogue | Rules covered |
|---|---|---|
| `TransactionPostedListener` → `PositionUpdateService` | intended `POSUPDT`; `PORTTRAN 2200-2240` | BR-25 (pre-validated by S2), BR-26–BR-32, BR-36–BR-38 (once OQ-1/2 answered) |
| `PositionArithmetic` | `ADD`/`SUBTRACT` COMP-3 | BR-32: `quantity.setScale(4, DOWN)`, `money.setScale(2, DOWN)`; precision guard 15 digits → `ArithmeticException` mapped to a rejected event (replaces silent truncation) |
| `PositionHistoryService` + `PositionHistoryRepository` (`INSERT … ON CONFLICT DO NOTHING`) | `2200-LOAD-TO-DB2` | BR-11–BR-17; populates `PROCESS_DATE/TIME/PROGRAM_ID/USER_ID/AUDIT_TIMESTAMP` (fixes D-05) |
| `PositionHistoryLoadJobConfig` (Spring Batch: `ItemReader` over `TRANSACTION_HISTORY`/file, `ItemWriter` → repository; `commit-interval=1000`; `ErrorThresholdListener`) | `HISTLD00` | BR-01–BR-05, BR-10, BR-18, BR-20–BR-22; restart via Spring Batch `ExecutionContext` (fixes D-15) |
| `PositionUpdateReplayJobConfig` (`commit-interval=500`) | `POSUPD00` in S6 DAG | BR-39, BR-40 |
| S8 `ErrorLogger` / exception translation | `ERRPROC`, `DB2-ERROR-ROUTINE`, `9000-ERROR-ROUTINE` | BR-07, BR-17, BR-23, BR-35 |
| S8 `AuditEventPublisher` | `AUDPROC`, `2300-UPDATE-AUDIT-TRAIL` | BR-33, BR-34 (with real before **and** after images, fixing D-18) |
| `PositionController`, `PositionHistoryController` | `INQPORT` FD read, `INQHIST` SQL | replaces direct table access from S4 (D-07) |
| `PortfolioClient` (WebClient/RestClient, circuit-breaker) | `READ PORTFOLIO-FILE` | BR-27/28/31 "Portfolio not found" path |

### 6.3 Entities

- `PositionEntity` — `@IdClass(PositionId{portfolioId, investmentId, positionDate})`; columns per §3.4 plus
  `status CHAR(1)` (new, from `POSREC`, resolves D-09) and optional `avg_cost DECIMAL(9,4)` (pending OQ-2).
- `PositionHistoryEntity` — `@IdClass(PositionHistoryId{accountNo, portfolioId, transDate, transTime})`;
  18 columns per §3.3; `@Immutable` (append-only, mirrors GRANT SELECT/INSERT only). Add
  `sequence_no` to the key if OQ-4 decides same-second transactions must not collide.

### 6.4 Events

- Inbound `TransactionPosted` (from S2): `transactionId, portfolioId, accountNo, investmentId, type(BU/SL/TR/FE),
  quantity(scale 4), price(scale 4), amount(scale 2), fees(scale 2), currency, transDate, transTime, sequenceNo,
  processUser`. Consumer is idempotent on `transactionId`.
- Outbound `PositionChanged` (to S7, optionally S1): `portfolioId, investmentId, positionDate, before{quantity,
  costBasis}, after{quantity, costBasis, marketValue}, status, cause = transactionId, realizedGainLoss`.
- Outbound `PositionUpdateRejected`: `transactionId, reasonCode ∈ {PORTFOLIO_NOT_FOUND, INSUFFICIENT_UNITS,
  TRANSFER_NOT_SUPPORTED, POSITION_NOT_ACTIVE, ARITHMETIC_OVERFLOW}` (from BR-27/28/30/38/32) — replaces
  `ERRPROC` messages `'Portfolio not found for update'`, `'Insufficient units for sale'`,
  `'Transfer processing not implemented'`.

### 6.5 Configuration (`application.yml`, bound to `PositionServiceProperties`)

```
position:
  history-load:
    commit-interval: 1000        # BR-18
    error-threshold: 100         # BR-04 (stop when errors > threshold)
    skip-duplicates: true        # BR-16
  position-update:
    commit-interval: 500         # BR-39 (INFERRED FROM DOCS)
    require-active-status: true  # BR-38 (INFERRED FROM DOCS)
    money-scale: 2
    quantity-scale: 4
    rounding-mode: DOWN          # COBOL truncation semantics, BR-32
  portfolio-service:
    base-url: http://portfolio-service:8080
  messaging:
    inbound-topic: transaction.posted
    outbound-topic: position.changed
    rejected-topic: position.update.rejected
```

Exit-status mapping for S6 (replaces BR-05/D-14): `0` no errors, `4` duplicates skipped or warnings only,
`8` one or more record errors but step completed, `12` error threshold reached, `16` infrastructure failure
(DB/broker unavailable) — pending OQ-6.

---

## 7. Open questions (require product / business decision)

| # | Question | Why it matters |
|---|---|---|
| OQ-1 | **What is the authoritative position-update algorithm for `POSUPDT`?** The program is empty. The only coded arithmetic (`PORTTRAN` 2210–2240) updates *portfolio-level* units/cost and on a sell subtracts the **sale proceeds** from cost (BR-29). Docs say cost basis and **average cost** must be maintained per position (BR-37). Should the Java service implement (a) the coded portfolio-total semantics, (b) an average-cost method (weighted average; sell reduces cost basis by `qty × avg_cost` and books realised gain/loss into `POSHIST.GAIN_LOSS`), or (c) another lot method (FIFO/specific-lot)? | Determines core domain logic and `GAIN_LOSS`/`COST_BASIS` values in `POSHIST`. |
| OQ-2 | Should `AVG_COST DECIMAL(9,4)` (documented, not in DDL) be added to `INVESTMENT_POSITIONS`, and what rounding applies to `cost_basis / quantity` (HALF_UP vs truncation)? | Schema (F1) and decimal fidelity (plan risk 2). |
| OQ-3 | Is `INVESTMENT_POSITIONS` a **daily snapshot** table (one row per `POSITION_DATE`, as the PK and `CURRENT_POSITIONS` view imply) or a current-state table? If snapshot: is the new day's row cloned from the prior day at start-of-day, and does `POSITION_DATE` = transaction date or processing date? | Drives entity key, replay semantics and the S7 report contract. |
| OQ-4 | `POSHIST` PK is `(ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)` with no sequence number, and `HISTLD00` silently drops −803 duplicates (BR-16). Two transactions for the same account/portfolio in the same second are lost. Is this acceptable, or should the key include `SEQ-NO`/`TRANSACTION_ID`? | Data-loss risk vs. backward compatibility of the ledger key. |
| OQ-5 | Transfers (`TR`): `PORTTRAN` rejects them as not implemented (BR-30) while validation accepts them with zero price/amount (BR-25). Should S3 implement transfers (between portfolios? between investments?) and what are the cost-basis rules? | Feature scope for Phase 2. |
| OQ-6 | Return-code convention: keep `HISTLD00`'s "RC = error count" (BR-05) for S6 parity, or adopt the documented 0/4/8/12/16 scale (proposal in §6.5)? Should VSAM/file errors (currently invisible to RC, BR-23) count as errors? | S6 DAG gating (`RC ≤ 4`) and I3 parity tests. |
| OQ-7 | Position status lifecycle: `POSREC` has A/C/P but no transitions are coded. When does a position become **Closed** (quantity reaches zero?) or **Pending** (unsettled trades?), and may transactions post to a Pending position? | BR-38 implementation. |
| OQ-8 | Which `POSHIST` layout is canonical — the DDL ledger (per-transaction amounts, fees, gain/loss) or the data-dictionary balance snapshot (`SHARE_BAL`, `AVG_COST`)? `INQHIST` expects yet another column set (D-06, D-07). | Contract with S4 and S7; F1 reconciliation. |
| OQ-9 | Should `PositionChanged` also update portfolio-level totals in S1 (the behaviour `PORTTRAN` 2210–2240 actually codes), or are portfolio totals derived by S1/S7 from positions? | Cross-service ownership of `PORT-TOTAL-*`. |
| OQ-10 | Fee semantics: `PORTTRAN` subtracts a fee from **cost** (BR-31), which lowers cost basis and inflates future gains; `POSHIST` carries `FEES` and `TOTAL_AMOUNT` separately. Should fees reduce cost basis, increase it (capitalised), or be expensed outside positions? | Financial correctness. |
| OQ-11 | Restartability: `HISTLD00` always reprocesses the whole file and relies on duplicate-skip (D-15). Is full-file idempotent reload acceptable for the Java batch, or must restart resume from the last committed chunk (Spring Batch default)? | Run-time and S6 checkpoint contract. |
| OQ-12 | `POSHIST` is range-partitioned for calendar 2024 only. What is the retention/partitioning policy going forward (yearly partitions, archival by `UTLMNT00`/S9)? | Ops design, S9 scope. |

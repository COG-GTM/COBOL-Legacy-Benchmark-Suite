# S2 — transaction-service: Modernization Specification (Phase 0)

| | |
|---|---|
| Component | **S2 transaction-service** (per `cobol-java-modernization-plan.md` §2) |
| COBOL sources analyzed | `src/programs/portfolio/PORTTRAN.cbl` (316 lines), `src/copybook/common/TRNREC.cpy`, `src/copybook/common/RETHND.cpy`, `src/database/db2/db2-definitions.sql` (`TRANSACTION_HISTORY`) |
| Supporting copybooks read for context | `src/copybook/common/PORTFLIO.cpy`, `ERRHAND.cpy`, `AUDITLOG.cpy`; `src/programs/common/ERRPROC.cbl` / `AUDPROC.cbl` (LINKAGE only); `src/copybook/batch/PRCSEQ.cpy` (process-sequence constants) |
| Documentation read | `documentation/technical/system-architecture.md` §1.1, §1.2.2, §2.2; `documentation/technical/data-dictionary.md` §1, §2.1, §2.3, §5, §6, §8, §9 |
| Programs referenced by docs but **absent from repo** | `TRNMAIN`, `TRNVAL00`, `POSUPD00`, `RPTGEN00`; `POSUPDT.cbl` exists but is **empty (0 lines)**; copybook `PORTREC` (COPYed by `PORTTRAN` line 40) does not exist |

Legend used throughout:

* **[CODE]** — rule is implemented in `PORTTRAN.cbl` and reachable at run time.
* **[CODE — DEAD]** — rule is implemented in `PORTTRAN.cbl` but the paragraph is never `PERFORM`ed (see BR-12); it documents the author's evident intent, not the executed behavior.
* **INFERRED FROM DOCS** — rule exists only in `documentation/technical/*.md` (or in DDL/copybook comments); there is no code for it in the repo.

No rule in this document was invented; every rule cites its source line(s).

---

## 1. Purpose & scope

The transaction-service is the modern replacement for the nightly **transaction intake and validation** step of the Investment Portfolio Management System. In the legacy system a fixed-block sequential file (`TRANFILE`, DDNAME on `PORTTRAN.cbl:18`) containing buy/sell/transfer/fee instructions is read once per batch cycle; every record is checked against the portfolio master and against basic field-level rules, valid records are counted as "processed", invalid ones are logged through the shared error handler `ERRPROC`, and the run stops early if more than 100 errors accumulate.

Business intent as described by the System Architecture Document (SAD §1.2.2 "TRNMAIN (TRNVAL00)"): *validate input transactions, perform initial error checking, prepare transactions for processing*, then hand valid transactions to the position-update step (`POSUPDT`/`POSUPD00`, which is S3 position-service in the target plan) and to the history load (`HISTLD00`).

In the target architecture S2:

* ingests transaction batches (file upload or stream), validates them, and persists them to `TRANSACTION_HISTORY` (owned table);
* publishes a `TransactionPosted` event per accepted transaction for S3 position-service to consume;
* reports rejected transactions to the platform error log (S8 `platform-common`) and exposes counts/return codes to the S6 batch-orchestrator.

**Out of scope for S2** (but present as dead code in `PORTTRAN`): mutating portfolio balances (`2200`–`2240`, lines 167–247). Per the decomposition plan that logic belongs to S3 position-service; it is documented here (§2.3) because it is the only code in the repo that shows how a transaction was meant to affect a position, and S3 will need the same reading.

---

## 2. Business rules

Rules are numbered `BR-nn`. Each rule cites `PORTTRAN.cbl:<line>` unless another file is given.

### 2.1 Program flow, control and thresholds

| # | Rule | Source | Status |
|---|---|---|---|
| **BR-01** | Program sequence is: `1000-INITIALIZE` → loop `2000-PROCESS-TRANSACTIONS` → `3000-TERMINATE` → `GOBACK`. There is no restart/checkpoint logic and no `RETURN-CODE` is set by the program itself. | `PORTTRAN.cbl:60-72` | [CODE] |
| **BR-02** | The processing loop runs **only if the transaction file opened successfully** (`WS-TRAN-STATUS = '00'`). A failure to open the *portfolio* file does **not** suppress the loop (see BR-05 and Discrepancy D-10). | `PORTTRAN.cbl:63-67` | [CODE] |
| **BR-03** | **Error-abort threshold**: the loop terminates when end-of-file is reached **or** when `WS-ERROR-COUNT > 100`. The condition is evaluated before each record read, so the run stops after the record that produces the **101st** error; the first 100 errors never stop the run. The threshold counts *all* calls to `9000-ERROR-ROUTINE` (file-open errors, validation errors, and — in dead code — update/audit errors alike). | `PORTTRAN.cbl:64-66`, `312` | [CODE] |
| **BR-04** | Initialization zeroes the file-status pair and the three counters (`WS-READ-COUNT`, `WS-PROCESS-COUNT`, `WS-ERROR-COUNT`, all `PIC 9(8) COMP`, i.e. max 99,999,999) and sets `MORE-RECORDS`. | `PORTTRAN.cbl:74-77`, `50-57` | [CODE] |
| **BR-05** | `TRANFILE` is opened `INPUT`; `PORTFILE` (KSDS, random access, key `PORT-ID`) is opened `I-O`. Each open failure (status ≠ `'00'`) produces one error via `9000-ERROR-ROUTINE` with text `Error opening transaction file` / `Error opening portfolio file`. The program does **not** stop after an open failure. | `PORTTRAN.cbl:17-28`, `79-89` | [CODE] |
| **BR-06** | One record is read per loop iteration. `AT END` sets `END-OF-FILE`; otherwise `WS-READ-COUNT` is incremented **before** validation, so `READ-COUNT` = total records in the file (or records consumed until the abort threshold). | `PORTTRAN.cbl:92-100` | [CODE] |
| **BR-07** | Termination closes both files and `DISPLAY`s three counters: `Transactions Read:`, `Transactions Process:`, `Errors Encountered:`. There is no trailer/control-total reconciliation, no output file, and no checkpoint update. | `PORTTRAN.cbl:302-309` | [CODE] |

### 2.2 Validation rules (reachable)

Validation is performed in `2100-VALIDATE-TRANSACTION` as a short-circuit chain: `2110-CHECK-PORTFOLIO` → `2120-CHECK-TRANSACTION-TYPE` → `2130-CHECK-AMOUNTS`. `ERR-TEXT` (from `ERRHAND.cpy`, `PIC X(80)`) is cleared to spaces first and each stage only runs if the previous one left `ERR-TEXT = SPACES`. Therefore **at most one error is reported per transaction record** (the first failing check).

| # | Rule | Source | Status |
|---|---|---|---|
| **BR-08** | **Single-error-per-record semantics**: a record is *valid* iff after all three checks `ERR-TEXT = SPACES`; then `WS-PROCESS-COUNT` is incremented. Otherwise `9000-ERROR-ROUTINE` is called once. A valid record has **no further effect** (see BR-12). | `PORTTRAN.cbl:102-118` | [CODE] |
| **BR-09** | **Portfolio ID required**: `TRN-PORTFOLIO-ID` (`PIC X(08)`) must not be all spaces. Error text: `Portfolio ID is required`. Note: a low-value/zero-filled or numeric-garbage ID is *not* caught here — only spaces. | `PORTTRAN.cbl:121-124` | [CODE] |
| **BR-10** | **Portfolio must exist**: `TRN-PORTFOLIO-ID` is moved to `PORT-ID` and a random `READ PORTFOLIO-FILE` is issued. `INVALID KEY` (VSAM status `2x`, e.g. `23` not found) produces `Invalid Portfolio ID: <8-char id>`. The portfolio's **status** (`PORT-STATUS` A/C/S in `PORTFLIO.cpy:24-27`) is **not** checked — closed or suspended portfolios accept transactions. | `PORTTRAN.cbl:126-133`; `PORTFLIO.cpy:13,24-27` | [CODE] |
| **BR-11** | **Transaction type whitelist**: `TRN-TYPE` must be one of `'BU'`, `'SL'`, `'TR'`, `'FE'`. Any other value → `Invalid Transaction Type: <2 chars>`. Comparison is exact, case-sensitive, 2 bytes. | `PORTTRAN.cbl:136-149`; `TRNREC.cpy:14-18` | [CODE] |
| **BR-12a** | **Quantity must be > 0 for every type, including `FE` and `TR`**: `TRN-QUANTITY <= ZERO` → `Quantity must be greater than zero`. (Negative or zero quantities are always rejected; there is no sign convention for sells — a sell carries a positive quantity.) | `PORTTRAN.cbl:152-155` | [CODE] |
| **BR-12b** | **Price must be > 0 unless type is `TR`**: `TRN-PRICE <= ZERO AND TRN-TYPE NOT = 'TR'` → `Price must be greater than zero`. A transfer may carry a zero price. | `PORTTRAN.cbl:157-160` | [CODE] |
| **BR-12c** | **Amount must be > 0 unless type is `TR`**: `TRN-AMOUNT <= ZERO AND TRN-TYPE NOT = 'TR'` → `Amount must be greater than zero`. A transfer may carry a zero amount. | `PORTTRAN.cbl:162-164` | [CODE] |
| **BR-13** | **Fields NOT validated by code** (explicitly): `TRN-DATE`, `TRN-TIME`, `TRN-SEQUENCE-NO`, `TRN-INVESTMENT-ID`, `TRN-CURRENCY`, `TRN-STATUS`, `TRN-PROCESS-DATE`, `TRN-PROCESS-USER`. There is no date-format check, no future-date check, no duplicate-key check, no `AMOUNT ≈ QUANTITY × PRICE` consistency check, and no currency check against `PORT-…` (the portfolio record has no currency field in `PORTFLIO.cpy`). | `PORTTRAN.cbl:102-165` (absence); `TRNREC.cpy` | [CODE] |

### 2.3 Position-update rules (implemented but unreachable — dead code)

| # | Rule | Source | Status |
|---|---|---|---|
| **BR-14** | **`2200-UPDATE-POSITIONS` is never invoked.** No `PERFORM 2200-…` exists anywhere in the program; `2100-VALIDATE-TRANSACTION` only increments `WS-PROCESS-COUNT` on success. Consequently `PORTTRAN` as written **never modifies the portfolio file, never writes an audit record, never sets `TRN-STATUS`, and never writes any output file.** Everything in BR-15 – BR-23 is therefore intent, not behavior. | `PORTTRAN.cbl:113-114` (only success action), `167` (unreferenced label) | [CODE — DEAD] |
| **BR-15** | Dispatch by type: `BU`→`2210-PROCESS-BUY`, `SL`→`2220-PROCESS-SELL`, `TR`→`2230-PROCESS-TRANSFER`, `FE`→`2240-PROCESS-FEE`; after the dispatch, `2300-UPDATE-AUDIT-TRAIL` is **always** performed (even when the update failed or was a not-implemented transfer). | `PORTTRAN.cbl:167-180` | [CODE — DEAD] |
| **BR-16** | Each update paragraph **re-reads** the portfolio record by `TRN-PORTFOLIO-ID` (random `READ`); `INVALID KEY` → `Portfolio not found for update` (BU/SL) or `Portfolio not found for fee` (FE), one error, and the paragraph exits without updating. | `PORTTRAN.cbl:183-189, 202-208, 232-238` | [CODE — DEAD] |
| **BR-17** | **BUY arithmetic**: `PORT-TOTAL-UNITS := PORT-TOTAL-UNITS + TRN-QUANTITY`; `PORT-TOTAL-COST := PORT-TOTAL-COST + TRN-AMOUNT`. Plain `ADD` with **no `ROUNDED` and no `ON SIZE ERROR`**: results are stored truncated to the receiving field's picture (low-order decimal digits beyond the target scale are dropped, high-order overflow is silently truncated — Enterprise COBOL default). Price is **not** used; there is no average-cost computation. | `PORTTRAN.cbl:191-192` | [CODE — DEAD] |
| **BR-18** | **SELL pre-condition (insufficient units)**: if `PORT-TOTAL-UNITS < TRN-QUANTITY` → error `Insufficient units for sale`, no update. Equality (`=`) is allowed → position may go to exactly zero. | `PORTTRAN.cbl:210-214` | [CODE — DEAD] |
| **BR-19** | **SELL arithmetic**: `PORT-TOTAL-UNITS := PORT-TOTAL-UNITS − TRN-QUANTITY`; `PORT-TOTAL-COST := PORT-TOTAL-COST − TRN-AMOUNT`. Same truncation semantics as BR-17. Cost is reduced by the **sale proceeds**, not by proportional cost basis — this is *not* an average-cost or FIFO cost-basis relief; `PORT-TOTAL-COST` may go negative (no check). | `PORTTRAN.cbl:216-217` | [CODE — DEAD] |
| **BR-20** | **TRANSFER is not implemented**: any `TR` reaching the update stage produces error `Transfer processing not implemented` (counted toward the 100-error threshold) and no change. | `PORTTRAN.cbl:226-229` | [CODE — DEAD] |
| **BR-21** | **FEE arithmetic**: `PORT-TOTAL-COST := PORT-TOTAL-COST − TRN-AMOUNT`; units unchanged; quantity and price are ignored (although BR-12a requires quantity > 0 for a fee to pass validation). No floor at zero. | `PORTTRAN.cbl:240` | [CODE — DEAD] |
| **BR-22** | After a successful arithmetic update the portfolio record is `REWRITE`n; `INVALID KEY` → `Error updating portfolio`. There is no commit/sync point: the VSAM rewrite is immediately durable and not undone on later error. | `PORTTRAN.cbl:194-198, 219-223, 242-246` | [CODE — DEAD] |
| **BR-23** | **Audit record per transaction** (`AUDITLOG.cpy` layout): `AUD-TIMESTAMP := FUNCTION CURRENT-DATE` (21 chars into a 26-char field, space-padded), `AUD-PROGRAM := 'PORTTRAN'`, `AUD-USER-ID := FUNCTION USER-ID`, `AUD-TYPE := 'TRAN'`; action mapping **`BU`→`CREATE`, `SL`→`DELETE`, `TR`→`UPDATE`, `FE`→`UPDATE`**; `AUD-STATUS := 'SUCC'` if `WS-PORT-STATUS = '00'` else `'FAIL'`; keys `AUD-PORTFOLIO-ID := TRN-PORTFOLIO-ID`, `AUD-ACCOUNT-NO := PORT-ACCOUNT-NO`; `AUD-BEFORE-IMAGE := PORT-RECORD` — **taken *after* the REWRITE**, so despite the comment "Store original portfolio state" it is actually the *after* image (first 100 bytes of the record); `AUD-AFTER-IMAGE` is left as initialized (spaces); `AUD-MESSAGE := 'Transaction: ' TRN-TYPE ' Amount: ' TRN-AMOUNT ' Units: ' TRN-QUANTITY` — the two COMP-3 numerics are moved with `DELIMITED BY SIZE` so they appear as **raw packed-decimal bytes**, not formatted digits. Then `CALL 'AUDPROC' USING AUDIT-RECORD`; `RETURN-CODE NOT = ZERO` → `Error writing audit record`. | `PORTTRAN.cbl:249-300`; `AUDITLOG.cpy` | [CODE — DEAD] |

### 2.4 Error handling & return codes

| # | Rule | Source | Status |
|---|---|---|---|
| **BR-24** | **Every error** goes through `9000-ERROR-ROUTINE`: `WS-ERROR-COUNT += 1`; `ERR-CATEGORY := 'PR'` (`ERR-CAT-PROC`) — **all errors, including validation errors, are classified as "processing"**, `ERR-CAT-VALID ('VL')` is never used; `ERR-PROGRAM := 'PORTTRAN'`; `CALL 'ERRPROC' USING ERR-MESSAGE`. `ERR-CODE`, `ERR-SEVERITY`, `ERR-TIMESTAMP`, `ERR-DETAILS` are **never populated** by `PORTTRAN` (they stay at whatever `ERRHAND.cpy` working storage holds — no `VALUE` clause, so binary zeros/garbage at first call, then stale). | `PORTTRAN.cbl:311-316`; `ERRHAND.cpy:11-15, 30-39` | [CODE] |
| **BR-25** | **Program return code**: `PORTTRAN` never `MOVE`s to `RETURN-CODE`; it relies on the special register. `ERRPROC` and `AUDPROC` report status through a trailing `LS-RETURN-CODE` field in their own linkage areas (`ERRPROC.cbl:45,80`; `AUDPROC.cbl:49,65,88,90`), **not** via the `RETURN-CODE` register. Therefore the job-step RC of `PORTTRAN` is expected to be **0 regardless of how many errors occurred**, and the check `IF RETURN-CODE NOT = ZERO` at line 296 can never be true from `AUDPROC`'s explicit logic. Inference from Enterprise COBOL semantics, not from an observed run. | `PORTTRAN.cbl:296, 311-316`; `ERRPROC.cbl:37-47,80`; `AUDPROC.cbl:33-51,65,88,90` | [CODE] (RC behavior INFERRED FROM LANGUAGE SEMANTICS) |
| **BR-26** | **Return-code vocabulary** (`RETHND.cpy`, not COPYed by `PORTTRAN` but the S8 standard): `RC-SUCCESS=0`, `RC-WARNING=4`, `RC-ERROR=8`, `RC-SEVERE=12`, `RC-CRITICAL=16` (`ERRHAND.cpy` names 16 `ERR-TERMINAL`). Error types `V` validation, `P` processing, `D` database, `F` file, `S` security. Action flags `C` continue, `A` abort, `R` retry with `MAX-RETRIES = 3`. Standard error codes `E001`–`E010` (invalid data, not found, duplicate, file error, DB error, security, processing, validation, version, timeout). | `RETHND.cpy:8-13, 24-29, 37-42, 46-56`; `ERRHAND.cpy:20-25` | [CODE — copybook constants only; no PORTTRAN usage] |
| **BR-27** | **Transaction status vocabulary** (`TRNREC.cpy`): `P` Pending, `D` Done, `F` Failed, `R` Reversed. `PORTTRAN` never reads or sets `TRN-STATUS`. The DB2 DDL comment gives a *different* vocabulary: `P`=Processed, `F`=Failed, `R`=Reversed (no `D`). See D-06. | `TRNREC.cpy:23-27`; `db2-definitions.sql:101` | [CODE — constants only] |

### 2.5 Rules that exist only in documentation (TRNVAL00 / POSUPD00 do not exist in the repo)

All of the following are **INFERRED FROM DOCS**. They describe a program (`TRNVAL00`, a.k.a. `TRNMAIN`) that is referenced by the SAD, the data dictionary, `README.md:89`, and `PRCSEQ.cpy:69`, but **no source exists**. They also use a *different record layout* (`TRANS-RECORD` with `TR-ACCOUNT-NO`/`TR-FUND-ID`, data-dictionary §2.1) than the one in the code (`TRNREC.cpy`); see D-02.

| # | Rule | Source | Status |
|---|---|---|---|
| **BR-28** | Account Number must be numeric and exist in the customer master. | `data-dictionary.md:239` | INFERRED FROM DOCS |
| **BR-29** | Fund ID must exist in the fund master. | `data-dictionary.md:240` | INFERRED FROM DOCS |
| **BR-30** | Transaction Date must not be a future date. | `data-dictionary.md:241` | INFERRED FROM DOCS |
| **BR-31** | Share Quantity must be non-zero for `BY`/`SL` (docs use `BY` for buy; code uses `BU`). Contrast BR-12a: code requires `> 0` for *all* types. | `data-dictionary.md:242`, `:57` | INFERRED FROM DOCS |
| **BR-32** | Amount must be non-zero for `FE`. Contrast BR-12c: code requires `> 0` for all types except `TR`. | `data-dictionary.md:243` | INFERRED FROM DOCS |
| **BR-33** | Price must be greater than zero for `BY`/`SL`. Contrast BR-12b: code also requires it for `FE`. | `data-dictionary.md:244` | INFERRED FROM DOCS |
| **BR-34** | Doc error-code catalogue: `E001` Invalid Account Number (reject), `E002` Invalid Fund ID (reject), `E003` Invalid Transaction Type (reject), `E004` Insufficient Position Balance (reject), `W001` Zero Dollar Transaction (warning, process), `W002` Duplicate Transaction ID (warning, log). Note `RETHND.cpy` assigns `E001`–`E004` *different* meanings (D-08). | `data-dictionary.md:255-262` | INFERRED FROM DOCS |
| **BR-35** | Input file has a header/detail/trailer structure (`TR-HEADER` `'D'`/`'T'`), 200-byte records; doc status vocabulary `P` pending / `C` complete / `E` error. Code layout (`TRNREC.cpy`) has no record-type indicator and is 152 bytes. | `data-dictionary.md:43-70` | INFERRED FROM DOCS |
| **BR-36** | Batch contract: `TRNVAL00` runs 18:00–18:15 with no prerequisite ("Day must be open"); `POSUPD00` runs only if `TRNVAL00` RC ≤ 0004; `HISTLD00` after `POSUPD00` RC ≤ 0004. RC meanings: 0000 success, 0004 warnings, 0008 errors-but-complete, 0012 critical/abend, 0016 environment error. Checkpoint every 1,000 transaction records (min. 2-minute interval); restartable. | `data-dictionary.md:272-277, 281-294, 300-305`; `PRCSEQ.cpy:68-71` | INFERRED FROM DOCS |
| **BR-37** | Transaction identifier: DDL says `TRANSACTION_ID CHAR(20)` = `YYYYMMDDHHMMSS` + 6-digit sequence. This equals the concatenation of `TRN-DATE`+`TRN-TIME`+`TRN-SEQUENCE-NO` from `TRNREC.cpy` (8+6+6 = 20), **skipping** `TRN-PORTFOLIO-ID` which sits between them in `TRN-KEY`. The data dictionary instead defines `TRANS-ID CHAR(12)` = `YYYYMMDD` + 4 digits. | `db2-definitions.sql:47, 98`; `TRNREC.cpy:7-11`; `data-dictionary.md:26` | INFERRED FROM DOCS/DDL |
| **BR-38** | Downstream position semantics that S2 must make possible for S3 (from SAD §1.2.2 and DD §5.2): share balance must never go negative; cost basis updated on every buy/sell; average cost recalculated on buys; position status must be Active. `PORTTRAN`'s dead code implements only the "never negative" check (BR-18) and does not maintain average cost. | `system-architecture.md:95-99`; `data-dictionary.md:248-251` | INFERRED FROM DOCS |

**Business-rule count: 40** (BR-01 … BR-38, with BR-12 split into 12a/12b/12c).

---

## 3. Data contracts

### 3.1 `TRNREC.cpy` — `TRANSACTION-RECORD` (TRANFILE input record, 152 bytes)

Offsets are 1-based. `COMP-3` lengths are `⌈(digits+1)/2⌉` bytes.

| Field | PIC | Bytes (offset) | Semantics | Proposed Java type | Target store mapping |
|---|---|---|---|---|---|
| `TRN-DATE` | `X(08)` | 8 (1) | Transaction date, `YYYYMMDD` (copybook comment; stored as text, **not** validated) | `LocalDate` (parse `BASIC_ISO_DATE`; reject unparsable) | `TRANSACTION_HISTORY.TRANSACTION_DATE DATE`; first 8 chars of `TRANSACTION_ID` |
| `TRN-TIME` | `X(06)` | 6 (9) | `HHMMSS` | `LocalTime` (pattern `HHmmss`) | `TRANSACTION_HISTORY.TRANSACTION_TIME TIME`; chars 9–14 of `TRANSACTION_ID` |
| `TRN-PORTFOLIO-ID` | `X(08)` | 8 (15) | Portfolio key; must be non-blank and exist (BR-09/10) | `String` (len 8, `@NotBlank`) | `TRANSACTION_HISTORY.PORTFOLIO_ID CHAR(8)` → FK `PORTFOLIO_MASTER.PORTFOLIO_ID`; VSAM `PORTFILE.PORT-ID` |
| `TRN-SEQUENCE-NO` | `X(06)` | 6 (23) | Sequence for multiple transactions in the same second | `String` (len 6) — keep textual; DDL comment calls it "6-digit", so an `int` is possible only after a decision (OQ-6) | last 6 chars of `TRANSACTION_ID` |
| `TRN-INVESTMENT-ID` | `X(10)` | 10 (29) | Security/fund identifier (unused by code) | `String` (len 10) | `TRANSACTION_HISTORY.INVESTMENT_ID CHAR(10)`; `INVESTMENT_POSITIONS.INVESTMENT_ID` (S3) |
| `TRN-TYPE` | `X(02)` + 88s | 2 (39) | `BU` buy, `SL` sell, `TR` transfer, `FE` fee | `enum TransactionType { BUY("BU"), SELL("SL"), TRANSFER("TR"), FEE("FE") }` | `TRANSACTION_HISTORY.TRANSACTION_TYPE CHAR(2)` |
| `TRN-QUANTITY` | `S9(11)V9(4) COMP-3` | 8 (41) | Units; must be > 0 (BR-12a) | `BigDecimal` scale **4**, precision 15, `RoundingMode.DOWN` on any conversion (COBOL truncation) | `TRANSACTION_HISTORY.QUANTITY DECIMAL(18,4)` |
| `TRN-PRICE` | `S9(11)V9(4) COMP-3` | 8 (49) | Unit price; > 0 unless `TR` (BR-12b) | `BigDecimal` scale **4**, precision 15, `RoundingMode.DOWN` | `TRANSACTION_HISTORY.PRICE DECIMAL(18,4)` |
| `TRN-AMOUNT` | `S9(13)V9(2) COMP-3` | 8 (57) | Gross amount; > 0 unless `TR` (BR-12c) | `BigDecimal` scale **2**, precision 15, `RoundingMode.DOWN` | `TRANSACTION_HISTORY.AMOUNT DECIMAL(18,2)` |
| `TRN-CURRENCY` | `X(03)` | 3 (65) | ISO-4217 code (unvalidated) | `String` (len 3) or `java.util.Currency` after OQ-7 | `TRANSACTION_HISTORY.CURRENCY_CODE CHAR(3)` |
| `TRN-STATUS` | `X(01)` + 88s | 1 (68) | `P` pending, `D` done, `F` failed, `R` reversed (never set by code) | `enum TransactionStatus { PENDING("P"), DONE("D"), FAILED("F"), REVERSED("R") }` — see D-06 for the DDL's conflicting `P`=Processed | `TRANSACTION_HISTORY.STATUS CHAR(1)` |
| `TRN-PROCESS-DATE` | `X(26)` | 26 (69) | DB2-style timestamp `YYYY-MM-DD-HH.MM.SS.ffffff` (never set by code) | `LocalDateTime` / `Instant` (nullable on input; set by S2 on persist) | `TRANSACTION_HISTORY.PROCESS_DATE TIMESTAMP` |
| `TRN-PROCESS-USER` | `X(08)` | 8 (95) | Processing user id (never set by code) | `String` (len 8) | `TRANSACTION_HISTORY.PROCESS_USER VARCHAR(8)` |
| `TRN-FILLER` | `X(50)` | 50 (103) | Reserved | not mapped | — |

Derived: `TRANSACTION_ID CHAR(20)` = `TRN-DATE ‖ TRN-TIME ‖ TRN-SEQUENCE-NO` (BR-37; needs confirmation, OQ-6).

### 3.2 `RETHND.cpy` — `RETURN-HANDLING` (S8 shared contract; used by `CKPRST`, not by `PORTTRAN`)

| Field | PIC | Semantics | Proposed Java type | Mapping |
|---|---|---|---|---|
| `RETURN-CODE` | `S9(4) COMP` + 88s | 0/4/8/12/16 severity ladder | `enum ReturnCode { SUCCESS(0), WARNING(4), ERROR(8), SEVERE(12), CRITICAL(16) }` (S8 `platform-common`) | batch step exit status; `RTNCODES` table (S8) |
| `REASON-CODE` | `S9(4) COMP` | Sub-reason | `short`/`int` | — |
| `MODULE-ID`, `FUNCTION-ID` | `X(8)` each | Originating module / function | `String` | `ERRLOG.PROGRAM_ID` |
| `PROGRAM-NAME`, `PARAGRAPH-NAME`, `ERROR-ROUTINE` | `X(8)` each | Location | `String` (or Java class/method names in logs) | log MDC fields |
| `ERROR-TYPE` | `X(1)` + 88s | `V` validation, `P` processing, `D` database, `F` file, `S` security | `enum ErrorType { VALIDATION, PROCESSING, DATABASE, FILE, SECURITY }` | `ERRLOG` (S8) |
| `ERROR-CODE` | `X(4)` | `E001`–`E010` | `enum StandardErrorCode` (values below) | `ERRLOG.ERROR_CODE CHAR(4)` |
| `ERROR-TEXT` | `X(80)` | Human text | `String` (≤ 80) | `ERRLOG.ERROR_DESC VARCHAR(100)` |
| `SYSTEM-CODE` / `SYSTEM-MSG` | `X(4)` / `X(80)` | e.g. VSAM status / SQLCODE text | `String` | — |
| `ACTION-FLAG` | `X(1)` + 88s | `C` continue, `A` abort, `R` retry | `enum ErrorAction { CONTINUE, ABORT, RETRY }` | — |
| `RETRY-COUNT` / `MAX-RETRIES` | `9(2) COMP` (`MAX-RETRIES VALUE 3`) | Retry bookkeeping | `int`; default max 3 in config | Spring Retry / Spring Batch skip/retry policy |
| `STD-ERROR-CODES` | `X(4)` constants | `E001` INVALID-DATA, `E002` NOT-FOUND, `E003` DUPLICATE, `E004` FILE-ERROR, `E005` DB-ERROR, `E006` SECURITY, `E007` PROCESSING, `E008` VALIDATION, `E009` VERSION, `E010` TIMEOUT | `enum StandardErrorCode` | `ERRLOG.ERROR_CODE` |

### 3.3 `ERRHAND.cpy` — `ERR-MESSAGE` (what `PORTTRAN` actually passes to `ERRPROC`)

| Field | PIC | Set by PORTTRAN? | Java |
|---|---|---|---|
| `ERR-DATE` / `ERR-TIME` | `X(10)` / `X(8)` | no | `Instant` (set by S8 logger) |
| `ERR-PROGRAM` | `X(8)` | `'PORTTRAN'` | `String` source component |
| `ERR-CATEGORY` | `X(2)` | `'PR'` always | `enum ErrorCategory { VSAM("VS"), VALIDATION("VL"), PROCESSING("PR"), SYSTEM("SY") }` — S2 should emit `VL` for BR-09…BR-12 (OQ-3) |
| `ERR-CODE` | `X(4)` | no | `StandardErrorCode` |
| `ERR-SEVERITY` | `S9(4) COMP` | no | `ReturnCode` |
| `ERR-TEXT` | `X(80)` | yes (messages in §2) | `String` |
| `ERR-DETAILS` | `X(256)` | no | `Map<String,String>` / JSON |

### 3.4 `AUDITLOG.cpy` — `AUDIT-RECORD` (dead-code output to `AUDPROC`)

| Field | PIC | Value set (BR-23) | Java |
|---|---|---|---|
| `AUD-TIMESTAMP` | `X(26)` | `CURRENT-DATE` | `Instant` |
| `AUD-SYSTEM-ID`, `AUD-TERMINAL` | `X(8)` | not set | `String`, nullable |
| `AUD-USER-ID` | `X(8)` | `FUNCTION USER-ID` | `String` (principal) |
| `AUD-PROGRAM` | `X(8)` | `'PORTTRAN'` | `String` |
| `AUD-TYPE` | `X(4)` + 88s | `'TRAN'` | `enum AuditType { TRAN, USER, SYST }` |
| `AUD-ACTION` | `X(8)` + 88s | `CREATE`/`DELETE`/`UPDATE` by type | `enum AuditAction` |
| `AUD-STATUS` | `X(4)` + 88s | `SUCC`/`FAIL` | `enum AuditStatus { SUCC, FAIL, WARN }` |
| `AUD-PORTFOLIO-ID` / `AUD-ACCOUNT-NO` | `X(8)` / `X(10)` | from TRN / PORT record | `String` |
| `AUD-BEFORE-IMAGE` / `AUD-AFTER-IMAGE` | `X(100)` | post-update record / spaces | JSON snapshot (S8 `AuditEvent`) |
| `AUD-MESSAGE` | `X(100)` | free text | `String` |

### 3.5 `PORTFLIO.cpy` — `PORT-RECORD` (fields *actually available* when `PORTTRAN` reads `PORTFILE`)

`PORTTRAN` line 40 says `COPY PORTREC`, which does not exist; the only portfolio copybook is `PORTFLIO.cpy`. Its fields: `PORT-ID X(8)`, `PORT-ACCOUNT-NO X(10)`, `PORT-CLIENT-NAME X(30)`, `PORT-CLIENT-TYPE X(1)` (I/C/T), `PORT-CREATE-DATE 9(8)`, `PORT-LAST-MAINT 9(8)`, `PORT-STATUS X(1)` (A/C/S), `PORT-TOTAL-VALUE S9(13)V99 COMP-3`, `PORT-CASH-BALANCE S9(13)V99 COMP-3`, `PORT-LAST-USER X(8)`, `PORT-LAST-TRANS 9(8)`, `PORT-FILLER X(50)`. **`PORT-TOTAL-UNITS` and `PORT-TOTAL-COST` — the two fields the dead update code mutates — do not exist in any copybook** (D-03). S2 only needs `PORT-ID` (existence check) from S1; S3 owns the balance semantics.

### 3.6 `TRANSACTION_HISTORY` (DB2 — owned by S2)

| Column | DB2 type | Source field | JPA/Java |
|---|---|---|---|
| `TRANSACTION_ID` | `CHAR(20)` PK | derived (BR-37) | `String` `@Id` |
| `PORTFOLIO_ID` | `CHAR(8)` NOT NULL, FK → `PORTFOLIO_MASTER` | `TRN-PORTFOLIO-ID` | `String` |
| `TRANSACTION_DATE` | `DATE` | `TRN-DATE` | `LocalDate` |
| `TRANSACTION_TIME` | `TIME` | `TRN-TIME` | `LocalTime` |
| `INVESTMENT_ID` | `CHAR(10)` | `TRN-INVESTMENT-ID` | `String` |
| `TRANSACTION_TYPE` | `CHAR(2)` | `TRN-TYPE` | `TransactionType` (`@Convert`) |
| `QUANTITY` | `DECIMAL(18,4)` | `TRN-QUANTITY` | `BigDecimal(18,4)` |
| `PRICE` | `DECIMAL(18,4)` | `TRN-PRICE` | `BigDecimal(18,4)` |
| `AMOUNT` | `DECIMAL(18,2)` | `TRN-AMOUNT` | `BigDecimal(18,2)` |
| `CURRENCY_CODE` | `CHAR(3)` | `TRN-CURRENCY` | `String` |
| `STATUS` | `CHAR(1)` | `TRN-STATUS` | `TransactionStatus` |
| `PROCESS_DATE` | `TIMESTAMP` | `TRN-PROCESS-DATE` | `LocalDateTime` |
| `PROCESS_USER` | `VARCHAR(8)` | `TRN-PROCESS-USER` | `String` |

Indexes: `IDX_TRANS_HIST_PORT (PORTFOLIO_ID, TRANSACTION_DATE)`, `IDX_TRANS_HIST_DATE (TRANSACTION_DATE, PORTFOLIO_ID)` (`db2-definitions.sql:73-77`). No COBOL program in the repo issues SQL against this table (verified with a repo-wide search for `TRANSACTION_HISTORY` in `*.cbl`/`*.cpy`: zero hits).

Note the widening: COBOL precision 15 → DB2 precision 18; DB2 column can hold values the COBOL record cannot. Java validation should enforce the **COBOL** limits (`|QUANTITY| < 10^11`, `|AMOUNT| < 10^13`) if file-format parity with `TSTGEN00`/`TSTVAL00` golden data is required (S10).

---

## 4. Interfaces

### 4.1 Inbound

| Legacy interface | Details | Target (per plan §2) | Style |
|---|---|---|---|
| `TRANFILE` (DDNAME) | `SELECT TRANSACTION-FILE ASSIGN TO TRANFILE`, sequential, fixed 152-byte `TRANSACTION-RECORD` (`PORTTRAN.cbl:17-21, 32-35`). **No JCL in the repo runs `PORTTRAN` or allocates `TRANFILE`** (only `TRANHIST` appears in `RPTPOS.jcl`/`UTLVAL.jcl`). Producer of the file is `TSTGEN00` (S10) for test data; production producer unknown. | S2 `TransactionBatchIngestJob` (Spring Batch `FlatFileItemReader` with a fixed-length tokenizer replicating the copybook offsets) **and** `POST /transactions/batches` (multipart upload) | file ingestion (batch) + REST upload |
| Job-step invocation | Docs: scheduled by S6 (`BCHCTL`/`PRCCTL`, process id `TRNVAL00`, 18:00–18:15) — INFERRED FROM DOCS; no JCL exists. | S6 batch-orchestrator triggers `POST /transactions/batches/{id}/run` or launches the Spring Batch job | REST call from S6 |

### 4.2 Outbound

| Legacy interface | Details | Target | Style |
|---|---|---|---|
| `PORTFILE` (DDNAME) — read | KSDS random `READ` by `PORT-ID` for existence (`PORTTRAN.cbl:23-28, 126-133`). | S1 portfolio-service `GET /portfolios/{id}` (existence + status) with local cache, **or** read of `PORTFOLIO_MASTER` if S1 and S2 share a schema in Phase 1 (decision for F1) | REST call (default) |
| `PORTFILE` — `REWRITE` | Dead code only (`194, 219, 242`). | Not implemented in S2. Replaced by `TransactionPosted` event consumed by S3 position-service | event |
| `CALL 'ERRPROC' USING ERR-MESSAGE` | Every error (`PORTTRAN.cbl:316`). `ERRPROC` appends to its `ERROR-LOG` file. | S8 `platform-common` `ErrorLogger.log(ErrorEvent)` → `ERRLOG` table | library call |
| `CALL 'AUDPROC' USING AUDIT-RECORD` | Dead code only (`294`). | S8 `platform-common` `AuditPublisher.publish(AuditEvent)` | library call (async event internally) |
| `DISPLAY` counters | `PORTTRAN.cbl:306-308` | Micrometer counters `transactions.read`, `transactions.accepted`, `transactions.rejected` + job execution summary returned to S6 | metrics / REST response |
| `RETURN-CODE` | Never set (BR-25). Docs expect 0/4/8 (BR-36). | Spring Batch `ExitStatus` mapped to `ReturnCode` (S8) and returned to S6 | library call |
| `TRANSACTION_HISTORY` | No SQL in code. Per plan, S2 **owns** this table. | `TransactionHistoryRepository` (Spring Data JPA) — insert per accepted **and** rejected record (status `F`) | DB write |
| `TransactionPosted` | Does not exist in legacy (dead code would have mutated `PORTFILE` in-process). | Published per accepted transaction; consumed by S3 | event (Kafka/RabbitMQ per F3) |

### 4.3 CICS
None. `PORTTRAN` has no `EXEC CICS`; the transaction path is batch-only.

### 4.4 SQL
None in `PORTTRAN` (no `EXEC SQL`). DDL only (`db2-definitions.sql:46-62, 73-77`).

### 4.5 JCL
None references `PORTTRAN`, `TRNVAL00`, `TRNMAIN` or `TRANFILE` (`src/jcl/**`). `PRCSEQ.cpy:68-71` lists the intended main sequence `TRNVAL00 → POSUPD00 → HISTLD00`.

---

## 5. Discrepancies (code vs documentation vs DDL)

| # | Discrepancy | Evidence |
|---|---|---|
| **D-01** | **Program names**: docs call the transaction step `TRNMAIN`/`TRNVAL00` and the position step `POSUPDT`/`POSUPD00`; the only transaction program in code is `PORTTRAN` (`src/programs/portfolio/`). `TRNVAL00`, `TRNMAIN`, `POSUPD00`, `RPTGEN00` have no source. `POSUPDT.cbl` exists but is **0 lines**. | `system-architecture.md:32-33, 89-99`; `data-dictionary.md:274-277`; `README.md:89`; `PRCSEQ.cpy:69-70`; `wc -l src/programs/batch/POSUPDT.cbl` = 0 |
| **D-02** | **Two incompatible transaction record layouts**: DD §2.1 `TRANS-RECORD` (200 bytes; `TR-ACCOUNT-NO 9(9)`, `TR-FUND-ID X(6)`, `TR-TRANS-ID X(12)`, type `BY`/`SL`/`FE`, status `P/C/E`, `S9(11)V999` quantity, `9(5)V9999` price) vs. code `TRNREC.cpy` (152 bytes; `TRN-PORTFOLIO-ID X(8)`, `TRN-INVESTMENT-ID X(10)`, date+time+sequence key, type `BU`/`SL`/`TR`/`FE`, status `P/D/F/R`, `S9(11)V9(4)` quantity, `S9(11)V9(4)` price, `TRN-CURRENCY`). Buy code differs (`BY` vs `BU`); `TR` transfer exists only in code. | `data-dictionary.md:48-70`; `TRNREC.cpy:6-31` |
| **D-03** | **`PORTTRAN` would not compile**: it `COPY PORTREC` (line 40) — no such copybook exists (`src/copybook/**`); and it references `PORT-TOTAL-UNITS` / `PORT-TOTAL-COST` (lines 191-192, 210, 216-217, 240) which are absent from `PORTFLIO.cpy` (which has `PORT-TOTAL-VALUE` and `PORT-CASH-BALANCE` instead). The development backlog nevertheless marks "Update portfolio positions" and all four transaction types `[x]` complete. | `PORTTRAN.cbl:40, 191-240`; `PORTFLIO.cpy:28-30`; `development-backlog.md:156-171` |
| **D-04** | **Position update is dead code**: `2200-UPDATE-POSITIONS` is never performed (BR-14), yet SAD §1.2.2 and the backlog describe `PORTTRAN`/`POSUPDT` as updating positions, maintaining cost basis and recording transaction history. `PORTTRAN` records no history at all. | `PORTTRAN.cbl:102-118, 167`; `system-architecture.md:95-99` |
| **D-05** | **"Transfer between portfolios" marked complete in backlog** but code says `Transfer processing not implemented`. | `development-backlog.md:169`; `PORTTRAN.cbl:227` |
| **D-06** | **Status vocabularies conflict**: `TRNREC.cpy` `P`=Pending, `D`=Done, `F`=Failed, `R`=Reversed; DDL comment `P`=Processed, `F`=Failed, `R`=Reversed (no `D`); DD §2.1 `P`=Pending, `C`=Complete, `E`=Error. | `TRNREC.cpy:24-27`; `db2-definitions.sql:101`; `data-dictionary.md:66-68` |
| **D-07** | **Validation rules differ**: docs (DD §5.1) require account/fund existence, non-future date, non-zero qty for buy/sell, non-zero amount for fee, price > 0 for buy/sell. Code requires portfolio existence, qty > 0 for *all* types, price > 0 and amount > 0 for all but `TR`; no date, duplicate, or fund check. | `data-dictionary.md:239-244`; `PORTTRAN.cbl:120-165` |
| **D-08** | **Error-code meanings differ**: DD §6 `E001` Invalid Account, `E002` Invalid Fund, `E003` Invalid Trans Type, `E004` Insufficient Balance; `RETHND.cpy` `E001` Invalid Data, `E002` Not Found, `E003` Duplicate, `E004` File Error. `PORTTRAN` uses neither — it never sets `ERR-CODE`. | `data-dictionary.md:257-260`; `RETHND.cpy:47-50`; `PORTTRAN.cbl:311-316` |
| **D-09** | **Transaction id formats differ**: DD `TRANS-ID CHAR(12)` = `YYYYMMDD`+4 digits; DDL `TRANSACTION_ID CHAR(20)` = `YYYYMMDDHHMMSS`+6 digits; code has no single id field (composite `TRN-KEY` of 28 bytes including portfolio id). | `data-dictionary.md:26`; `db2-definitions.sql:47, 98`; `TRNREC.cpy:7-11` |
| **D-10** | **Portfolio-file open failure is not fatal and would mask validation**: after a failed `OPEN I-O PORTFOLIO-FILE` the loop still runs (BR-02). A `READ` on an unopened file returns status `47`, which is *not* an `INVALID KEY` condition, so `2110-CHECK-PORTFOLIO` would leave `ERR-TEXT` blank and every record with a non-blank portfolio id would be counted as processed. (Language-semantics inference; not observed.) | `PORTTRAN.cbl:63-67, 85-89, 127-133` |
| **D-11** | **Linkage layout mismatch with `ERRPROC`/`AUDPROC`**: `PORTTRAN` passes `ERR-MESSAGE` (starts with 18-byte `ERR-TIMESTAMP`, then `ERR-PROGRAM`) but `ERRPROC`'s `LS-ERROR-REQUEST` starts with `LS-PROGRAM-ID` and ends with an extra `LS-RETURN-CODE` — fields are offset by 18 bytes and the callee writes 2 bytes past the caller's area. Likewise `AUDIT-RECORD` begins with a 26-byte timestamp absent from `AUDPROC`'s `LS-AUDIT-REQUEST`, and `AUDPROC`'s trailing `LS-RETURN-CODE` has no counterpart. `PORTTRAN` also tests the `RETURN-CODE` special register, which neither callee sets. | `ERRHAND.cpy:30-39` vs `ERRPROC.cbl:38-45`; `AUDITLOG.cpy:62-91` vs `AUDPROC.cbl:34-49`; `PORTTRAN.cbl:294-296, 316` |
| **D-12** | **Audit "before image" is actually an after image** and the audit message embeds raw packed-decimal bytes (BR-23). | `PORTTRAN.cbl:277-287` |
| **D-13** | **Doc RC contract cannot be met by code**: docs gate `POSUPD00` on `TRNVAL00 RC ≤ 0004`, but `PORTTRAN` never sets `RETURN-CODE` (always 0), so a run with 101 errors that aborted early would still let the position update proceed. | `data-dictionary.md:303`; `PORTTRAN.cbl` (no `RETURN-CODE` assignment) |
| **D-14** | **Checkpoint/restart documented but absent**: DD §8.3 "Transaction processor: every 1000 records", §8.1 `TRNVAL00` Restart=Yes; `PORTTRAN` has no `CKPRST`/`BCHCTL` calls. | `data-dictionary.md:274, 291`; `PORTTRAN.cbl` (no `COPY CKPRST`, no `CALL`) |
| **D-15** | **SAD flow §2.2 says `TRNVAL00 → POSUPD00 → HISTLD00 → RPTGEN00`** with DB2 commit points; code has one monolithic in-process flow (validate, then in-process VSAM rewrite in dead code) and no DB2 at all in the transaction path. | `system-architecture.md:280-305`; `PORTTRAN.cbl` |
| **D-16** | **No JCL** runs `PORTTRAN`/`TRNVAL00`, and no DD allocates `TRANFILE`; DD §9.1 gives a scheduling window for a job that does not exist. | `src/jcl/**`; `data-dictionary.md:300-305` |
| **D-17** | **`RETHND.cpy` is described as an S2 copybook** in the plan but is not `COPY`ed by `PORTTRAN`; its only consumer is `CKPRST.cbl:24` (S6). `PORTTRAN` uses `ERRHAND.cpy` instead, whose severity ladder names 16 `ERR-TERMINAL` vs `RETHND`'s `RC-CRITICAL`. | `PORTTRAN.cbl:43`; `CKPRST.cbl:24`; `ERRHAND.cpy:25`; `RETHND.cpy:13` |
| **D-18** | **DD §1.2 common element sizes** (`AMOUNT 11,2`; `SHARE-QTY 11,3`; `PRICE 9,4`) match neither `TRNREC.cpy` (`13,2`; `11,4`; `11,4`) nor the DDL (`18,2`; `18,4`; `18,4`). | `data-dictionary.md:35-37`; `TRNREC.cpy:19-21`; `db2-definitions.sql:53-55` |

---

## 6. Proposed Java design

Service: `transaction-service` (Spring Boot 3.x, Java 21). Depends on `libs/domain-model` (F1) and `libs/platform-common` (F2). No Java code is written here; names are proposals for Phase 2.

### 6.1 Package layout

```
com.cog.portfolio.transaction
├── api/                       REST layer (S6 + operators + uploads)
│   ├── TransactionBatchController      POST /transactions/batches (upload), POST /{id}/run, GET /{id}
│   ├── TransactionController           GET /transactions/{transactionId}, GET /portfolios/{pid}/transactions?from&to
│   └── dto/                            TransactionBatchSummary(read, accepted, rejected, returnCode),
│                                       TransactionView, RejectedTransactionView(reason, errorCode)
├── application/
│   ├── TransactionIngestionService     orchestrates: parse → validate → persist → publish; enforces BR-03 abort threshold
│   ├── TransactionValidator            BR-09..BR-12 as an ordered, short-circuit chain (BR-08) returning the FIRST violation
│   ├── PortfolioExistenceGateway       port interface for BR-10 (impl in infrastructure)
│   └── TransactionIdFactory            BR-37 (date‖time‖sequence) — behind a flag until OQ-6 is decided
├── domain/
│   ├── Transaction                     immutable record mirroring TRNREC (types per §3.1)
│   ├── TransactionType, TransactionStatus  enums (Level-88 groups)
│   ├── ValidationFailure(errorCode, category, text)   text = exact COBOL message strings for parity
│   └── event/TransactionPosted         {transactionId, portfolioId, investmentId, type, quantity(4), price(4), amount(2), currency, transactionDate, transactionTime, batchId}
├── infrastructure/
│   ├── persistence/TransactionHistoryEntity, TransactionHistoryRepository (Spring Data JPA → TRANSACTION_HISTORY)
│   ├── persistence/TransactionBatchEntity, TransactionBatchRepository (new table TRANSACTION_BATCH: id, source file, counters, return code, started/finished — replaces the DISPLAYed counters, BR-07)
│   ├── portfolio/RestPortfolioExistenceGateway  (calls S1 GET /portfolios/{id}; Caffeine cache per batch)
│   ├── messaging/TransactionPostedPublisher     (Spring Cloud Stream binder chosen in F3; outbox table for at-least-once)
│   └── file/TranfileRecordMapper                fixed-length mapper: offsets from §3.1, COMP-3 unpack with scale 4/4/2, RoundingMode.DOWN
├── batch/
│   ├── TransactionIngestJobConfig      Spring Batch job `transactionIngestJob`: FlatFileItemReader(TranfileRecordMapper) → ValidatingItemProcessor(TransactionValidator) → CompositeItemWriter(historyWriter, eventWriter)
│   ├── ErrorThresholdListener          stops the step when rejected > 100 (configurable `transaction.max-errors=100`, BR-03)
│   └── ReturnCodeStepListener          maps counters → ReturnCode: 0 none rejected, 4 some rejected, 8 threshold hit or file open failure (replaces D-13 gap; pending OQ-4)
└── config/
    ├── TransactionServiceProperties    max-errors, chunk-size (1000 per BR-36 checkpoint frequency), portfolio-status-check (OQ-2), id-strategy (OQ-6)
    └── OpenApiConfig
```

### 6.2 Spring components summary

| Component | Type | Legacy origin |
|---|---|---|
| `transactionIngestJob` | Spring Batch `Job` (1 step, chunk = 1000, restartable via JobRepository) | `0000-MAIN` loop + D-14 checkpoint docs |
| `TransactionValidator` | `@Component`, pure | `2100`–`2130` |
| `RestPortfolioExistenceGateway` | `@Component` (WebClient) | `2110` `READ PORTFOLIO-FILE` |
| `TransactionHistoryRepository` | `JpaRepository<TransactionHistoryEntity, String>` | `TRANSACTION_HISTORY` DDL |
| `TransactionPostedPublisher` | `@Component` + transactional outbox | replaces dead `2200`/`REWRITE` |
| `ErrorLogger` (from S8) | library | `CALL 'ERRPROC'` |
| `AuditPublisher` (from S8) | library | `CALL 'AUDPROC'` (dead code; S2 emits `AuditAction.CREATE` for every accepted transaction, not the BU/SL/TR mapping of BR-23 — see OQ-8) |
| `TransactionBatchController` | `@RestController` | JCL/S6 trigger, `DISPLAY` counters |

### 6.3 Entities

* `TransactionHistoryEntity` — columns exactly as §3.6; `@Id transactionId`; `@Enumerated` via `AttributeConverter` for `TransactionType`/`TransactionStatus`; `BigDecimal` columns with `precision=18, scale=4|2`.
* `TransactionBatchEntity` (new) — `batchId UUID`, `sourceName`, `readCount`, `acceptedCount`, `rejectedCount`, `returnCode`, `startedAt`, `finishedAt`, `status`.
* Rejected records are persisted in `TRANSACTION_HISTORY` with `STATUS='F'` and the violation text in a new nullable column `REJECT_REASON VARCHAR(80)` **only if OQ-5 is answered yes**; otherwise they go to `ERRLOG` (S8) only.

### 6.4 Events

* `TransactionPosted` v1 (JSON, schema in `libs/domain-model`). Key = `portfolioId` (ordering per portfolio for S3). Published after the `TRANSACTION_HISTORY` insert commits (outbox).
* `TransactionBatchCompleted` v1 — `{batchId, readCount, acceptedCount, rejectedCount, returnCode}` for S6/S7.

### 6.5 Configuration

```yaml
transaction:
  max-errors: 100              # BR-03 (abort when rejected > max-errors)
  chunk-size: 1000             # BR-36 checkpoint frequency
  id-strategy: DATE_TIME_SEQ   # BR-37; alternatives pending OQ-6
  portfolio-check:
    mode: EXISTS               # legacy parity; ACTIVE_ONLY once OQ-2 is decided
  numeric:
    rounding: DOWN             # COBOL truncation parity for all BigDecimal conversions
```

### 6.6 Parity tests to be written in Phase 2 (one per business rule)

Golden cases: blank portfolio id → `Portfolio ID is required`; unknown id → `Invalid Portfolio ID: XXXXXXXX`; type `XX` → `Invalid Transaction Type: XX`; qty 0 for `FE` → rejected (BR-12a); `TR` with price 0 and amount 0 → accepted (BR-12b/c); 101st error stops the batch and records read = errors-so-far + accepted (BR-03); a record failing two checks yields exactly one error (BR-08). Numeric fixtures must include a value with 5 decimals to prove `RoundingMode.DOWN` truncation.

---

## 7. Open questions (require product/business decision)

1. **OQ-1 — Which validation rule-set is authoritative?** Code (`PORTTRAN`: portfolio exists; qty > 0 for all types; price/amount > 0 except `TR`) or docs (`TRNVAL00`: account/fund existence, no future date, qty ≠ 0 for buy/sell, amount ≠ 0 for fee, price > 0 for buy/sell)? In particular: must a **fee carry a positive quantity** (code says yes, docs say no)? Must a **transaction date be validated** at all?
2. **OQ-2 — Portfolio status gating.** Code accepts transactions for Closed/Suspended portfolios (BR-10); DD §5.2 says position status must be Active. Should S2 reject on `PORTFOLIO_MASTER.STATUS ≠ 'A'` (and should `ACTIVE_PORTFOLIOS` view semantics — `CLOSE_DATE > CURRENT DATE` — apply)?
3. **OQ-3 — Error classification.** All legacy errors are category `PR` with no code/severity (BR-24). Should S2 emit `VL` + `E008`/`E001`–`E003` per DD §6, and which of the two conflicting `E001`–`E004` tables (D-08) wins?
4. **OQ-4 — Return-code contract for S6.** Legacy RC is always 0 (BR-25/D-13). Proposed: 0 = no rejects, 4 = some rejects, 8 = abort threshold or file-open failure. Confirm, and confirm that S3 must not run when RC > 4 (DD §9.1).
5. **OQ-5 — Persist rejected transactions?** Legacy writes rejects only to the error log. Should `TRANSACTION_HISTORY` hold them with `STATUS='F'` (making the `F` status meaningful) or should they live only in `ERRLOG`?
6. **OQ-6 — Transaction identifier.** Adopt the DDL format `YYYYMMDDHHMMSS+seq(6)` (drops portfolio id, so uniqueness relies on the file producer's sequence numbers), the DD 12-char format, or a service-generated UUID with the legacy key kept as a natural-key column? Is `TRN-SEQUENCE-NO` guaranteed numeric?
7. **OQ-7 — Currency.** `TRN-CURRENCY` is never validated and `PORTFLIO.cpy` has no currency; `PORTFOLIO_MASTER.CURRENCY_CODE` exists. Must transaction currency equal portfolio currency, or is FX conversion expected?
8. **OQ-8 — Audit semantics.** Dead code maps `BU→CREATE`, `SL→DELETE`, `TR/FE→UPDATE` and stores an *after* image labelled "before" (BR-23, D-12). Should S2's audit event mirror that mapping for report continuity (S7 `RPTAUD00`), or use a neutral `TRANSACTION_ACCEPTED`/`TRANSACTION_REJECTED` action?
9. **OQ-9 — Transfer (`TR`) semantics.** Never implemented (BR-20). What does a transfer mean — between portfolios (needs a counter-party portfolio id, which `TRNREC` lacks), between investments within a portfolio, or cash movement? Should S2 accept `TR` at all until defined?
10. **OQ-10 — Duplicate handling.** No duplicate check exists in code; DD lists `W002 Duplicate Transaction ID` as *warning, log*. Should a duplicate `TRANSACTION_ID` be rejected (PK violation), logged and skipped, or accepted as a re-post?
11. **OQ-11 — Abort-threshold semantics.** Keep the legacy "stop after the 101st error, RC still 0, already-accepted records stand" behavior, or make the batch all-or-nothing? Should the threshold be per batch or per portfolio?
12. **OQ-12 — Cost-basis relief on SELL.** Dead code reduces cost by sale *proceeds* (BR-19), which is not an accounting method; DD §5.2 says average cost is recalculated on buys only. This is S3's rule, but S2 must decide whether `TransactionPosted` carries enough data (price *and* amount — proposed yes) for S3 to implement either average-cost or proceeds-based relief.
13. **OQ-13 — Precision limits.** Enforce COBOL picture limits (15 digits) on input for golden-file parity, or allow the DB2 `DECIMAL(18,x)` range?

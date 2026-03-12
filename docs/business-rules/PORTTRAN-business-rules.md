# PORTTRAN Business Rules

## 1. Program Overview

| Attribute   | Details |
|-------------|---------|
| **Program** | PORTTRAN -- Portfolio Transaction Processing |
| **Source**  | `src/programs/portfolio/PORTTRAN.cbl` |
| **Author**  | [Author name] |
| **Date**    | 2024-03-20 |

### Inputs

| Input | Assignment | Organization | Description |
|-------|-----------|--------------|-------------|
| Transaction file | `TRANFILE` | Sequential | Transaction records using `TRNREC` copybook layout |
| Portfolio master file | `PORTFILE` | Indexed (random access, key = `PORT-ID`) | Portfolio records using `PORTREC` copybook layout |

### Outputs

| Output | Description |
|--------|-------------|
| Updated portfolio master file | In-place REWRITE of portfolio records in `PORTFILE` |
| Audit trail records | Written via `CALL 'AUDPROC'` using `AUDIT-RECORD` structure |
| Error records | Written via `CALL 'ERRPROC'` using `ERR-MESSAGE` structure |
| Console summary | Displays `WS-READ-COUNT`, `WS-PROCESS-COUNT`, `WS-ERROR-COUNT` |

### Referenced Copybooks

| Copybook | File | Purpose |
|----------|------|---------|
| `TRNREC` | `src/copybook/common/TRNREC.cpy` | Transaction record layout |
| `PORTREC` | `src/copybook/common/PORTFLIO.cpy` (see note) | Portfolio master record layout |
| `ERRHAND` | `src/copybook/common/ERRHAND.cpy` | Error handling definitions |
| `AUDITLOG` | `src/copybook/common/AUDITLOG.cpy` | Audit trail record definitions |

> **Note:** The program issues `COPY PORTREC` but no file named `PORTREC.cpy` exists in the repository. The portfolio record layout is defined in `PORTFLIO.cpy`. The build environment likely resolves this via a library alias. Additionally, the program references fields `PORT-TOTAL-UNITS` and `PORT-TOTAL-COST` which do not appear in `PORTFLIO.cpy` (which defines `PORT-TOTAL-VALUE` and `PORT-CASH-BALANCE`). This suggests the runtime copybook may differ from the checked-in version, or that a separate `PORTREC.cpy` exists outside this repository.

---

## 2. Initialization Rules

**Paragraph:** `1000-INITIALIZE` (line 74)

| # | Rule | Source Line |
|---|------|-------------|
| I-1 | All file-status fields (`WS-TRAN-STATUS`, `WS-PORT-STATUS`) and counters (`WS-READ-COUNT`, `WS-PROCESS-COUNT`, `WS-ERROR-COUNT`) are initialized to zero/spaces via `INITIALIZE`. | 75--76 |
| I-2 | `WS-EOF-FLAG` is set to `'N'` (`MORE-RECORDS` = TRUE). | 77 |
| I-3 | `TRANSACTION-FILE` is opened for INPUT. If `WS-TRAN-STATUS` is not `'00'`, the error text `"Error opening transaction file"` is raised via `ERRPROC`. | 79--83 |
| I-4 | `PORTFOLIO-FILE` is opened for I-O (read/write). If `WS-PORT-STATUS` is not `'00'`, the error text `"Error opening portfolio file"` is raised via `ERRPROC`. | 85--89 |

> **Observation:** If the transaction file fails to open, `WS-TRAN-STATUS` will not be `'00'`, so the main processing loop is skipped (guarded at line 63). However, the portfolio file open error does *not* prevent the loop from executing -- the guard only checks `WS-TRAN-STATUS`.

---

## 3. Processing Loop Rules

**Paragraph:** `0000-MAIN` (line 60) / `2000-PROCESS-TRANSACTIONS` (line 92)

| # | Rule | Source Line |
|---|------|-------------|
| L-1 | The processing loop runs only if `WS-TRAN-STATUS = '00'` (transaction file opened successfully). | 63 |
| L-2 | Transactions are read sequentially from `TRANSACTION-FILE` until end-of-file. | 93--95 |
| L-3 | **Circuit Breaker:** Processing halts immediately if `WS-ERROR-COUNT > 100`. | 66 |
| L-4 | On each successful read (not EOF), `WS-READ-COUNT` is incremented by 1. | 97 |
| L-5 | After incrementing the read count, `2100-VALIDATE-TRANSACTION` is performed. | 98 |

**Input/Output per iteration:**

| Direction | Field | Description |
|-----------|-------|-------------|
| Input | `TRANSACTION-RECORD` (TRN-*) | One record read from `TRANFILE` |
| Output | `WS-READ-COUNT` | Incremented by 1 per record read |

---

## 4. Validation Rules

**Paragraph:** `2100-VALIDATE-TRANSACTION` (line 102)

Validation uses a **three-stage short-circuit chain**. If any stage sets `ERR-TEXT` to a non-space value, subsequent stages are skipped.

```
2110-CHECK-PORTFOLIO  -->  2120-CHECK-TRANSACTION-TYPE  -->  2130-CHECK-AMOUNTS
         |                          |                              |
    fail? stop               fail? stop                      fail? stop
```

### 4a. Portfolio Existence -- `2110-CHECK-PORTFOLIO` (line 120)

| # | Rule | Error Text | Source Line |
|---|------|-----------|-------------|
| V-1 | `TRN-PORTFOLIO-ID` must not be spaces. | `"Portfolio ID is required"` | 121--123 |
| V-2 | `TRN-PORTFOLIO-ID` must exist as a key in `PORTFOLIO-FILE`. A `READ` is issued with the portfolio ID as the key. | `"Invalid Portfolio ID: {TRN-PORTFOLIO-ID}"` | 126--133 |

**Input:** `TRN-PORTFOLIO-ID`
**Output (on success):** `PORT-*` record loaded into memory as a side effect of the `READ`.
**Output (on failure):** `ERR-TEXT` set with the appropriate error message.

### 4b. Transaction Type Whitelist -- `2120-CHECK-TRANSACTION-TYPE` (line 136)

| # | Rule | Error Text | Source Line |
|---|------|-----------|-------------|
| V-3 | `TRN-TYPE` must be one of: `'BU'` (Buy), `'SL'` (Sell), `'TR'` (Transfer), `'FE'` (Fee). Any other value is rejected. | `"Invalid Transaction Type: {TRN-TYPE}"` | 137--148 |

**Input:** `TRN-TYPE`
**Output:** Pass (no change) or fail (`ERR-TEXT` set).

### 4c. Amount Validation -- `2130-CHECK-AMOUNTS` (line 151)

| # | Rule | Condition | Error Text | Source Line |
|---|------|-----------|-----------|-------------|
| V-4 | `TRN-QUANTITY` must be > 0 for **all** transaction types (including fees and transfers). | `TRN-QUANTITY <= 0` | `"Quantity must be greater than zero"` | 152--155 |
| V-5 | `TRN-PRICE` must be > 0 **unless** `TRN-TYPE = 'TR'`. | `TRN-PRICE <= 0 AND TRN-TYPE NOT = 'TR'` | `"Price must be greater than zero"` | 157--160 |
| V-6 | `TRN-AMOUNT` must be > 0 **unless** `TRN-TYPE = 'TR'`. | `TRN-AMOUNT <= 0 AND TRN-TYPE NOT = 'TR'` | `"Amount must be greater than zero"` | 162--164 |

**Input:** `TRN-QUANTITY`, `TRN-PRICE`, `TRN-AMOUNT`, `TRN-TYPE`
**Output:** Pass (no change) or fail (`ERR-TEXT` set).

### 4d. Validation Outcome (line 113)

| Outcome | Action | Source Line |
|---------|--------|-------------|
| All three checks pass (`ERR-TEXT = SPACES`) | `WS-PROCESS-COUNT` incremented by 1 | 114 |
| Any check fails (`ERR-TEXT NOT = SPACES`) | `9000-ERROR-ROUTINE` called; `WS-ERROR-COUNT` incremented by 1 | 116 |

> **Observation:** When validation passes, the code increments `WS-PROCESS-COUNT` but does **not** call `2200-UPDATE-POSITIONS`. There is no `PERFORM 2200-UPDATE-POSITIONS` in the validation success path. This means position updates are never actually invoked from the main processing flow. This appears to be a bug or missing linkage in the program.

---

## 5. Position Update Rules

**Paragraph:** `2200-UPDATE-POSITIONS` (line 167)

> **Important:** As noted above, `2200-UPDATE-POSITIONS` is defined but is never `PERFORM`ed from the main processing loop or from `2100-VALIDATE-TRANSACTION`. The rules below document the *intended* behavior as coded.

The paragraph dispatches to a sub-paragraph based on `TRN-TYPE`:

| TRN-TYPE | Action Paragraph |
|----------|-----------------|
| `'BU'` | `2210-PROCESS-BUY` |
| `'SL'` | `2220-PROCESS-SELL` |
| `'TR'` | `2230-PROCESS-TRANSFER` |
| `'FE'` | `2240-PROCESS-FEE` |

After the type-specific processing, `2300-UPDATE-AUDIT-TRAIL` is always called (line 179).

### 5a. Buy -- `2210-PROCESS-BUY` (line 182)

**Input:** `TRN-PORTFOLIO-ID`, `TRN-QUANTITY`, `TRN-AMOUNT`

| # | Rule | Source Line |
|---|------|-------------|
| B-1 | Re-read portfolio by `TRN-PORTFOLIO-ID`. If not found, error `"Portfolio not found for update"` and exit paragraph. | 183--189 |
| B-2 | `PORT-TOTAL-UNITS = PORT-TOTAL-UNITS + TRN-QUANTITY` | 191 |
| B-3 | `PORT-TOTAL-COST = PORT-TOTAL-COST + TRN-AMOUNT` | 192 |
| B-4 | `REWRITE PORTFOLIO-RECORD`. If rewrite fails (invalid key), error `"Error updating portfolio"`. | 194--198 |

**Output:** Updated `PORT-TOTAL-UNITS`, `PORT-TOTAL-COST` in `PORTFILE`.

> **Note:** No upper-bound or balance limit checks are performed.

### 5b. Sell -- `2220-PROCESS-SELL` (line 201)

**Input:** `TRN-PORTFOLIO-ID`, `TRN-QUANTITY`, `TRN-AMOUNT`

| # | Rule | Source Line |
|---|------|-------------|
| S-1 | Re-read portfolio by `TRN-PORTFOLIO-ID`. If not found, error `"Portfolio not found for update"` and exit paragraph. | 202--208 |
| S-2 | `PORT-TOTAL-UNITS` must be >= `TRN-QUANTITY`. If not, error `"Insufficient units for sale"` and exit paragraph. | 210--214 |
| S-3 | `PORT-TOTAL-UNITS = PORT-TOTAL-UNITS - TRN-QUANTITY` | 216 |
| S-4 | `PORT-TOTAL-COST = PORT-TOTAL-COST - TRN-AMOUNT` | 217 |
| S-5 | `REWRITE PORTFOLIO-RECORD`. If rewrite fails (invalid key), error `"Error updating portfolio"`. | 219--223 |

**Output:** Updated `PORT-TOTAL-UNITS`, `PORT-TOTAL-COST` in `PORTFILE`.

> **Note:** No check prevents `PORT-TOTAL-COST` from going negative.

### 5c. Transfer -- `2230-PROCESS-TRANSFER` (line 226)

| # | Rule | Source Line |
|---|------|-------------|
| T-1 | Always errors with `"Transfer processing not implemented"`. This is a **stub**. | 227--228 |

### 5d. Fee -- `2240-PROCESS-FEE` (line 231)

**Input:** `TRN-PORTFOLIO-ID`, `TRN-AMOUNT`

| # | Rule | Source Line |
|---|------|-------------|
| F-1 | Re-read portfolio by `TRN-PORTFOLIO-ID`. If not found, error `"Portfolio not found for fee"` and exit paragraph. | 232--238 |
| F-2 | `PORT-TOTAL-COST = PORT-TOTAL-COST - TRN-AMOUNT` | 240 |
| F-3 | `REWRITE PORTFOLIO-RECORD`. If rewrite fails (invalid key), error `"Error updating portfolio"`. | 242--246 |

**Output:** Updated `PORT-TOTAL-COST` in `PORTFILE`. Units are unchanged.

> **Note:** No check prevents `PORT-TOTAL-COST` from going negative. `TRN-QUANTITY` is validated (must be > 0) but is never used during fee processing.

---

## 6. Audit Trail Rules

**Paragraph:** `2300-UPDATE-AUDIT-TRAIL` (line 249)

Called after every position-update attempt (buy, sell, transfer, or fee).

### 6a. Audit Record Field Mapping

| Audit Field | Source | Value / Logic | Source Line |
|-------------|--------|---------------|-------------|
| `AUD-TIMESTAMP` | `FUNCTION CURRENT-DATE` | System date/time at audit creation (format: `YYYYMMDDHHMMSSFF...`) | 252 |
| `AUD-PROGRAM` | Literal | `'PORTTRAN'` | 253 |
| `AUD-USER-ID` | `FUNCTION USER-ID` | Runtime user identifier | 254 |
| `AUD-TYPE` | Literal | `'TRAN'` | 255 |
| `AUD-ACTION` | `TRN-TYPE` | `'BU'` -> `'CREATE  '`, `'SL'` -> `'DELETE  '`, `'TR'` -> `'UPDATE  '`, `'FE'` -> `'UPDATE  '` | 257--266 |
| `AUD-STATUS` | `WS-PORT-STATUS` | `'00'` -> `'SUCC'`, else -> `'FAIL'` | 268--272 |
| `AUD-PORTFOLIO-ID` | `TRN-PORTFOLIO-ID` | Direct move | 274 |
| `AUD-ACCOUNT-NO` | `PORT-ACCOUNT-NO` | From portfolio record | 275 |
| `AUD-BEFORE-IMAGE` | `PORT-RECORD` | Snapshot of portfolio record (see caveat below) | 278 |
| `AUD-MESSAGE` | Concatenation | `'Transaction: ' + TRN-TYPE + ' Amount: ' + TRN-AMOUNT + ' Units: ' + TRN-QUANTITY` | 281--287 |

### 6b. Audit Write

| # | Rule | Source Line |
|---|------|-------------|
| A-1 | Audit record is written via `CALL 'AUDPROC' USING AUDIT-RECORD`. | 294 |
| A-2 | If `RETURN-CODE` is not zero after the call, error `"Error writing audit record"` is raised via `ERRPROC`. | 296--299 |

> **Caveat:** The code comment at line 277 says *"Store original portfolio state"*, but `AUD-BEFORE-IMAGE` is populated **after** the `REWRITE` has already occurred (the audit paragraph is called after the position update). This means `PORT-RECORD` may contain the **post-update** state rather than the pre-update state. This is a potential bug.

---

## 7. Error Handling Rules

**Paragraph:** `9000-ERROR-ROUTINE` (line 311)

| # | Rule | Source Line |
|---|------|-------------|
| E-1 | `WS-ERROR-COUNT` is incremented by 1. | 312 |
| E-2 | `ERR-CATEGORY` is set to `ERR-CAT-PROC` (value `'PR'`). | 313 |
| E-3 | `ERR-PROGRAM` is set to `'PORTTRAN'`. | 314 |
| E-4 | Error is dispatched via `CALL 'ERRPROC' USING ERR-MESSAGE`. | 316 |

**Input:** `ERR-TEXT` (must be populated by the caller before invoking this paragraph).

**Output:** Error record sent to external error processor; `WS-ERROR-COUNT` incremented.

---

## 8. Termination Rules

**Paragraph:** `3000-TERMINATE` (line 302)

| # | Rule | Source Line |
|---|------|-------------|
| T-1 | `TRANSACTION-FILE` is closed. | 303 |
| T-2 | `PORTFOLIO-FILE` is closed. | 304 |
| T-3 | Summary is displayed to the console: transactions read, transactions processed, errors encountered. | 306--308 |

**Console output format:**
```
Transactions Read:    {WS-READ-COUNT}
Transactions Process: {WS-PROCESS-COUNT}
Errors Encountered:   {WS-ERROR-COUNT}
```

---

## 9. Known Gaps and Observations

| # | Category | Observation |
|---|----------|-------------|
| G-1 | **Missing linkage** | `2200-UPDATE-POSITIONS` is defined but never called from the main processing flow. After validation succeeds, the program increments `WS-PROCESS-COUNT` but does not perform the position update. This appears to be a bug or incomplete implementation. |
| G-2 | **No negative-cost guard** | Sell and fee transactions subtract from `PORT-TOTAL-COST` without checking for negative results. A sell with `TRN-AMOUNT` larger than `PORT-TOTAL-COST` will produce a negative cost basis. |
| G-3 | **Redundant portfolio READ** | The portfolio is read once during validation (`2110-CHECK-PORTFOLIO`) and again during processing (`2210-PROCESS-BUY`, `2220-PROCESS-SELL`, `2240-PROCESS-FEE`). There is no record locking between these two reads, creating a window for concurrent modification. |
| G-4 | **Unused quantity in fee processing** | Fee transactions require `TRN-QUANTITY > 0` during validation (rule V-4) but never use `TRN-QUANTITY` during fee processing (`2240-PROCESS-FEE`). |
| G-5 | **Transfer stub** | Transfer type (`'TR'`) passes validation but always fails during processing with `"Transfer processing not implemented"`. |
| G-6 | **Audit before-image timing** | The comment says *"Store original portfolio state"* but `AUD-BEFORE-IMAGE` is captured after the `REWRITE`, so it may contain the post-update state rather than the pre-update state. |
| G-7 | **No transaction rollback** | If the audit write fails (via `AUDPROC`) after a successful portfolio update, there is no mechanism to roll back the portfolio change. The portfolio remains updated but the audit trail is incomplete. |
| G-8 | **Portfolio open failure not blocking** | If `PORTFOLIO-FILE` fails to open, the error is reported but the main loop guard (line 63) only checks `WS-TRAN-STATUS`, not `WS-PORT-STATUS`. Processing could theoretically proceed with an unopened portfolio file. |
| G-9 | **Copybook mismatch** | The program issues `COPY PORTREC` but the repository contains `PORTFLIO.cpy` (not `PORTREC.cpy`). The program references `PORT-TOTAL-UNITS` and `PORT-TOTAL-COST`, but the checked-in `PORTFLIO.cpy` defines `PORT-TOTAL-VALUE` and `PORT-CASH-BALANCE` instead. |

---

## Appendix A: Copybook Field Reference

### A.1 Transaction Record (`TRNREC.cpy`)

Source: `src/copybook/common/TRNREC.cpy`

```
01  TRANSACTION-RECORD
    05  TRN-KEY
        10  TRN-DATE             PIC X(08)           -- Transaction date (YYYYMMDD)
        10  TRN-TIME             PIC X(06)           -- Transaction time (HHMMSS)
        10  TRN-PORTFOLIO-ID     PIC X(08)           -- Portfolio identifier
        10  TRN-SEQUENCE-NO      PIC X(06)           -- Sequence number
    05  TRN-DATA
        10  TRN-INVESTMENT-ID    PIC X(10)           -- Investment identifier
        10  TRN-TYPE             PIC X(02)           -- BU/SL/TR/FE
            88  TRN-TYPE-BUY       VALUE 'BU'
            88  TRN-TYPE-SELL      VALUE 'SL'
            88  TRN-TYPE-TRANS     VALUE 'TR'
            88  TRN-TYPE-FEE       VALUE 'FE'
        10  TRN-QUANTITY         PIC S9(11)V9(4) COMP-3  -- Signed, 11.4 packed decimal
        10  TRN-PRICE            PIC S9(11)V9(4) COMP-3  -- Signed, 11.4 packed decimal
        10  TRN-AMOUNT           PIC S9(13)V9(2) COMP-3  -- Signed, 13.2 packed decimal
        10  TRN-CURRENCY         PIC X(03)           -- Currency code
        10  TRN-STATUS           PIC X(01)           -- P/D/F/R
            88  TRN-STATUS-PEND    VALUE 'P'
            88  TRN-STATUS-DONE    VALUE 'D'
            88  TRN-STATUS-FAIL    VALUE 'F'
            88  TRN-STATUS-REV     VALUE 'R'
    05  TRN-AUDIT
        10  TRN-PROCESS-DATE     PIC X(26)           -- Processing timestamp
        10  TRN-PROCESS-USER     PIC X(08)           -- Processing user ID
    05  TRN-FILLER               PIC X(50)           -- Reserved
```

### A.2 Portfolio Master Record (`PORTFLIO.cpy`)

Source: `src/copybook/common/PORTFLIO.cpy`

```
01  PORT-RECORD
    05  PORT-KEY
        10  PORT-ID              PIC X(8)            -- Portfolio identifier (record key)
        10  PORT-ACCOUNT-NO      PIC X(10)           -- Account number
    05  PORT-CLIENT-INFO
        10  PORT-CLIENT-NAME     PIC X(30)           -- Client name
        10  PORT-CLIENT-TYPE     PIC X(1)            -- I=Individual, C=Corporate, T=Trust
            88  PORT-INDIVIDUAL    VALUE 'I'
            88  PORT-CORPORATE     VALUE 'C'
            88  PORT-TRUST         VALUE 'T'
    05  PORT-PORTFOLIO-INFO
        10  PORT-CREATE-DATE     PIC 9(8)            -- Creation date (YYYYMMDD)
        10  PORT-LAST-MAINT      PIC 9(8)            -- Last maintenance date
        10  PORT-STATUS          PIC X(1)            -- A=Active, C=Closed, S=Suspended
            88  PORT-ACTIVE        VALUE 'A'
            88  PORT-CLOSED        VALUE 'C'
            88  PORT-SUSPENDED     VALUE 'S'
    05  PORT-FINANCIAL-INFO
        10  PORT-TOTAL-VALUE     PIC S9(13)V99 COMP-3  -- Signed, 13.2 packed decimal
        10  PORT-CASH-BALANCE    PIC S9(13)V99 COMP-3  -- Signed, 13.2 packed decimal
    05  PORT-AUDIT-INFO
        10  PORT-LAST-USER       PIC X(8)            -- Last update user
        10  PORT-LAST-TRANS      PIC 9(8)            -- Last transaction date
    05  PORT-FILLER              PIC X(50)           -- Reserved
```

> **Note:** The PORTTRAN program references `PORT-TOTAL-UNITS` and `PORT-TOTAL-COST` for position updates. These fields do not exist in the checked-in `PORTFLIO.cpy`. The runtime copybook (`PORTREC`) may define these fields differently. See observation G-9 above.

### A.3 Error Handling (`ERRHAND.cpy`)

Source: `src/copybook/common/ERRHAND.cpy`

```
01  ERR-CATEGORIES
    05  ERR-CAT-VSAM             PIC X(2) VALUE 'VS'  -- VSAM errors
    05  ERR-CAT-VALID            PIC X(2) VALUE 'VL'  -- Validation errors
    05  ERR-CAT-PROC             PIC X(2) VALUE 'PR'  -- Processing errors
    05  ERR-CAT-SYSTEM           PIC X(2) VALUE 'SY'  -- System errors

01  ERR-RETURN-CODES
    05  ERR-SUCCESS              PIC S9(4) COMP VALUE +0
    05  ERR-WARNING              PIC S9(4) COMP VALUE +4
    05  ERR-ERROR                PIC S9(4) COMP VALUE +8
    05  ERR-SEVERE               PIC S9(4) COMP VALUE +12
    05  ERR-TERMINAL             PIC S9(4) COMP VALUE +16

01  ERR-MESSAGE
    05  ERR-TIMESTAMP
        10  ERR-DATE             PIC X(10)           -- Error date
        10  ERR-TIME             PIC X(8)            -- Error time
    05  ERR-PROGRAM              PIC X(8)            -- Originating program
    05  ERR-CATEGORY             PIC X(2)            -- Error category code
    05  ERR-CODE                 PIC X(4)            -- Specific error code
    05  ERR-SEVERITY             PIC S9(4) COMP      -- Severity level
    05  ERR-TEXT                 PIC X(80)           -- Human-readable error text
    05  ERR-DETAILS              PIC X(256)          -- Extended error details

01  ERR-VSAM-STATUSES
    05  ERR-VSAM-SUCCESS         PIC X(2) VALUE '00'
    05  ERR-VSAM-DUPKEY          PIC X(2) VALUE '22'
    05  ERR-VSAM-NOTFND          PIC X(2) VALUE '23'
    05  ERR-VSAM-EOF             PIC X(2) VALUE '10'

01  ERR-VSAM-MSGS
    05  ERR-VSAM-22              PIC X(80) VALUE 'Duplicate record key'
    05  ERR-VSAM-23              PIC X(80) VALUE 'Record not found'
    05  ERR-OTHER                PIC X(80) VALUE 'Unexpected VSAM error'
```

### A.4 Audit Trail Record (`AUDITLOG.cpy`)

Source: `src/copybook/common/AUDITLOG.cpy`

```
01  AUDIT-RECORD
    05  AUD-HEADER
        10  AUD-TIMESTAMP        PIC X(26)           -- Audit timestamp
        10  AUD-SYSTEM-ID        PIC X(8)            -- System identifier
        10  AUD-USER-ID          PIC X(8)            -- User identifier
        10  AUD-PROGRAM          PIC X(8)            -- Program name
        10  AUD-TERMINAL         PIC X(8)            -- Terminal ID
    05  AUD-TYPE                 PIC X(4)            -- TRAN/USER/SYST
        88  AUD-TRANSACTION        VALUE 'TRAN'
        88  AUD-USER-ACTION        VALUE 'USER'
        88  AUD-SYSTEM-EVENT       VALUE 'SYST'
    05  AUD-ACTION               PIC X(8)            -- CREATE/UPDATE/DELETE/INQUIRE/etc.
        88  AUD-CREATE             VALUE 'CREATE  '
        88  AUD-UPDATE             VALUE 'UPDATE  '
        88  AUD-DELETE             VALUE 'DELETE  '
        88  AUD-INQUIRE            VALUE 'INQUIRE '
        88  AUD-LOGIN              VALUE 'LOGIN   '
        88  AUD-LOGOUT             VALUE 'LOGOUT  '
        88  AUD-STARTUP            VALUE 'STARTUP '
        88  AUD-SHUTDOWN           VALUE 'SHUTDOWN'
    05  AUD-STATUS               PIC X(4)            -- SUCC/FAIL/WARN
        88  AUD-SUCCESS            VALUE 'SUCC'
        88  AUD-FAILURE            VALUE 'FAIL'
        88  AUD-WARNING            VALUE 'WARN'
    05  AUD-KEY-INFO
        10  AUD-PORTFOLIO-ID     PIC X(8)            -- Portfolio identifier
        10  AUD-ACCOUNT-NO       PIC X(10)           -- Account number
    05  AUD-BEFORE-IMAGE         PIC X(100)          -- Pre-update record snapshot
    05  AUD-AFTER-IMAGE          PIC X(100)          -- Post-update record snapshot
    05  AUD-MESSAGE              PIC X(100)          -- Descriptive audit message
```

# Field Mappings: VSAM Copybooks → PostgreSQL / Java

Field-by-field mapping from the COBOL copybooks of the COBOL Legacy Benchmark Suite to the
PostgreSQL baseline schema (`ddl/V1__baseline_schema.sql`) and Java types.

General conversion rules:

| COBOL construct | SQL type | Java type | Notes |
|---|---|---|---|
| `PIC S9(n)V9(m) COMP-3` (packed decimal) | `NUMERIC(n+m, m)` | `java.math.BigDecimal` | Exact decimal; sign preserved by SQL/Java. Never float/double. |
| `PIC X(n)` (alphanumeric) | `VARCHAR(n)` / `CHAR(n)` for fixed-width codes | `String` | COBOL pads with trailing spaces; trim on read, no padding needed on write. |
| `PIC 9(8)` date (YYYYMMDD) | `DATE` | `java.time.LocalDate` | Unsigned zoned decimal; parse `yyyyMMdd`. Zero value = null. |
| `PIC X(8)` date / `PIC X(6)` time | `DATE` / `TIME` | `LocalDate` / `LocalTime` | Character-form `YYYYMMDD` / `HHMMSS`. |
| `PIC X(26)` timestamp | `TIMESTAMP` | `java.time.LocalDateTime` | DB2 timestamp format `YYYY-MM-DD-HH.MM.SS.NNNNNN`. |
| `PIC S9(4) COMP` (binary halfword) | `SMALLINT` | `short` / `Integer` | |
| `FILLER` | *(not migrated)* | — | Reserved space in the VSAM record; carries no data. |
| 88-level condition names | `CHECK` constraint | enum / constants | Documented per field below. |

## PORTFLIO.cpy (`PORT-RECORD`) → `PORTFOLIO_MASTER`

VSAM KSDS primary key `PORT-KEY` = `PORT-ID` + `PORT-ACCOUNT-NO` → composite PK `(PORTFOLIO_ID, ACCOUNT_NO)`.

| Copybook field | PIC clause | SQL column | SQL type | Java type | Notes |
|---|---|---|---|---|---|
| PORT-ID | PIC X(8) | PORTFOLIO_ID | CHAR(8) | String | PK part 1; fixed-width identifier. |
| PORT-ACCOUNT-NO | PIC X(10) | ACCOUNT_NO | CHAR(10) | String | PK part 2. |
| PORT-CLIENT-NAME | PIC X(30) | CLIENT_NAME | VARCHAR(30) | String | Trailing spaces trimmed. |
| PORT-CLIENT-TYPE | PIC X(1) | CLIENT_TYPE | CHAR(1) | String/enum | 88-levels: 'I' Individual, 'C' Corporate, 'T' Trust (CHECK). |
| PORT-CREATE-DATE | PIC 9(8) | CREATE_DATE | DATE | LocalDate | YYYYMMDD numeric. |
| PORT-LAST-MAINT | PIC 9(8) | LAST_MAINT_DATE | DATE | LocalDate | YYYYMMDD numeric; 0 → NULL. |
| PORT-STATUS | PIC X(1) | STATUS | CHAR(1) | String/enum | 88-levels: 'A' Active, 'C' Closed, 'S' Suspended (CHECK). |
| PORT-TOTAL-VALUE | PIC S9(13)V99 COMP-3 | TOTAL_VALUE | NUMERIC(15,2) | BigDecimal | Signed packed decimal. |
| PORT-CASH-BALANCE | PIC S9(13)V99 COMP-3 | CASH_BALANCE | NUMERIC(15,2) | BigDecimal | Signed packed decimal. |
| PORT-LAST-USER | PIC X(8) | LAST_MAINT_USER | VARCHAR(8) | String | |
| PORT-LAST-TRANS | PIC 9(8) | LAST_TRANS_NO | BIGINT | Long | Unsigned 8-digit transaction sequence. |
| PORT-FILLER | PIC X(50) | — | — | — | Reserved space, not migrated. |

## TRNREC.cpy (`TRANSACTION-RECORD`) → `PORTFOLIO_TRANSACTION`

VSAM KSDS primary key `TRN-KEY` = `TRN-DATE` + `TRN-TIME` + `TRN-PORTFOLIO-ID` + `TRN-SEQUENCE-NO`
→ composite PK `(TRANS_DATE, TRANS_TIME, PORTFOLIO_ID, SEQUENCE_NO)`.

| Copybook field | PIC clause | SQL column | SQL type | Java type | Notes |
|---|---|---|---|---|---|
| TRN-DATE | PIC X(08) | TRANS_DATE | DATE | LocalDate | PK part 1; YYYYMMDD character. |
| TRN-TIME | PIC X(06) | TRANS_TIME | TIME | LocalTime | PK part 2; HHMMSS character. |
| TRN-PORTFOLIO-ID | PIC X(08) | PORTFOLIO_ID | CHAR(8) | String | PK part 3; FK → PORTFOLIO_MASTER(PORTFOLIO_ID). |
| TRN-SEQUENCE-NO | PIC X(06) | SEQUENCE_NO | CHAR(6) | String | PK part 4; zero-padded sequence kept as CHAR to preserve format. |
| TRN-INVESTMENT-ID | PIC X(10) | INVESTMENT_ID | CHAR(10) | String | |
| TRN-TYPE | PIC X(02) | TRANS_TYPE | CHAR(2) | String/enum | 88-levels: 'BU' Buy, 'SL' Sell, 'TR' Transfer, 'FE' Fee (CHECK). |
| TRN-QUANTITY | PIC S9(11)V9(4) COMP-3 | QUANTITY | NUMERIC(15,4) | BigDecimal | Signed packed decimal. |
| TRN-PRICE | PIC S9(11)V9(4) COMP-3 | PRICE | NUMERIC(15,4) | BigDecimal | Signed packed decimal. |
| TRN-AMOUNT | PIC S9(13)V9(2) COMP-3 | AMOUNT | NUMERIC(15,2) | BigDecimal | Signed packed decimal. |
| TRN-CURRENCY | PIC X(03) | CURRENCY_CODE | CHAR(3) | String | ISO 4217 code. |
| TRN-STATUS | PIC X(01) | STATUS | CHAR(1) | String/enum | 88-levels: 'P' Pending, 'D' Done, 'F' Failed, 'R' Reversed (CHECK). |
| TRN-PROCESS-DATE | PIC X(26) | PROCESS_DATE | TIMESTAMP | LocalDateTime | DB2 timestamp format. |
| TRN-PROCESS-USER | PIC X(08) | PROCESS_USER | VARCHAR(8) | String | |
| TRN-FILLER | PIC X(50) | — | — | — | Reserved space, not migrated. |

## POSREC.cpy (`POSITION-RECORD`) → `PORTFOLIO_POSITION`

VSAM KSDS primary key `POS-KEY` = `POS-PORTFOLIO-ID` + `POS-DATE` + `POS-INVESTMENT-ID`
→ composite PK `(PORTFOLIO_ID, POSITION_DATE, INVESTMENT_ID)`.

| Copybook field | PIC clause | SQL column | SQL type | Java type | Notes |
|---|---|---|---|---|---|
| POS-PORTFOLIO-ID | PIC X(08) | PORTFOLIO_ID | CHAR(8) | String | PK part 1; FK → PORTFOLIO_MASTER(PORTFOLIO_ID). |
| POS-DATE | PIC X(08) | POSITION_DATE | DATE | LocalDate | PK part 2; YYYYMMDD character. |
| POS-INVESTMENT-ID | PIC X(10) | INVESTMENT_ID | CHAR(10) | String | PK part 3. |
| POS-QUANTITY | PIC S9(11)V9(4) COMP-3 | QUANTITY | NUMERIC(15,4) | BigDecimal | Signed packed decimal. |
| POS-COST-BASIS | PIC S9(13)V9(2) COMP-3 | COST_BASIS | NUMERIC(15,2) | BigDecimal | Signed packed decimal. |
| POS-MARKET-VALUE | PIC S9(13)V9(2) COMP-3 | MARKET_VALUE | NUMERIC(15,2) | BigDecimal | Signed packed decimal. |
| POS-CURRENCY | PIC X(03) | CURRENCY_CODE | CHAR(3) | String | ISO 4217 code. |
| POS-STATUS | PIC X(01) | STATUS | CHAR(1) | String/enum | 88-levels: 'A' Active, 'C' Closed, 'P' Pending (CHECK). |
| POS-LAST-MAINT-DATE | PIC X(26) | LAST_MAINT_DATE | TIMESTAMP | LocalDateTime | DB2 timestamp format. |
| POS-LAST-MAINT-USER | PIC X(08) | LAST_MAINT_USER | VARCHAR(8) | String | |
| POS-FILLER | PIC X(50) | — | — | — | Reserved space, not migrated. |

## HISTREC.cpy (`HISTORY-RECORD`) → `HISTORY_RECORD`

VSAM KSDS primary key `HIST-KEY` = `HIST-PORTFOLIO-ID` + `HIST-DATE` + `HIST-TIME` + `HIST-SEQ-NO`
→ composite PK `(PORTFOLIO_ID, HIST_DATE, HIST_TIME, SEQ_NO)`.

| Copybook field | PIC clause | SQL column | SQL type | Java type | Notes |
|---|---|---|---|---|---|
| HIST-PORTFOLIO-ID | PIC X(08) | PORTFOLIO_ID | CHAR(8) | String | PK part 1; FK → PORTFOLIO_MASTER(PORTFOLIO_ID). |
| HIST-DATE | PIC X(08) | HIST_DATE | DATE | LocalDate | PK part 2; YYYYMMDD character. |
| HIST-TIME | PIC X(06) | HIST_TIME | TIME | LocalTime | PK part 3; HHMMSS character. |
| HIST-SEQ-NO | PIC X(04) | SEQ_NO | CHAR(4) | String | PK part 4; zero-padded sequence kept as CHAR. |
| HIST-RECORD-TYPE | PIC X(02) | RECORD_TYPE | CHAR(2) | String/enum | 88-levels: 'PT' Portfolio, 'PS' Position, 'TR' Transaction (CHECK). |
| HIST-ACTION-CODE | PIC X(01) | ACTION_CODE | CHAR(1) | String/enum | 88-levels: 'A' Add, 'C' Change, 'D' Delete (CHECK). |
| HIST-BEFORE-IMAGE | PIC X(400) | BEFORE_IMAGE | VARCHAR(400) | String | Raw record image before change. |
| HIST-AFTER-IMAGE | PIC X(400) | AFTER_IMAGE | VARCHAR(400) | String | Raw record image after change. |
| HIST-REASON-CODE | PIC X(04) | REASON_CODE | CHAR(4) | String | |
| HIST-PROCESS-DATE | PIC X(26) | PROCESS_DATE | TIMESTAMP | LocalDateTime | DB2 timestamp format. |
| HIST-PROCESS-USER | PIC X(08) | PROCESS_USER | VARCHAR(8) | String | |
| HIST-FILLER | PIC X(50) | — | — | — | Reserved space, not migrated. |

## ERRHAND.cpy (`ERR-MESSAGE`) → `ERROR_LOG`

ERRHAND.cpy is a working-storage copybook; the persisted structure is `ERR-MESSAGE`
(constants `ERR-CATEGORIES`, `ERR-RETURN-CODES`, `ERR-VSAM-STATUSES`, `ERR-VSAM-MSGS` are
program literals, not stored fields — they become Java constants/enums). Log table → surrogate
identity PK `ERROR_LOG_ID` (no VSAM key; insert-only).

| Copybook field | PIC clause | SQL column | SQL type | Java type | Notes |
|---|---|---|---|---|---|
| ERR-DATE | PIC X(10) | ERROR_DATE | DATE | LocalDate | YYYY-MM-DD character. |
| ERR-TIME | PIC X(8) | ERROR_TIME | TIME | LocalTime | HH.MM.SS / HH:MM:SS character. |
| ERR-PROGRAM | PIC X(8) | PROGRAM_ID | VARCHAR(8) | String | |
| ERR-CATEGORY | PIC X(2) | ERROR_CATEGORY | CHAR(2) | String/enum | 'VS' VSAM, 'VL' Validation, 'PR' Processing, 'SY' System (CHECK). |
| ERR-CODE | PIC X(4) | ERROR_CODE | CHAR(4) | String | |
| ERR-SEVERITY | PIC S9(4) COMP | ERROR_SEVERITY | SMALLINT | short/Integer | Binary halfword; values 0/4/8/12/16 per ERR-RETURN-CODES. |
| ERR-TEXT | PIC X(80) | ERROR_TEXT | VARCHAR(80) | String | |
| ERR-DETAILS | PIC X(256) | ERROR_DETAILS | VARCHAR(256) | String | |

Constant groups (not persisted): ERR-CAT-VSAM/VALID/PROC/SYSTEM (PIC X(2) literals),
ERR-SUCCESS/WARNING/ERROR/SEVERE/TERMINAL (S9(4) COMP literals), ERR-VSAM-SUCCESS/DUPKEY/
NOTFND/EOF (PIC X(2) literals), ERR-VSAM-22/23/OTHER (PIC X(80) message literals).

## AUDITLOG.cpy (`AUDIT-RECORD`) → `AUDIT_LOG`

Log table → surrogate identity PK `AUDIT_LOG_ID` (insert-only audit trail).

| Copybook field | PIC clause | SQL column | SQL type | Java type | Notes |
|---|---|---|---|---|---|
| AUD-TIMESTAMP | PIC X(26) | AUDIT_TIMESTAMP | TIMESTAMP | LocalDateTime | DB2 timestamp format. |
| AUD-SYSTEM-ID | PIC X(8) | SYSTEM_ID | VARCHAR(8) | String | |
| AUD-USER-ID | PIC X(8) | USER_ID | VARCHAR(8) | String | |
| AUD-PROGRAM | PIC X(8) | PROGRAM_ID | VARCHAR(8) | String | |
| AUD-TERMINAL | PIC X(8) | TERMINAL_ID | VARCHAR(8) | String | |
| AUD-TYPE | PIC X(4) | AUDIT_TYPE | CHAR(4) | String/enum | 88-levels: 'TRAN', 'USER', 'SYST' (CHECK). |
| AUD-ACTION | PIC X(8) | AUDIT_ACTION | CHAR(8) | String/enum | 88-levels: 'CREATE', 'UPDATE', 'DELETE', 'INQUIRE', 'LOGIN', 'LOGOUT', 'STARTUP', 'SHUTDOWN' — space-padded to 8 chars in COBOL; CHECK compares RTRIM'd value. |
| AUD-STATUS | PIC X(4) | AUDIT_STATUS | CHAR(4) | String/enum | 88-levels: 'SUCC', 'FAIL', 'WARN' (CHECK). |
| AUD-PORTFOLIO-ID | PIC X(8) | PORTFOLIO_ID | CHAR(8) | String | Optional reference to portfolio. |
| AUD-ACCOUNT-NO | PIC X(10) | ACCOUNT_NO | CHAR(10) | String | Optional reference to account. |
| AUD-BEFORE-IMAGE | PIC X(100) | BEFORE_IMAGE | VARCHAR(100) | String | |
| AUD-AFTER-IMAGE | PIC X(100) | AFTER_IMAGE | VARCHAR(100) | String | |
| AUD-MESSAGE | PIC X(100) | AUDIT_MESSAGE | VARCHAR(100) | String | |

## Signs and Padding Notes

- **COMP-3 (packed decimal)**: stores sign in the final nibble (C=+, D=−, F=unsigned).
  PostgreSQL `NUMERIC` and Java `BigDecimal` carry the sign natively; no special handling needed
  after conversion. ETL from raw VSAM data must unpack nibbles and apply the sign nibble.
- **Zoned decimal (`PIC 9(n)` / `PIC S9(n)`)**: sign overpunched in the last byte for signed fields.
  All zoned fields here are unsigned dates/sequences.
- **Alphanumeric padding**: COBOL `PIC X(n)` fields are space-padded to length n. `CHAR(n)` columns
  preserve fixed width (Postgres pads with spaces); `VARCHAR` columns should be stored trimmed.
  Comparisons against 88-level values that include trailing spaces (e.g. `AUD-ACTION` `'INQUIRE '`)
  must trim before comparing in Java.
- **Low-values / spaces in numeric fields**: legacy VSAM records may contain spaces or low-values
  in numeric fields for "empty" records; ETL should map these to NULL or 0 per field nullability.
- **Dates**: numeric `PIC 9(8)` and character `PIC X(8)` dates are both `YYYYMMDD`; value `00000000`
  or spaces → NULL.

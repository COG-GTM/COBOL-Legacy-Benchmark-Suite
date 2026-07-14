# CLBS Java Migration - Conventions

This document records the mapping conventions used to translate the CLBS COBOL copybooks and data structures into Java.

## Numeric types

### Packed decimal (COMP-3)

COBOL `PIC S9(m)V9(n) COMP-3` fields are mapped to `java.math.BigDecimal` with the exact precision and scale.

| COBOL picture | Precision | Scale | Java type | JPA annotation |
|---------------|-----------|-------|-----------|----------------|
| `PIC S9(13)V99 COMP-3` | 15 | 2 | `BigDecimal` | `@Column(precision = 15, scale = 2)` |
| `PIC S9(11)V9(4) COMP-3` | 15 | 4 | `BigDecimal` | `@Column(precision = 15, scale = 4)` |
| `PIC S9(11)V9(4) COMP-3` | 15 | 4 | `BigDecimal` | `@Column(precision = 15, scale = 4)` |
| `PIC S9(13)V9(2) COMP-3` | 15 | 2 | `BigDecimal` | `@Column(precision = 15, scale = 2)` |
| `PIC S9(13)V9(2) COMP-3` | 15 | 2 | `BigDecimal` | `@Column(precision = 15, scale = 2)` |

Precision is `m + n`; scale is `n`. The sign is stored in the low-order nibble of the packed decimal and is represented by `BigDecimal` sign.

### Binary / computational (COMP)

Small binary integer fields are mapped to Java boxed integers or `BigDecimal` depending on usage.

| COBOL picture | Java type | Notes |
|---------------|-----------|-------|
| `PIC S9(4) COMP` | `Integer` | Return codes, small counters |
| `PIC S9(8) COMP` | `Integer` or `Long` | Counters, record IDs |
| `PIC S9(4) COMP-3` | `BigDecimal` (scale 0) | Packed-digit return codes |

## Display / alphanumeric types

### `PIC X(n)`

- Mapped to `java.lang.String`.
- COBOL `PIC X(n)` values are left-aligned and space-padded to the declared length.
- For comparisons, use `String.trim()` or `StringUtils.trimToEmpty()` when trailing spaces are not significant.
- JPA `String` columns are declared with the same length as the `PIC` clause.

### `PIC 9(n)` (display numeric)

- Values used as identifiers or codes (e.g., `ACCOUNT-NO`, `TRANS-ID`) are mapped to `String` if they are not used in arithmetic.
- Values used in arithmetic should be converted to `BigDecimal` or `Integer` on read and padded on write.

## Date and time formats

Dates are stored as `YYYYMMDD` strings in COBOL. The Java mapping uses `String` for the raw values and `java.time.LocalDate` when date arithmetic is required.

| COBOL | Format | Java raw | Java computed |
|-------|--------|----------|---------------|
| `PIC 9(8)` | `YYYYMMDD` | `String` | `LocalDate` via `DateTimeFormatter.BASIC_ISO_DATE` |
| `PIC X(8)` | `YYYYMMDD` | `String` | `LocalDate` via `DateTimeFormatter.BASIC_ISO_DATE` |
| `PIC 9(6)` | `HHMMSS` | `String` | `LocalTime` via `DateTimeFormatter.ofPattern("HHmmss")` |
| `PIC X(26)` | ISO timestamp | `String` | `LocalDateTime` via `DateTimeFormatter.ISO_LOCAL_DATE_TIME` |

When persisting to the relational target, `YYYYMMDD` and `HHMMSS` should be converted to `DATE`/`TIME` or `TIMESTAMP` columns. For the JPA entities in this project, raw string fields are preserved where the copybook defines them as `X` or `9` fixed-length strings, and `LocalDate`/`LocalDateTime` are used where the DB2 DDL defines `DATE`/`TIMESTAMP` columns.

## String padding rules

- **Fixed-length strings**: COBOL `PIC X(n)` values are left-justified and padded on the right with spaces. Java serialization back to COBOL records should use `StringUtils.rightPad(value, n)` to restore the original length.
- **Zero-padded numeric strings**: `PIC 9(n)` values are right-justified and padded on the left with `0` (e.g., `00012345`). Use `StringUtils.leftPad(value, n, '0')` when writing them.
- **Packed decimals**: `BigDecimal` must be converted using `BigDecimal.setScale(n)` before writing to ensure the correct number of decimal places. The absolute value must not exceed the declared precision.

## Example: `PORTFLIO` copybook mapping

```cobol
01  PORT-RECORD.
    05  PORT-KEY.
        10  PORT-ID             PIC X(8).
        10  PORT-ACCOUNT-NO     PIC X(10).
    05  PORT-CLIENT-INFO.
        10  PORT-CLIENT-NAME    PIC X(30).
        10  PORT-CLIENT-TYPE    PIC X(1).
    05  PORT-PORTFOLIO-INFO.
        10  PORT-CREATE-DATE    PIC 9(8).
        10  PORT-LAST-MAINT     PIC 9(8).
        10  PORT-STATUS         PIC X(1).
    05  PORT-FINANCIAL-INFO.
        10  PORT-TOTAL-VALUE    PIC S9(13)V99 COMP-3.
        10  PORT-CASH-BALANCE   PIC S9(13)V99 COMP-3.
    05  PORT-AUDIT-INFO.
        10  PORT-LAST-USER      PIC X(8).
        10  PORT-LAST-TRANS     PIC 9(8).
```

Maps to the `Portfolio` entity:

| COBOL field | Java field | Type |
|-------------|-----------|------|
| `PORT-ID` | `portfolioId` | `String` (8) |
| `PORT-ACCOUNT-NO` | `accountNumber` | `String` (10) |
| `PORT-CLIENT-NAME` | `clientName` | `String` (30) |
| `PORT-CLIENT-TYPE` | `clientType` | `String` (1) |
| `PORT-CREATE-DATE` | `createDate` | `String` (8) / `YYYYMMDD` |
| `PORT-LAST-MAINT` | `lastMaintDate` | `String` (8) / `YYYYMMDD` |
| `PORT-STATUS` | `status` | `String` (1) |
| `PORT-TOTAL-VALUE` | `totalValue` | `BigDecimal` (precision 15, scale 2) |
| `PORT-CASH-BALANCE` | `cashBalance` | `BigDecimal` (precision 15, scale 2) |
| `PORT-LAST-USER` | `lastUser` | `String` (8) |
| `PORT-LAST-TRANS` | `lastTrans` | `String` (8) / `YYYYMMDD` |

## Validation / golden master

The `PORTVALD` validation module is ported to `PortfolioValidationService` and uses the same return-code constants from `PORTVAL`:

| Constant | Value | Meaning |
|----------|-------|---------|
| `VAL-SUCCESS` | `0` | Valid |
| `VAL-INVALID-ID` | `1` | Invalid portfolio ID |
| `VAL-INVALID-ACCT` | `2` | Invalid account number |
| `VAL-INVALID-TYPE` | `3` | Invalid investment type |
| `VAL-INVALID-AMT` | `4` | Amount outside valid range |

The golden-master harness reads `inputs.csv` and `expected-outputs.csv` and compares the Java service output against the expected values. This pattern is intended to be reused for later migrated programs by adding new fixture directories and test classes.

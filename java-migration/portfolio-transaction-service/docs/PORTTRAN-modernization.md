# PORTTRAN Modernization Note (Rules Recovery)

**Source program:** `src/programs/portfolio/PORTTRAN.cbl` — *Portfolio Transaction Processing*
**Target:** Java 21 + Spring Boot module `java-migration/portfolio-transaction-service`
**Migration style:** additive — the original COBOL is left untouched.

This document recovers the business logic of PORTTRAN in plain English for
non-mainframe stakeholders and records every decision made while porting it, so
reviewers and auditors can verify behavioral equivalence.

---

## 1. What PORTTRAN does (plain English)

PORTTRAN is a nightly batch program that applies a file of portfolio
**transactions** (buys, sells, fees, transfers) to a master file of portfolio
**positions**. For each transaction it:

1. Reads the next transaction from the sequential transaction file.
2. **Validates** it (portfolio exists, transaction type is recognized, amounts
   are positive).
3. If valid, **updates the portfolio position** — adds or subtracts units and
   cost basis depending on the transaction type.
4. Writes an **audit trail** record describing what happened.
5. Keeps running totals of records read, processed, and errored, and **aborts the
   run if more than 100 errors occur** (a safety circuit breaker).

The unit of work is one transaction; the portfolio file is an indexed VSAM
dataset (KSDS) keyed by portfolio id, read and rewritten in place.

---

## 2. Copybook record layouts

### 2.1 Transaction input — `TRNREC` (`01 TRANSACTION-RECORD`)

| COBOL field | PIC | Meaning | Java |
|---|---|---|---|
| `TRN-DATE` | `X(08)` | Transaction date YYYYMMDD | `transactionDate` |
| `TRN-TIME` | `X(06)` | Transaction time HHMMSS | `transactionTime` |
| `TRN-PORTFOLIO-ID` | `X(08)` | Portfolio id | `portfolioId` |
| `TRN-SEQUENCE-NO` | `X(06)` | Sequence number | `sequenceNo` |
| `TRN-INVESTMENT-ID` | `X(10)` | Investment id | `investmentId` |
| `TRN-TYPE` | `X(02)` | BU / SL / TR / FE | `type` (raw) |
| `TRN-QUANTITY` | `S9(11)V9(4) COMP-3` | Units, **scale 4** | `quantity` `BigDecimal` |
| `TRN-PRICE` | `S9(11)V9(4) COMP-3` | Unit price, **scale 4** | `price` `BigDecimal` |
| `TRN-AMOUNT` | `S9(13)V9(2) COMP-3` | Money, **scale 2** | `amount` `BigDecimal` |
| `TRN-CURRENCY` | `X(03)` | ISO currency | `currency` |
| `TRN-STATUS` | `X(01)` | P/D/F/R | `status` |

The composite key `TRN-KEY = TRN-DATE + TRN-TIME + TRN-PORTFOLIO-ID +
TRN-SEQUENCE-NO` uniquely identifies a transaction — this is the natural
**duplicate-transaction detection key** (`PortfolioTransaction.naturalKey()`).

### 2.2 Portfolio position — `PORTREC` (reconstructed)

> **Important finding:** PORTTRAN does `COPY PORTREC`, but **no `PORTREC` copybook
> exists in the repository.** The fields below are reconstructed from how the
> program actually uses the record. This is the single biggest field-mapping
> ambiguity in this migration and should be confirmed against the real copybook
> if/when it is located.

| COBOL field (used by PORTTRAN) | Inferred PIC | Java |
|---|---|---|
| `PORT-ID` (RECORD KEY) | `X(8)` | `portfolioId` (PK) |
| `PORT-ACCOUNT-NO` | `X(10)` | `accountNo` |
| `PORT-TOTAL-UNITS` | `S9(11)V9(4) COMP-3` (matches `TRN-QUANTITY`) | `totalUnits` `DECIMAL(15,4)` |
| `PORT-TOTAL-COST` | `S9(13)V9(2) COMP-3` (matches `TRN-AMOUNT`) | `totalCost` `DECIMAL(15,2)` |

The closest *documented* layout is `PORTFLIO.cpy` (`01 PORT-RECORD`), but it
defines `PORT-TOTAL-VALUE` / `PORT-CASH-BALANCE` rather than
`PORT-TOTAL-UNITS` / `PORT-TOTAL-COST`. PORTTRAN clearly uses the latter names,
so PORTREC is a distinct (missing) layout. The scales above are chosen so the
`ADD`/`SUBTRACT` operations against the transaction fields are exact.

---

## 3. Business rules (paragraph by paragraph)

| COBOL paragraph | Rule | Java method |
|---|---|---|
| `2110-CHECK-PORTFOLIO` | Blank portfolio id → `"Portfolio ID is required"`; otherwise the portfolio must exist (`INVALID KEY` → `"Invalid Portfolio ID: <id>"`) | `checkPortfolio` |
| `2120-CHECK-TRANSACTION-TYPE` | Type must be one of BU/SL/TR/FE, else `"Invalid Transaction Type: <type>"` | `checkTransactionType` |
| `2130-CHECK-AMOUNTS` | `quantity <= 0` → error; `price <= 0` **and type ≠ TR** → error; `amount <= 0` **and type ≠ TR** → error | `checkAmounts` |
| `2210-PROCESS-BUY` | `totalUnits += quantity`, `totalCost += amount` | `processBuy` |
| `2220-PROCESS-SELL` | If `totalUnits < quantity` → `"Insufficient units for sale"`; else `totalUnits -= quantity`, `totalCost -= amount` | `processSell` |
| `2230-PROCESS-TRANSFER` | Always `"Transfer processing not implemented"` | `processTransfer` |
| `2240-PROCESS-FEE` | `totalCost -= amount` (units unchanged) | `processFee` |
| `2300/2310` | Write audit record; action = CREATE(BU)/DELETE(SL)/UPDATE(TR,FE) | `writeAuditTrail` |
| `0000-MAIN` loop | Process until end-of-file **or error count > 100** | `processBatch` |

**Validation ordering matters:** checks run portfolio → type → amounts and stop
at the first failure (`IF ERR-TEXT = SPACES` guards each step). The boundary on
SELL is strict `<` (selling *exactly* the held units is allowed).

---

## 4. Decimal / arithmetic parity (COBOL fixed-point vs Java `BigDecimal`)

This is the most audit-sensitive area. Findings:

1. **Never floating point.** All money/quantity fields are COBOL packed decimal
   (`COMP-3`) with a fixed implied scale, modeled as `BigDecimal` with a fixed
   `scale` (4 for units/quantity/price, 2 for money). H2/Postgres columns are
   `DECIMAL(15,s)`.

2. **Truncation, not rounding.** COBOL `ADD`/`SUBTRACT`/`MOVE` **without the
   `ROUNDED` phrase truncate** excess fractional digits toward zero. PORTTRAN
   never uses `ROUNDED`, so the Java code normalizes values into the field scale
   with `RoundingMode.DOWN` (`CobolDecimal.normalize`). Example: a quantity of
   `10.00009` becomes `10.0000`, **not** `10.0001`. This is asserted by the test
   `quantityTruncatedNotRounded`.

3. **Same-scale arithmetic is exact.** Because `PORT-TOTAL-UNITS` and
   `TRN-QUANTITY` share scale 4 (and the cost/amount fields share scale 2), the
   add/subtract operations introduce no rounding at all — the values line up
   digit-for-digit with the mainframe.

4. **Overflow / `SIZE ERROR` divergence (documented, not replicated).** PORTTRAN
   specifies no `ON SIZE ERROR`, so on the mainframe a result exceeding the
   field's digit count silently loses high-order digits. Java `BigDecimal` grows
   unbounded instead. Given the bounded inputs used by this flow this does not
   arise in practice, but it is a genuine semantic difference and is called out
   here for auditors. If strict parity on overflow is required, add a
   precision (`TOTAL_DIGITS = 15`) guard.

---

## 5. Safeguards recovered

| Safeguard | In PORTTRAN? | Modernization |
|---|---|---|
| **Error circuit breaker** | Yes — loop stops once `WS-ERROR-COUNT > 100` | `processBatch` stops and flags `halted=true` (trips at 101 errors) |
| **Audit before-image** | Yes — `MOVE PORT-RECORD TO AUD-BEFORE-IMAGE` for recovery | `AuditRecord` written per processed transaction |
| **Duplicate-transaction prevention** | Implicit via unique `TRN-KEY` | exposed as `PortfolioTransaction.naturalKey()`; enforcement is a candidate DB unique constraint |
| **Checkpoint / restart** | Not in PORTTRAN (lives in `CKPRST.cbl`) | out of scope for this entity; noted for the broader batch flow |

---

## 6. Defect recovered: `2200-UPDATE-POSITIONS` is never invoked

In the COBOL **as written**, the main loop `2000-PROCESS-TRANSACTIONS` only
performs `2100-VALIDATE-TRANSACTION`. The paragraph `2200-UPDATE-POSITIONS`
(and its BUY/SELL/FEE/TRANSFER children and audit call) is **defined but never
`PERFORM`ed** — so the program as written validates transactions and counts
them, but **never actually updates any position or writes any audit record**.
This is a latent wiring defect.

The modernized service wires **validation → update** as the program's structure
clearly intends (`processTransaction` = `validateTransaction` + `updatePositions`).
To keep the as-written validation behavior independently verifiable,
`validateTransaction(...)` is exposed as a separate public method and tested on
its own. This decision is intentional and flagged here for reviewers.

---

## 7. How to verify parity

```bash
cd java-migration/portfolio-transaction-service
JAVA_HOME=/path/to/jdk-21 mvn clean test
```

The suite `PortfolioTransactionServiceTest` (20 tests) drives representative
**normal, boundary, and error** inputs and asserts outputs derived directly from
the COBOL above. Each test's display name cites the COBOL paragraph it verifies.

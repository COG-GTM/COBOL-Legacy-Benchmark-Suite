# Position valuation and update — Java 21 / Spring Boot port

A behaviour-preserving port of the position valuation and update slice of the COBOL Investment
Portfolio Management System. The behavioural specification it implements is
[`docs/modernization/position-valuation-spec.md`](../../docs/modernization/position-valuation-spec.md);
rule numbers below (`R-x.y`) and open questions (`OQ-n`) refer to that document.

Nothing under `src/programs/`, `src/copybook/` or `data/` is modified by this module.

```
mvn -f modernization/position-valuation/pom.xml clean test
```

Java 21, Spring Boot 3.3.5, no database, no network. Money is `BigDecimal` throughout, and every
store states its scale and `RoundingMode` (§11 of the spec).

## What this slice does, and one thing it does not

`PORTTRAN` as committed **validates transactions and never updates a position** — no
`PERFORM 2200-UPDATE-POSITIONS` statement exists in the program (R-10.1). The named entry point
for the slice, `src/programs/batch/POSUPDT.cbl`, is a zero-byte file. Both behaviours are
therefore available and the choice is explicit:

| `clbs.porttran.apply-updates` | Behaviour |
| --- | --- |
| `false` (default) | Reproduces the COBOL exactly: validate, count, log; no balance changes. |
| `true` | Additionally runs the buy/sell/transfer/fee paragraphs the program contains but never reaches. |

This is **OQ-2** and needs a human answer before cutover.

## COBOL to Java map

### `PORTVALD.cbl` → `validation/PortfolioValidator`

| COBOL paragraph | Java method |
| --- | --- |
| `0000-MAIN` | `validate(char, String)` |
| `1000-VALIDATE-ID` | `validatePortfolioId(String)` |
| `2000-VALIDATE-ACCOUNT` | `validateAccountNumber(String)` |
| `3000-VALIDATE-TYPE` | `validateInvestmentType(String)` |
| `4000-VALIDATE-AMOUNT` | `validateAmount(String)` |

Linkage `LS-RETURN-CODE`/`LS-ERROR-MSG` → `validation/ValidationResult`; the `88`-levels on
`LS-VALIDATE-TYPE` → `validation/ValidationType`. The defects in R-1.3, R-2.2 and R-4.1 are
reproduced, not corrected, and pinned by golden vectors.

### `PORTTRAN.cbl` → three classes

| COBOL paragraph | Java method |
| --- | --- |
| `0000-MAIN`, `2000-PROCESS-TRANSACTIONS`, `3000-TERMINATE` | `batch/PositionUpdateBatch.run(List)` |
| `9000-ERROR-ROUTINE` | `batch/PositionUpdateBatch.raise(String)` + `error/ErrorProcessor` |
| `2100-VALIDATE-TRANSACTION` | `service/TransactionValidationService.validate(TransactionRecord)` |
| `2110-CHECK-PORTFOLIO` | `service/TransactionValidationService.checkPortfolio` |
| `2120-CHECK-TRANSACTION-TYPE` | `service/TransactionValidationService.checkTransactionType` |
| `2130-CHECK-AMOUNTS` | `service/TransactionValidationService.checkAmounts` |
| `2200-UPDATE-POSITIONS` | `service/PositionUpdateService.apply(TransactionRecord)` |
| `2210-PROCESS-BUY` | `service/PositionUpdateService.processBuy` |
| `2220-PROCESS-SELL` | `service/PositionUpdateService.processSell` |
| `2230-PROCESS-TRANSFER` | `service/PositionUpdateService.processTransfer` |
| `2240-PROCESS-FEE` | `service/PositionUpdateService.processFee` |
| `2300-UPDATE-AUDIT-TRAIL`, `2310-WRITE-AUDIT-RECORD` | `service/PositionUpdateService.writeAuditTrail` + `audit/AuditTrailWriter` |

### `PORTUPDT.cbl` → `service/PortfolioUpdateService`

| COBOL paragraph | Java method |
| --- | --- |
| `2100-PROCESS-UPDATE` | `applyUpdate(boolean, char, String)` |
| `2200-APPLY-UPDATE` (`V` branch) | `convertValue(String)` |

### `RPTPOS00.cbl` → `service/PositionValuationService`

| COBOL paragraph | Java method |
| --- | --- |
| `2110-FORMAT-POSITION` (the `COMPUTE`) | `changePercent(BigDecimal, BigDecimal)` |

### Subroutines and copybooks

| COBOL | Java |
| --- | --- |
| `ERRPROC.cbl`, `ERRHAND.cpy` | `error/ErrorProcessor`, `error/ErrorRecord` |
| `AUDPROC.cbl`, `AUDITLOG.cpy` | `audit/AuditTrailWriter`, `audit/AuditRecord` |
| `TRNREC.cpy` | `domain/TransactionRecord`, `domain/TransactionType` |
| `POSREC.cpy` | `domain/PositionRecord` |
| `PORTREC` (missing — OQ-1) | `domain/PortfolioPosition` |
| `PORTFILE` VSAM KSDS (`READ`/`REWRITE … INVALID KEY`) | `repository/PortfolioPositionStore`, `repository/InMemoryPortfolioPositionStore` |

### COBOL language semantics

The rules that make the port produce the same numbers live in `cobol/`, separate from the business
logic, so they can be reviewed once:

| Class | Models |
| --- | --- |
| `cobol/PackedField` | A `PIC S9(n)V9(m)` field: truncation toward zero at store (R-5.1) and silent high-order wrap with no `ON SIZE ERROR` (R-5.2). |
| `cobol/CobolDecimal` | `ADD`/`SUBTRACT`/`COMPUTE` against a named receiving field, with `DECIMAL128` intermediates. |
| `cobol/CobolString` | `PIC X(n)` `MOVE`, reference modification, and the `NUMERIC`/`ZEROS`/`SPACES` class tests (R-5.4). |
| `cobol/SignedEditedField` | The report field `PIC +ZZ9.99` (R-6.2). |

## Parity evidence

Expected values are captured from the compiled COBOL, not hand-computed:

```
modernization/position-valuation/parity/generate-golden-vectors.sh   # needs GnuCOBOL (cobc)
```

It compiles `parity/cobol/PVDRIVE.cbl` (calls the unmodified `PORTVALD`), `PARITHM.cbl`,
`PDIVZER.cbl` and `PUPDMOV.cbl`, and writes `src/test/resources/parity/*.csv`. The tests in
`src/test/java/com/clbs/posval/parity/` read those files and assert row by row, so a divergence
between the port and the COBOL fails the build. GnuCOBOL is needed only to regenerate the
vectors; `mvn test` runs against the committed CSVs.

## Deliberate deviations from the COBOL

Everything else is reproduced as-is, including the defects.

| Deviation | Why |
| --- | --- |
| The audit record carries a real timestamp and correctly aligned fields (R-9.4, R-9.5). | Reproducing `AUDPROC`'s field-overlay bug would write corrupt audit data. Documented as OQ-8. |
| A non-numeric `UPDT-NEW-VALUE` throws instead of producing a number (R-11.4). | The COBOL result is implementation-defined; inventing one would be a silent money bug. OQ-11. |
| A zero divisor returns an explicit size-error outcome rather than a stale value (R-6.3). | On z/OS the same statement may abend. The caller decides. OQ-9. |
| `PORTFILE` is an in-memory keyed store. | Swap in a JPA/JDBC implementation of `PortfolioPositionStore`; no business logic depends on which is bound. |

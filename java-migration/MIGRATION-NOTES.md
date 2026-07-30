# Portfolio TRANSACTION entity — COBOL → Java 21 / Spring Boot 3 migration notes

This module migrates **one** entity of the legacy investment/annuity portfolio system: the portfolio
**transaction**, defined by the copybook `src/copybook/common/TRNREC.cpy` and stored on the VSAM KSDS
`TRANHIST`. No COBOL source was modified — the legacy programs stay in place so the before/after can
be compared side by side.

Sources used:

| Artefact | Path | What was taken from it |
| --- | --- | --- |
| Copybook | `src/copybook/common/TRNREC.cpy` | Record layout, field widths, 88-level values |
| Transaction processing | `src/programs/portfolio/PORTTRAN.cbl` | Validation, position update, audit action, batch counters |
| Field validation | `src/programs/portfolio/PORTVALD.cbl` + `src/copybook/common/PORTVAL.cpy` | Portfolio id / account / type / amount rules and return codes |
| Sequencing | `src/programs/batch/PRCSEQ00.cbl` | Sequence numbering convention |
| Portfolio master | `src/programs/portfolio/PORTMSTR.cbl`, `src/copybook/common/PORTFLIO.cpy` | `REWRITE` semantics, portfolio existence check |
| Positions | `src/copybook/common/POSREC.cpy` | Context for the units/cost fields updated by a transaction |
| File definition | `src/database/vsam/vsam-definitions.txt` | KSDS organization, key position/length |
| Test data generators | `src/programs/test/TSTGEN00.cbl`, `src/programs/portfolio/PORTTEST.cbl` | Conventions for the representative records (see “Test data”) |

---

## 1. Extracted business rules

Every rule is numbered, references the COBOL paragraph it came from, and is asserted by at least one
test. Rules marked **derived** are not stated literally in the COBOL — see
[Open questions](#4-open-questions-for-the-legacy-owners).

### Transaction validation — `PORTTRAN.cbl`

| # | Rule | COBOL paragraph | Java | Test |
| --- | --- | --- | --- | --- |
| BR-01 | `TRN-PORTFOLIO-ID` must not be spaces; error text `Portfolio ID is required` | `2110-CHECK-PORTFOLIO` | `TransactionValidator.checkPortfolio` | `TransactionValidatorTest.blankPortfolioIdRejected` |
| BR-02 | The portfolio record must be readable on `PORTFILE`; error text `Invalid Portfolio ID: <id>` | `2110-CHECK-PORTFOLIO` | `TransactionValidator.checkPortfolio` + `PortfolioReferenceValidator` | `TransactionValidatorTest.unknownPortfolioRejected` |
| BR-03 | `TRN-TYPE` must be one of `BU`, `SL`, `TR`, `FE`; otherwise `Invalid Transaction Type: <type>` | `2120-CHECK-TRANSACTION-TYPE` | `TransactionType` enum + `TransactionValidator.checkTransactionType` | `TransactionValidatorTest.unknownTypeRejected` |
| BR-04 | `TRN-QUANTITY` must be `> 0` — **no `TR` exemption**; `Quantity must be greater than zero` | `2130-CHECK-AMOUNTS` | `TransactionValidator.checkAmounts` | `TransactionValidatorTest.zeroQuantityRejected`, `transferWithZeroQuantityStillRejected` |
| BR-05 | `TRN-PRICE` must be `> 0` unless `TRN-TYPE = 'TR'`; `Price must be greater than zero` | `2130-CHECK-AMOUNTS` | `TransactionValidator.checkAmounts` | `TransactionValidatorTest.zeroPriceRejectedExceptForTransfer` |
| BR-06 | `TRN-AMOUNT` must be `> 0` unless `TRN-TYPE = 'TR'`; `Amount must be greater than zero` | `2130-CHECK-AMOUNTS` | `TransactionValidator.checkAmounts` | `TransactionValidatorTest.zeroAmountRejectedExceptForTransfer` |
| BR-07 | The three checks run in order 2110 → 2120 → 2130 and stop at the first that sets `ERR-TEXT` | `2100-VALIDATE-TRANSACTION` | `TransactionValidator.validate` | `TransactionValidatorTest.firstFailureWins` |
| BR-08 | The batch reads until end of file **or** `WS-ERROR-COUNT > 100`; a valid record increments `WS-PROCESS-COUNT`, an invalid one `WS-ERROR-COUNT` | `0000-MAIN`, `2100-VALIDATE-TRANSACTION` | `PortfolioTransactionService.runBatch` | `PortfolioTransactionServiceTest.batchCounters`, `batchStopsAfterHundredErrors` |

### Position update — `PORTTRAN.cbl`

| # | Rule | COBOL paragraph | Java | Test |
| --- | --- | --- | --- | --- |
| BR-09 | `BU`: `PORT-TOTAL-UNITS += TRN-QUANTITY` and `PORT-TOTAL-COST += TRN-AMOUNT` | `2210-PROCESS-BUY` | `TransactionPostingService.processBuy` | `TransactionPostingServiceTest.buyAddsUnitsAndCost` |
| BR-10 | `SL`: reject with `Insufficient units for sale` when `PORT-TOTAL-UNITS < TRN-QUANTITY`, otherwise subtract units and cost | `2220-PROCESS-SELL` | `TransactionPostingService.processSell` | `TransactionPostingServiceTest.sellSubtractsUnitsAndCost`, `sellRejectedWhenUnitsInsufficient`, `sellExactlyAvailableUnits` |
| BR-11 | `TR`: always fails with `Transfer processing not implemented` — the paragraph has no implementation | `2230-PROCESS-TRANSFER` | `TransactionPostingService.processTransfer` | `TransactionPostingServiceTest.transferNotImplemented`, `PortfolioTransactionServiceTest.transferPassesValidationButFailsProcessing` |
| BR-12 | `FE`: `PORT-TOTAL-COST -= TRN-AMOUNT`, units unchanged | `2240-PROCESS-FEE` | `TransactionPostingService.processFee` | `TransactionPostingServiceTest.feeSubtractsCostOnly` |
| BR-13 | Audit action per type: `BU → CREATE`, `SL → DELETE`, `TR → UPDATE`, `FE → UPDATE` | `2300-UPDATE-AUDIT-TRAIL` | `TransactionType.getAuditAction` | `TransactionPostingServiceTest.auditActionMapping` |
| BR-14 | Any error text increments the error counter and is reported through the error handler (`CALL 'ERRPROC'`) | `9000-ERROR-ROUTINE` | `TransactionProcessingException` / `TransactionValidationException` + `GlobalExceptionHandler` | `PortfolioTransactionControllerTest.validationFailureCarriesCobolErrorText` |

### Field validation — `PORTVALD.cbl` / `PORTVAL.cpy`

| # | Rule | COBOL paragraph | Java | Test |
| --- | --- | --- | --- | --- |
| BR-15 | Portfolio id: first 4 characters must equal `PORT` and characters 5-8 must be numeric; else return code `1` (`VAL-INVALID-ID`), message `Invalid Portfolio ID format` | `1000-VALIDATE-ID` | `PortfolioFieldValidator.validatePortfolioId` | `PortfolioFieldValidatorTest.validatePortfolioId` |
| BR-16 | Account number must be numeric and non-zero; else return code `2` (`VAL-INVALID-ACCT`) | `2000-VALIDATE-ACCOUNT` | `PortfolioFieldValidator.validateAccountNumber` | `PortfolioFieldValidatorTest.validateAccountNumber` |
| BR-17 | Investment type must be `STK`, `BND`, `MMF` or `ETF`; else return code `3` (`VAL-INVALID-TYPE`) | `3000-VALIDATE-TYPE` | `PortfolioFieldValidator.validateInvestmentType` | `PortfolioFieldValidatorTest.validInvestmentTypes`, `invalidInvestmentType` |
| BR-18 | Amount must lie within `VAL-MIN-AMOUNT = -9999999999999.99` and `VAL-MAX-AMOUNT = 9999999999999.99` inclusive; else return code `4` (`VAL-INVALID-AMT`) | `4000-VALIDATE-AMOUNT` | `PortfolioFieldValidator.validateAmount` | `PortfolioFieldValidatorTest.validateAmountRange` |
| BR-19 | `LS-VALIDATE-TYPE` dispatches `I`/`A`/`T`/`M`; anything else is rejected with `Invalid validation type` | `0000-MAIN` | `PortfolioFieldValidator.validate` | `PortfolioFieldValidatorTest.dispatchOnValidateType` |

### Sequencing and key order — `PRCSEQ00.cbl`, `TRNREC.cpy`

| # | Rule | COBOL paragraph | Java | Test |
| --- | --- | --- | --- | --- |
| BR-20 | Sequence numbers are assigned 1, 2, 3 … in read order and zero filled into `TRN-SEQUENCE-NO PIC X(06)`; the next value for a date + portfolio is the highest existing value + 1 | `1200-BUILD-SEQUENCE`, `1210-ADD-TO-SEQUENCE` | `TransactionSequenceService.nextSequenceNo` | `PortfolioTransactionServiceTest.sequenceAssignment`, `PortfolioTransactionRepositoryTest.maxSequenceNoPerDateAndPortfolio` |
| BR-21 | Records are read in VSAM key order: `TRN-DATE`, `TRN-TIME`, `TRN-PORTFOLIO-ID`, `TRN-SEQUENCE-NO` | `05 TRN-KEY`, `2000-PROCESS-TRANSACTIONS` | `TransactionKey.compareTo`, `PortfolioTransactionRepository.findAllInKeySequence` | `TransactionKeyTest.vsamKeyOrdering`, `PortfolioTransactionRepositoryTest.sequentialReadIsInKeyOrder` |

### Derived rules

| # | Rule | Basis | Java | Test |
| --- | --- | --- | --- | --- |
| BR-22 **(derived)** | `TRN-AMOUNT = TRN-QUANTITY × TRN-PRICE`, truncated (not rounded) to 2 decimals | No paragraph computes the amount; the product of two `S9(11)V9(4)` fields moved into `S9(13)V9(2)` truncates because the COBOL would carry no `ROUNDED` phrase | `TransactionAmountCalculator` | `TransactionAmountCalculatorTest` (5 tests) |
| BR-23 **(derived)** | `TRN-STATUS` transitions: `P → D`, `P → F`, `D → R`; `F` and `R` are terminal | The programs never assign `TRN-STATUS`; the model follows how `2100-VALIDATE-TRANSACTION` classifies records as processed or failed | `TransactionStatus.allowedTransitions` | `TransactionStatusTest`, `PortfolioTransactionServiceTest.statusTransitions` |

---

## 2. Copybook → Java field mapping

`01 TRANSACTION-RECORD` (`TRNREC.cpy`). Offsets are 1-based byte positions in the mainframe record.
`COMP-3` fields hold `ceil((digits + 1) / 2)` bytes, so a 15-digit packed field occupies 8 bytes.

| Bytes | COBOL field | PIC | Java field | Java type | SQL column |
| --- | --- | --- | --- | --- | --- |
| 1–8 | `TRN-DATE` | `X(08)` (`YYYYMMDD`) | `trnKey.trnDate` | `String` | `trn_date VARCHAR(8)` |
| 9–14 | `TRN-TIME` | `X(06)` (`HHMMSS`) | `trnKey.trnTime` | `String` | `trn_time VARCHAR(6)` |
| 15–22 | `TRN-PORTFOLIO-ID` | `X(08)` | `trnKey.trnPortfolioId` | `String` | `trn_portfolio_id VARCHAR(8)` |
| 23–28 | `TRN-SEQUENCE-NO` | `X(06)` | `trnKey.trnSequenceNo` | `String` | `trn_sequence_no VARCHAR(6)` |
| 29–38 | `TRN-INVESTMENT-ID` | `X(10)` | `trnInvestmentId` | `String` | `trn_investment_id VARCHAR(10)` |
| 39–40 | `TRN-TYPE` (88: `BU`/`SL`/`TR`/`FE`) | `X(02)` | `trnType` | `TransactionType` enum | `trn_type VARCHAR(2)` + `CHECK` |
| 41–48 | `TRN-QUANTITY` | `S9(11)V9(4) COMP-3` | `trnQuantity` | `BigDecimal` | `trn_quantity DECIMAL(15,4)` |
| 49–56 | `TRN-PRICE` | `S9(11)V9(4) COMP-3` | `trnPrice` | `BigDecimal` | `trn_price DECIMAL(15,4)` |
| 57–64 | `TRN-AMOUNT` | `S9(13)V9(2) COMP-3` | `trnAmount` | `BigDecimal` | `trn_amount DECIMAL(15,2)` |
| 65–67 | `TRN-CURRENCY` | `X(03)` | `trnCurrency` | `String` | `trn_currency VARCHAR(3)` |
| 68 | `TRN-STATUS` (88: `P`/`D`/`F`/`R`) | `X(01)` | `trnStatus` | `TransactionStatus` enum | `trn_status VARCHAR(1)` + `CHECK` |
| 69–94 | `TRN-PROCESS-DATE` | `X(26)` | `trnProcessDate` | `String` | `trn_process_date VARCHAR(26)` |
| 95–102 | `TRN-PROCESS-USER` | `X(08)` | `trnProcessUser` | `String` | `trn_process_user VARCHAR(8)` |
| 103–152 | `TRN-FILLER` | `X(50)` | *not mapped* | — | — |

**Total copybook length: 152 bytes** (102 bytes of data + 50 bytes of filler); key length 28 bytes.

Type decisions:

* **All monetary and quantity fields are `BigDecimal`** with explicit `precision`/`scale`; `double`
  and `float` are never used. The `DECIMAL(15,x)` columns hold the full 15-digit capacity of the
  packed fields — asserted by `PortfolioTransactionRepositoryTest.storesFifteenDigitValues`.
* **Dates and times stay `String`** inside the key so that the JVM sort order is byte-for-byte
  identical to the VSAM key sequence the batch relies on (BR-21).
  `TransactionKey.getTransactionDate()` / `getTransactionTime()` expose typed views.
* **`TRN-FILLER` is not mapped** — it is record padding, counted only for the record length.

---

## 3. COBOL operation → service operation

| COBOL | Service method | Endpoint |
| --- | --- | --- |
| Keyed `READ TRANSACTION-FILE` | `PortfolioTransactionService.findByKey` | `GET /api/v1/transactions/{transactionKey}` |
| Sequential read, `2000-PROCESS-TRANSACTIONS` | `PortfolioTransactionService.browse` | `GET /api/v1/transactions` |
| `WRITE TRANSACTION-RECORD` | `PortfolioTransactionService.insert` | `POST /api/v1/transactions` |
| `REWRITE TRANSACTION-RECORD` | `PortfolioTransactionService.rewrite` | `PUT /api/v1/transactions/{transactionKey}` |
| `TRN-STATUS` change (derived) | `PortfolioTransactionService.transitionStatus` | `POST /api/v1/transactions/{transactionKey}/status` |
| `2100-VALIDATE-TRANSACTION` + `2200-UPDATE-POSITIONS` | `PortfolioTransactionService.process` | `POST /api/v1/transactions/{transactionKey}/process` |
| `0000-MAIN` batch driver | `PortfolioTransactionService.runBatch` | not exposed over HTTP |

Traceability is machine-readable: every migrated type and method carries
`@CobolOrigin(program = …, paragraph = …, rules = …, derived = …)`
(`com.cognition.portfolio.traceability.CobolOrigin`), so
`grep -r "2220-PROCESS-SELL" java-migration/src` lands on the Java that replaces it.

The generated OpenAPI 3 contract is committed at [`docs/openapi.yaml`](docs/openapi.yaml) and served
live at `/v3/api-docs` (Swagger UI at `/swagger-ui.html`).

---

## 4. Open questions for the legacy owners

| # | Question | What was implemented | Where |
| --- | --- | --- | --- |
| OQ-1 | Nothing in the supplied programs computes `TRN-AMOUNT`; the batch only validates the value that arrives on the input file. Is the amount produced upstream, and does that program use `ROUNDED`? | The most literal reading of a COBOL `COMPUTE` **without** `ROUNDED`: quantity × price truncated to 2 decimals (BR-22). The amount may also be supplied explicitly on the API, in which case it is stored as given. | `TransactionAmountCalculator` |
| OQ-2 | The portfolio existence check (BR-02) reads the portfolio master, which is a different entity and out of scope for this slice. Should this service call a portfolio service, or is a format check enough? | `PortfolioReferenceValidator` interface; the default implementation applies the `PORTVALD 1000-VALIDATE-ID` format rule only. A deployment that owns the portfolio table supplies its own bean. | `FormatOnlyPortfolioReferenceValidator`, `TransactionMigrationConfiguration` |
| OQ-3 | `2130-CHECK-AMOUNTS` exempts `TR` from the price and amount checks but **not** from the quantity check, so a transfer with zero quantity is rejected. Intentional, or an oversight? | The literal reading: the quantity check applies to every type (BR-04). | `TransactionValidator.checkAmounts` |
| OQ-4 | `vsam-definitions.txt` declares `TRANHIST` with `RECORD LENGTH 300` and `KEY LENGTH 20`, but `TRNREC.cpy` is 152 bytes and its key group is 28 bytes. Which is authoritative? | The copybook, per the migration convention that the copybook is the source of truth for the layout. The key is modelled as 28 bytes. | `TransactionKey.KEY_LENGTH` |
| OQ-5 | No program ever assigns `TRN-STATUS`; the 88-levels `P`/`D`/`F`/`R` exist without a documented lifecycle. Which transitions are legal, and who may reverse a transaction? | The derived model `P → D`, `P → F`, `D → R`, with `F`/`R` terminal (BR-23). | `TransactionStatus` |
| OQ-6 | `2200-UPDATE-POSITIONS` (and therefore `2210`–`2240` and the audit trail) is **unreachable**: no paragraph in `PORTTRAN.cbl` performs it. `2100-VALIDATE-TRANSACTION` only bumps the counters. Is the position update dead code, or a missing `PERFORM`? | Both readings are preserved. `runBatch` is the literal batch: validate and count, no position update. `process` wires validation to the position update — the evident intent — and is what the REST API exposes. | `PortfolioTransactionService.runBatch` vs `process` |
| OQ-7 | `PORTTRAN.cbl` declares `TRANSACTION-FILE` as `ORGANIZATION IS SEQUENTIAL`, while `vsam-definitions.txt` defines `TRANHIST` as a KSDS. Is the batch fed a sequential extract of the KSDS? | The entity is modelled on the KSDS (keyed access plus key-ordered browse); the batch path is exposed as an in-memory list, mirroring a sequential feed. | `PortfolioTransactionRepository` |
| OQ-8 | `PORTVALD 2000-VALIDATE-ACCOUNT` applies `IS NOT NUMERIC` to the whole `LS-INPUT-VALUE PIC X(50)`, so a right-padded account number is only numeric if the caller pads with zeros. Is the field expected to arrive zero filled? | The check is applied to the trimmed account number the caller supplies. | `PortfolioFieldValidator.validateAccountNumber` |
| OQ-9 | `TRN-PROCESS-DATE PIC X(26)` has no documented format. Samples in the repo suggest the DB2 timestamp form `YYYY-MM-DD-HH.MM.SS.NNNNNN`. | Kept as a 26-character string; no parsing or reformatting is applied. | `PortfolioTransaction.trnProcessDate` |

---

## 5. Test data

**There is no ASCII seed extract of `TRANHIST` in this repository** — the only file-level artefact is
`src/database/vsam/vsam-definitions.txt`, which describes the KSDS but contains no records. Rather
than invent a flat file, the representative records used by the tests and by the `seed` profile were
derived from:

* the record layout and 88-level values in `TRNREC.cpy`;
* `PORTTEST.cbl` `2100-GENERATE-KEY`, which builds ids as `'PORT' + WS-RECORD-COUNT` and account
  numbers as `WS-RECORD-COUNT + 1000000000`;
* `TSTGEN00.cbl` `2300-GEN-TRANSACTION`, which writes one transaction record per configured volume.

The eight seed rows (`src/main/resources/db/seed/V900__seed_representative_transactions.sql`) cover
every `TRN-TYPE` and every `TRN-STATUS`, use sequential `TRN-SEQUENCE-NO` values within a
date + portfolio, and reconcile against BR-22 — all asserted by `SeedDataTest`. They load only under
the `seed` profile, so the default schema stays empty.

If a production extract becomes available, the parsing work still to do is the `COMP-3` decode
(two BCD digits per byte, sign in the low nibble: `C` positive, `D` negative, `F` unsigned) for
`TRN-QUANTITY`, `TRN-PRICE` and `TRN-AMOUNT`; the remaining fields are display characters.

---

## 6. Running it

```bash
cd java-migration
mvn clean test                  # 76 tests
mvn spring-boot:run             # http://localhost:8080/swagger-ui.html
mvn spring-boot:run -Dspring-boot.run.profiles=seed   # with the representative records
```

The default datasource is in-memory H2; every connection property is environment-substituted
(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`), so no credential is committed. Flyway owns the schema and
`spring.jpa.hibernate.ddl-auto` is `validate`, which keeps the entity and the migration in lockstep.

# Java Migration Foundation

## Dual-implementation layout

The Java migration is intentionally additive. Existing COBOL programs, copybooks,
JCL, maps, and database definitions remain under `src/` and continue to be the
legacy reference implementation. The Java implementation is under the new
top-level `java/` directory:

```text
project-root/
├── src/                    # Existing COBOL implementation (unchanged)
├── java/
│   ├── pom.xml             # Maven reactor parent
│   ├── portfolio-domain/   # JPA domain model and COBOL code conversions
│   ├── portfolio-persistence/ # Repositories and Flyway migrations
│   └── portfolio-app/      # Spring Boot application and integration tests
└── documentation/
```

Keeping both implementations visible makes the copybook-to-Java mapping
auditable while the migration proceeds incrementally.

## Module responsibilities

* **portfolio-domain** contains the `TransactionRecord`, `PositionRecord`, and
  `HistoryRecord` JPA entities, their embedded composite keys, domain enums,
  code-based JPA converters, and enum unit tests.
* **portfolio-persistence** contains Spring Data JPA repositories and the
  portable Flyway schema in `src/main/resources/db/migration`.
* **portfolio-app** contains `PortfolioApplication`, runtime and test
  configuration, and Spring Boot integration tests that validate Flyway,
  mappings, persistence round trips, and derived repository queries.

Spring Boot dependency management supplies third-party versions. The root
`java/pom.xml` uses Java 17 source compatibility; CI runs the build on Temurin
21.

## Copybook to JPA mappings

### Transaction record

| COBOL field | Java field | Database column | Java type |
| --- | --- | --- | --- |
| `TRN-DATE` | `key.transactionDate` | `transaction_date` | `LocalDate` |
| `TRN-TIME` | `key.transactionTime` | `transaction_time` | `LocalTime` |
| `TRN-PORTFOLIO-ID` | `key.portfolioId` | `portfolio_id` | `String(8)` |
| `TRN-SEQUENCE-NO` | `key.sequenceNo` | `sequence_no` | `String(6)` |
| `TRN-INVESTMENT-ID` | `investmentId` | `investment_id` | `String(10)` |
| `TRN-TYPE` | `transactionType` | `transaction_type` | `TransactionType` |
| `TRN-QUANTITY` | `quantity` | `quantity` | `BigDecimal(18,4)` |
| `TRN-PRICE` | `price` | `price` | `BigDecimal(18,4)` |
| `TRN-AMOUNT` | `amount` | `amount` | `BigDecimal(18,2)` |
| `TRN-CURRENCY` | `currencyCode` | `currency_code` | `String(3)` |
| `TRN-STATUS` | `status` | `status` | `TransactionStatus` |
| `TRN-PROCESS-DATE` | `processedAt` | `processed_at` | `Instant` |
| `TRN-PROCESS-USER` | `processUser` | `process_user` | `String(8)` |

`TransactionType` preserves the COBOL codes `BU` = BUY, `SL` = SELL,
`TR` = TRANSFER, and `FE` = FEE. `TransactionStatus` preserves `P` = PENDING,
`D` = DONE, `F` = FAILED, and `R` = REVERSED.

### Position record

| COBOL field | Java field | Database column | Java type |
| --- | --- | --- | --- |
| `POS-PORTFOLIO-ID` | `key.portfolioId` | `portfolio_id` | `String(8)` |
| `POS-DATE` | `key.positionDate` | `position_date` | `LocalDate` |
| `POS-INVESTMENT-ID` | `key.investmentId` | `investment_id` | `String(10)` |
| `POS-QUANTITY` | `quantity` | `quantity` | `BigDecimal(18,4)` |
| `POS-COST-BASIS` | `costBasis` | `cost_basis` | `BigDecimal(18,2)` |
| `POS-MARKET-VALUE` | `marketValue` | `market_value` | `BigDecimal(18,2)` |
| `POS-CURRENCY` | `currencyCode` | `currency_code` | `String(3)` |
| `POS-STATUS` | `status` | `status` | `PositionStatus` |
| `POS-LAST-MAINT-DATE` | `lastMaintAt` | `last_maint_at` | `Instant` |
| `POS-LAST-MAINT-USER` | `lastMaintUser` | `last_maint_user` | `String(8)` |

`PositionStatus` preserves `A` = ACTIVE, `C` = CLOSED, and `P` = PENDING.

### History record

| COBOL field | Java field | Database column | Java type |
| --- | --- | --- | --- |
| `HIST-PORTFOLIO-ID` | `key.portfolioId` | `portfolio_id` | `String(8)` |
| `HIST-DATE` | `key.historyDate` | `history_date` | `LocalDate` |
| `HIST-TIME` | `key.historyTime` | `history_time` | `LocalTime` |
| `HIST-SEQ-NO` | `key.sequenceNo` | `sequence_no` | `String(4)` |
| `HIST-RECORD-TYPE` | `recordType` | `record_type` | `HistoryRecordType` |
| `HIST-ACTION-CODE` | `actionCode` | `action_code` | `HistoryActionCode` |
| `HIST-BEFORE-IMAGE` | `beforeImage` | `before_image` | `String(400)` |
| `HIST-AFTER-IMAGE` | `afterImage` | `after_image` | `String(400)` |
| `HIST-REASON-CODE` | `reasonCode` | `reason_code` | `String(4)` |
| `HIST-PROCESS-DATE` | `processedAt` | `processed_at` | `Instant` |
| `HIST-PROCESS-USER` | `processUser` | `process_user` | `String(8)` |

`HistoryRecordType` preserves `PT` = PORTFOLIO, `PS` = POSITION, and
`TR` = TRANSACTION. `HistoryActionCode` preserves `A` = ADD, `C` = CHANGE,
and `D` = DELETE. The before image, after image, and reason code are nullable,
matching the record design.

All enum values are persisted using their COBOL code through null-safe
`AttributeConverter` implementations rather than Java enum names.

## Build and run

From the repository root, build all modules and run the tests with:

```shell
mvn -f java/pom.xml clean install
```

The application defaults to a local PostgreSQL database:

```shell
createdb portfolio
DB_USERNAME=portfolio DB_PASSWORD=portfolio \
  mvn -f java/pom.xml -pl portfolio-app spring-boot:run
```

Set `DB_URL` when PostgreSQL is not using the default connection:

```shell
DB_URL=jdbc:postgresql://localhost:5432/portfolio \
DB_USERNAME=myuser DB_PASSWORD=mypassword \
  mvn -f java/pom.xml -pl portfolio-app spring-boot:run
```

Hibernate is configured with `ddl-auto: validate`; it does not create or
modify tables. Flyway applies the schema migration on application startup.
The migration uses ANSI-compatible SQL for PostgreSQL and H2 PostgreSQL mode.

## Flyway and H2 tests

The `portfolio-app` tests use `@SpringBootTest` with the `test` profile. That
profile uses an in-memory H2 database in PostgreSQL compatibility mode, enables
Flyway, and retains `ddl-auto: validate`. Each test startup therefore applies
`V1__create_portfolio_schema.sql`, then asks Hibernate to validate the JPA
mappings against the migrated schema. Integration tests also query
`flyway_schema_history`, perform save/flush/clear/reload round trips for each
entity, and verify raw database enum codes.

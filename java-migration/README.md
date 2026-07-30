# Portfolio Transaction Service (Java 21 / Spring Boot 3)

Migration of the COBOL portfolio **transaction** entity (`src/copybook/common/TRNREC.cpy`, VSAM KSDS
`TRANHIST`) into a standalone Spring Boot service. The legacy COBOL is untouched; this module sits
beside it so the before/after is visible side by side.

Read [`MIGRATION-NOTES.md`](MIGRATION-NOTES.md) first: it holds the numbered business rules with the
COBOL paragraph each came from, the copybook → Java field mapping with byte offsets, and the open
questions for the legacy owners.

## Build and run

```bash
mvn clean test                                        # 87 tests
mvn spring-boot:run                                   # http://localhost:8080/swagger-ui.html
mvn spring-boot:run -Dspring-boot.run.profiles=seed   # loads the representative records
```

Requires JDK 21. The default datasource is in-memory H2 and every connection property is
environment-substituted (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`); no credential is committed.

## Layout

| Path | Contents |
| --- | --- |
| `transaction/domain` | `PortfolioTransaction` entity, `TransactionKey` (the 28-byte VSAM key), `TransactionType` / `TransactionStatus` enums from the copybook 88-levels |
| `transaction/validation` | `PORTVALD.cbl` field rules and the `PORTTRAN.cbl` `2100`-series transaction validation |
| `transaction/service` | Position posting (`2210`–`2240`), amount derivation, sequence assignment, CRUD and batch driver |
| `transaction/repository` | Keyed read plus key-ordered and paged browse |
| `transaction/web` | REST controller, DTOs, `@RestControllerAdvice` error mapping |
| `traceability` | `@CobolOrigin`, which tags every migrated type/method with its program, paragraph and rule ids |
| `resources/db/migration` | Flyway schema with the exact COBOL precision and scale |
| `resources/db/seed` | Representative records, `seed` profile only |
| `docs/openapi.yaml` | Generated OpenAPI 3 contract |

## API

| Operation | Endpoint | COBOL |
| --- | --- | --- |
| Keyed read | `GET /api/v1/transactions/{key}` | keyed `READ` |
| Browse (paged, key order) | `GET /api/v1/transactions` | sequential `READ` |
| Insert | `POST /api/v1/transactions` | `WRITE` |
| Rewrite | `PUT /api/v1/transactions/{key}` | `REWRITE` |
| Status transition | `POST /api/v1/transactions/{key}/status` | derived (BR-23) |
| Validate + post positions | `POST /api/v1/transactions/{key}/process` | `2100` + `2200` |

`{key}` is the 28-character VSAM key: `TRN-DATE(8) || TRN-TIME(6) || TRN-PORTFOLIO-ID(8) ||
TRN-SEQUENCE-NO(6)`, e.g. `20240320093015PORT0001000001`.

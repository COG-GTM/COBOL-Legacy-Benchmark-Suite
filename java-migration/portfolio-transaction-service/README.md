# portfolio-transaction-service

Java 21 / Spring Boot modernization of the COBOL **PORTTRAN** (Portfolio
Transaction Processing) position-management routine from
`src/programs/portfolio/PORTTRAN.cbl`.

This is an **additive** migration: the original COBOL is left intact. The module
preserves PORTTRAN's exact validation rules, fixed-decimal (COMP-3) arithmetic,
and safeguards, with a behavioral-equivalence test suite for auditors.

## Build & test

```bash
# Requires JDK 21
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn clean test
```

## Layout

| Path | Purpose |
|---|---|
| `domain/PortfolioTransaction` | Input record (copybook `TRNREC`) |
| `domain/PortfolioPosition` | Position/master record (reconstructed `PORTREC`), JPA entity |
| `domain/TransactionType` | `TRN-TYPE` codes BU/SL/TR/FE |
| `domain/CobolDecimal` | COBOL packed-decimal scale + truncation semantics |
| `repository/PortfolioPositionRepository` | Replaces VSAM `PORTFOLIO-FILE` |
| `service/PortfolioTransactionService` | PORTTRAN paragraphs 2100–2310 + main loop |
| `resources/db/migration` | Flyway schema (V1) + seed data (V2) |
| `docs/PORTTRAN-modernization.md` | Rules-recovery / modernization note |

## Profiles

- default/dev: in-memory H2
- `prod`: PostgreSQL; all credentials via environment variables
  (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) — no hardcoded secrets.

See [`docs/PORTTRAN-modernization.md`](docs/PORTTRAN-modernization.md) for the
full business-logic recovery, decimal-parity analysis, recovered safeguards, and
the `2200-UPDATE-POSITIONS` wiring defect.

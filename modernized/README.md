# Portfolio Service — Modernized from COBOL

This Spring Boot application is the modern Java 21 equivalent of the COBOL Investment Portfolio Management System's batch processing and reporting subsystem.

## COBOL-to-Java Migration Mapping

| COBOL Program | Description | Java Equivalent |
|---|---|---|
| `PORTTRAN.cbl` | Portfolio Transaction Processing | `TransactionService` + `TransactionController` |
| `RPTPOS00.cbl` | Daily Position Report Generator | `PositionReportService` + `ReportController` |

## COBOL Copybook → JPA Entity Mapping

| Copybook | COBOL Structure | Java Entity |
|---|---|---|
| `PORTREC` | Portfolio Master Record | `Portfolio` |
| `TRNREC` | Transaction Record | `Transaction` |
| `POSREC` | Position Record | `Position` |

## API Endpoints

### Portfolio Transactions
- `POST /api/transactions` — Process a new portfolio transaction (buy/sell)
- `GET /api/transactions/{id}` — Retrieve transaction details
- `GET /api/portfolios/{portfolioId}/transactions` — List transactions for a portfolio

### Position Reporting
- `GET /api/portfolios/{portfolioId}/positions` — Current positions for a portfolio
- `GET /api/reports/daily-position` — Generate daily position report
- `GET /api/reports/daily-position?date={date}` — Position report for specific date

## Running

```bash
cd modernized
mvn spring-boot:run
```

The H2 console is available at `http://localhost:8080/h2-console`.

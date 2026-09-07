# Java Portfolio Migration

Esta migración convierte el flujo COBOL de portafolios en una aplicación Java
revisable por personas, manteniendo las reglas de negocio y haciendo explícitas
las áreas provisionales que necesitan validación humana.

## Overview and stack

The application is a Java 17 / Spring Boot 3.3.13 migration of the COBOL
portfolio system. It uses Spring MVC, Thymeleaf, Spring Security, Spring Data
JPA, Spring Batch 5, Flyway, HikariCP, H2, Actuator and `BigDecimal` for all
financial values. The original COBOL files under `src/` and architecture
documentation are treated as read-only specifications.

## Quick start

```bash
cd java-migration
mvn clean verify
mvn spring-boot:run
```

URLs:

* <http://localhost:8080/>
* <http://localhost:8080/login>
* <http://localhost:8080/inquiry/menu>
* <http://localhost:8080/h2-console>
* <http://localhost:8080/actuator/health>

The H2 console uses JDBC URL `jdbc:h2:mem:posmvp`, username `sa`, and an empty
password, and requires login (any demo user).

## Demo credentials and authorization

| User | Password | Role |
| --- | --- | --- |
| `admin` | `admin123` | ADMIN |
| `operator` | `operator123` | OPERATOR |
| `user` | `user123` | USER |

AUTHFILE rights:

| User | Rights |
| --- | --- |
| admin | `INQONLN/READ`, `PORTMSTR/READ`, `PORTMSTR/UPDATE`, `BATCH/EXECUTE` |
| operator | `INQONLN/READ`, `PORTMSTR/READ`, `BATCH/EXECUTE` |
| user | `INQONLN/READ`, `PORTMSTR/READ` |

Demo users and the empty `sa` H2 password exist only for the `demo` profile;
disable the profile (`--spring.profiles.active=prod`) for any non-local
deployment.

## Demo dataset

The `demo` profile loads data once, only when `PORTFOLIO_MASTER` is empty.
Portfolios `PORT0001` through `PORT0010` use accounts
`1000000001` through `1000000010`, deterministic security IDs, seeded
positions, approximately fifty transactions, and ten POSHIST rows for
`PORT0001`. The last fee and buy transaction for each portfolio remain
`PENDING` so the daily batch has work to process. Processed transactions are
not mirrored into the seeded investment positions; the seeded positions remain
the inquiry view, while batch position updates demonstrate the reconstructed
POSUPD flow.

## Batch duplicate behavior

`HISTLD00` reads completed transactions in ascending transaction date/time
order, commits in chunks of 1,000, and checks POSHIST primary keys before
writing. A duplicate is silently counted as a duplicate and skipped. This is
the Java equivalent of the COBOL DB2 `SQLCODE -803` branch. The step returns
`COMPLETED` with `RC=4` when duplicates were encountered, and is safe to rerun.

## COBOL-to-Java mapping

### Programs

| COBOL program | Java migration |
| --- | --- |
| `src/programs/batch/BCHCTL00.cbl` | Spring Batch `JobRepository` |
| `src/programs/batch/CKPRST.cbl` | Batch checkpoint metadata and execution contexts |
| `src/programs/batch/HISTLD00.cbl` | `BatchConfig.histLoadJob`, processor and duplicate-skipping writer |
| `src/programs/batch/POSUPDT.cbl` | `PositionUpdateTasklet` (provisional reconstruction) |
| `src/programs/batch/PRCSEQ00.cbl` | `ReturnCodeDecider` and batch flow |
| `src/programs/batch/RCVPRC00.cbl` | Spring Batch restartable job execution |
| `src/programs/batch/RPTAUD00.cbl` | `AuditReportService` / `auditReportJob` |
| `src/programs/batch/RPTPOS00.cbl` | `PositionReportService` / `positionReportJob` |
| `src/programs/batch/RPTSTA00.cbl` | `StatsReportService` / `statsReportJob` |
| `src/programs/batch/RTNANA00.cbl` | `ReturnCodeAnalysisService` / `rtnAnalysisJob` |
| `src/programs/batch/RTNCDE00.cbl` | `ReturnCodeRecordingListener` and RTNCODES |
| `src/programs/common/AUDPROC.cbl` | AuditLog persistence |
| `src/programs/common/DB2CMT.cbl` | `@Transactional` boundaries |
| `src/programs/common/DB2CONN.cbl` | HikariCP DataSource |
| `src/programs/common/DB2ERR.cbl` | ErrorLog and exception advice |
| `src/programs/common/DB2STAT.cbl` | Actuator and report statistics |
| `src/programs/common/ERRPROC.cbl` | `ErrorLoggingService` |
| `src/programs/online/CURSMGR.cbl` | Spring Data `Pageable` |
| `src/programs/online/DB2ONLN.cbl` | JPA DataSource access |
| `src/programs/online/DB2RECV.cbl` | `@Retryable`, three attempts |
| `src/programs/online/ERRHNDL.cbl` | REST/MVC exception advice |
| `src/programs/online/INQHIST.cbl` | `InquiryController` history route |
| `src/programs/online/INQONLN.cbl` | `InquiryController` menu dispatcher |
| `src/programs/online/INQPORT.cbl` | `InquiryController` position route |
| `src/programs/online/SECMGR.cbl` | `AuthorizationService` and Spring Security |
| `src/programs/portfolio/PORTADD.cbl` | `PortfolioService.create` |
| `src/programs/portfolio/PORTDEL.cbl` | `PortfolioService.delete` |
| `src/programs/portfolio/PORTMSTR.cbl` | `PortfolioRestController` |
| `src/programs/portfolio/PORTREAD.cbl` | Portfolio REST read endpoints |
| `src/programs/portfolio/PORTTEST.cbl` | DemoDataLoader |
| `src/programs/portfolio/PORTTRAN.cbl` | `PortfolioTransactionService` |
| `src/programs/portfolio/PORTUPDT.cbl` | `PortfolioService.applyUpdate` |
| `src/programs/portfolio/PORTVALD.cbl` | `PortfolioValidationService` |
| `src/programs/test/TSTGEN00.cbl` | DemoDataLoader, deterministic test data |
| `src/programs/test/TSTVAL00.cbl` | JUnit and Spring integration tests |
| `src/programs/utility/UTLMNT00.cbl` | Not ported; utility outside migration scope |
| `src/programs/utility/UTLMON00.cbl` | Actuator health/metrics |
| `src/programs/utility/UTLVAL00.cbl` | Batch validation tasklets |

### Copybooks

| Copybook | Java migration |
| --- | --- |
| `src/copybook/batch/BCHCON.cpy` | `BatchConstants` |
| `src/copybook/batch/BCHCTL.cpy` | Spring Batch metadata |
| `src/copybook/batch/CKPRST.cpy` | Execution context counters |
| `src/copybook/batch/PRCSEQ.cpy` | ReturnCodeDecider flow |
| `src/copybook/common/AUDITLOG.cpy` | `AuditLog` |
| `src/copybook/common/COMMON.cpy` | Shared domain conventions |
| `src/copybook/common/ERRHAND.cpy` | `ErrorLog` and advice |
| `src/copybook/common/HISTREC.cpy` | `PositionHistory` |
| `src/copybook/common/PORTFLIO.cpy` | `Portfolio` |
| `src/copybook/common/PORTVAL.cpy` | `PortfolioValidationService` |
| `src/copybook/common/POSREC.cpy` | `InvestmentPosition` |
| `src/copybook/common/RETHND.cpy` | Return-code handling |
| `src/copybook/common/RTNCODE.cpy` | `ReturnCodeRecord` |
| `src/copybook/common/TRNREC.cpy` | `Transaction` |
| `src/copybook/db2/DBPROC.cpy` | Repository services |
| `src/copybook/db2/DBTBLS.cpy` | Flyway schema and JPA entities |
| `src/copybook/db2/SQLCA.cpy` | Data access exception handling |
| `src/copybook/online/DB2REQ.cpy` | Repository request flow |
| `src/copybook/online/ERRHND.cpy` | MVC/REST error models |
| `src/copybook/online/INQCOM.cpy` | Inquiry controller model |

### JCL and BMS

| JCL | REST or batch mapping |
| --- | --- |
| `RPTPOS.jcl` | `POST /api/batch/report/position` |
| `RPTAUD.jcl` | `POST /api/batch/report/audit` |
| `RPTSTA.jcl` | `POST /api/batch/report/stats` |
| `RTNANA.jcl` | `POST /api/batch/analysis/rtn` |
| `PORTADD.jcl` | `POST /api/portfolios` |
| `PORTREAD.jcl` | `GET /api/portfolios/{id}` |
| `PORTUPDT.jcl` | `PUT /api/portfolios/{id}/{updateType}` |
| `PORTDEL.jcl` | `DELETE /api/portfolios/{id}` |
| `PORTTEST.jcl` / `TSTGEN.jcl` | DemoDataLoader |
| `PORTDEF.jcl` | Flyway schema |
| `TSTVAL.jcl` | Maven/JUnit verification |
| Utility JCL | Actuator or not ported |

| BMS map | Thymeleaf template |
| --- | --- |
| `MENMAP` in `INQSET.bms` | `templates/menu.html` |
| `POSMAP` in `INQSET.bms` | `templates/position.html` |
| `HISMAP` in `INQSET.bms` | `templates/history.html` |
| `ERRMAP` in `INQSET.bms` | `templates/error.html` |

## Infrastructure mapping

| COBOL facility | Java implementation |
| --- | --- |
| DB2ONLN | HikariCP and Spring Data JPA |
| CURSMGR | `Pageable`, repository readers |
| DB2RECV | `@Retryable` with 3 attempts |
| ERRHNDL / ERRPROC | Exception advice and `ErrorLog` |
| FATAL | HTTP 500 / failed batch job |
| WARNING / INFO | Continue behavior and logged warnings |
| DB2CONN / CMT / STAT / ERR | DataSource, `@Transactional`, Actuator, ErrorLog |
| BCHCTL / CKPRST / PRCSEQ / RCVPRC | Spring Batch `JobRepository` and `ReturnCodeDecider` |

## PROVISIONAL — human validation required

`trnValStep` and `posUpdStep` are reconstructions because no original COBOL
source for `TRNVAL00` or `POSUPD00` was supplied. They deliberately carry the
source comments:

* `// PROVISIONAL – requiere validación humana: TRNVAL00 sin fuente COBOL original`
* `// PROVISIONAL – requiere validación humana: POSUPD00 sin fuente COBOL original`

The validation reconstruction reuses portfolio ID format validation,
investment-type extraction with `investmentTypeOf`, amount-range validation,
and `PortfolioTransactionService.validateAmounts`. The position reconstruction
reuses `PortfolioTransactionService.process`, its BUY/SELL/FEE rules, transfer
handling, and BigDecimal quantity/cost/market-value arithmetic. These rules
must be reviewed against the missing source before production use.

## Type mapping

| COBOL type | Java type |
| --- | --- |
| `PIC S9(... )V99 COMP-3` | `BigDecimal` scale 2 |
| `PIC S9(... )V999 COMP-3` | `BigDecimal` scale 3 |
| `PIC X` | `String` |
| `PIC 9(8)` date | `LocalDate` |
| time fields | `LocalTime` |
| CICS/DB2 return codes | `int`, `ExitStatus`, `FlowExecutionStatus` |
| VSAM keyed record | JPA entity and embedded ID |

## Schema reconciliation

* POSHIST quantity and price remain `DECIMAL(15,3)` and monetary fields remain
  `DECIMAL(15,2)`, matching POSHIST.sql.
* POSHIST `ACCOUNT_NO` is widened to 10 characters to match portfolio account
  fields.
* H2 treats `RESOURCE` as problematic, so AUTHFILE uses `RESOURCE_NAME`.
* `totalUnits` and `totalCost` are explicit portfolio fields because the
  transaction service operates on portfolio totals.

## REST API

All examples use the admin demo account:

```bash
curl -u admin:admin123 http://localhost:8080/api/portfolios
curl -u admin:admin123 http://localhost:8080/api/portfolios/PORT0001
curl -u admin:admin123 http://localhost:8080/api/portfolios/PORT0001/positions
curl -u admin:admin123 -X POST http://localhost:8080/api/batch/daily
curl -u admin:admin123 -X POST http://localhost:8080/api/batch/hist-load
curl -u admin:admin123 -X POST http://localhost:8080/api/batch/report/position
curl -u admin:admin123 http://localhost:8080/api/transactions?portfolioId=PORT0001
curl -u admin:admin123 http://localhost:8080/api/transactions/PORT0001/history
```

`COMPLETED` batch jobs return HTTP 200, `STOPPED` jobs return 409, and failed
jobs return 500. The JSON includes execution ID, exit description, and step
read/write/skip/return-code summaries.

`/api/**` is CSRF-exempt because it is meant for HTTP Basic clients (curl/JCL
replacement), not browser sessions.

## Verification

1. In H2 console, run:
   `select * from PORTFOLIO_MASTER;`,
   `select * from INVESTMENT_POSITIONS;`,
   `select * from POSHIST;`, and
   `select * from RTNCODES;`.
2. Open `/inquiry/menu`, choose position inquiry or transaction history, and
   use account `1000000001`.
3. Login at `/login` with `admin/admin123`; successful form login redirects to
   `/inquiry/menu`.
4. Run `POST /api/batch/daily` and inspect the JSON status and report files.
5. Run `POST /api/batch/hist-load` repeatedly; duplicate history rows are
   skipped and reported with `RC=4`.

## Reproducing the demo video

1. Start the application with `mvn spring-boot:run`.
2. Open the login page and sign in as `admin/admin123`.
3. Open the menu and inspect account `1000000001`.
4. View positions, then return and view the ten seeded history rows.
5. Trigger `POST /api/batch/daily` with HTTP Basic authentication.
6. Open the H2 console and inspect POSHIST, RTNCODES, and generated reports.

## Tests

Run the complete verification suite:

```bash
mvn clean verify
```

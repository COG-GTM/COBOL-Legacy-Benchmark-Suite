# COBOL to Java Migration Notes

## Overview

This document details the migration of the COBOL Investment Portfolio Management System to a Java 17 Spring Boot 3.2.5 application. The original system comprised 37 COBOL programs, 20 copybooks, 5 DB2 SQL definitions, 1 BMS map, CICS transaction definitions, VSAM file definitions, and 15+ JCL scripts.

## Technology Stack Mapping

| COBOL / Mainframe | Java / Spring Boot |
|---|---|
| COBOL programs | Spring services, controllers, batch jobs |
| CICS transactions | REST endpoints + Thymeleaf UI |
| DB2 database | H2 (dev) / PostgreSQL (prod) via JPA |
| VSAM files | Relational database tables |
| JCL batch scripts | Spring Batch jobs |
| BMS maps (3270 screens) | Thymeleaf HTML templates |
| CICS security | Spring Security (role-based) |
| DB2 connection management | HikariCP connection pool |
| COBOL copybooks | JPA entities + DTOs + enums |
| Level-88 conditions | Java enums |
| COMP-3 packed decimal | BigDecimal with precision/scale |

## Program-to-Class Mapping

### Portfolio Programs (8)

| COBOL Program | Java Class | Package |
|---|---|---|
| PORTMSTR.cbl | PortfolioService.java | service.portfolio |
| PORTTRAN.cbl | TransactionProcessingService.java | service.transaction |
| PORTADD.cbl | PortfolioService.createPortfolio() | service.portfolio |
| PORTREAD.cbl | PortfolioService.readPortfolio() | service.portfolio |
| PORTUPDT.cbl | PortfolioService.updatePortfolio() | service.portfolio |
| PORTDEL.cbl | PortfolioService.deletePortfolio() | service.portfolio |
| PORTVALD.cbl | PortfolioValidator.java | service.portfolio |
| PORTTEST.cbl | PortfolioServiceTest.java | test |

### Batch Programs (10)

| COBOL Program | Java Class | Package |
|---|---|---|
| HISTLD00.cbl | HistoryLoadJobConfig.java | batch.jobs |
| BCHCTL00.cbl | BatchControlService.java | service.batch |
| PRCSEQ00.cbl | ProcessSequenceService.java | service.batch |
| RCVPRC00.cbl | BatchRecoveryService.java | service.batch |
| CKPRST.cbl | CheckpointRestartListener.java | batch.listeners |
| RPTPOS00.cbl | PositionReportJobConfig.java | batch.jobs |
| RPTAUD00.cbl | AuditReportJobConfig.java | batch.jobs |
| RPTSTA00.cbl | StatisticsReportJobConfig.java | batch.jobs |
| RTNANA00.cbl | ReturnCodeAnalysisJobConfig.java | batch.jobs |
| RTNCDE00.cbl | ReturnCodeService.java | service.common |

### Online Programs (8)

| COBOL Program | Java Class | Package |
|---|---|---|
| INQONLN.cbl | InquiryController.java | web.controller |
| INQPORT.cbl | PortfolioInquiryService.java | service.inquiry |
| INQHIST.cbl | HistoryInquiryService.java | service.inquiry |
| SECMGR.cbl | SecurityService.java | service.inquiry |
| DB2RECV.cbl | Db2RecoveryService.java | service.inquiry |
| CURSMGR.cbl | Eliminated (Spring Data pagination) | — |
| DB2ONLN.cbl | Eliminated (HikariCP) | — |
| ERRHNDL.cbl | GlobalExceptionHandler.java | web.advice |

### Common Programs (6)

| COBOL Program | Java Class | Package |
|---|---|---|
| ERRPROC.cbl | ErrorProcessingService.java | service.common |
| DB2CONN.cbl | Eliminated (HikariCP DataSource) | — |
| DB2CMT.cbl | @Transactional annotations | — |
| DB2ERR.cbl | Db2ExceptionTranslator.java | service.common |
| DB2STAT.cbl | Db2HealthIndicator.java | service.common |
| AUDPROC.cbl | AuditService.java | service.common |

### Utility Programs (3)

| COBOL Program | Java Class | Package |
|---|---|---|
| UTLMNT00.cbl | FileMaintenanceJobConfig.java | batch.jobs |
| UTLMON00.cbl | SystemMonitorService.java | service.utility |
| UTLVAL00.cbl | DataValidationJobConfig.java | batch.jobs |

### Test Programs (2)

| COBOL Program | Java Class | Package |
|---|---|---|
| TSTGEN00.cbl | TestDataGenerator.java | service.utility |
| TSTVAL00.cbl | TestValidationSuite.java | service.utility |

## Copybook-to-Entity Mapping

| Copybook | Java Class | Type |
|---|---|---|
| PORTFLIO.cpy | Portfolio.java | @Entity |
| TRNREC.cpy | Transaction.java + TransactionKey.java | @Entity + @Embeddable |
| POSREC.cpy | Position.java + PositionKey.java | @Entity + @Embeddable |
| HISTREC.cpy | HistoryRecord.java | @Entity |
| DBTBLS.cpy | PositionHistory.java, ErrorLogEntry.java | @Entity |
| AUDITLOG.cpy | AuditRecord.java | @Entity |
| BCHCTL.cpy | BatchControlRecord.java | @Entity |
| CKPRST.cpy | CheckpointControl.java | @Entity |
| COMMON.cpy | CommonConstants.java | Constants |
| ERRHAND.cpy | ErrorCategory.java, ReturnCode.java, ErrorType.java, ErrorSeverity.java, StandardErrorCode.java | Enums |
| PORTVAL.cpy | PortfolioValidator.java | Service |
| RTNCODE.cpy | ReturnCodeArea.java | DTO |
| RETHND.cpy | StandardErrorCode.java | Enum |
| INQCOM.cpy | InquiryRequest.java, InquiryResponse.java | DTOs |
| DB2REQ.cpy | Eliminated (DataSource) | — |
| ERRHND.cpy | OnlineErrorInfo.java | DTO |
| BCHCON.cpy | BatchConstants.java | Constants |
| PRCSEQ.cpy | ProcessSequence.java | DTO |

## Data Type Conversion Rules

| COBOL Type | Java Type | Notes |
|---|---|---|
| PIC X(n) | String | @Column(length=n) |
| PIC 9(n) | int / long | Based on size |
| PIC S9(n)V9(m) COMP-3 | BigDecimal | @Column(precision=n+m, scale=m) |
| PIC S9(13)V99 COMP-3 | BigDecimal(15,2) | Financial amounts |
| PIC S9(11)V9(4) COMP-3 | BigDecimal(18,4) | Quantities/prices |
| PIC S9(11)V9(3) COMP-3 | BigDecimal(15,3) | Position history amounts |
| Level-88 conditions | Java enum | With code field and fromCode() |
| PIC X(1) with 88-levels | enum or Character | Mapped via converter |
| OCCURS n TIMES | List<T> or array | @OneToMany or embedded |
| REDEFINES | Separate fields | Each redefinition becomes its own field |
| Group items | Embedded fields | Flattened into entity |

## Behavioral Differences

### VSAM → JPA
| VSAM Concept | JPA Equivalent |
|---|---|
| File Status '00' (success) | Normal return from repository method |
| File Status '22' (duplicate key) | DataIntegrityViolationException → DuplicatePortfolioException |
| File Status '23' (not found) | Optional.empty() → PortfolioNotFoundException |
| OPEN/CLOSE file | Managed by DataSource connection pool |
| READ/WRITE/REWRITE/DELETE | findById/save/save/delete |
| START/READ NEXT (sequential) | findAll with Sort/Pageable |

### CICS → Spring
| CICS Concept | Spring Equivalent |
|---|---|
| EXEC CICS RECEIVE MAP | @GetMapping / @PostMapping |
| EXEC CICS SEND MAP | Return Thymeleaf template name |
| EXEC CICS LINK PROGRAM | Service method call (DI) |
| EXEC CICS RETURN TRANSID | Redirect or page navigation |
| EIBRESP / EIBRESP2 | HTTP status codes via @ControllerAdvice |
| Pseudo-conversational | Stateless REST API |
| COMMAREA | Request parameters / DTOs |
| PF keys (PF7/PF8) | Pagination links (Previous/Next) |
| CICS ASSIGN USERID | SecurityContextHolder.getContext() |

### DB2 → Spring Data JPA
| DB2 Concept | Spring Equivalent |
|---|---|
| EXEC SQL CONNECT | HikariCP auto-connection |
| EXEC SQL COMMIT | @Transactional (auto-commit) |
| EXEC SQL ROLLBACK | @Transactional rollback on exception |
| SQLCODE -803 (duplicate) | DataIntegrityViolationException |
| SQLCODE -911 (deadlock) | DeadlockLoserDataAccessException |
| SQLCODE -30081 (connection) | DataAccessResourceFailureException |
| DECLARE CURSOR / OPEN / FETCH | Spring Data JPA queries with Pageable |
| DB2 stored procedures | @Scheduled tasks or Spring Batch steps |

### Batch Processing
| JCL / COBOL Batch | Spring Batch |
|---|---|
| JCL JOB card | Job bean definition |
| JCL STEP | Step bean definition |
| COND parameter | ConditionalFlow with ExitStatus |
| DD statements | application.yml properties |
| VSAM sequential read | RepositoryItemReader |
| WRITE to output file | ItemWriter (file or DB) |
| ABEND handling | Skip/Retry policies |
| Checkpoint/restart | StepExecutionListener with ExecutionContext |
| COMMIT INTERVAL (1000) | chunk(1000) |
| Return codes (0/4/8/12/16) | ExitStatus with custom codes |

## Configuration Mapping

| JCL/CICS Config | Spring Config |
|---|---|
| CICS CSD PINQ transaction | SecurityConfiguration URL patterns |
| JCL DD SYSOUT | SLF4J logging output |
| JCL DD database DSN | spring.datasource.url |
| CICS DFHCSDUP definitions | @PreAuthorize role annotations |
| JCL PARM values | application.yml properties |
| VSAM DEFINE CLUSTER | Flyway migration DDL |

## Database Migrations

| Migration File | Source | Tables Created |
|---|---|---|
| V1__create_core_tables.sql | db2-definitions.sql | portfolio_master, investment_positions, transaction_history + indexes + views |
| V2__create_poshist_table.sql | POSHIST.sql | poshist + indexes |
| V3__create_errlog_table.sql | ERRLOG.sql | errlog + indexes |
| V4__create_rtncodes_table.sql | RTNCODES.sql | rtncodes + index |
| V5__create_vsam_replacement_tables.sql | vsam-definitions.txt | audit_log, history_record, batch_control, checkpoint_control + indexes |

## Security Mapping

| CICS Security | Spring Security |
|---|---|
| PINQ transaction (inquiry) | ROLE_INQUIRY |
| Portfolio update access | ROLE_UPDATE |
| Admin/maintenance access | ROLE_ADMIN |
| CICS ASSIGN USERID | Authentication principal |
| AUTHFILE SQL query | InMemoryUserDetailsManager (expandable to JDBC) |
| Access logging (AUDITLOG) | AuditService.logAccess() |

## BMS Map → Thymeleaf Template Mapping

| BMS Map | Template | Description |
|---|---|---|
| MENMAP | inquiry/menu.html | Main menu with 3 options |
| POSMAP | inquiry/portfolio-position.html | Portfolio position display |
| HISMAP | inquiry/transaction-history.html | Transaction history with pagination |
| ERRMAP | error.html | Error display |

## Test Coverage

The migration includes:
- **Unit tests**: PortfolioServiceTest, TransactionProcessingServiceTest, PortfolioValidatorTest
- **Integration tests**: PortfolioIntegrationTest (REST API), SecurityTest, ErrorHandlingTest
- **Batch tests**: HistoryLoadJobTest

All tests use H2 in-memory database with Flyway migrations for schema setup.

## Running the Application

```bash
cd java-migration
mvn spring-boot:run
```

Access points:
- Web UI: http://localhost:8080/inquiry
- REST API: http://localhost:8080/api/portfolios
- H2 Console: http://localhost:8080/h2-console
- Health Check: http://localhost:8080/actuator/health

## Known Limitations

1. **TRANSFER transaction type**: Not implemented (throws UnsupportedOperationException), matching COBOL behavior
2. **Report formatting**: Reports output to logs rather than fixed-format files; PIC edit masks converted to DecimalFormat
3. **CURSMGR**: Eliminated; cursor management replaced by Spring Data pagination
4. **DB2ONLN/DB2CONN**: Eliminated; connection pooling handled by HikariCP
5. **UserDetailsService**: Currently uses InMemoryUserDetailsManager; production should use JDBC-backed implementation

# COBOL to Java Migration Traceability Document

## Overview
This document maps every COBOL artifact to its Java equivalent in the migrated application.

## Data Type Translation Rules

| COBOL Picture Clause | Java Type | JPA Annotation | Notes |
|---|---|---|---|
| `PIC X(n)` | `String` | `@Column(length=n)` | Fixed-length character |
| `PIC S9(13)V99 COMP-3` | `BigDecimal` | `@Column(precision=15, scale=2)` | Packed decimal |
| `PIC S9(11)V9(4) COMP-3` | `BigDecimal` | `@Column(precision=15, scale=4)` | Packed decimal with 4 decimal places |
| `PIC S9(12)V9(3) COMP-3` | `BigDecimal` | `@Column(precision=15, scale=3)` | Packed decimal with 3 decimal places |
| `PIC 9(8)` (date) | `LocalDate` | `@Column` | YYYYMMDD format |
| `PIC X(26)` (timestamp) | `LocalDateTime` | `@Column` | Full timestamp |
| `PIC S9(8) COMP` | `int` | `@Column` | Binary integer |
| Level-88 conditions | Java `enum` | - | Each set of level-88s becomes one enum |
| `OCCURS n TIMES` | `List<T>` | `@Transient` or child entity | Arrays become collections |

## Copybook to Java Class Mapping

| Copybook | Java Class(es) | Package |
|---|---|---|
| `PORTFLIO.cpy` | `Portfolio.java`, `ClientType.java`, `PortfolioStatus.java` | `domain`, `domain.enums` |
| `POSREC.cpy` | `Position.java`, `PositionId.java` | `domain` |
| `TRNREC.cpy` | `Transaction.java` | `domain` |
| `HISTREC.cpy` | `HistoryRecord.java` | `domain` |
| `AUDITLOG.cpy` | `AuditLog.java` | `domain` |
| `DBTBLS.cpy` | `PosHistRecord.java`, `PosHistId.java`, `ErrorLogRecord.java`, `ErrorLogId.java` | `domain` |
| `COMMON.cpy` | `ReturnCode.java` | `domain.enums` |
| `RETHND.cpy` | `ReturnHandling.java`, `ErrorType.java`, `ActionFlag.java` | `domain`, `domain.enums` |
| `ERRHAND.cpy` | `ErrorInfo.java` | `domain` |
| `RTNCODE.cpy` | `ReturnCodeInfo.java` | `domain` |
| `PORTVAL.cpy` | `PortfolioValidation.java` | `domain` |
| `BCHCTL.cpy` | `BatchControlRecord.java`, `BatchControlId.java` | `domain` |
| `CKPRST.cpy` | `CheckpointData.java` | `domain` |
| `BCHCON.cpy` | `BatchConstants.java` | `domain` |
| `PRCSEQ.cpy` | `ProcessSequence.java` | `domain` |
| `DBPROC.cpy` | `DbProcedureParams.java` | `domain` |

## COBOL Program to Java Service/Class Mapping

### Common Programs
| COBOL Program | Java Class | Notes |
|---|---|---|
| `ERRPROC.cbl` | `ErrorProcessingService.java` | Batch error handler |
| `ERRHNDL.cbl` | `OnlineErrorHandler.java` | Spring `@ControllerAdvice` |
| `DB2CONN.cbl` | Eliminated | Spring HikariCP connection pooling |
| `DB2CMT.cbl` | Eliminated | Spring `@Transactional` |
| `DB2ERR.cbl` | `DatabaseErrorHandler.java` | SQLException translation |
| `DB2STAT.cbl` | `DatabaseStatisticsService.java` | Table statistics |
| `AUDPROC.cbl` | `AuditService.java` | JPA-based audit logging |

### Online Programs
| COBOL Program | Java Class | Notes |
|---|---|---|
| `SECMGR.cbl` | `SecurityConfig.java`, `UserAuthenticationService.java` | Spring Security replaces RACF |
| `INQONLN.cbl` | `InquiryController.java` | Spring MVC `@Controller` |
| `INQPORT.cbl` | `PortfolioInquiryService.java` | Position inquiry |
| `INQHIST.cbl` | `HistoryInquiryService.java` | Transaction history with pagination |
| `CURSMGR.cbl` | Eliminated | Cursor management irrelevant in web UI |
| `DB2ONLN.cbl` | Eliminated | Spring manages connections |
| `DB2RECV.cbl` | `DatabaseRecoveryService.java` | Spring Retry `@Retryable` |

### Portfolio Programs
| COBOL Program | Java Class | Notes |
|---|---|---|
| `PORTMSTR.cbl` | `PortfolioMasterService.java` | CRUD orchestrator |
| `PORTADD.cbl` | `PortfolioService.createPortfolio()` | Create operation |
| `PORTREAD.cbl` | `PortfolioService.getPortfolio()` | Read via JPA `findById` |
| `PORTUPDT.cbl` | `PortfolioService.updatePortfolio()` | Update via JPA `save` |
| `PORTDEL.cbl` | `PortfolioService.deletePortfolio()` | Soft delete (set status=C) |
| `PORTTRAN.cbl` | `PortfolioTransactionService.java` | Transaction processing |
| `PORTVALD.cbl` | `PortfolioValidationService.java` | Input validation |
| `PORTTEST.cbl` | `PortfolioServiceTest.java` | JUnit test |

### Batch Programs
| COBOL Program | Java Class | Notes |
|---|---|---|
| `BCHCTL00.cbl` | `BatchControlService.java` | Job control + Spring Batch `JobRepository` |
| `CKPRST.cbl` | Spring Batch `ExecutionContext` | Built-in chunk-oriented processing |
| `PRCSEQ00.cbl` | `ProcessSequenceService.java` | Process sequencing |
| `RCVPRC00.cbl` | `RecoveryProcessService.java` | Job recovery |
| `HISTLD00.cbl` | `HistoryLoadJob.java` | Spring Batch job, `chunk(1000)` |
| `RPTPOS00.cbl` | `PositionReportJob.java` | Position report generation |
| `RPTAUD00.cbl` | `AuditReportJob.java` | Audit report generation |
| `RPTSTA00.cbl` | `StatisticsReportJob.java` | Statistics report |
| `RTNANA00.cbl` | `ReturnAnalysisJob.java` | Return code analysis |
| `RTNCDE00.cbl` | `ReturnCodeJob.java` | Return code definitions |

### Utility Programs
| COBOL Program | Java Class | Notes |
|---|---|---|
| `UTLMNT00.cbl` | `FileMaintenanceService.java` | DB maintenance, archiving |
| `UTLMON00.cbl` | `SystemMonitorService.java` + Actuator | JVM metrics via Micrometer |
| `UTLVAL00.cbl` | `DataValidationService.java` | Cross-reference validation |
| `TSTGEN00.cbl` | `TestDataGenerator.java` | Test data generation |
| `TSTVAL00.cbl` | `TestValidationSuite.java` | Integration tests |

## VSAM to JPA Repository Mapping

| VSAM File | Repository | Key Structure |
|---|---|---|
| `PORTMSTR` (KSDS) | `PortfolioRepository` | `portfolioId` (simple PK) |
| `TRANHIST` (KSDS) | `TransactionRepository` | `transactionId` (simple PK) |
| `POSHIST` (KSDS) | `PosHistRepository` | `@IdClass(PosHistId)` composite PK |

## BMS Map to Web UI Mapping

| BMS Map | Thymeleaf Template | URL Pattern |
|---|---|---|
| `MENMAP` | `menu.html` | `GET /` or `GET /menu` |
| `POSMAP` | `positions.html` | `GET /portfolio/{id}/positions` |
| `HISMAP` (10 rows) | `history.html` | `GET /portfolio/{id}/history?page=0&size=10` |
| `ERRMAP` | `error.html` | Error response page |

## CICS to Spring Mapping

| CICS Construct | Spring Equivalent |
|---|---|
| `EXEC CICS RECEIVE MAP` | HTTP request handling (`@GetMapping`) |
| `EXEC CICS SEND MAP` | Thymeleaf template rendering |
| `EXEC CICS RETURN` | HTTP response |
| `EXEC CICS LINK PROGRAM` | Service method injection (`@Autowired`) |
| `EXEC CICS READ FILE` | `repository.findById()` |
| `EXEC CICS ASSIGN USERID` | `SecurityContextHolder.getContext().getAuthentication()` |
| COMMAREA | Method parameters and return types |
| PF keys | Pagination controls and navigation links |

## DB2 to Spring/JPA Mapping

| DB2 Pattern | Spring Equivalent |
|---|---|
| `EXEC SQL SELECT` | JPA `@Query` / Repository methods |
| `EXEC SQL INSERT` | `repository.save()` |
| `EXEC SQL UPDATE` | `repository.save()` |
| `EXEC SQL DELETE` | `repository.delete()` |
| `EXEC SQL COMMIT` | `@Transactional` (auto-commit) |
| `EXEC SQL ROLLBACK` | Spring rollback-on-exception |
| SQLCODE checking | `DataAccessException` hierarchy |
| SQLCODE -803 | `DataIntegrityViolationException` |
| Deadlock handling | `@Retryable` with `DeadlockLoserDataAccessException` |

## Error Code Preservation

| COBOL Return Code | Java Enum | Value |
|---|---|---|
| `RC-SUCCESS` | `ReturnCode.SUCCESS` | 0 |
| `RC-WARNING` | `ReturnCode.WARNING` | 4 |
| `RC-ERROR` | `ReturnCode.ERROR` | 8 |
| `RC-SEVERE` | `ReturnCode.SEVERE` | 12 |
| `RC-CRITICAL` | `ReturnCode.CRITICAL` | 16 |

## Key Decisions

1. **Soft Delete**: Portfolio deletion uses soft delete (status='C') matching COBOL behavior
2. **Chunk Size**: History load job uses `chunk(1000)` matching `WS-COMMIT-THRESHOLD`
3. **BigDecimal Precision**: All COMP-3 fields use `BigDecimal` with matching precision/scale
4. **Composite Keys**: VSAM composite keys use `@IdClass` pattern for JPA compatibility
5. **Date Handling**: COBOL `PIC 9(8)` dates converted to `LocalDate`; timestamps to `LocalDateTime`
6. **Error Hierarchy**: COBOL return codes preserved as enum constants; error types mapped to exception subclasses
7. **Security**: RACF/CICS security replaced with Spring Security; auth checks via `SecurityContextHolder`
8. **Connection Pooling**: DB2CONN.cbl eliminated; Spring Boot auto-configures HikariCP
9. **Commit Management**: DB2CMT.cbl eliminated; `@Transactional` handles commit/rollback
10. **BMS Maps**: Terminal-based UI replaced with Thymeleaf templates preserving same data display

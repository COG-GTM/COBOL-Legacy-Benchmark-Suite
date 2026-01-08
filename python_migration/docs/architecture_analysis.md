# Architecture Analysis Document: COBOL-to-Python Migration

## Investment Portfolio Management System

Version: 1.0
Date: 2026-01-08

## 1. Executive Summary

This document provides a comprehensive analysis of the COBOL Legacy Benchmark Suite Investment Portfolio Management System and maps all components to their proposed Python equivalents. The migration covers batch processing, online transaction processing (CICS), reporting, utilities, and testing components.

## 2. System Overview

The Investment Portfolio Management System is a production-grade COBOL application designed for z/OS environments. It manages investment portfolios, processes financial transactions, maintains position history, and provides online inquiry capabilities.

### 2.1 Original Architecture Layers

The system consists of five primary layers:

1. **Batch Processing Layer** - Sequential transaction processing pipeline
2. **Online Layer (CICS)** - Real-time inquiry and user interaction
3. **Reporting System** - Analytical and audit report generation
4. **Utility Layer** - System maintenance and monitoring tools
5. **Test Layer** - Data generation and validation for benchmarking

## 3. Component Mapping: COBOL to Python

### 3.1 Batch Processing Layer

| COBOL Program | Purpose | Python Equivalent | Module |
|---------------|---------|-------------------|--------|
| TRNVAL00 | Transaction Validation | `TransactionValidator` class | `src/batch/transaction_validator.py` |
| POSUPD00/POSUPDT | Position Update | `PositionManager` class | `src/batch/position_manager.py` |
| HISTLD00 | History Load to DB2 | `HistoryLoader` class | `src/batch/history_loader.py` |
| BCHCTL00 | Batch Control Processor | `BatchController` class | `src/batch/batch_controller.py` |
| RPTPOS00 | Position Report Generator | `PositionReportGenerator` class | `src/batch/reports/position_report.py` |
| RPTAUD00 | Audit Report Generator | `AuditReportGenerator` class | `src/batch/reports/audit_report.py` |
| RPTSTA00 | Statistics Report Generator | `StatisticsReportGenerator` class | `src/batch/reports/statistics_report.py` |

### 3.2 Online Transaction Processing Layer

| COBOL Program | Purpose | Python Equivalent | Module |
|---------------|---------|-------------------|--------|
| INQONLN | Main Online Controller | Flask Blueprint `inquiry_bp` | `src/web/routes/inquiry.py` |
| INQPORT | Portfolio Position Inquiry | `PortfolioInquiryService` class | `src/web/services/portfolio_service.py` |
| INQHIST | Transaction History Inquiry | `HistoryInquiryService` class | `src/web/services/history_service.py` |
| SECMGR | Security Manager | `SecurityManager` class | `src/security/security_manager.py` |
| CURSMGR | Cursor Manager | SQLAlchemy ORM | `src/database/` |
| DB2ONLN | Online DB2 Controller | SQLAlchemy connection pool | `src/database/connection.py` |
| DB2RECV | DB2 Recovery | SQLAlchemy transaction management | `src/database/connection.py` |
| ERRHNDL | Error Handler | Python exception handling + logging | `src/web/error_handler.py` |

### 3.3 Utility Layer

| COBOL Program | Purpose | Python Equivalent | Module |
|---------------|---------|-------------------|--------|
| UTLMNT00 | File Maintenance | `MaintenanceUtility` class | `src/batch/utilities/maintenance.py` |
| UTLMON00 | System Monitor | `SystemMonitor` class | `src/batch/utilities/monitor.py` |
| UTLVAL00 | Data Validation | `DataValidator` class | `src/batch/utilities/validator.py` |

### 3.4 Test Layer

| COBOL Program | Purpose | Python Equivalent | Module |
|---------------|---------|-------------------|--------|
| TSTGEN00 | Test Data Generator | `TestDataGenerator` class | `tests/generators/test_data_generator.py` |
| TSTVAL00 | Test Validation Suite | pytest test suite | `tests/` |

## 4. Data Structure Mapping

### 4.1 COBOL Copybooks to Python Dataclasses

#### POSREC.cpy -> Position Model

```python
@dataclass
class Position:
    portfolio_id: str          # POS-PORTFOLIO-ID (8 chars)
    date: str                  # POS-DATE (YYYYMMDD)
    investment_id: str         # POS-INVESTMENT-ID (10 chars)
    quantity: Decimal          # POS-QUANTITY S9(11)V9(4)
    cost_basis: Decimal        # POS-COST-BASIS S9(13)V9(2)
    market_value: Decimal      # POS-MARKET-VALUE S9(13)V9(2)
    currency: str              # POS-CURRENCY (3 chars)
    status: str                # POS-STATUS (A/C/P)
    last_maint_date: datetime  # POS-LAST-MAINT-DATE
    last_maint_user: str       # POS-LAST-MAINT-USER
```

#### TRNREC.cpy -> Transaction Model

```python
@dataclass
class Transaction:
    date: str                  # TRN-DATE (YYYYMMDD)
    time: str                  # TRN-TIME (HHMMSS)
    portfolio_id: str          # TRN-PORTFOLIO-ID (8 chars)
    sequence_no: str           # TRN-SEQUENCE-NO (6 chars)
    investment_id: str         # TRN-INVESTMENT-ID (10 chars)
    type: TransactionType      # TRN-TYPE (BU/SL/TR/FE)
    quantity: Decimal          # TRN-QUANTITY S9(11)V9(4)
    price: Decimal             # TRN-PRICE S9(11)V9(4)
    amount: Decimal            # TRN-AMOUNT S9(13)V9(2)
    currency: str              # TRN-CURRENCY (3 chars)
    status: TransactionStatus  # TRN-STATUS (P/D/F/R)
    process_date: datetime     # TRN-PROCESS-DATE
    process_user: str          # TRN-PROCESS-USER
```

#### HISTREC.cpy -> History Model

```python
@dataclass
class HistoryRecord:
    portfolio_id: str          # HIST-PORTFOLIO-ID (8 chars)
    date: str                  # HIST-DATE (YYYYMMDD)
    time: str                  # HIST-TIME (HHMMSS)
    seq_no: str                # HIST-SEQ-NO (4 chars)
    record_type: str           # HIST-RECORD-TYPE (PT/PS/TR)
    action_code: str           # HIST-ACTION-CODE (A/C/D)
    before_image: str          # HIST-BEFORE-IMAGE
    after_image: str           # HIST-AFTER-IMAGE
    reason_code: str           # HIST-REASON-CODE
    process_date: datetime     # HIST-PROCESS-DATE
    process_user: str          # HIST-PROCESS-USER
```

#### BCHCTL.cpy -> BatchControl Model

```python
@dataclass
class BatchControl:
    job_name: str              # BCT-JOB-NAME (8 chars)
    process_date: str          # BCT-PROCESS-DATE (YYYYMMDD)
    sequence_no: int           # BCT-SEQUENCE-NO
    status: BatchStatus        # BCT-STATUS (R/A/W/D/E)
    step_name: str             # BCT-STEP-NAME
    program_name: str          # BCT-PROGRAM-NAME
    start_time: str            # BCT-START-TIME
    end_time: str              # BCT-END-TIME
    prereq_count: int          # BCT-PREREQ-COUNT
    prereq_jobs: List[str]     # BCT-PREREQ-JOBS
    return_code: int           # BCT-RETURN-CODE
    error_desc: str            # BCT-ERROR-DESC
    restart_count: int         # BCT-RESTART-COUNT
```

## 5. Database Migration

### 5.1 VSAM Files to SQLite/PostgreSQL

| VSAM File | Type | Python Equivalent | Table Name |
|-----------|------|-------------------|------------|
| POSMSTRE | KSDS | SQLAlchemy Model | `positions` |
| TRANHIST | ESDS | SQLAlchemy Model | `transaction_history` |
| BCHCTL | KSDS | SQLAlchemy Model | `batch_control` |
| AUDITLOG | KSDS | SQLAlchemy Model | `audit_log` |

### 5.2 DB2 Tables to SQLAlchemy Models

| DB2 Table | Purpose | SQLAlchemy Model |
|-----------|---------|------------------|
| POSHIST | Position History | `PositionHistory` |
| ERRLOG | Error Log | `ErrorLog` |
| AUTHFILE | Authorization | `Authorization` |
| AUDITLOG | Audit Trail | `AuditLog` |

## 6. Process Flow Migration

### 6.1 Batch Processing Flow

**Original COBOL Flow:**
```
TRNVAL00 -> POSUPD00 -> HISTLD00 -> RPTPOS00/RPTAUD00/RPTSTA00
```

**Python Equivalent:**
```python
# Batch processing pipeline
def run_batch_pipeline(input_file: str) -> BatchResult:
    # Step 1: Validate transactions
    validator = TransactionValidator()
    valid_transactions = validator.validate_transactions(input_file)
    
    # Step 2: Update positions
    position_manager = PositionManager()
    position_manager.update_positions(valid_transactions)
    
    # Step 3: Load history to database
    history_loader = HistoryLoader()
    history_loader.load_history(valid_transactions)
    
    # Step 4: Generate reports
    reports = ReportGenerator()
    reports.generate_all_reports()
    
    return BatchResult(success=True)
```

### 6.2 Online Inquiry Flow

**Original CICS Flow:**
```
User -> CICS -> INQONLN -> SECMGR -> INQPORT/INQHIST -> DB2 -> Response
```

**Python Flask Equivalent:**
```python
# Flask route handling
@inquiry_bp.route('/api/portfolio/<account_id>')
@require_auth
def get_portfolio(account_id: str):
    security_manager.validate_access(current_user, 'portfolio', 'read')
    portfolio_service = PortfolioInquiryService()
    return portfolio_service.get_position(account_id)
```

## 7. Security Migration

### 7.1 RACF/CICS Security to Python

| COBOL Security | Python Equivalent |
|----------------|-------------------|
| RACF User Validation | Flask-Login + JWT |
| CICS Transaction Security | Route decorators |
| DB2 Authorization | SQLAlchemy row-level security |
| Audit Logging | Python logging + DB audit table |

### 7.2 Security Manager Functions

| SECMGR Function | Python Implementation |
|-----------------|----------------------|
| SEC-VALIDATE (V) | `SecurityManager.validate_user()` |
| SEC-AUTHORIZE (A) | `SecurityManager.check_authorization()` |
| SEC-AUDIT (L) | `SecurityManager.log_access()` |

## 8. Error Handling Migration

### 8.1 COBOL Error Codes to Python Exceptions

| COBOL Error | Python Exception |
|-------------|------------------|
| E001 - Invalid Account | `InvalidAccountError` |
| E002 - Invalid Fund ID | `InvalidFundError` |
| E003 - Invalid Transaction Type | `InvalidTransactionTypeError` |
| E004 - Insufficient Balance | `InsufficientBalanceError` |
| W001 - Zero Dollar Transaction | `ZeroAmountWarning` |
| W002 - Duplicate Transaction | `DuplicateTransactionWarning` |

### 8.2 Return Codes

| COBOL RC | Python Equivalent |
|----------|-------------------|
| 0000 | `BatchResult(success=True, warnings=[])` |
| 0004 | `BatchResult(success=True, warnings=[...])` |
| 0008 | `BatchResult(success=False, errors=[...])` |
| 0012 | Raise `CriticalError` exception |
| 0016 | Raise `EnvironmentError` exception |

## 9. Technology Stack

### 9.1 Python Dependencies

- **Web Framework**: Flask 3.x
- **ORM**: SQLAlchemy 2.x
- **Database**: SQLite (dev) / PostgreSQL (prod)
- **Authentication**: Flask-Login, PyJWT
- **Validation**: Pydantic
- **Testing**: pytest
- **Reporting**: ReportLab (PDF), openpyxl (Excel)

### 9.2 Project Structure

```
python_migration/
├── docs/
│   ├── architecture_analysis.md
│   └── data_architecture.md
├── src/
│   ├── batch/
│   │   ├── __init__.py
│   │   ├── transaction_validator.py
│   │   ├── position_manager.py
│   │   ├── history_loader.py
│   │   ├── batch_controller.py
│   │   └── reports/
│   │       ├── position_report.py
│   │       ├── audit_report.py
│   │       └── statistics_report.py
│   ├── web/
│   │   ├── __init__.py
│   │   ├── app.py
│   │   ├── routes/
│   │   │   └── inquiry.py
│   │   ├── services/
│   │   │   ├── portfolio_service.py
│   │   │   └── history_service.py
│   │   └── templates/
│   ├── security/
│   │   ├── __init__.py
│   │   └── security_manager.py
│   ├── models/
│   │   ├── __init__.py
│   │   ├── position.py
│   │   ├── transaction.py
│   │   └── history.py
│   └── database/
│       ├── __init__.py
│       └── connection.py
└── tests/
    └── generators/
        └── test_data_generator.py
```

## 10. Migration Considerations

### 10.1 Key Differences

1. **File Handling**: VSAM indexed files replaced with SQL database tables
2. **Transaction Processing**: CICS pseudo-conversational replaced with stateless REST APIs
3. **Screen Handling**: BMS maps replaced with HTML templates and JSON responses
4. **Checkpoint/Restart**: Database transactions with savepoints
5. **Security**: RACF replaced with JWT-based authentication

### 10.2 Preserved Business Logic

All business rules from the original COBOL programs are preserved:
- Transaction validation rules (account, fund, date, amount checks)
- Position calculation algorithms (cost basis, average cost)
- Audit trail requirements
- Error handling and recovery procedures

## 11. Conclusion

This migration plan provides a comprehensive mapping from the COBOL Investment Portfolio Management System to a modern Python implementation. The Python version maintains all business logic while leveraging modern frameworks and best practices for web development, database management, and security.

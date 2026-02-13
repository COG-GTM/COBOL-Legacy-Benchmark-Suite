# Technology Stack Selection: COBOL to Python Migration

Version: 1.0
Date: 2026-02-13

## 1. Overview

This document specifies the technology selections for migrating the COBOL Legacy Benchmark Suite (Investment Portfolio Management System) from Enterprise COBOL on z/OS to a modern Python-based architecture.

## 2. Database: PostgreSQL

**Selection**: PostgreSQL 16+

**Rationale**:
- Superior support for DECIMAL/NUMERIC types critical for financial calculations (matches DB2's DECIMAL precision)
- Native partitioning support (POSHIST table uses quarterly partitioning in DB2)
- ACID compliance matching DB2's transactional guarantees
- Mature stored procedure support for migrating DB2 procedures (e.g., ERRLOG_CLEANUP)
- Strong indexing capabilities including partial indexes and expression indexes
- Foreign key constraints matching the existing DB2 schema relationships
- Enterprise-grade concurrency control comparable to DB2's isolation levels (CS mapped to READ COMMITTED)
- Extensive SQLAlchemy support with full ORM feature coverage

**DB2 Table Migration Mapping**:

| DB2 Table/Object       | PostgreSQL Equivalent         | Notes                                    |
|------------------------|-------------------------------|------------------------------------------|
| POSHIST                | `poshist`                     | Partitioned by `trans_date` (quarterly range partitioning) |
| ERRLOG                 | `errlog`                      | Includes cleanup function migration      |
| PORTFOLIO_MASTER       | `portfolio_master`            | Primary portfolio records                |
| INVESTMENT_POSITIONS   | `investment_positions`        | Foreign key to portfolio_master preserved |
| TRANSACTION_HISTORY    | `transaction_history`         | Foreign key to portfolio_master preserved |
| RTNCODES               | `return_codes`                | Return code logging                      |
| ACTIVE_PORTFOLIOS view | `active_portfolios` (view)    | Migrated with PostgreSQL syntax          |
| CURRENT_POSITIONS view | `current_positions` (view)    | `CURRENT DATE` mapped to `CURRENT_DATE`  |
| PORTPLAN (bind plan)   | Connection pool configuration | Replaced by SQLAlchemy connection pooling |

**DB2 Feature Migration**:

| DB2 Feature           | PostgreSQL Equivalent                 |
|-----------------------|---------------------------------------|
| STOGROUP/BUFFERPOOL   | Tablespace configuration              |
| PARTITION BY RANGE    | Native declarative partitioning       |
| COMPRESS YES          | TOAST compression + pg_compression    |
| ISOLATION(CS)         | READ COMMITTED isolation level        |
| GRANT SELECT/INSERT   | PostgreSQL GRANT statements           |
| BIND PLAN             | SQLAlchemy connection pool + session   |

## 3. VSAM File Replacement: PostgreSQL

**Selection**: PostgreSQL (same instance as DB2 replacement)

**Rationale**:
- Consolidates all data storage into a single database engine, reducing operational complexity
- PostgreSQL KSDS equivalent: standard indexed tables with composite primary keys
- VSAM share options (2,3) mapped to PostgreSQL's MVCC (read-while-write support)
- VSAM recovery features replaced by PostgreSQL WAL-based recovery
- Eliminates need for separate SQLite databases and associated connection management

**VSAM File Migration Mapping**:

| VSAM File  | Org  | PostgreSQL Table       | Key Mapping                              |
|------------|------|------------------------|------------------------------------------|
| PORTMSTR   | KSDS | `vsam_portfolio_master`| `(portfolio_id, account_type, branch_id)` composite PK |
| TRANHIST   | KSDS | `vsam_transaction_history` | `(trans_date, trans_time, portfolio_id, sequence_no)` composite PK |
| POSHIST    | KSDS | `vsam_position_history`| `(portfolio_id, position_date, investment_id)` composite PK |

**VSAM Feature Migration**:

| VSAM Feature         | PostgreSQL Equivalent              |
|----------------------|------------------------------------|
| KSDS (Key-Sequenced) | B-tree indexed table               |
| CI/CA FREESPACE      | fillfactor storage parameter       |
| SHAREOPTIONS(2,3)    | MVCC (concurrent read/write)       |
| RECOVERY             | WAL-based point-in-time recovery   |
| BUFFER SPACE         | shared_buffers configuration       |

## 4. Job Scheduling: Apache Airflow

**Selection**: Apache Airflow 2.x

**Rationale**:
- DAG-based workflow definition naturally models the existing batch job dependencies (TRNVAL00 -> POSUPD00 -> HISTLD00 -> Reports)
- Built-in support for conditional execution based on return codes (equivalent to JCL COND parameter)
- Native checkpoint/restart capabilities via task retries and catchup
- Web UI for monitoring job execution (replaces z/OS operator console)
- Scheduling with time windows maps directly to the existing batch schedule (1800-2000)
- Sensor operators can implement dependency checking (equivalent to JCL job dependencies)
- PostgreSQL as metadata backend aligns with our database choice

**JCL Migration Mapping**:

| JCL File          | Airflow DAG                    | Schedule      |
|-------------------|--------------------------------|---------------|
| Batch pipeline    | `portfolio_batch_pipeline`     | Daily 18:00   |
| RPTPOS.jcl        | Task in reporting DAG          | After HISTLD00|
| RPTAUD.jcl        | Task in reporting DAG          | After HISTLD00|
| RPTSTA.jcl        | Task in reporting DAG          | After HISTLD00|
| UTLMNT.jcl        | `utility_maintenance`          | Weekly        |
| UTLMON.jcl        | `utility_monitoring`           | Every 15 min  |
| UTLVAL.jcl        | `utility_validation`           | Daily         |
| TSTGEN.jcl        | `test_data_generation`         | On-demand     |
| TSTVAL.jcl        | `test_validation`              | On-demand     |

**z/OS Scheduler Feature Migration**:

| z/OS Feature           | Airflow Equivalent                    |
|------------------------|---------------------------------------|
| Job dependencies       | DAG task dependencies                 |
| COND parameter (RC)    | `trigger_rule` + BranchPythonOperator |
| Time windows           | `schedule_interval` + `execution_timeout` |
| Checkpoint/restart     | Task retries + `catchup=True`         |
| JCL EXEC PGM           | PythonOperator / BashOperator         |
| STEPLIB                | Python import paths                   |

## 5. Web Interface

### 5.1 Backend Framework: Flask

**Selection**: Flask 3.x with Flask-RESTful

**Rationale**:
- Lightweight framework suitable for the focused API surface (portfolio inquiry, history lookup, error display)
- Minimal boilerplate compared to Django for a system with 4 primary screens
- Flask-SQLAlchemy integrates directly with our SQLAlchemy ORM models
- Easy to add authentication middleware (replacing CICS SECMGR)
- RESTful API design maps naturally to the existing CICS transaction model

### 5.2 Frontend Framework: React

**Selection**: React 18+ with TypeScript

**Rationale**:
- Component-based architecture maps well to BMS screen maps (MENMAP, POSMAP, HISMAP, ERRMAP)
- React Router for screen navigation (replaces CICS screen flow managed by INQONLN)
- Strong ecosystem for data tables (history view with 10 scrollable rows)
- TypeScript provides type safety matching COBOL's strict data typing

**BMS Screen Migration Mapping**:

| BMS Map  | React Component          | Route            |
|----------|--------------------------|------------------|
| MENMAP   | `MainMenu`               | `/`              |
| POSMAP   | `PortfolioPosition`      | `/portfolio`     |
| HISMAP   | `TransactionHistory`     | `/history`       |
| ERRMAP   | `ErrorDisplay`           | `/error`         |

**CICS Feature Migration**:

| CICS Feature         | Web Equivalent                        |
|----------------------|---------------------------------------|
| Transaction PINQ     | REST API endpoints                    |
| DFHCOMMAREA          | HTTP request/response + session state |
| BMS SEND MAP         | React component render                |
| BMS RECEIVE MAP      | Form submission / API call            |
| PF keys (PF3/PF7/PF8)| Navigation buttons + keyboard shortcuts |
| SECMGR validation    | JWT authentication middleware          |
| ERRHNDL              | Error boundary components + API error handling |

## 6. Testing Framework: pytest

**Selection**: pytest 8.x

**Rationale**:
- Industry standard for Python testing
- Fixture system for database setup/teardown (replaces TSTGEN00 test data generation)
- Parametrized tests for validating business rules (replaces TSTVAL00 test cases)
- Plugin ecosystem: pytest-cov (coverage), pytest-asyncio (async tests), pytest-flask (API testing)
- Assertion introspection provides detailed failure messages

**Test Migration Mapping**:

| COBOL Test Component | pytest Equivalent                  |
|----------------------|------------------------------------|
| TSTGEN00             | pytest fixtures + factory_boy      |
| TSTVAL00             | pytest test modules + assertions   |
| JCL test jobs        | pytest configuration + CI pipeline |

## 7. Additional Technology Choices

| Component              | Technology           | Purpose                               |
|------------------------|----------------------|---------------------------------------|
| ORM                    | SQLAlchemy 2.x       | Database abstraction and model layer  |
| Data validation        | Pydantic 2.x         | Copybook field validation rules       |
| Database migrations    | Alembic 1.x          | Schema version control                |
| API documentation      | Flask-OpenAPI3        | Auto-generated API docs               |
| Logging                | Python logging + structlog | Replaces ERRLOG/ERRPROC          |
| Configuration          | python-dotenv + pydantic-settings | Environment-based config     |
| Task queue (future)    | Celery (if needed)   | Async processing beyond Airflow       |

## 8. Python Version

**Selection**: Python 3.12+

**Rationale**:
- Latest stable release with performance improvements
- Native dataclass support for copybook translation
- Type hints for maintaining COBOL's strict typing discipline
- `decimal` module for precise financial calculations matching COBOL COMP-3

## 9. Deployment Architecture Summary

```
                    ┌─────────────────────┐
                    │   React Frontend    │
                    │   (TypeScript)      │
                    └─────────┬───────────┘
                              │ REST API
                    ┌─────────▼───────────┐
                    │   Flask Backend     │
                    │   (Python 3.12+)    │
                    │   + SQLAlchemy ORM  │
                    └─────────┬───────────┘
                              │
                    ┌─────────▼───────────┐
                    │   PostgreSQL 16+    │
                    │   (DB2 + VSAM)      │
                    └─────────────────────┘
                              │
                    ┌─────────▼───────────┐
                    │   Apache Airflow    │
                    │   (Batch Scheduler) │
                    └─────────────────────┘
```

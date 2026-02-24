# Technology Stack Selection: COBOL to Python Migration

## Overview

This document specifies the target technology stack for migrating the Investment Portfolio Management System from Enterprise COBOL on z/OS to a modern Python-based architecture. Each selection includes justification and migration considerations.

---

## 1. Database: PostgreSQL

### Selection: PostgreSQL 16+

### Justification
- **DB2 compatibility**: PostgreSQL has the closest SQL dialect to DB2 among open-source databases, minimizing query rewriting effort.
- **DECIMAL precision**: Native `NUMERIC`/`DECIMAL` types support the exact precision required for financial calculations (up to `DECIMAL(18,4)` used in the COBOL system).
- **Advanced features**: Supports views, CTEs, window functions, stored procedures, and triggers — all used in the existing DB2 schema.
- **ACID compliance**: Full transaction support with savepoints, matching the DB2CMT commit/rollback/savepoint pattern.
- **JSON support**: Native JSONB type for flexible storage if needed for audit trail before/after images.
- **Connection pooling**: PgBouncer or built-in pooling matches the DB2ONLN connection pool pattern.
- **Mature ecosystem**: Excellent SQLAlchemy support, Alembic migrations, and psycopg2/asyncpg drivers.

### DB2 Table Migration Plan

| DB2 Table | PostgreSQL Table | Key Changes |
|-----------|-----------------|-------------|
| PORTFOLIO_MASTER | portfolio_master | CHAR → VARCHAR where appropriate; TIMESTAMP uses `TIMESTAMP WITH TIME ZONE` |
| INVESTMENT_POSITIONS | investment_positions | DECIMAL(18,4) → NUMERIC(18,4); composite PK preserved |
| TRANSACTION_HISTORY | transaction_history | TIME column → TIME type; CHAR(20) ID preserved |
| POSHIST | position_history | Maps from DBTBLS copybook host variables |
| ERRLOG | error_log | ERROR_TIMESTAMP → TIMESTAMPTZ |
| AUTHFILE | auth_file | Inferred from SECMGR program SQL usage |
| AUDITLOG | audit_log | Inferred from SECMGR program SQL usage |
| RTNCODES | return_codes | INTEGER types preserved |

### DB2-Specific Constructs Migration

| DB2 Construct | PostgreSQL Equivalent |
|---------------|----------------------|
| `CURRENT DATE` | `CURRENT_DATE` |
| `CURRENT TIMESTAMP` | `NOW()` or `CURRENT_TIMESTAMP` |
| `BIND PLAN` (PORTPLAN) | No equivalent needed — managed by SQLAlchemy |
| `ISOLATION(CS)` | `SET default_transaction_isolation = 'read committed'` |
| Embedded SQL (EXEC SQL) | SQLAlchemy ORM / Core queries |

---

## 2. File Storage (VSAM Replacement): PostgreSQL

### Selection: PostgreSQL (same instance as primary database)

### Justification
- **Unified data layer**: Consolidating VSAM files into PostgreSQL tables eliminates the need for a separate file storage system and simplifies data access patterns.
- **KSDS equivalent**: PostgreSQL B-tree indexes on primary keys provide the same key-sequenced access as VSAM KSDS.
- **Transaction support**: VSAM RECOVERY mode is replaced by PostgreSQL's ACID transactions.
- **Concurrent access**: VSAM SHARE OPTIONS(2,3) (read-while-update) maps to PostgreSQL's MVCC.
- **Simplicity**: One database technology instead of two reduces operational complexity.

### VSAM File Migration Plan

| VSAM File | PostgreSQL Table | Key Mapping |
|-----------|-----------------|-------------|
| PORTMSTR (Portfolio Master, KSDS, 400 bytes) | `vsam_portfolio_master` | PK: portfolio_id + account_type + branch_id |
| TRANHIST (Transaction History, KSDS, 300 bytes) | `vsam_transaction_history` | PK: trans_date + trans_time + portfolio_id + sequence_no |
| POSHIST (Position History, KSDS, 350 bytes) | `vsam_position_history` | PK: portfolio_id + position_date + investment_id |
| BATCH-CONTROL-FILE | `batch_control` | PK: batch_key |

### VSAM Access Pattern Mapping

| VSAM Operation | PostgreSQL Equivalent |
|---------------|----------------------|
| READ (by key) | `SELECT ... WHERE pk = ?` |
| READ NEXT | Cursor with `ORDER BY pk` |
| WRITE | `INSERT INTO ...` |
| REWRITE | `UPDATE ... WHERE pk = ?` |
| DELETE | `DELETE FROM ... WHERE pk = ?` |
| START (position cursor) | `SELECT ... WHERE pk >= ? ORDER BY pk` |

---

## 3. Job Scheduling: Apache Airflow

### Selection: Apache Airflow 2.x

### Justification
- **JCL replacement**: Airflow DAGs provide the same job sequencing and dependency management as JCL EXEC PGM steps.
- **Dependency management**: Airflow's task dependency model maps directly to the PRCSEQ00 process sequencing logic.
- **Checkpoint/restart**: Airflow's task-level retry and re-run capabilities replace the CKPRST checkpoint/restart pattern.
- **Monitoring**: Built-in web UI replaces z/OS SDSF for job monitoring; alerting replaces UTLMON00 system monitoring.
- **Scheduling**: Cron-based and event-driven scheduling replaces z/OS job scheduler.
- **Industry standard**: Widely adopted for ETL and batch processing workflows in financial services.

### JCL to Airflow Migration Plan

| JCL Job | Airflow DAG | Tasks |
|---------|-------------|-------|
| RPTPOS.jcl | `dag_daily_position_report` | init → generate_report → archive |
| RPTAUD.jcl | `dag_audit_report` | init → generate_audit_report → generate_error_report → archive |
| RPTSTA.jcl | `dag_statistics_report` | init → collect_stats → generate_report → archive |
| (Batch load) | `dag_history_load` | init → validate → load_to_db → commit → stats |

### JCL Construct Mapping

| JCL Construct | Airflow Equivalent |
|--------------|-------------------|
| `//STEP EXEC PGM=` | `PythonOperator` or `BashOperator` task |
| `//DD DSN=` | Task parameters / Airflow Variables |
| `COND=(0,NE)` | `trigger_rule='all_success'` |
| `RESTART` | Airflow task retry / manual re-run |
| `JOB CLASS` | Airflow pools and queues |

---

## 4. Web Interface: Flask + React

### Selection: Flask (backend) + React (frontend)

### Backend: Flask

#### Justification
- **Lightweight**: Flask's minimal footprint is appropriate for replacing the focused CICS transaction set (3 inquiry screens + menu).
- **REST API**: Flask-RESTful provides clean REST endpoint creation for each CICS transaction equivalent.
- **Flexibility**: No opinionated ORM or template system — we use SQLAlchemy independently.
- **Extensions**: Flask-Login (security/SECMGR replacement), Flask-CORS, Flask-Migrate (Alembic integration).

### Frontend: React

#### Justification
- **Component model**: Each BMS map (MENMAP, POSMAP, HISMAP, ERRMAP) maps cleanly to a React component.
- **State management**: React state/context replaces CICS COMMAREA for passing data between screens.
- **Table rendering**: React table libraries handle the scrollable history display (HISMAP with 10-row pagination).
- **Industry standard**: Largest ecosystem and hiring pool for financial services frontend development.

### BMS Screen Migration Plan

| BMS Map | React Component | Flask Endpoint |
|---------|----------------|----------------|
| MENMAP (Main Menu) | `<MainMenu />` | — (client-side routing) |
| POSMAP (Portfolio Positions) | `<PortfolioPositions />` | `GET /api/portfolios/{id}/positions` |
| HISMAP (Transaction History) | `<TransactionHistory />` | `GET /api/portfolios/{id}/history?page=&size=` |
| ERRMAP (Error Display) | `<ErrorDisplay />` | Error responses in API JSON |

### CICS Command Mapping

| CICS Command | Python/Flask Equivalent |
|-------------|----------------------|
| `EXEC CICS RECEIVE MAP` | Flask request parsing (`request.json`, `request.args`) |
| `EXEC CICS SEND MAP` | Flask JSON response (`jsonify(...)`) |
| `EXEC CICS LINK PROGRAM` | Python function/method call |
| `EXEC CICS READ FILE` | SQLAlchemy query |
| `EXEC CICS ASSIGN USERID` | Flask-Login `current_user` |
| COMMAREA | Function parameters / request context |

---

## 5. Testing Framework: pytest

### Selection: pytest 8.x

### Justification
- **Industry standard**: Most widely used Python testing framework.
- **Fixtures**: pytest fixtures replace TSTGEN00 test data generation with reusable, composable test setup.
- **Parametrize**: `@pytest.mark.parametrize` enables systematic testing of all transaction types (BU/SL/TR/FE) and status codes.
- **Plugins**: pytest-cov (coverage), pytest-asyncio (async tests), pytest-flask (API testing), factory-boy (test data factories).

### Test Program Migration Plan

| COBOL Test Program | pytest Equivalent |
|-------------------|-------------------|
| TSTGEN00 (Test Data Generator) | `tests/conftest.py` fixtures + `tests/factories.py` (factory-boy) |
| TSTVAL00 (Test Validation Suite) | `tests/test_*.py` test modules |
| TSTVAL00 FUNCTIONAL tests | `tests/unit/` — unit tests for each service |
| TSTVAL00 INTEGRATE tests | `tests/integration/` — database and API integration tests |
| TSTVAL00 PERFORM tests | `tests/performance/` — benchmark tests with pytest-benchmark |
| TSTVAL00 ERROR tests | `tests/unit/test_error_handling.py` |

---

## 6. Additional Technology Selections

| Component | Selection | Justification |
|-----------|-----------|---------------|
| **ORM** | SQLAlchemy 2.x | Industry standard Python ORM; declarative models, connection pooling, migration support |
| **Migrations** | Alembic | Integrated with SQLAlchemy; version-controlled schema migrations |
| **Data Validation** | Pydantic 2.x | Type-safe data validation replacing COBOL copybook field definitions and PORTVALD validation |
| **Logging** | Python `logging` + structlog | Replaces ERRPROC/ERRHNDL error logging; structured JSON logs for monitoring |
| **Configuration** | python-dotenv + Pydantic Settings | Replaces JCL DD statements and CICS system variables |
| **API Documentation** | Flask-OpenAPI / Swagger | Auto-generated API docs for CICS replacement endpoints |
| **Authentication** | Flask-Login + JWT | Replaces SECMGR CICS authentication |
| **Task Queue** | Celery (optional) | For async processing within the Flask app if needed |

---

## 7. Python Dependencies Summary

```
# Core
flask>=3.0
sqlalchemy>=2.0
alembic>=1.13
pydantic>=2.0
psycopg2-binary>=2.9

# Web
flask-restful>=0.3
flask-cors>=4.0
flask-login>=0.6
flask-migrate>=4.0
gunicorn>=22.0

# Scheduling
apache-airflow>=2.8

# Testing
pytest>=8.0
pytest-cov>=5.0
pytest-flask>=1.3
factory-boy>=3.3

# Utilities
python-dotenv>=1.0
structlog>=24.0
```

---

## 8. Architecture Diagram (Target State)

```
┌─────────────────────────────────────────────────────────┐
│                    React Frontend                        │
│  ┌──────────┐ ┌──────────────┐ ┌────────────────────┐  │
│  │ MainMenu │ │ Positions    │ │ TransactionHistory │  │
│  └──────────┘ └──────────────┘ └────────────────────┘  │
└────────────────────────┬────────────────────────────────┘
                         │ REST API (JSON)
┌────────────────────────▼────────────────────────────────┐
│                    Flask Backend                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ Auth API │ │ Port API │ │ Hist API │ │ Rpt API  │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
│                         │                               │
│  ┌──────────────────────▼──────────────────────────┐   │
│  │              Service Layer                       │   │
│  │  PortfolioService  TransactionService  etc.      │   │
│  └──────────────────────┬──────────────────────────┘   │
│                         │                               │
│  ┌──────────────────────▼──────────────────────────┐   │
│  │           SQLAlchemy ORM Models                  │   │
│  └──────────────────────┬──────────────────────────┘   │
└─────────────────────────┼───────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────┐
│                   PostgreSQL                             │
│  ┌────────────────┐  ┌────────────────┐                 │
│  │  DB2 Tables    │  │  VSAM Tables   │                 │
│  │  (migrated)    │  │  (migrated)    │                 │
│  └────────────────┘  └────────────────┘                 │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                  Apache Airflow                          │
│  ┌────────────┐ ┌────────────┐ ┌────────────┐          │
│  │ Report DAG │ │  Load DAG  │ │ Maint DAG  │          │
│  └────────────┘ └────────────┘ └────────────┘          │
└─────────────────────────────────────────────────────────┘
```

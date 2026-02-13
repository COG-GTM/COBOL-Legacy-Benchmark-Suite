# Portfolio Management System - Python Migration

This project is the Python migration of the COBOL Legacy Benchmark Suite (CLBS) Investment Portfolio Management System, originally implemented in Enterprise COBOL for z/OS mainframes.

## Project Structure

```
portfolio-management-system/
├── src/
│   ├── batch/              # Batch processing modules (migrated from COBOL batch programs)
│   ├── api/                # REST API endpoints (replaces CICS online transactions)
│   ├── services/           # Business logic services
│   ├── database/           # Database connection and session management
│   ├── models/             # SQLAlchemy ORM models and Pydantic schemas
│   │   ├── transaction.py  # From TRNREC.cpy - transaction records
│   │   ├── position.py     # From POSREC.cpy - position records
│   │   ├── history.py      # From HISTREC.cpy - change history records
│   │   ├── inquiry.py      # From INQCOM.cpy - inquiry communication
│   │   ├── db2_request.py  # From DB2REQ.cpy - database request handling
│   │   └── error.py        # From ERRHND.cpy + ERRHAND.cpy - error handling
│   ├── reports/            # Report generators (migrated from RPT* programs)
│   ├── utilities/          # Utility functions (migrated from UTL* programs)
│   └── config/             # Application configuration
├── tests/                  # pytest test suite
├── migrations/             # PostgreSQL migration scripts
│   ├── 001_create_db2_tables.sql   # DB2 table migrations
│   └── 002_create_vsam_tables.sql  # VSAM file migrations
├── airflow/                # Apache Airflow DAGs (replaces JCL/z/OS scheduler)
├── frontend/               # React TypeScript frontend (replaces BMS screen maps)
├── docs/                   # Documentation
│   └── migration/          # Migration planning documents
├── requirements.txt        # Python dependencies
├── .gitignore             # Git ignore rules
└── README.md              # This file
```

## Technology Stack

| Component          | Technology        | Replaces                    |
|--------------------|-------------------|-----------------------------|
| Language           | Python 3.12+      | Enterprise COBOL for z/OS   |
| Database           | PostgreSQL 16+     | DB2 for z/OS + VSAM files   |
| ORM                | SQLAlchemy 2.x     | Embedded SQL / VSAM I/O     |
| Data Validation    | Pydantic 2.x       | COBOL copybook definitions  |
| Backend API        | Flask 3.x          | CICS online transactions    |
| Frontend           | React + TypeScript | BMS screen maps             |
| Job Scheduling     | Apache Airflow     | z/OS Scheduler + JCL        |
| Testing            | pytest             | TSTGEN00 / TSTVAL00         |

## Data Models

Each Python model in `src/models/` is mapped from its corresponding COBOL copybook:

| Python Model        | COBOL Copybook | Key Data Types                          |
|---------------------|----------------|-----------------------------------------|
| `TransactionRecord` | TRNREC.cpy     | COMP-3 decimals -> `Decimal`, 88-levels -> `Enum` |
| `PositionRecord`    | POSREC.cpy     | COMP-3 decimals -> `Decimal`, status flags -> `Enum` |
| `HistoryRecord`     | HISTREC.cpy    | PIC X(400) images -> `str`, action codes -> `Enum` |
| `InquiryRequest`    | INQCOM.cpy     | COMP binary -> `int`, function codes -> `Enum` |
| `DB2Request`        | DB2REQ.cpy     | COMP binary -> `int`, request types -> `Enum` |
| `ErrorHandling`     | ERRHND.cpy     | COMP binary -> `int`, severity/action -> `Enum` |
| `ErrorMessage`      | ERRHAND.cpy    | Return codes -> `IntEnum`, categories -> `Enum` |

## Database Migrations

Migration scripts in `migrations/` convert the original DB2 DDL and VSAM definitions to PostgreSQL:

- **001_create_db2_tables.sql**: Migrates DB2 tables (PORTFOLIO_MASTER, INVESTMENT_POSITIONS, TRANSACTION_HISTORY, POSHIST, ERRLOG, RTNCODES) with partitioning, indexes, views, and stored procedures
- **002_create_vsam_tables.sql**: Converts VSAM KSDS files (PORTMSTR, TRANHIST, POSHIST) to PostgreSQL tables with composite primary keys and appropriate fillfactor settings

## Migration Documentation

- `docs/migration/architecture-analysis.md` - Complete analysis of the original COBOL architecture
- `docs/migration/technology-stack.md` - Technology selection rationale and migration mappings

## Getting Started

### Prerequisites

- Python 3.12+
- PostgreSQL 16+
- Node.js 20+ (for frontend)

### Installation

```bash
cd portfolio-management-system
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

### Database Setup

```bash
psql -U postgres -c "CREATE DATABASE portfolio_mgmt;"
psql -U postgres -d portfolio_mgmt -f migrations/001_create_db2_tables.sql
psql -U postgres -d portfolio_mgmt -f migrations/002_create_vsam_tables.sql
```

### Running Tests

```bash
pytest tests/
```

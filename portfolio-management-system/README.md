# Investment Portfolio Management System — Python Migration

This project is a Python migration of the Investment Portfolio Management System, originally implemented in Enterprise COBOL for IBM z/OS mainframes. The migration preserves all business logic, data validation rules, and processing workflows while modernizing the technology stack.

## Project Structure

```
portfolio-management-system/
├── src/                        # Application source code
│   ├── batch/                  # Batch processing modules (from JCL/COBOL batch programs)
│   ├── api/                    # REST API endpoints (replaces CICS online transactions)
│   ├── services/               # Business logic services
│   ├── database/               # Database connection and session management
│   ├── models/                 # Data models (SQLAlchemy ORM + Pydantic)
│   │   ├── orm.py              # SQLAlchemy ORM models for all database tables
│   │   ├── transaction.py      # Transaction record model (from TRNREC copybook)
│   │   ├── position.py         # Position record model (from POSREC copybook)
│   │   ├── history.py          # History record model (from HISTREC copybook)
│   │   ├── inquiry.py          # Inquiry communication model (from INQCOM copybook)
│   │   ├── db2_request.py      # DB2 request model (from DB2REQ copybook)
│   │   └── error.py            # Error handling model (from ERRHND copybook)
│   ├── reports/                # Report generators (replaces RPTxxx00 programs)
│   ├── utilities/              # Utility functions (replaces UTLxxx00 programs)
│   └── config/                 # Application configuration
├── tests/                      # Test suite (pytest)
├── migrations/                 # Database migration scripts (PostgreSQL DDL)
│   ├── 001_create_db2_tables.sql   # DB2 table equivalents
│   └── 002_create_vsam_tables.sql  # VSAM file equivalents
├── airflow/                    # Apache Airflow DAGs (replaces JCL batch scheduling)
│   └── dags/
├── frontend/                   # React web UI (replaces BMS screen maps)
├── docs/                       # Documentation
│   └── migration/
│       ├── architecture-analysis.md  # COBOL architecture analysis
│       └── technology-stack.md       # Technology stack selection
├── requirements.txt            # Python dependencies
├── .gitignore                  # Git ignore rules
└── README.md                   # This file
```

## Technology Stack

| Component | Technology | Replaces |
|-----------|-----------|----------|
| Language | Python 3.11+ | Enterprise COBOL |
| Database | PostgreSQL 16+ | DB2 for z/OS |
| ORM | SQLAlchemy 2.x | Embedded SQL / EXEC SQL |
| Data Validation | Pydantic 2.x | COBOL copybook field definitions |
| Web Backend | Flask | CICS Transaction Server |
| Web Frontend | React | BMS Screen Maps (3270 terminals) |
| Job Scheduling | Apache Airflow | z/OS Job Scheduler / JCL |
| Testing | pytest | TSTGEN00 / TSTVAL00 |
| File Storage | PostgreSQL tables | VSAM KSDS files |

## Migration Status

### Phase I: Infrastructure Assessment & Setup
- [x] Architecture analysis (`docs/migration/architecture-analysis.md`)
- [x] Technology stack selection (`docs/migration/technology-stack.md`)
- [x] Python project structure

### Phase II: Data Layer Migration
- [x] DB2 DDL conversion to PostgreSQL (`migrations/001_create_db2_tables.sql`)
- [x] VSAM definitions conversion to PostgreSQL (`migrations/002_create_vsam_tables.sql`)
- [x] SQLAlchemy ORM models (`src/models/orm.py`)
- [x] Copybook translation to Pydantic models (`src/models/`)

### Phase III: Business Logic Migration (Planned)
- [ ] Batch processing programs
- [ ] Online transaction processing
- [ ] Report generators

### Phase IV: Interface Migration (Planned)
- [ ] REST API endpoints
- [ ] React frontend
- [ ] Airflow DAGs

### Phase V: Testing & Validation (Planned)
- [ ] Unit tests
- [ ] Integration tests
- [ ] Performance benchmarks

## Data Models

### COBOL Copybook to Python Model Mapping

| COBOL Copybook | Python Module | Description |
|---------------|--------------|-------------|
| TRNREC.cpy | `src/models/transaction.py` | Transaction records (buy/sell/transfer/fee) |
| POSREC.cpy | `src/models/position.py` | Investment position records |
| HISTREC.cpy | `src/models/history.py` | Change history with before/after images |
| INQCOM.cpy | `src/models/inquiry.py` | Online inquiry request/response contract |
| DB2REQ.cpy | `src/models/db2_request.py` | Database connection management interface |
| ERRHND.cpy | `src/models/error.py` | Centralized error handling |

### COBOL Data Type to Python Type Mapping

| COBOL Type | Python Type | Notes |
|-----------|------------|-------|
| `PIC X(n)` | `str` (max_length=n) | Fixed-length alphanumeric |
| `PIC 9(n)` | `int` | Unsigned integer |
| `PIC S9(n) COMP` | `int` | Signed binary integer |
| `PIC S9(n)V9(m) COMP-3` | `Decimal(n+m, m)` | Packed decimal for exact arithmetic |
| `PIC S9(n)V99` | `Decimal(n+2, 2)` | Display numeric with 2 decimal places |
| 88-level conditions | `Enum` | Boolean condition names |

## Database Tables

### DB2 Migrated Tables
- `portfolio_master` — Portfolio records
- `investment_positions` — Investment position records
- `transaction_history` — Transaction log
- `position_history` — Position history (POSHIST)
- `error_log` — Application error log (ERRLOG)
- `auth_file` — User authorization (AUTHFILE)
- `audit_log` — Security audit trail (AUDITLOG)
- `return_codes` — Return code logging (RTNCODES)

### VSAM Migrated Tables
- `vsam_portfolio_master` — Portfolio master file (PORTMSTR)
- `vsam_transaction_history` — Transaction history file (TRANHIST)
- `vsam_position_history` — Position history file (POSHIST)
- `batch_control` — Batch job control records

## Getting Started

### Prerequisites
- Python 3.11 or higher
- PostgreSQL 16 or higher

### Installation

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt

# Run database migrations
psql -U postgres -d portfolio_mgmt -f migrations/001_create_db2_tables.sql
psql -U postgres -d portfolio_mgmt -f migrations/002_create_vsam_tables.sql
```

### Running Tests

```bash
pytest tests/ -v --cov=src
```

## Documentation

- [Architecture Analysis](docs/migration/architecture-analysis.md) — Comprehensive analysis of the COBOL system architecture
- [Technology Stack](docs/migration/technology-stack.md) — Technology selections with justifications

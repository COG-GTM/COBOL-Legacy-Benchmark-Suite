# Portfolio Management System

A Python implementation of the Investment Portfolio Management System, migrated from the COBOL Legacy Benchmark Suite.

## Overview

This project represents the Python migration of an enterprise-grade Investment Portfolio Management System originally implemented in Enterprise COBOL for z/OS mainframes. The migration preserves all business logic, data structures, and processing patterns while modernizing the technology stack.

## Project Structure

```
portfolio-management-system/
├── src/
│   ├── batch/              # Batch processing modules (from COBOL batch programs)
│   ├── api/                # REST API endpoints (from CICS online programs)
│   ├── services/           # Business logic services
│   ├── database/           # Database connection and session management
│   ├── models/             # Data models (from COBOL copybooks)
│   ├── reports/            # Report generators (from COBOL report programs)
│   ├── utilities/          # Utility functions (from COBOL utility programs)
│   └── config/             # Configuration management
├── tests/                  # pytest test suite
├── migrations/             # PostgreSQL database migrations
├── airflow/                # Apache Airflow DAGs for job scheduling
├── frontend/               # React frontend (Phase III)
├── docs/                   # Documentation
│   └── migration/          # Migration documentation
├── requirements.txt        # Python dependencies
└── README.md               # This file
```

## Technology Stack

| Component | Original (COBOL/z/OS) | Python Migration |
|-----------|----------------------|------------------|
| Database | DB2 | PostgreSQL 15+ |
| File Storage | VSAM | PostgreSQL (consolidated) |
| Job Scheduling | z/OS Scheduler + JCL | Apache Airflow |
| Online Interface | CICS + BMS | Flask REST API |
| Frontend | 3270 Terminal | React + TypeScript |
| Testing | COBOL Test Programs | pytest |
| ORM | N/A | SQLAlchemy 2.x |

## Data Models

The following COBOL copybooks have been translated to Python data models:

| Copybook | Python Module | Description |
|----------|---------------|-------------|
| TRNREC | `src/models/transaction.py` | Transaction records |
| POSREC | `src/models/position.py` | Position records |
| HISTREC | `src/models/history.py` | History records |
| INQCOM | `src/models/inquiry.py` | Inquiry communication area |
| DB2REQ | `src/models/db2_request.py` | DB2 request handling |
| ERRHND | `src/models/error.py` | Error handling |

## Database Schema

The database schema has been migrated from DB2 to PostgreSQL:

| Original Table | PostgreSQL Table | Notes |
|----------------|------------------|-------|
| PORTFOLIO_MASTER | portfolio_master | From VSAM PORTMSTR |
| INVESTMENT_POSITIONS | investment_positions | From VSAM POSHIST |
| TRANSACTION_HISTORY | transaction_history | From VSAM TRANHIST |
| POSHIST | position_history | Partitioned by quarter |
| ERRLOG | error_log | Error logging |
| AUTHFILE | auth_permissions | Authentication |
| AUDITLOG | audit_log | Audit trail |

## Installation

### Prerequisites

- Python 3.11+
- PostgreSQL 15+
- Apache Airflow 2.x (for batch scheduling)

### Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/COG-GTM/COBOL-Legacy-Benchmark-Suite.git
   cd COBOL-Legacy-Benchmark-Suite/portfolio-management-system
   ```

2. Create a virtual environment:
   ```bash
   python -m venv venv
   source venv/bin/activate  # On Windows: venv\Scripts\activate
   ```

3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```

4. Configure the database:
   ```bash
   # Create a .env file with your database configuration
   cp .env.example .env
   # Edit .env with your PostgreSQL connection string
   ```

5. Run database migrations:
   ```bash
   psql -d your_database -f migrations/001_initial_schema.sql
   ```

## Configuration

Create a `.env` file in the project root with the following variables:

```env
# Database
DATABASE_URL=postgresql://user:password@localhost:5432/portfolio

# Application
APP_NAME=Portfolio Management System
DEBUG=false

# Security
SECRET_KEY=your-secret-key-here
JWT_ALGORITHM=HS256
JWT_EXPIRATION_HOURS=24

# Logging
LOG_LEVEL=INFO
```

## Migration Documentation

Detailed migration documentation is available in the `docs/migration/` directory:

- **architecture-analysis.md**: Comprehensive analysis of the original COBOL architecture
- **technology-stack.md**: Technology stack selection and justifications

## COBOL to Python Mapping

### Program Migration

| COBOL Program | Python Module | Purpose |
|---------------|---------------|---------|
| BCHCTL00 | `src/batch/batch_control.py` | Batch control processor |
| PRCSEQ00 | `src/batch/process_sequence.py` | Process sequence manager |
| TRNVAL00 | `src/batch/transaction_validation.py` | Transaction validation |
| POSUPD00 | `src/batch/position_update.py` | Position update |
| HISTLD00 | `src/batch/history_load.py` | History load |
| INQONLN | `src/api/inquiry.py` | Online inquiry controller |
| INQPORT | `src/api/portfolio.py` | Portfolio inquiry |
| INQHIST | `src/api/history.py` | History inquiry |
| SECMGR | `src/services/security.py` | Security manager |
| RPTPOS00 | `src/reports/position_report.py` | Position report |
| RPTAUD00 | `src/reports/audit_report.py` | Audit report |
| RPTSTA00 | `src/reports/statistics_report.py` | Statistics report |

### Data Type Mapping

| COBOL Type | Python Type | PostgreSQL Type |
|------------|-------------|-----------------|
| PIC X(n) | str | VARCHAR(n) or CHAR(n) |
| PIC 9(n) | int | INTEGER or BIGINT |
| PIC S9(n)V9(m) COMP-3 | Decimal | DECIMAL(n+m, m) |
| PIC S9(n) COMP | int | INTEGER or BIGINT |

## Development

### Running Tests

```bash
pytest tests/ -v --cov=src
```

### Code Quality

```bash
# Linting
flake8 src/ tests/

# Type checking
mypy src/

# Formatting
black src/ tests/
isort src/ tests/
```

## Migration Phases

This project is part of a multi-phase migration effort:

- **Phase I** (Complete): Infrastructure Assessment & Setup
  - Architecture analysis
  - Technology stack selection
  - Project structure setup

- **Phase II** (Complete): Data Layer Migration
  - Database schema conversion
  - SQLAlchemy ORM models
  - Copybook translation to Python data models

- **Phase III** (Planned): Business Logic Migration
  - Batch processing implementation
  - Online inquiry implementation
  - Report generation

- **Phase IV** (Planned): Integration & Testing
  - End-to-end testing
  - Performance optimization
  - Production deployment

## License

This project is part of the COBOL Legacy Benchmark Suite, designed for evaluating LLM translation tools in COBOL modernization efforts.

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for guidelines on contributing to this project.

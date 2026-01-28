# Portfolio Management System - Python Migration

This is the Python implementation of the Investment Portfolio Management System, migrated from the COBOL Legacy Benchmark Suite (CLBS).

## Overview

The Portfolio Management System provides comprehensive investment portfolio management capabilities including:

- Portfolio position tracking and valuation
- Transaction processing and validation
- Historical data management and reporting
- Online inquiry system for portfolio positions and transaction history
- Batch processing for bulk operations
- Audit and compliance reporting

## Architecture

The system follows a layered architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend (React)                         │
│                  Portfolio Inquiry UI                       │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    API Layer (Flask)                        │
│              REST endpoints for all operations              │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  Service Layer                              │
│    Business logic, validation, transaction processing       │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  Data Layer (SQLAlchemy)                    │
│           ORM models, database operations                   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                  PostgreSQL Database                        │
│     Migrated from DB2 + VSAM file storage                   │
└─────────────────────────────────────────────────────────────┘
```

## Project Structure

```
portfolio-management-system/
├── src/
│   ├── api/                # REST API endpoints (Flask routes)
│   ├── batch/              # Batch processing modules
│   ├── config/             # Configuration settings
│   ├── database/           # Database connection and utilities
│   ├── models/             # SQLAlchemy ORM models and Pydantic schemas
│   ├── reports/            # Report generation modules
│   ├── services/           # Business logic services
│   └── utilities/          # Utility functions
├── tests/                  # Test suite (pytest)
├── migrations/             # Database migration scripts (Alembic)
├── airflow/                # Apache Airflow DAGs for batch scheduling
│   └── dags/              # DAG definitions
├── frontend/               # React frontend application
├── docs/                   # Documentation
│   └── migration/         # Migration documentation
├── requirements.txt        # Python dependencies
└── .gitignore             # Git ignore rules
```

## Technology Stack

| Component | Technology | Replaces |
|-----------|------------|----------|
| Database | PostgreSQL | DB2 for z/OS |
| File Storage | PostgreSQL tables | VSAM KSDS files |
| ORM | SQLAlchemy | Embedded SQL |
| Backend API | Flask | CICS transactions |
| Frontend | React | BMS screen maps |
| Job Scheduling | Apache Airflow | z/OS Job Scheduler (JCL) |
| Data Validation | Pydantic | COBOL copybook validation |
| Testing | pytest | TSTGEN00/TSTVAL00 |

## Installation

### Prerequisites

- Python 3.11+
- PostgreSQL 15+
- Node.js 18+ (for frontend)

### Backend Setup

```bash
# Create virtual environment
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt

# Configure environment
cp .env.example .env
# Edit .env with your database credentials

# Run database migrations
alembic upgrade head

# Start the Flask development server
flask run
```

### Frontend Setup

```bash
cd frontend
npm install
npm run dev
```

## Configuration

Environment variables (set in `.env` file):

```
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=portfolio_db
DB_USER=portfolio_user
DB_PASSWORD=your_password

# Application
APP_DEBUG=false
APP_SECRET_KEY=your-secret-key
APP_LOG_LEVEL=INFO

# Batch Processing
APP_BATCH_COMMIT_FREQUENCY=1000
APP_BATCH_MAX_ERRORS=100
```

## Migration from COBOL

This system is a Python migration of the COBOL Legacy Benchmark Suite. Key migration mappings:

### Programs Migrated

| COBOL Program | Python Module | Description |
|---------------|---------------|-------------|
| BCHCTL00 | src/batch/control.py | Batch control processing |
| HISTLD00 | src/batch/history_load.py | History loading to database |
| INQONLN | src/api/inquiry.py | Online inquiry controller |
| INQPORT | src/services/position.py | Portfolio position service |
| INQHIST | src/services/history.py | Transaction history service |
| RPTPOS00 | src/reports/position.py | Position report generator |
| RPTAUD00 | src/reports/audit.py | Audit report generator |

### Data Models Migrated

| COBOL Copybook | Python Model | Description |
|----------------|--------------|-------------|
| TRNREC | src/models/transaction.py | Transaction records |
| POSREC | src/models/position.py | Position records |
| HISTREC | src/models/history.py | History records |
| INQCOM | src/models/inquiry.py | Inquiry communication |
| DB2REQ | src/models/db2_request.py | Database request handling |
| ERRHND | src/models/error.py | Error handling |

### Database Tables Migrated

| Original | New Table | Source |
|----------|-----------|--------|
| POSHIST (DB2) | poshist | Position history |
| ERRLOG (DB2) | error_log | Error logging |
| AUTHFILE (DB2) | auth_file | User authorization |
| AUDITLOG (DB2) | audit_log | Audit trail |
| PORTMSTR (VSAM) | portfolio_master | Portfolio master |
| TRANHIST (VSAM) | transaction_file | Transaction history |

## API Endpoints

### Portfolio Operations

- `GET /api/portfolios` - List portfolios
- `GET /api/portfolios/{id}` - Get portfolio details
- `GET /api/portfolios/{id}/positions` - Get portfolio positions
- `GET /api/portfolios/{id}/history` - Get transaction history

### Inquiry Operations

- `POST /api/inquiry/position` - Position inquiry
- `POST /api/inquiry/history` - History inquiry

### Reports

- `GET /api/reports/position` - Generate position report
- `GET /api/reports/audit` - Generate audit report
- `GET /api/reports/statistics` - Generate statistics report

## Testing

```bash
# Run all tests
pytest

# Run with coverage
pytest --cov=src --cov-report=html

# Run specific test file
pytest tests/test_models.py
```

## Batch Processing

Batch jobs are scheduled using Apache Airflow. DAGs are defined in `airflow/dags/`:

- `batch_control_dag.py` - Main batch orchestration
- `history_load_dag.py` - History loading to database
- `position_report_dag.py` - Daily position reports
- `audit_report_dag.py` - Audit report generation

## Documentation

- [Architecture Analysis](docs/migration/architecture-analysis.md) - Original COBOL system analysis
- [Technology Stack](docs/migration/technology-stack.md) - Technology selection rationale

## License

This project is part of the COBOL Legacy Benchmark Suite for evaluating LLM translation tools.

## Contributing

See [CONTRIBUTING.md](../CONTRIBUTING.md) for development guidelines.

# Investment Portfolio Management System - Python Migration

This is a Python migration of the COBOL Legacy Benchmark Suite, an Investment Portfolio Management System that simulates production-grade mainframe applications.

## Overview

This migration converts the original COBOL/CICS/DB2 system to a modern Python stack:

- **Web Framework**: FastAPI for REST API endpoints
- **Database**: SQLAlchemy ORM with SQLite (configurable for PostgreSQL)
- **Authentication**: JWT-based authentication (replaces RACF)
- **Batch Processing**: Python modules with workflow orchestration

## Project Structure

```
python_migration/
├── app/
│   ├── api/           # FastAPI REST endpoints (replaces CICS programs)
│   │   ├── auth.py    # Authentication endpoints (SECMGR)
│   │   ├── portfolio.py # Portfolio inquiry (INQPORT)
│   │   ├── history.py # Transaction history (INQHIST)
│   │   └── inquiry.py # Main inquiry controller (INQONLN)
│   ├── auth/          # Authentication module
│   │   └── security.py # JWT security (replaces SECMGR/RACF)
│   ├── batch/         # Batch processing modules
│   │   ├── transaction_validator.py # TRNVAL00
│   │   ├── position_updater.py      # POSUPD00
│   │   ├── history_loader.py        # HISTLD00
│   │   ├── batch_controller.py      # BCHCTL00
│   │   ├── process_sequencer.py     # PRCSEQ00
│   │   ├── recovery_handler.py      # RCVPRC00
│   │   └── job_runner.py            # JCL replacement
│   ├── database/      # Database layer
│   │   ├── connection.py # DB connection (replaces DB2CONN)
│   │   └── models.py     # SQLAlchemy models (DB2 tables)
│   ├── models/        # Pydantic models (COBOL copybooks)
│   │   ├── position.py   # POSREC.cpy
│   │   ├── transaction.py # TRNREC.cpy
│   │   ├── portfolio.py  # PORTFLIO.cpy
│   │   ├── history.py    # HISTREC.cpy
│   │   ├── audit.py      # AUDITLOG.cpy
│   │   ├── error.py      # ERRHAND.cpy
│   │   ├── batch_control.py # BCHCTL.cpy
│   │   └── inquiry.py    # INQCOM.cpy
│   ├── templates/     # HTML templates (BMS maps)
│   ├── utils/         # Utility modules
│   └── main.py        # FastAPI application entry point
├── pyproject.toml     # Poetry dependencies
└── README.md          # This file
```

## Migration Mapping

### COBOL Programs to Python Modules

| COBOL Program | Python Module | Description |
|---------------|---------------|-------------|
| INQONLN | app/api/inquiry.py | Main inquiry controller |
| INQPORT | app/api/portfolio.py | Portfolio position inquiry |
| INQHIST | app/api/history.py | Transaction history inquiry |
| SECMGR | app/auth/security.py | Security manager |
| TRNVAL00 | app/batch/transaction_validator.py | Transaction validation |
| POSUPD00 | app/batch/position_updater.py | Position update |
| HISTLD00 | app/batch/history_loader.py | History loader |
| BCHCTL00 | app/batch/batch_controller.py | Batch control |
| PRCSEQ00 | app/batch/process_sequencer.py | Process sequencing |
| RCVPRC00 | app/batch/recovery_handler.py | Recovery handler |

### COBOL Copybooks to Pydantic Models

| Copybook | Pydantic Model | Description |
|----------|----------------|-------------|
| POSREC.cpy | app/models/position.py | Position record |
| TRNREC.cpy | app/models/transaction.py | Transaction record |
| PORTFLIO.cpy | app/models/portfolio.py | Portfolio record |
| HISTREC.cpy | app/models/history.py | History record |
| AUDITLOG.cpy | app/models/audit.py | Audit log record |
| ERRHAND.cpy | app/models/error.py | Error handling |
| BCHCTL.cpy | app/models/batch_control.py | Batch control |
| INQCOM.cpy | app/models/inquiry.py | Inquiry communication |

### DB2 Tables to SQLAlchemy Models

| DB2 Table | SQLAlchemy Model | Description |
|-----------|------------------|-------------|
| POSHIST | PositionHistory | Position history |
| ERRLOG | ErrorLog | Error log |
| AUTHFILE | AuthFile | Authorization file |
| AUDITLOG | AuditLog | Audit log |
| PORTFOLIO_MASTER | PortfolioMaster | Portfolio master |
| INVESTMENT_POSITIONS | InvestmentPosition | Investment positions |
| TRANSACTION_HISTORY | TransactionHistory | Transaction history |

## Installation

```bash
# Install dependencies
poetry install

# Run the application
poetry run uvicorn app.main:app --reload
```

## API Documentation

Once running, access the API documentation at:
- Swagger UI: http://localhost:8000/docs
- ReDoc: http://localhost:8000/redoc

## Running Batch Jobs

```bash
# Run daily batch processing
poetry run python -m app.batch.job_runner daily --date 20240115

# Run individual jobs
poetry run python -m app.batch.job_runner trnval --date 20240115
poetry run python -m app.batch.job_runner posupd --date 20240115
poetry run python -m app.batch.job_runner histld --date 20240115
```

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| DATABASE_URL | Database connection string | sqlite:///./portfolio.db |
| SECRET_KEY | JWT secret key | (change in production) |
| LOG_LEVEL | Logging level | INFO |
| LOG_FORMAT | Log format (text/json) | text |
| DEBUG | Debug mode | false |

## License

This project is part of the COBOL Legacy Benchmark Suite for evaluating LLM translation tools.

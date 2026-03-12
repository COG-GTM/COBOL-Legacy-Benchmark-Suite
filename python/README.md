# Investment Portfolio Management System

Python migration of the COBOL Legacy Benchmark Suite - an Investment Portfolio Management System.

## Overview

This system manages investment portfolios, transactions, positions, and reporting. It was migrated from
a mainframe COBOL/CICS/DB2/VSAM environment to a modern Python stack using FastAPI, SQLAlchemy, and Pydantic.

## Architecture

| COBOL Component | Python Replacement |
|---|---|
| VSAM KSDS files | SQLAlchemy + SQLite/PostgreSQL |
| DB2 embedded SQL | SQLAlchemy ORM |
| CICS online programs | FastAPI REST API |
| BMS maps | Pydantic request/response schemas |
| JCL batch jobs | Python batch runner (`src/batch/runner.py`) |
| COBOL copybooks | Pydantic models (`src/models/`) |
| COBOL CALL | Python method calls / dependency injection |

## Quick Start

```bash
cd python
pip install -e ".[dev]"
alembic upgrade head
uvicorn src.api.app:app --reload
```

## Running Batch Processing

```bash
python -m src.batch.runner --full-cycle --date 2024-01-15
python -m src.batch.runner --step validate --date 2024-01-15
```

## Running Tests

```bash
pytest
```

## Project Structure

- `src/models/` - Pydantic data models (from COBOL copybooks)
- `src/db/` - Database layer (SQLAlchemy tables, engine, repository)
- `src/common/` - Shared utilities (error handling, audit, logging, constants)
- `src/portfolio/` - Portfolio CRUD services
- `src/batch/` - Batch processing pipeline
- `src/api/` - FastAPI REST API (replacing CICS online)
- `src/reports/` - Report generation
- `src/utils/` - Maintenance, monitoring, data validation
- `tests/` - Test suite

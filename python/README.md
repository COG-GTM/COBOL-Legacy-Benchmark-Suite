# Investment Portfolio Management System - Python Migration

This project is a Python migration of the **COBOL Legacy Benchmark Suite (CLBS)** Investment Portfolio Management System.

## Overview

The original system is a production-grade COBOL application designed as a comprehensive benchmark for evaluating LLM-based COBOL modernization tools. This Python port faithfully translates the COBOL data structures, business logic, and processing patterns into idiomatic Python while preserving the exact semantics of the original system.

## Architecture

The migration follows a layered architecture:

- **`src/models/`** - Pydantic v2 data models translated from COBOL copybooks
- **`src/db/`** - SQLAlchemy 2.0 database layer (replaces VSAM and DB2)
- **`src/batch/`** - Batch processing pipeline (transaction validation, position updates, history loading)
- **`src/api/`** - FastAPI REST endpoints (replaces CICS online screens)
- **`src/portfolio/`** - Portfolio management business logic
- **`src/reports/`** - Report generation (position, audit, statistics)
- **`src/common/`** - Shared constants and enumerations from COBOL copybooks
- **`src/utils/`** - Utility and maintenance functions

## Key Design Decisions

- **Decimal precision**: All monetary and quantity fields use Python's `decimal.Decimal` to match COBOL packed-decimal (`COMP-3`) precision exactly.
- **Fixed-width string mapping**: COBOL `PIC X(n)` fields are mapped to Pydantic `str` fields with `max_length=n` validators.
- **Date handling**: COBOL `PIC X(08)` date fields (YYYYMMDD) are converted to `datetime.date`; `PIC X(26)` timestamps to `datetime.datetime`.
- **Enum fidelity**: All COBOL level-88 condition values are translated to Python `Enum` types preserving the original codes.

## Getting Started

```bash
# Install dependencies
pip install -e ".[dev]"

# Run tests
pytest
```

## Migration Phases

1. **Phase 1** - Project setup, data models, and constants (this phase)
2. **Phase 2** - Database layer and Alembic migrations
3. **Phase 3** - Batch processing pipeline
4. **Phase 4** - API layer (FastAPI)
5. **Phase 5** - Reports and utilities

## Source Copybook Mapping

| Python Model | COBOL Copybook(s) |
|---|---|
| `portfolio.py` | `PORTFLIO.cpy`, `PORTVAL.cpy` |
| `transaction.py` | `TRNREC.cpy` |
| `position.py` | `POSREC.cpy` |
| `market_data.py` | `HISTREC.cpy` |
| `audit.py` | `AUDITLOG.cpy` |
| `error.py` | `ERRHAND.cpy`, `RETHND.cpy` |
| `batch_control.py` | `BCHCTL.cpy`, `BCHCON.cpy`, `PRCSEQ.cpy`, `CKPRST.cpy` |
| `security.py` | `INQCOM.cpy`, `DB2REQ.cpy`, `ERRHND.cpy` |
| `constants.py` | `RTNCODE.cpy`, `COMMON.cpy` |

# Investment Portfolio Management System (Python)

Migrated from the COBOL Legacy Benchmark Suite to Python. This project preserves all business logic,
data structures, and processing flows from the original COBOL/CICS/DB2/VSAM system.

## Architecture

| COBOL Layer | Python Replacement |
|---|---|
| CICS Online Programs | FastAPI REST API |
| Batch JCL Jobs | CLI batch runner (`src/batch/runner.py`) |
| VSAM KSDS Files | SQLAlchemy ORM + SQLite/PostgreSQL |
| DB2 Embedded SQL | SQLAlchemy queries |
| BMS Maps | Pydantic request/response schemas |
| Copybooks | Pydantic models + SQLAlchemy tables |
| COBOL DISPLAY | Python logging |

## Quick Start

```bash
cd python
pip install -e ".[dev]"

# Run tests
pytest

# Start the API server
uvicorn src.api.app:app --reload

# Run batch processing
python -m src.batch.runner --full-cycle --process-date 20240101
```

## Project Structure

- `src/models/` - Pydantic data models (from COBOL copybooks)
- `src/db/` - Database layer (SQLAlchemy ORM, replaces VSAM/DB2)
- `src/portfolio/` - Portfolio CRUD services (from PORTMSTR, PORTADD, etc.)
- `src/batch/` - Batch processing pipeline (from BCHCTL00, PRCSEQ00, etc.)
- `src/api/` - REST API (replaces CICS online programs)
- `src/reports/` - Report generators (from RPTPOS00, RPTAUD00, RPTSTA00)
- `src/common/` - Shared utilities (error handling, audit, logging)
- `src/utils/` - Maintenance, monitoring, data validation utilities
- `tests/` - Comprehensive test suite

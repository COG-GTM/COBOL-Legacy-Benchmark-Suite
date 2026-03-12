# Investment Portfolio Management System - Python Migration

This project is a Python migration of the COBOL Legacy Benchmark Suite (CLBS)
Investment Portfolio Management System. It translates the mainframe COBOL
application into a modern Python stack using Pydantic, SQLAlchemy, FastAPI,
and Alembic.

## Phase 1 - Foundation (this phase)

- Python project scaffolding and tooling
- Pydantic data models translated from all 16 COBOL copybooks
- SQLAlchemy ORM models derived from the DB2 DDL
- Alembic migration framework with initial schema migration
- Database connection management (PostgreSQL / SQLite)
- Unit tests for model validation

## Project Structure

```
python/
  models/          # Pydantic data models (from COBOL copybooks)
    enums.py       # Enums and constants (COMMON.cpy)
    portfolio.py   # Portfolio master record (PORTFLIO.cpy)
    transaction.py # Transaction record (TRNREC.cpy)
    position.py    # Position record (POSREC.cpy)
    audit.py       # Audit trail record (AUDITLOG.cpy)
    errors.py      # Error handling (ERRHAND.cpy, RTNCODE.cpy)
    batch.py       # Batch control (BCHCTL.cpy, BCHCON.cpy, CKPRST.cpy, PRCSEQ.cpy)
    db_models.py   # DB2 host variable models (DBTBLS.cpy, DBPROC.cpy)
  db/              # Database layer
    schema.py      # SQLAlchemy ORM models (from db2-definitions.sql)
    connection.py  # Engine, session factory, config
    migrations/    # Alembic migrations
  tests/           # Unit and integration tests
    test_models.py # Model validation tests
```

## Getting Started

```bash
# Install dependencies (from the python/ directory)
pip install -e ".[dev]"

# Run tests
pytest

# Run linter
ruff check .

# Generate a new Alembic migration
alembic revision --autogenerate -m "description"

# Apply migrations
alembic upgrade head
```

## Key Design Decisions

- **Decimal everywhere**: All financial amounts use `decimal.Decimal`, never `float`.
- **Pydantic v2**: Strict validation mirrors COBOL field-level checks.
- **Single database**: The VSAM + DB2 dual-storage pattern is collapsed into one
  relational database (PostgreSQL for production, SQLite for dev/test).
- **Business rules preserved**: Every validation rule from the COBOL source is
  faithfully reproduced in Python validators.

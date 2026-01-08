# Investment Portfolio Management System - Python Migration

This is a Python implementation of the Investment Portfolio Management System, migrated from the original COBOL/CICS mainframe application.

## Overview

The system provides comprehensive portfolio management capabilities including:

- **Batch Processing**: Transaction validation, position updates, and history loading
- **Online Inquiry**: REST API for portfolio and transaction inquiries
- **Reporting**: Position reports, audit trails, and system statistics
- **Security**: User authentication, authorization, and audit logging

## Architecture

The Python implementation follows a layered architecture that mirrors the original COBOL system:

### Batch Processing Layer
- `TransactionValidator` - Validates incoming financial transactions (migrated from TRNVAL00)
- `PositionManager` - Updates portfolio positions (migrated from POSUPD00)
- `HistoryLoader` - Loads audit history records (migrated from HISTLD00)
- `BatchController` - Manages batch job execution with checkpoint/restart

### Online Layer (Flask Web Application)
- Portfolio inquiry endpoints (migrated from INQPORT)
- Transaction history endpoints (migrated from INQHIST)
- Report generation endpoints
- Authentication and authorization (migrated from SECMGR)

### Data Models
- `Position` - Portfolio position records (maps to POSREC.cpy)
- `Transaction` - Financial transaction records (maps to TRNREC.cpy)
- `History` - Audit history records (maps to HISTREC.cpy)
- `BatchControl` - Batch job control records (maps to BCHCTL.cpy)

## Installation

```bash
# Install dependencies
pip install -e .

# For development
pip install -e ".[dev]"

# For PostgreSQL support
pip install -e ".[postgres]"
```

## Usage

### Running the Web Server

```bash
# Using the CLI
portfolio-server

# Or directly
python -m src.web.app
```

The server will start on `http://localhost:5000`.

### API Endpoints

#### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/logout` - User logout
- `GET /api/auth/session` - Get session info

#### Portfolio Inquiry
- `GET /api/portfolio/list` - List all portfolios
- `GET /api/portfolio/<id>/positions` - Get portfolio positions
- `GET /api/portfolio/<id>/positions/<investment_id>` - Get position detail
- `GET /api/portfolio/<id>/summary` - Get portfolio summary

#### History Inquiry
- `GET /api/history/<id>/transactions` - Get transaction history
- `GET /api/history/<id>/audit` - Get audit history

#### Reports
- `GET /api/reports/position` - Generate position report
- `GET /api/reports/audit` - Generate audit report
- `GET /api/reports/statistics` - Generate statistics report

### Batch Processing

```python
from src.batch import TransactionValidator, PositionManager, HistoryLoader

# Validate transactions
validator = TransactionValidator()
result = validator.validate_transactions(transactions)

# Update positions
manager = PositionManager()
result = manager.process_transactions(validated_transactions)

# Load history
loader = HistoryLoader()
loader.load_position_history(portfolio_id, before, after, action)
```

## Configuration

Environment variables:
- `DATABASE_URL` - Database connection string (default: sqlite:///portfolio.db)
- `SECRET_KEY` - Flask secret key
- `SESSION_TIMEOUT` - Session timeout in seconds (default: 1800)
- `MAX_LOGIN_ATTEMPTS` - Max failed login attempts (default: 3)

## Migration Notes

This implementation preserves the business logic and data structures from the original COBOL system while modernizing the technology stack:

| COBOL Component | Python Equivalent |
|-----------------|-------------------|
| VSAM files | SQLite/PostgreSQL with SQLAlchemy |
| DB2 tables | SQLAlchemy ORM models |
| CICS transactions | Flask REST API endpoints |
| BMS screens | JSON API responses |
| Copybooks | Python dataclasses |
| RACF security | Custom authentication module |

## Documentation

See the `docs/` directory for detailed documentation:
- `architecture_analysis.md` - System architecture mapping
- `data_architecture.md` - Data structure translation plan

## License

MIT License

# Investment Portfolio Management System - Business Rule Test Suite

## Purpose

These tests **document and validate the business rules** extracted from the COBOL source code in the Investment Portfolio Management System. They serve as **acceptance criteria** for any modernization or translation effort (e.g., COBOL to Java, Python, or any other modern language).

Because the original COBOL programs require a z/OS mainframe environment (CICS, DB2, VSAM) to execute, this test suite re-implements the core business logic as pure Python functions and validates them using **pytest**. This makes the rules:

1. **Actually runnable** — no z/OS or mainframe needed
2. **A precise specification** — each test maps to a specific COBOL paragraph or rule
3. **Directly usable as acceptance tests** — any modern re-implementation must pass these tests

## Structure

```
tests/
├── README.md                          # This file
├── conftest.py                        # Shared pytest fixtures
├── business_rules/
│   ├── __init__.py
│   └── validators.py                  # Business rule functions (Python re-implementation)
├── test_portfolio_validation.py       # Portfolio validation (PORTVALD.cbl, PORTMSTR.cbl)
├── test_transaction_rules.py          # Transaction processing (PORTTRAN.cbl)
├── test_batch_processing.py           # Batch processing (PRCSEQ00.cbl, HISTLD00.cbl)
├── test_security.py                   # Security rules (SECMGR.cbl)
├── test_reporting.py                  # Reporting rules (RPTPOS00.cbl)
└── test_audit.py                      # Audit trail rules (PORTTRAN.cbl, AUDPROC.cbl)
```

## COBOL Source Traceability

| Test File | COBOL Program(s) | Key Business Rules |
|---|---|---|
| `test_portfolio_validation.py` | `PORTVALD.cbl`, `PORTMSTR.cbl` | Portfolio ID format, account number, investment type, name, status, client type, amount range |
| `test_transaction_rules.py` | `PORTTRAN.cbl` | Transaction type, buy/sell/fee/transfer processing, error threshold, status lifecycle |
| `test_batch_processing.py` | `PRCSEQ00.cbl`, `HISTLD00.cbl`, `BCHCTL00.cbl` | Dependency checking, job sequence, commit threshold, duplicate handling |
| `test_security.py` | `SECMGR.cbl` | User validation, authorization, three-phase security pipeline |
| `test_reporting.py` | `RPTPOS00.cbl` | Position change percentage calculation |
| `test_audit.py` | `PORTTRAN.cbl`, `AUDPROC.cbl` | Audit action mapping, status mapping, record content, write failure handling |

## Running the Tests

```bash
# Install pytest (if not already installed)
pip install pytest

# Run all tests
pytest tests/ -v

# Run a specific test file
pytest tests/test_portfolio_validation.py -v

# Run a specific test class
pytest tests/test_portfolio_validation.py::TestPortfolioIdValidation -v
```

## Using as Acceptance Tests

When translating the COBOL programs to a modern language:

1. Replace the functions in `tests/business_rules/validators.py` with calls to your new implementation
2. Run `pytest` — all tests should still pass
3. Any failing test indicates a deviation from the original COBOL business rules

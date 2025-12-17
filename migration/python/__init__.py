"""
COBOL to Python Migration Package

This package contains Python implementations of the COBOL programs from the
Investment Portfolio Management System (COBOL Legacy Benchmark Suite).

The migration follows a layered architecture that mirrors the original COBOL system:

Modules:
- models: Data models (dataclasses) replacing COBOL copybooks
- database: SQLAlchemy ORM models replacing VSAM files and DB2 tables
- batch: Batch processing programs (TRNVAL00, POSUPD00, HISTLD00)
- reporting: Reporting programs (RPTPOS00, RPTAUD00, RPTSTA00)

Usage:
    from migration.python.batch import TransactionValidator, PositionManager, HistoryLoader
    from migration.python.reporting import PositionReportGenerator, AuditReportGenerator
    from migration.python.database import DatabaseManager, init_database
    
    # Initialize database
    db = init_database('sqlite:///portfolio.db')
    
    # Validate transactions
    validator = TransactionValidator(db)
    results = validator.validate_transactions('transactions.json')
    
    # Update positions
    manager = PositionManager(db)
    manager.process_transactions(valid_transactions)
    
    # Generate reports
    reporter = PositionReportGenerator(db)
    report = reporter.generate_report()

For detailed documentation, see:
- migration/docs/system-component-analysis.md
- migration/docs/data-architecture-translation-plan.md
"""

__version__ = '1.0.0'
__author__ = 'COBOL Migration Team'

# Convenience imports
from migration.python.database.session import DatabaseManager, init_database
from migration.python.batch.transaction_validator import TransactionValidator
from migration.python.batch.position_manager import PositionManager
from migration.python.batch.history_loader import HistoryLoader
from migration.python.reporting.position_report import PositionReportGenerator
from migration.python.reporting.audit_report import AuditReportGenerator
from migration.python.reporting.statistics_report import StatisticsReportGenerator

__all__ = [
    # Database
    'DatabaseManager',
    'init_database',
    # Batch processing
    'TransactionValidator',
    'PositionManager',
    'HistoryLoader',
    # Reporting
    'PositionReportGenerator',
    'AuditReportGenerator',
    'StatisticsReportGenerator',
]

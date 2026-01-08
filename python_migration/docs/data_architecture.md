# Data Architecture Translation Plan

## COBOL to Python Data Migration

Version: 1.0
Date: 2026-01-08

## 1. Overview

This document details the translation plan for migrating data structures from the COBOL Investment Portfolio Management System to Python. It covers VSAM file replacement, DB2 table migration, and COBOL copybook conversion to Python dataclasses.

## 2. VSAM File Migration

### 2.1 Position Master File (POSMSTRE)

**Original VSAM Definition:**
- Type: KSDS (Key-Sequenced Data Set)
- Key: ACCOUNT-NO + FUND-ID (15 bytes)
- Record Length: 250 bytes

**Python SQLAlchemy Model:**

```python
class Position(Base):
    __tablename__ = 'positions'
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    portfolio_id = Column(String(8), nullable=False, index=True)
    date = Column(String(8), nullable=False)
    investment_id = Column(String(10), nullable=False)
    quantity = Column(Numeric(15, 4), nullable=False, default=0)
    cost_basis = Column(Numeric(15, 2), nullable=False, default=0)
    market_value = Column(Numeric(15, 2), nullable=False, default=0)
    currency = Column(String(3), nullable=False, default='USD')
    status = Column(String(1), nullable=False, default='A')
    last_maint_date = Column(DateTime, nullable=True)
    last_maint_user = Column(String(8), nullable=True)
    
    __table_args__ = (
        UniqueConstraint('portfolio_id', 'date', 'investment_id', name='uix_position_key'),
        Index('ix_position_portfolio', 'portfolio_id'),
    )
```

### 2.2 Transaction History File (TRANHIST)

**Original VSAM Definition:**
- Type: ESDS (Entry-Sequenced Data Set)
- Record Length: 300 bytes

**Python SQLAlchemy Model:**

```python
class TransactionHistory(Base):
    __tablename__ = 'transaction_history'
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    date = Column(String(8), nullable=False)
    time = Column(String(6), nullable=False)
    portfolio_id = Column(String(8), nullable=False, index=True)
    sequence_no = Column(String(6), nullable=False)
    investment_id = Column(String(10), nullable=False)
    transaction_type = Column(String(2), nullable=False)
    quantity = Column(Numeric(15, 4), nullable=False)
    price = Column(Numeric(15, 4), nullable=False)
    amount = Column(Numeric(15, 2), nullable=False)
    currency = Column(String(3), nullable=False, default='USD')
    status = Column(String(1), nullable=False, default='P')
    process_date = Column(DateTime, nullable=True)
    process_user = Column(String(8), nullable=True)
    
    __table_args__ = (
        Index('ix_transaction_date', 'date'),
        Index('ix_transaction_portfolio', 'portfolio_id'),
    )
```

### 2.3 Batch Control File (BCHCTL)

**Original VSAM Definition:**
- Type: KSDS
- Key: PROCESS-DATE + PROCESS-ID (16 bytes)
- Record Length: 200 bytes

**Python SQLAlchemy Model:**

```python
class BatchControl(Base):
    __tablename__ = 'batch_control'
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    job_name = Column(String(8), nullable=False)
    process_date = Column(String(8), nullable=False)
    sequence_no = Column(Integer, nullable=False)
    status = Column(String(1), nullable=False, default='R')
    step_name = Column(String(8), nullable=True)
    program_name = Column(String(8), nullable=True)
    start_time = Column(String(8), nullable=True)
    end_time = Column(String(8), nullable=True)
    prereq_count = Column(Integer, nullable=False, default=0)
    return_code = Column(Integer, nullable=True)
    error_desc = Column(String(80), nullable=True)
    restart_count = Column(Integer, nullable=False, default=0)
    attempt_timestamp = Column(DateTime, nullable=True)
    complete_timestamp = Column(DateTime, nullable=True)
    
    __table_args__ = (
        UniqueConstraint('job_name', 'process_date', 'sequence_no', name='uix_batch_key'),
    )
```

## 3. DB2 Table Migration

### 3.1 Position History Table (POSHIST)

**Original DB2 DDL:**
```sql
CREATE TABLE POSHIST (
    ACCOUNT_NO      DECIMAL(9,0)    NOT NULL,
    FUND_ID         CHAR(6)         NOT NULL,
    TRANS_DATE      DATE            NOT NULL,
    SHARE_BAL       DECIMAL(14,3)   NOT NULL,
    COST_BASIS      DECIMAL(13,2)   NOT NULL,
    AVG_COST        DECIMAL(9,4)    NOT NULL,
    PROC_TIMESTAMP  TIMESTAMP       NOT NULL,
    PRIMARY KEY (ACCOUNT_NO, FUND_ID, TRANS_DATE)
);
```

**Python SQLAlchemy Model:**

```python
class PositionHistory(Base):
    __tablename__ = 'position_history'
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    account_no = Column(Numeric(9, 0), nullable=False)
    fund_id = Column(String(6), nullable=False)
    trans_date = Column(Date, nullable=False)
    share_bal = Column(Numeric(14, 3), nullable=False)
    cost_basis = Column(Numeric(13, 2), nullable=False)
    avg_cost = Column(Numeric(9, 4), nullable=False)
    proc_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    
    __table_args__ = (
        UniqueConstraint('account_no', 'fund_id', 'trans_date', name='uix_poshist_key'),
        Index('ix_poshist_account', 'account_no'),
    )
```

### 3.2 Error Log Table (ERRLOG)

**Original DB2 DDL:**
```sql
CREATE TABLE ERRLOG (
    ERROR_TIMESTAMP TIMESTAMP       NOT NULL,
    PROGRAM_ID     CHAR(8)         NOT NULL,
    ERROR_CODE     CHAR(4)         NOT NULL,
    ACCOUNT_NO     DECIMAL(9,0),
    FUND_ID        CHAR(6),
    TRANS_ID       CHAR(12),
    ERROR_DESC     VARCHAR(100)    NOT NULL,
    PRIMARY KEY (ERROR_TIMESTAMP, PROGRAM_ID)
);
```

**Python SQLAlchemy Model:**

```python
class ErrorLog(Base):
    __tablename__ = 'error_log'
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    error_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    program_id = Column(String(8), nullable=False)
    error_code = Column(String(4), nullable=False)
    account_no = Column(Numeric(9, 0), nullable=True)
    fund_id = Column(String(6), nullable=True)
    trans_id = Column(String(12), nullable=True)
    error_desc = Column(String(100), nullable=False)
    
    __table_args__ = (
        Index('ix_error_timestamp', 'error_timestamp'),
        Index('ix_error_program', 'program_id'),
    )
```

### 3.3 Audit Log Table (AUDITLOG)

**Python SQLAlchemy Model:**

```python
class AuditLog(Base):
    __tablename__ = 'audit_log'
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    user_id = Column(String(8), nullable=False)
    terminal_id = Column(String(4), nullable=True)
    trans_id = Column(String(4), nullable=True)
    program = Column(String(8), nullable=False)
    access_type = Column(String(8), nullable=False)
    resource_name = Column(String(50), nullable=True)
    action_result = Column(String(20), nullable=True)
    
    __table_args__ = (
        Index('ix_audit_timestamp', 'timestamp'),
        Index('ix_audit_user', 'user_id'),
    )
```

### 3.4 Authorization Table (AUTHFILE)

**Python SQLAlchemy Model:**

```python
class Authorization(Base):
    __tablename__ = 'authorization'
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(String(8), nullable=False)
    resource = Column(String(8), nullable=False)
    access_type = Column(String(8), nullable=False)
    granted_date = Column(DateTime, nullable=False, default=datetime.utcnow)
    granted_by = Column(String(8), nullable=True)
    expiry_date = Column(DateTime, nullable=True)
    
    __table_args__ = (
        UniqueConstraint('user_id', 'resource', 'access_type', name='uix_auth_key'),
        Index('ix_auth_user', 'user_id'),
    )
```

## 4. COBOL Copybook to Python Dataclass Mapping

### 4.1 POSREC.cpy -> Position Dataclass

```python
from dataclasses import dataclass
from decimal import Decimal
from datetime import datetime
from typing import Optional
from enum import Enum

class PositionStatus(str, Enum):
    ACTIVE = 'A'
    CLOSED = 'C'
    PENDING = 'P'

@dataclass
class PositionRecord:
    """Maps to COBOL POSREC.cpy"""
    portfolio_id: str           # POS-PORTFOLIO-ID PIC X(08)
    date: str                   # POS-DATE PIC X(08)
    investment_id: str          # POS-INVESTMENT-ID PIC X(10)
    quantity: Decimal           # POS-QUANTITY PIC S9(11)V9(4)
    cost_basis: Decimal         # POS-COST-BASIS PIC S9(13)V9(2)
    market_value: Decimal       # POS-MARKET-VALUE PIC S9(13)V9(2)
    currency: str = 'USD'       # POS-CURRENCY PIC X(03)
    status: PositionStatus = PositionStatus.ACTIVE
    last_maint_date: Optional[datetime] = None
    last_maint_user: Optional[str] = None
    
    def validate(self) -> bool:
        """Validate position record fields"""
        if len(self.portfolio_id) > 8:
            return False
        if len(self.date) != 8 or not self.date.isdigit():
            return False
        if len(self.investment_id) > 10:
            return False
        return True
```

### 4.2 TRNREC.cpy -> Transaction Dataclass

```python
class TransactionType(str, Enum):
    BUY = 'BU'
    SELL = 'SL'
    TRANSFER = 'TR'
    FEE = 'FE'

class TransactionStatus(str, Enum):
    PENDING = 'P'
    DONE = 'D'
    FAILED = 'F'
    REVERSED = 'R'

@dataclass
class TransactionRecord:
    """Maps to COBOL TRNREC.cpy"""
    date: str                   # TRN-DATE PIC X(08)
    time: str                   # TRN-TIME PIC X(06)
    portfolio_id: str           # TRN-PORTFOLIO-ID PIC X(08)
    sequence_no: str            # TRN-SEQUENCE-NO PIC X(06)
    investment_id: str          # TRN-INVESTMENT-ID PIC X(10)
    transaction_type: TransactionType
    quantity: Decimal           # TRN-QUANTITY PIC S9(11)V9(4)
    price: Decimal              # TRN-PRICE PIC S9(11)V9(4)
    amount: Decimal             # TRN-AMOUNT PIC S9(13)V9(2)
    currency: str = 'USD'       # TRN-CURRENCY PIC X(03)
    status: TransactionStatus = TransactionStatus.PENDING
    process_date: Optional[datetime] = None
    process_user: Optional[str] = None
    
    def validate(self) -> bool:
        """Validate transaction record fields"""
        if len(self.date) != 8 or not self.date.isdigit():
            return False
        if len(self.time) != 6 or not self.time.isdigit():
            return False
        if self.quantity == 0 and self.transaction_type in [TransactionType.BUY, TransactionType.SELL]:
            return False
        if self.price <= 0 and self.transaction_type in [TransactionType.BUY, TransactionType.SELL]:
            return False
        return True
```

### 4.3 HISTREC.cpy -> History Dataclass

```python
class HistoryRecordType(str, Enum):
    PORTFOLIO = 'PT'
    POSITION = 'PS'
    TRANSACTION = 'TR'

class HistoryActionCode(str, Enum):
    ADD = 'A'
    CHANGE = 'C'
    DELETE = 'D'

@dataclass
class HistoryRecord:
    """Maps to COBOL HISTREC.cpy"""
    portfolio_id: str           # HIST-PORTFOLIO-ID PIC X(08)
    date: str                   # HIST-DATE PIC X(08)
    time: str                   # HIST-TIME PIC X(06)
    seq_no: str                 # HIST-SEQ-NO PIC X(04)
    record_type: HistoryRecordType
    action_code: HistoryActionCode
    before_image: str           # HIST-BEFORE-IMAGE PIC X(400)
    after_image: str            # HIST-AFTER-IMAGE PIC X(400)
    reason_code: str            # HIST-REASON-CODE PIC X(04)
    process_date: Optional[datetime] = None
    process_user: Optional[str] = None
```

### 4.4 BCHCTL.cpy -> BatchControl Dataclass

```python
class BatchStatus(str, Enum):
    READY = 'R'
    ACTIVE = 'A'
    WAITING = 'W'
    DONE = 'D'
    ERROR = 'E'

@dataclass
class PrerequisiteJob:
    name: str
    sequence: int
    return_code: int

@dataclass
class BatchControlRecord:
    """Maps to COBOL BCHCTL.cpy"""
    job_name: str               # BCT-JOB-NAME PIC X(8)
    process_date: str           # BCT-PROCESS-DATE PIC X(8)
    sequence_no: int            # BCT-SEQUENCE-NO PIC 9(4)
    status: BatchStatus = BatchStatus.READY
    step_name: Optional[str] = None
    program_name: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    prereq_count: int = 0
    prereq_jobs: list = None
    return_code: Optional[int] = None
    error_desc: Optional[str] = None
    restart_count: int = 0
    attempt_timestamp: Optional[datetime] = None
    complete_timestamp: Optional[datetime] = None
    
    def __post_init__(self):
        if self.prereq_jobs is None:
            self.prereq_jobs = []
```

## 5. Data Type Mapping Reference

### 5.1 COBOL to Python Type Mapping

| COBOL Type | Python Type | Notes |
|------------|-------------|-------|
| PIC X(n) | str | Fixed-length string |
| PIC 9(n) | int | Unsigned integer |
| PIC S9(n) | int | Signed integer |
| PIC 9(n)V9(m) | Decimal | Decimal with implied decimal |
| PIC S9(n)V9(m) | Decimal | Signed decimal |
| COMP / COMP-4 | int | Binary integer |
| COMP-3 | Decimal | Packed decimal |
| 88 level | Enum | Condition names |

### 5.2 Decimal Precision Mapping

| COBOL Definition | Python Decimal | Precision |
|------------------|----------------|-----------|
| PIC S9(11)V9(4) | Decimal(15, 4) | 15 total, 4 decimal |
| PIC S9(13)V9(2) | Decimal(15, 2) | 15 total, 2 decimal |
| PIC 9(5)V9(4) | Decimal(9, 4) | 9 total, 4 decimal |
| PIC 9(9) | int | Integer |

## 6. Migration Scripts

### 6.1 Database Schema Creation

```python
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from models import Base

def create_database(db_url: str = 'sqlite:///portfolio.db'):
    """Create all database tables"""
    engine = create_engine(db_url)
    Base.metadata.create_all(engine)
    return engine

def get_session(engine):
    """Get database session"""
    Session = sessionmaker(bind=engine)
    return Session()
```

### 6.2 Data Migration Utilities

```python
def migrate_vsam_to_db(vsam_file: str, model_class, session):
    """Generic VSAM to database migration"""
    records = parse_vsam_file(vsam_file)
    for record in records:
        db_record = model_class(**record)
        session.add(db_record)
    session.commit()

def parse_cobol_decimal(value: str, total_digits: int, decimal_places: int) -> Decimal:
    """Parse COBOL packed decimal to Python Decimal"""
    if not value:
        return Decimal(0)
    sign = -1 if value[0] == '-' else 1
    numeric_value = value.lstrip('-+')
    integer_part = numeric_value[:-decimal_places] if decimal_places > 0 else numeric_value
    decimal_part = numeric_value[-decimal_places:] if decimal_places > 0 else ''
    return Decimal(f"{sign * int(integer_part)}.{decimal_part}")
```

## 7. Validation Rules Migration

### 7.1 Transaction Validation Rules

| Rule ID | COBOL Rule | Python Implementation |
|---------|------------|----------------------|
| E001 | Account must be numeric and exist | `account_no.isdigit() and account_exists(account_no)` |
| E002 | Fund ID must exist | `fund_exists(fund_id)` |
| E003 | Transaction date not future | `trans_date <= datetime.now().strftime('%Y%m%d')` |
| E004 | Share quantity non-zero for BUY/SELL | `quantity != 0 if type in ['BU', 'SL']` |
| E005 | Price > 0 for BUY/SELL | `price > 0 if type in ['BU', 'SL']` |

### 7.2 Position Validation Rules

| Rule ID | COBOL Rule | Python Implementation |
|---------|------------|----------------------|
| P001 | Share balance non-negative | `share_balance >= 0` |
| P002 | Cost basis updated on BUY/SELL | `update_cost_basis()` |
| P003 | Average cost recalculated on BUY | `recalculate_avg_cost()` |
| P004 | Position must be ACTIVE | `status == 'A'` |

## 8. Conclusion

This data architecture translation plan provides a comprehensive mapping from COBOL data structures to Python equivalents. The migration preserves all data integrity constraints, validation rules, and business logic while leveraging modern Python features like dataclasses, enums, and SQLAlchemy ORM.

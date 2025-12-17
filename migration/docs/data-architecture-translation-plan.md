# Data Architecture Translation Plan: COBOL to Python Migration

Version: 1.0  
Date: 2024-12-17  
Document Type: Migration Planning

## Executive Summary

This document outlines the detailed plan for translating the COBOL Legacy Benchmark Suite data layer from mainframe-based storage (VSAM files and DB2 tables) to modern Python-based data storage solutions. The plan covers VSAM file replacement strategies, DB2 table conversion, and copybook data structure translation to Python classes.

## 1. Current Data Architecture Overview

### 1.1 Storage Technologies

The current system uses two primary data storage technologies:

**VSAM (Virtual Storage Access Method)**:
- KSDS (Key-Sequenced Data Sets) for indexed access
- ESDS (Entry-Sequenced Data Sets) for sequential access
- Used for operational data requiring high-performance access

**DB2 for z/OS**:
- Relational database for historical data
- Used for reporting and complex queries
- Supports SQL-based access patterns

### 1.2 Data Flow

```
+------------------+     +------------------+     +------------------+
|  Transaction     |     |  Position        |     |  History         |
|  Input File      | --> |  Master VSAM     | --> |  DB2 Tables      |
|  (Sequential)    |     |  (KSDS)          |     |                  |
+------------------+     +------------------+     +------------------+
                               |
                               v
                         +------------------+
                         |  Transaction     |
                         |  History VSAM    |
                         |  (KSDS)          |
                         +------------------+
```

## 2. VSAM File Translation Plan

### 2.1 Portfolio Master File (PORTMSTR)

**Current VSAM Definition**:
- Organization: KSDS
- Record Length: 400 bytes
- Key Length: 12 bytes (Portfolio ID + Account Type + Branch ID)
- Key Position: 1

**Python Translation Strategy**:

**Option A: SQLite/PostgreSQL Table (Recommended)**
```python
from sqlalchemy import Column, String, Numeric, DateTime, Enum
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

class PortfolioMaster(Base):
    __tablename__ = 'portfolio_master'
    
    # Primary Key (composite)
    portfolio_id = Column(String(8), primary_key=True)
    account_type = Column(String(2), primary_key=True)
    branch_id = Column(String(2), primary_key=True)
    
    # Portfolio Data
    account_name = Column(String(50))
    account_status = Column(Enum('A', 'C', 'P', name='account_status'))
    open_date = Column(DateTime)
    close_date = Column(DateTime, nullable=True)
    
    # Financial Data
    total_value = Column(Numeric(15, 2))
    cash_balance = Column(Numeric(15, 2))
    
    # Audit Fields
    last_maint_date = Column(DateTime)
    last_maint_user = Column(String(8))
```

**Option B: File-Based Storage (JSON/Parquet)**
```python
import json
from dataclasses import dataclass, asdict
from typing import Optional
from datetime import datetime
from decimal import Decimal

@dataclass
class PortfolioMaster:
    portfolio_id: str
    account_type: str
    branch_id: str
    account_name: str
    account_status: str  # A=Active, C=Closed, P=Pending
    open_date: datetime
    close_date: Optional[datetime]
    total_value: Decimal
    cash_balance: Decimal
    last_maint_date: datetime
    last_maint_user: str
    
    @property
    def key(self) -> str:
        return f"{self.portfolio_id}{self.account_type}{self.branch_id}"
```

**Migration Rationale**: SQLite/PostgreSQL is recommended because:
1. Supports indexed access similar to VSAM KSDS
2. Provides ACID transactions for data integrity
3. Enables SQL queries for reporting
4. Scales to PostgreSQL for production workloads

### 2.2 Transaction History File (TRANHIST)

**Current VSAM Definition**:
- Organization: KSDS
- Record Length: 300 bytes
- Key Length: 28 bytes (Date + Time + Portfolio ID + Sequence)
- Key Position: 1

**Python Translation**:

```python
from sqlalchemy import Column, String, Numeric, DateTime, Integer, Index
from sqlalchemy.ext.declarative import declarative_base
from decimal import Decimal
from datetime import datetime
from enum import Enum

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

class TransactionHistory(Base):
    __tablename__ = 'transaction_history'
    
    # Primary Key (composite - matches VSAM key structure)
    trans_date = Column(String(8), primary_key=True)  # YYYYMMDD
    trans_time = Column(String(6), primary_key=True)  # HHMMSS
    portfolio_id = Column(String(8), primary_key=True)
    sequence_no = Column(String(6), primary_key=True)
    
    # Transaction Data
    investment_id = Column(String(10), nullable=False)
    trans_type = Column(String(2), nullable=False)  # BU, SL, TR, FE
    quantity = Column(Numeric(15, 4), nullable=False)
    price = Column(Numeric(15, 4), nullable=False)
    amount = Column(Numeric(15, 2), nullable=False)
    currency = Column(String(3), default='USD')
    status = Column(String(1), default='P')  # P, D, F, R
    
    # Audit Fields
    process_date = Column(DateTime)
    process_user = Column(String(8))
    
    # Indexes for common access patterns
    __table_args__ = (
        Index('ix_tranhist_portfolio', 'portfolio_id', 'trans_date'),
        Index('ix_tranhist_investment', 'investment_id', 'trans_date'),
    )
```

### 2.3 Position History File (POSHIST - VSAM)

**Current VSAM Definition**:
- Organization: KSDS
- Record Length: 350 bytes
- Key Length: 26 bytes (Portfolio ID + Date + Investment ID)
- Key Position: 1

**Python Translation**:

```python
class PositionHistory(Base):
    __tablename__ = 'position_history_vsam'
    
    # Primary Key (composite)
    portfolio_id = Column(String(8), primary_key=True)
    position_date = Column(String(8), primary_key=True)  # YYYYMMDD
    investment_id = Column(String(10), primary_key=True)
    
    # Position Data
    quantity = Column(Numeric(15, 4), nullable=False)
    cost_basis = Column(Numeric(15, 2), nullable=False)
    market_value = Column(Numeric(15, 2), nullable=False)
    currency = Column(String(3), default='USD')
    status = Column(String(1), default='A')  # A=Active, C=Closed, P=Pending
    
    # Audit Fields
    last_maint_date = Column(DateTime)
    last_maint_user = Column(String(8))
```

### 2.4 Batch Control File (BCHCTL)

**Current VSAM Definition**:
- Organization: KSDS
- Record Length: 200 bytes
- Key Length: 20 bytes (Job Name + Process Date + Sequence)

**Python Translation**:

```python
class BatchControl(Base):
    __tablename__ = 'batch_control'
    
    # Primary Key
    job_name = Column(String(8), primary_key=True)
    process_date = Column(String(8), primary_key=True)
    sequence_no = Column(Integer, primary_key=True)
    
    # Status
    status = Column(String(1), default='R')  # R=Ready, A=Active, W=Waiting, D=Done, E=Error
    
    # Process Control
    step_name = Column(String(8))
    program_name = Column(String(8))
    start_time = Column(DateTime)
    end_time = Column(DateTime)
    
    # Return Information
    return_code = Column(Integer, default=0)
    error_desc = Column(String(80))
    
    # Statistics
    restart_count = Column(Integer, default=0)
    attempt_timestamp = Column(DateTime)
    complete_timestamp = Column(DateTime)
```

**Dependency Management Table**:

```python
class BatchDependency(Base):
    __tablename__ = 'batch_dependency'
    
    id = Column(Integer, primary_key=True, autoincrement=True)
    job_name = Column(String(8), nullable=False)
    process_date = Column(String(8), nullable=False)
    sequence_no = Column(Integer, nullable=False)
    
    # Prerequisite Job
    prereq_name = Column(String(8), nullable=False)
    prereq_seq = Column(Integer, nullable=False)
    prereq_rc = Column(Integer)  # Required return code
    
    __table_args__ = (
        Index('ix_batchdep_job', 'job_name', 'process_date', 'sequence_no'),
    )
```

## 3. DB2 Table Conversion Plan

### 3.1 POSHIST Table (DB2)

**Current DB2 Definition** (from POSHIST.sql):

```sql
CREATE TABLE POSHIST (
    ACCOUNT_NO        CHAR(8)         NOT NULL,
    PORTFOLIO_ID      CHAR(10)        NOT NULL,
    TRANS_DATE        DATE            NOT NULL,
    TRANS_TIME        TIME            NOT NULL,
    TRANS_TYPE        CHAR(2)         NOT NULL,
    SECURITY_ID       CHAR(12)        NOT NULL,
    QUANTITY          DECIMAL(15,3)   NOT NULL,
    PRICE             DECIMAL(15,3)   NOT NULL,
    AMOUNT            DECIMAL(15,2)   NOT NULL,
    FEES              DECIMAL(15,2)   NOT NULL WITH DEFAULT 0,
    TOTAL_AMOUNT      DECIMAL(15,2)   NOT NULL,
    COST_BASIS        DECIMAL(15,2)   NOT NULL,
    GAIN_LOSS         DECIMAL(15,2)   NOT NULL,
    PROCESS_DATE      DATE            NOT NULL,
    PROCESS_TIME      TIME            NOT NULL,
    PROGRAM_ID        CHAR(8)         NOT NULL,
    USER_ID           CHAR(8)         NOT NULL,
    AUDIT_TIMESTAMP   TIMESTAMP       NOT NULL WITH DEFAULT
);
```

**Python SQLAlchemy Translation**:

```python
from sqlalchemy import Column, String, Numeric, Date, Time, DateTime, Index
from sqlalchemy.ext.declarative import declarative_base
from datetime import date, time, datetime
from decimal import Decimal

Base = declarative_base()

class PositionHistoryDB2(Base):
    """
    Migrated from DB2 POSHIST table.
    Stores all portfolio transaction history for reporting.
    """
    __tablename__ = 'poshist'
    
    # Primary Key (composite)
    account_no = Column(String(8), primary_key=True)
    portfolio_id = Column(String(10), primary_key=True)
    trans_date = Column(Date, primary_key=True)
    trans_time = Column(Time, primary_key=True)
    
    # Transaction Details
    trans_type = Column(String(2), nullable=False)  # BU=Buy, SL=Sell, TR=Transfer
    security_id = Column(String(12), nullable=False)
    quantity = Column(Numeric(15, 3), nullable=False)
    price = Column(Numeric(15, 3), nullable=False)
    amount = Column(Numeric(15, 2), nullable=False)
    fees = Column(Numeric(15, 2), nullable=False, default=0)
    total_amount = Column(Numeric(15, 2), nullable=False)
    cost_basis = Column(Numeric(15, 2), nullable=False)
    gain_loss = Column(Numeric(15, 2), nullable=False)
    
    # Processing Information
    process_date = Column(Date, nullable=False)
    process_time = Column(Time, nullable=False)
    program_id = Column(String(8), nullable=False)
    user_id = Column(String(8), nullable=False)
    audit_timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    
    # Secondary Indexes (matching DB2 indexes)
    __table_args__ = (
        Index('ix_poshist_security', 'security_id', 'trans_date'),
        Index('ix_poshist_process', 'process_date', 'program_id'),
    )
```

### 3.2 ERRLOG Table

**Current DB2 Definition**:

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

**Python SQLAlchemy Translation**:

```python
class ErrorLog(Base):
    """
    Migrated from DB2 ERRLOG table.
    Stores error information for audit and troubleshooting.
    """
    __tablename__ = 'errlog'
    
    # Primary Key
    error_timestamp = Column(DateTime, primary_key=True)
    program_id = Column(String(8), primary_key=True)
    
    # Error Details
    error_type = Column(String(1))  # S=System, A=Application, D=Data
    error_severity = Column(Integer)  # 1=Info, 2=Warn, 3=Error, 4=Severe
    error_code = Column(String(8))
    error_message = Column(String(200))
    
    # Context Information
    account_no = Column(String(10), nullable=True)
    fund_id = Column(String(6), nullable=True)
    trans_id = Column(String(12), nullable=True)
    
    # Processing Information
    process_date = Column(Date)
    process_time = Column(Time)
    user_id = Column(String(8))
    additional_info = Column(String(500), nullable=True)
```

## 4. Copybook Data Structure Translation

### 4.1 TRNREC.cpy - Transaction Record

**COBOL Copybook**:
```cobol
01  TRANSACTION-RECORD.
    05  TRN-KEY.
        10  TRN-DATE           PIC X(08).
        10  TRN-TIME           PIC X(06).
        10  TRN-PORTFOLIO-ID   PIC X(08).
        10  TRN-SEQUENCE-NO    PIC X(06).
    05  TRN-DATA.
        10  TRN-INVESTMENT-ID  PIC X(10).
        10  TRN-TYPE           PIC X(02).
        10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3.
        10  TRN-PRICE          PIC S9(11)V9(4) COMP-3.
        10  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3.
        10  TRN-CURRENCY       PIC X(03).
        10  TRN-STATUS         PIC X(01).
    05  TRN-AUDIT.
        10  TRN-PROCESS-DATE   PIC X(26).
        10  TRN-PROCESS-USER   PIC X(08).
```

**Python Dataclass Translation**:

```python
from dataclasses import dataclass, field
from decimal import Decimal
from datetime import datetime
from typing import Optional
from enum import Enum

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
class TransactionKey:
    """Corresponds to TRN-KEY in TRNREC.cpy"""
    date: str  # YYYYMMDD format
    time: str  # HHMMSS format
    portfolio_id: str
    sequence_no: str
    
    def __post_init__(self):
        assert len(self.date) == 8, "Date must be 8 characters (YYYYMMDD)"
        assert len(self.time) == 6, "Time must be 6 characters (HHMMSS)"
        assert len(self.portfolio_id) <= 8, "Portfolio ID max 8 characters"
        assert len(self.sequence_no) <= 6, "Sequence number max 6 characters"

@dataclass
class TransactionData:
    """Corresponds to TRN-DATA in TRNREC.cpy"""
    investment_id: str
    trans_type: TransactionType
    quantity: Decimal  # S9(11)V9(4) -> 15 digits, 4 decimal places
    price: Decimal     # S9(11)V9(4) -> 15 digits, 4 decimal places
    amount: Decimal    # S9(13)V9(2) -> 15 digits, 2 decimal places
    currency: str = 'USD'
    status: TransactionStatus = TransactionStatus.PENDING

@dataclass
class TransactionAudit:
    """Corresponds to TRN-AUDIT in TRNREC.cpy"""
    process_date: datetime
    process_user: str

@dataclass
class TransactionRecord:
    """
    Complete transaction record corresponding to TRANSACTION-RECORD in TRNREC.cpy.
    
    COBOL COMP-3 (packed decimal) fields are translated to Python Decimal
    for precise financial calculations.
    """
    key: TransactionKey
    data: TransactionData
    audit: Optional[TransactionAudit] = None
    
    @classmethod
    def from_dict(cls, d: dict) -> 'TransactionRecord':
        """Create TransactionRecord from dictionary (e.g., from JSON or database)"""
        key = TransactionKey(
            date=d['date'],
            time=d['time'],
            portfolio_id=d['portfolio_id'],
            sequence_no=d['sequence_no']
        )
        data = TransactionData(
            investment_id=d['investment_id'],
            trans_type=TransactionType(d['trans_type']),
            quantity=Decimal(str(d['quantity'])),
            price=Decimal(str(d['price'])),
            amount=Decimal(str(d['amount'])),
            currency=d.get('currency', 'USD'),
            status=TransactionStatus(d.get('status', 'P'))
        )
        audit = None
        if 'process_date' in d and d['process_date']:
            audit = TransactionAudit(
                process_date=datetime.fromisoformat(d['process_date']),
                process_user=d.get('process_user', '')
            )
        return cls(key=key, data=data, audit=audit)
```

### 4.2 POSREC.cpy - Position Record

**COBOL Copybook**:
```cobol
01  POSITION-RECORD.
    05  POS-KEY.
        10  POS-PORTFOLIO-ID   PIC X(08).
        10  POS-DATE           PIC X(08).
        10  POS-INVESTMENT-ID  PIC X(10).
    05  POS-DATA.
        10  POS-QUANTITY       PIC S9(11)V9(4) COMP-3.
        10  POS-COST-BASIS     PIC S9(13)V9(2) COMP-3.
        10  POS-MARKET-VALUE   PIC S9(13)V9(2) COMP-3.
        10  POS-CURRENCY       PIC X(03).
        10  POS-STATUS         PIC X(01).
    05  POS-AUDIT.
        10  POS-LAST-MAINT-DATE   PIC X(26).
        10  POS-LAST-MAINT-USER   PIC X(08).
```

**Python Dataclass Translation**:

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
class PositionKey:
    """Corresponds to POS-KEY in POSREC.cpy"""
    portfolio_id: str
    date: str  # YYYYMMDD format
    investment_id: str

@dataclass
class PositionData:
    """Corresponds to POS-DATA in POSREC.cpy"""
    quantity: Decimal      # S9(11)V9(4) COMP-3
    cost_basis: Decimal    # S9(13)V9(2) COMP-3
    market_value: Decimal  # S9(13)V9(2) COMP-3
    currency: str = 'USD'
    status: PositionStatus = PositionStatus.ACTIVE

@dataclass
class PositionAudit:
    """Corresponds to POS-AUDIT in POSREC.cpy"""
    last_maint_date: datetime
    last_maint_user: str

@dataclass
class PositionRecord:
    """
    Complete position record corresponding to POSITION-RECORD in POSREC.cpy.
    """
    key: PositionKey
    data: PositionData
    audit: Optional[PositionAudit] = None
    
    @property
    def unrealized_gain_loss(self) -> Decimal:
        """Calculate unrealized gain/loss"""
        return self.data.market_value - self.data.cost_basis
    
    @property
    def average_cost(self) -> Decimal:
        """Calculate average cost per unit"""
        if self.data.quantity == 0:
            return Decimal('0')
        return self.data.cost_basis / self.data.quantity
```

### 4.3 HISTREC.cpy - History Record

**COBOL Copybook**:
```cobol
01  HISTORY-RECORD.
    05  HIST-KEY.
        10  HIST-PORTFOLIO-ID  PIC X(08).
        10  HIST-DATE          PIC X(08).
        10  HIST-TIME          PIC X(06).
        10  HIST-SEQ-NO        PIC X(04).
    05  HIST-DATA.
        10  HIST-RECORD-TYPE   PIC X(02).
        10  HIST-ACTION-CODE   PIC X(01).
        10  HIST-BEFORE-IMAGE  PIC X(400).
        10  HIST-AFTER-IMAGE   PIC X(400).
        10  HIST-REASON-CODE   PIC X(04).
    05  HIST-AUDIT.
        10  HIST-PROCESS-DATE  PIC X(26).
        10  HIST-PROCESS-USER  PIC X(08).
```

**Python Dataclass Translation**:

```python
from dataclasses import dataclass
from datetime import datetime
from typing import Optional, Any
from enum import Enum
import json

class HistoryRecordType(str, Enum):
    PORTFOLIO = 'PT'
    POSITION = 'PS'
    TRANSACTION = 'TR'

class HistoryActionCode(str, Enum):
    ADD = 'A'
    CHANGE = 'C'
    DELETE = 'D'

@dataclass
class HistoryKey:
    """Corresponds to HIST-KEY in HISTREC.cpy"""
    portfolio_id: str
    date: str  # YYYYMMDD
    time: str  # HHMMSS
    seq_no: str

@dataclass
class HistoryData:
    """Corresponds to HIST-DATA in HISTREC.cpy"""
    record_type: HistoryRecordType
    action_code: HistoryActionCode
    before_image: Optional[dict]  # JSON representation of before state
    after_image: Optional[dict]   # JSON representation of after state
    reason_code: str

@dataclass
class HistoryAudit:
    """Corresponds to HIST-AUDIT in HISTREC.cpy"""
    process_date: datetime
    process_user: str

@dataclass
class HistoryRecord:
    """
    Complete history record corresponding to HISTORY-RECORD in HISTREC.cpy.
    
    The COBOL before/after images (400 bytes each) are translated to
    JSON dictionaries for flexible storage of different record types.
    """
    key: HistoryKey
    data: HistoryData
    audit: Optional[HistoryAudit] = None
```

### 4.4 BCHCTL.cpy - Batch Control Record

**Python Dataclass Translation**:

```python
from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional
from enum import Enum

class BatchStatus(str, Enum):
    READY = 'R'
    ACTIVE = 'A'
    WAITING = 'W'
    DONE = 'D'
    ERROR = 'E'

@dataclass
class BatchPrerequisite:
    """Corresponds to BCT-PREREQ-JOBS in BCHCTL.cpy"""
    prereq_name: str
    prereq_seq: int
    prereq_rc: int

@dataclass
class BatchControlKey:
    """Corresponds to BCT-KEY in BCHCTL.cpy"""
    job_name: str
    process_date: str
    sequence_no: int

@dataclass
class BatchProcessControl:
    """Corresponds to BCT-PROCESS-CONTROL in BCHCTL.cpy"""
    step_name: str
    program_name: str
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None

@dataclass
class BatchReturnInfo:
    """Corresponds to BCT-RETURN-INFO in BCHCTL.cpy"""
    return_code: int = 0
    error_desc: str = ''

@dataclass
class BatchStatistics:
    """Corresponds to BCT-STATISTICS in BCHCTL.cpy"""
    restart_count: int = 0
    attempt_timestamp: Optional[datetime] = None
    complete_timestamp: Optional[datetime] = None

@dataclass
class BatchControlRecord:
    """
    Complete batch control record corresponding to BATCH-CONTROL-RECORD in BCHCTL.cpy.
    """
    key: BatchControlKey
    status: BatchStatus = BatchStatus.READY
    process_control: Optional[BatchProcessControl] = None
    prerequisites: List[BatchPrerequisite] = field(default_factory=list)
    return_info: BatchReturnInfo = field(default_factory=BatchReturnInfo)
    statistics: BatchStatistics = field(default_factory=BatchStatistics)
    
    def check_prerequisites_met(self, completed_jobs: dict) -> bool:
        """
        Check if all prerequisites are satisfied.
        
        Args:
            completed_jobs: Dict mapping job_name to return_code
            
        Returns:
            True if all prerequisites are met
        """
        for prereq in self.prerequisites:
            if prereq.prereq_name not in completed_jobs:
                return False
            if completed_jobs[prereq.prereq_name] > prereq.prereq_rc:
                return False
        return True
```

## 5. Database Schema Migration Script

### 5.1 SQLAlchemy Models Module

```python
# migration/python/models/database.py

from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from contextlib import contextmanager

Base = declarative_base()

class DatabaseManager:
    """
    Database connection manager replacing VSAM and DB2 connections.
    """
    
    def __init__(self, connection_string: str = 'sqlite:///portfolio.db'):
        self.engine = create_engine(connection_string, echo=False)
        self.Session = sessionmaker(bind=self.engine)
    
    def create_all_tables(self):
        """Create all tables defined in the models."""
        Base.metadata.create_all(self.engine)
    
    def drop_all_tables(self):
        """Drop all tables (use with caution)."""
        Base.metadata.drop_all(self.engine)
    
    @contextmanager
    def session_scope(self):
        """
        Provide a transactional scope around a series of operations.
        Replaces COBOL COMMIT/ROLLBACK patterns.
        """
        session = self.Session()
        try:
            yield session
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()
```

### 5.2 Migration Script

```python
# migration/python/utils/migrate_schema.py

from sqlalchemy import text
from models.database import DatabaseManager, Base

def create_migration_schema(db_manager: DatabaseManager):
    """
    Create the complete database schema for the migrated system.
    """
    db_manager.create_all_tables()
    
    # Create additional indexes for performance
    with db_manager.session_scope() as session:
        # Index for transaction lookups by portfolio
        session.execute(text('''
            CREATE INDEX IF NOT EXISTS ix_trans_portfolio_date 
            ON transaction_history (portfolio_id, trans_date)
        '''))
        
        # Index for position lookups
        session.execute(text('''
            CREATE INDEX IF NOT EXISTS ix_pos_portfolio_investment 
            ON position_history_vsam (portfolio_id, investment_id)
        '''))

def migrate_vsam_to_db(vsam_file_path: str, db_manager: DatabaseManager):
    """
    Migrate data from VSAM file export to database.
    
    This would be used during actual migration to load existing data.
    """
    # Implementation would read VSAM export file and insert into database
    pass
```

## 6. Data Type Mapping Reference

### 6.1 COBOL to Python Type Mappings

| COBOL Type | Python Type | Notes |
|------------|-------------|-------|
| PIC X(n) | str | Fixed-length string, may need padding/trimming |
| PIC 9(n) | int | Unsigned integer |
| PIC S9(n) | int | Signed integer |
| PIC S9(n)V9(m) | Decimal | Signed decimal with m decimal places |
| PIC S9(n)V9(m) COMP-3 | Decimal | Packed decimal, use Decimal for precision |
| PIC S9(n) COMP | int | Binary integer |
| 88 level | Enum or bool | Condition names become enum values |

### 6.2 Precision Considerations

For financial calculations, always use `decimal.Decimal` instead of `float`:

```python
from decimal import Decimal, ROUND_HALF_UP

# Set precision for financial calculations
def financial_round(value: Decimal, places: int = 2) -> Decimal:
    """Round to specified decimal places using banker's rounding."""
    return value.quantize(Decimal(10) ** -places, rounding=ROUND_HALF_UP)

# Example: Calculate cost basis
quantity = Decimal('100.5000')  # S9(11)V9(4)
price = Decimal('25.7500')      # S9(11)V9(4)
amount = financial_round(quantity * price)  # Results in Decimal('2587.88')
```

## 7. File Access Pattern Translation

### 7.1 VSAM KSDS Operations to SQL

| VSAM Operation | SQL Equivalent | Python SQLAlchemy |
|----------------|----------------|-------------------|
| READ (key) | SELECT WHERE key = ? | session.query(Model).get(key) |
| READ NEXT | SELECT ORDER BY key LIMIT 1 OFFSET n | session.query(Model).order_by(key).offset(n).first() |
| WRITE | INSERT | session.add(obj) |
| REWRITE | UPDATE | session.merge(obj) |
| DELETE | DELETE | session.delete(obj) |
| START (key) | SELECT WHERE key >= ? | session.query(Model).filter(key >= value) |

### 7.2 Sequential File Operations

```python
# COBOL: READ TRANSACTION-FILE AT END SET END-OF-FILE TO TRUE
# Python equivalent using generator:

def read_transactions(file_path: str):
    """Generator that yields transaction records from file."""
    with open(file_path, 'r') as f:
        for line in f:
            if line.strip():
                yield parse_transaction(line)

# Usage:
for transaction in read_transactions('transactions.dat'):
    process_transaction(transaction)
```

## 8. Implementation Recommendations

### 8.1 Development Environment

- **Database**: SQLite for development, PostgreSQL for production
- **ORM**: SQLAlchemy for database abstraction
- **Data Validation**: Pydantic for input validation
- **Testing**: pytest with factory_boy for test data

### 8.2 Migration Phases

**Phase 1**: Create Python data models and database schema
**Phase 2**: Implement data access layer (replacing VSAM operations)
**Phase 3**: Migrate batch processing programs
**Phase 4**: Migrate online inquiry functions
**Phase 5**: Migrate reporting programs
**Phase 6**: Data migration and validation

### 8.3 Data Integrity Considerations

1. Use database transactions for all multi-record operations
2. Implement optimistic locking for concurrent access
3. Maintain audit trails for all data changes
4. Implement checkpoint/restart using database savepoints

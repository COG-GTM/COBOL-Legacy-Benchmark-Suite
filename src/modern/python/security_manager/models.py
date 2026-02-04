"""
SQLAlchemy ORM models for the Security Manager.

Maps to the original COBOL DB2 tables:
- AUTHFILE: Authorization rules table
- AUDITLOG: Security audit trail table
"""

from datetime import datetime
from sqlalchemy import Column, String, DateTime, Index, create_engine
from sqlalchemy.orm import declarative_base, sessionmaker

Base = declarative_base()


class AuthFile(Base):
    """
    Authorization rules table.
    
    Maps to COBOL AUTHFILE table used in P200-CHECK-AUTH.
    Contains user permissions for resources and access types.
    
    Original COBOL columns:
    - USER_ID: PIC X(8)
    - RESOURCE: PIC X(8)
    - ACCESS_TYPE: PIC X(8)
    """
    __tablename__ = 'authfile'
    
    user_id = Column(String(8), primary_key=True, nullable=False)
    resource = Column(String(8), primary_key=True, nullable=False)
    access_type = Column(String(8), primary_key=True, nullable=False)
    
    __table_args__ = (
        Index('idx_authfile_lookup', 'user_id', 'resource', 'access_type'),
    )
    
    def __repr__(self) -> str:
        return f"<AuthFile(user_id='{self.user_id}', resource='{self.resource}', access_type='{self.access_type}')>"


class AuditLog(Base):
    """
    Security audit trail table.
    
    Maps to COBOL AUDITLOG table used in P300-LOG-ACCESS.
    Records all access attempts for security auditing.
    
    Original COBOL columns:
    - TIMESTAMP: PIC X(26)
    - USER_ID: PIC X(8)
    - TERMINAL_ID: PIC X(4)
    - TRANS_ID: PIC X(4)
    - PROGRAM: PIC X(8)
    - ACCESS_TYPE: PIC X(8)
    """
    __tablename__ = 'auditlog'
    
    id = Column(String(36), primary_key=True)
    timestamp = Column(DateTime, nullable=False, default=datetime.utcnow)
    user_id = Column(String(8), nullable=False)
    terminal_id = Column(String(4), nullable=False)
    trans_id = Column(String(4), nullable=False)
    program = Column(String(8), nullable=False)
    access_type = Column(String(8), nullable=False)
    
    __table_args__ = (
        Index('idx_auditlog_user', 'user_id'),
        Index('idx_auditlog_timestamp', 'timestamp'),
    )
    
    def __repr__(self) -> str:
        return f"<AuditLog(timestamp='{self.timestamp}', user_id='{self.user_id}', program='{self.program}')>"


def get_engine(database_url: str = "sqlite:///security.db"):
    """Create database engine."""
    return create_engine(database_url, echo=False)


def get_session_factory(engine):
    """Create session factory for database operations."""
    return sessionmaker(bind=engine)


def init_db(engine):
    """Initialize database tables."""
    Base.metadata.create_all(engine)

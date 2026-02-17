"""
Database models for the Security Manager service.

Maps the COBOL DB2 tables AUTHFILE and AUDITLOG to SQLAlchemy ORM models,
preserving the original column schemas for compatibility.
"""

from datetime import datetime

from sqlalchemy import Column, DateTime, Index, String
from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    pass


class AuthFile(Base):
    """Authorization rules table.

    Corresponds to the COBOL DB2 AUTHFILE table used in P200-CHECK-AUTH.
    Each row grants a specific user permission to a specific resource
    with a specific access type.
    """

    __tablename__ = "AUTHFILE"

    USER_ID = Column(String(8), primary_key=True)
    RESOURCE = Column(String(8), primary_key=True)
    ACCESS_TYPE = Column(String(8), primary_key=True)

    __table_args__ = (
        Index("IDX_AUTHFILE_LOOKUP", "USER_ID", "RESOURCE", "ACCESS_TYPE"),
    )

    def __repr__(self) -> str:
        return (
            f"<AuthFile(USER_ID={self.USER_ID!r}, "
            f"RESOURCE={self.RESOURCE!r}, "
            f"ACCESS_TYPE={self.ACCESS_TYPE!r})>"
        )


class AuditLog(Base):
    """Audit trail table.

    Corresponds to the COBOL DB2 AUDITLOG table used in P300-LOG-ACCESS.
    Records every access attempt with full context information.
    """

    __tablename__ = "AUDITLOG"

    id = Column(String(36), primary_key=True)
    TIMESTAMP = Column(DateTime, nullable=False, default=datetime.utcnow)
    USER_ID = Column(String(8), nullable=False)
    TERMINAL_ID = Column(String(4), nullable=False)
    TRANS_ID = Column(String(4), nullable=False)
    PROGRAM = Column(String(8), nullable=False)
    ACCESS_TYPE = Column(String(8), nullable=False)

    __table_args__ = (
        Index("IDX_AUDITLOG_USER", "USER_ID", "TIMESTAMP"),
        Index("IDX_AUDITLOG_TIME", "TIMESTAMP"),
    )

    def __repr__(self) -> str:
        return (
            f"<AuditLog(USER_ID={self.USER_ID!r}, "
            f"PROGRAM={self.PROGRAM!r}, "
            f"ACCESS_TYPE={self.ACCESS_TYPE!r}, "
            f"TIMESTAMP={self.TIMESTAMP!r})>"
        )

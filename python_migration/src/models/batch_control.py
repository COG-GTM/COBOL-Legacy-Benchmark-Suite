"""
Batch Control model - Maps to COBOL BCHCTL.cpy
Represents batch job control and checkpoint records.
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import Optional, List
from enum import Enum

from sqlalchemy import Column, Integer, String, DateTime, Index, UniqueConstraint
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()


class BatchStatus(str, Enum):
    """Batch status codes - maps to BCT-STATUS 88 levels"""
    READY = 'R'
    ACTIVE = 'A'
    WAITING = 'W'
    DONE = 'D'
    ERROR = 'E'


@dataclass
class PrerequisiteJob:
    """Prerequisite job definition"""
    name: str
    sequence: int
    return_code: int


@dataclass
class BatchControlRecord:
    """
    Batch control record dataclass - maps to COBOL BCHCTL.cpy
    
    Original COBOL structure:
    01  BATCH-CONTROL-RECORD.
        05  BCT-KEY.
            10  BCT-JOB-NAME      PIC X(8).
            10  BCT-PROCESS-DATE  PIC X(8).
            10  BCT-SEQUENCE-NO   PIC 9(4).
        05  BCT-DATA.
            10  BCT-STATUS        PIC X(1).
            10  BCT-PROCESS-CONTROL.
                15  BCT-STEP-NAME    PIC X(8).
                15  BCT-PROGRAM-NAME PIC X(8).
                15  BCT-START-TIME   PIC X(8).
                15  BCT-END-TIME     PIC X(8).
            10  BCT-DEPENDENCIES.
                15  BCT-PREREQ-COUNT PIC 9(2) COMP.
                15  BCT-PREREQ-JOBS  OCCURS 10 TIMES.
            10  BCT-RETURN-INFO.
                15  BCT-RETURN-CODE  PIC S9(4) COMP.
                15  BCT-ERROR-DESC   PIC X(80).
        05  BCT-STATISTICS.
            10  BCT-RESTART-COUNT  PIC 9(2) COMP.
            10  BCT-ATTEMPT-TS     PIC X(26).
            10  BCT-COMPLETE-TS    PIC X(26).
    """
    job_name: str
    process_date: str
    sequence_no: int
    status: BatchStatus = BatchStatus.READY
    step_name: Optional[str] = None
    program_name: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    prereq_count: int = 0
    prereq_jobs: List[PrerequisiteJob] = field(default_factory=list)
    return_code: Optional[int] = None
    error_desc: Optional[str] = None
    restart_count: int = 0
    attempt_timestamp: Optional[datetime] = None
    complete_timestamp: Optional[datetime] = None
    records_read: int = 0
    records_written: int = 0

    def __post_init__(self):
        """Ensure proper types after initialization"""
        if isinstance(self.status, str):
            self.status = BatchStatus(self.status)

    @property
    def key(self) -> str:
        """Return composite key matching COBOL BCT-KEY"""
        return f"{self.job_name}{self.process_date}{self.sequence_no:04d}"

    @property
    def is_complete(self) -> bool:
        """Check if batch job is complete"""
        return self.status == BatchStatus.DONE

    @property
    def is_error(self) -> bool:
        """Check if batch job has error"""
        return self.status == BatchStatus.ERROR

    @property
    def can_start(self) -> bool:
        """Check if batch job can start (prerequisites met)"""
        if self.status != BatchStatus.READY:
            return False
        return all(
            prereq.return_code <= 4 
            for prereq in self.prereq_jobs
        )

    def check_prerequisites(self, completed_jobs: dict) -> bool:
        """
        Check if all prerequisites are satisfied.
        completed_jobs: dict mapping job_name to return_code
        """
        for prereq in self.prereq_jobs:
            if prereq.name not in completed_jobs:
                return False
            if completed_jobs[prereq.name] > prereq.return_code:
                return False
        return True

    def start(self) -> None:
        """Mark job as started"""
        self.status = BatchStatus.ACTIVE
        self.start_time = datetime.now().strftime('%H%M%S%f')[:8]
        self.attempt_timestamp = datetime.now()

    def complete(self, return_code: int, error_desc: str = None) -> None:
        """Mark job as complete"""
        if return_code <= 4:
            self.status = BatchStatus.DONE
        else:
            self.status = BatchStatus.ERROR
        self.return_code = return_code
        self.error_desc = error_desc
        self.end_time = datetime.now().strftime('%H%M%S%f')[:8]
        self.complete_timestamp = datetime.now()

    def validate(self) -> tuple[bool, list[str]]:
        """
        Validate batch control record fields.
        Returns (is_valid, list of error messages)
        """
        errors = []
        
        if not self.job_name or len(self.job_name) > 8:
            errors.append("E001: Invalid job name (must be 1-8 characters)")
        
        if not self.process_date or len(self.process_date) != 8 or not self.process_date.isdigit():
            errors.append("E002: Invalid process date (must be YYYYMMDD)")
        
        if self.sequence_no < 0 or self.sequence_no > 9999:
            errors.append("E003: Invalid sequence number (must be 0-9999)")
        
        if self.prereq_count != len(self.prereq_jobs):
            errors.append("E004: Prerequisite count mismatch")
        
        return len(errors) == 0, errors

    def to_dict(self) -> dict:
        """Convert to dictionary for serialization"""
        return {
            'job_name': self.job_name,
            'process_date': self.process_date,
            'sequence_no': self.sequence_no,
            'status': self.status.value,
            'step_name': self.step_name,
            'program_name': self.program_name,
            'start_time': self.start_time,
            'end_time': self.end_time,
            'prereq_count': self.prereq_count,
            'prereq_jobs': [
                {'name': p.name, 'sequence': p.sequence, 'return_code': p.return_code}
                for p in self.prereq_jobs
            ],
            'return_code': self.return_code,
            'error_desc': self.error_desc,
            'restart_count': self.restart_count,
            'attempt_timestamp': self.attempt_timestamp.isoformat() if self.attempt_timestamp else None,
            'complete_timestamp': self.complete_timestamp.isoformat() if self.complete_timestamp else None,
            'records_read': self.records_read,
            'records_written': self.records_written,
        }

    @classmethod
    def from_dict(cls, data: dict) -> 'BatchControlRecord':
        """Create from dictionary"""
        prereq_jobs = [
            PrerequisiteJob(name=p['name'], sequence=p['sequence'], return_code=p['return_code'])
            for p in data.get('prereq_jobs', [])
        ]
        return cls(
            job_name=data['job_name'],
            process_date=data['process_date'],
            sequence_no=data['sequence_no'],
            status=BatchStatus(data.get('status', 'R')),
            step_name=data.get('step_name'),
            program_name=data.get('program_name'),
            start_time=data.get('start_time'),
            end_time=data.get('end_time'),
            prereq_count=data.get('prereq_count', 0),
            prereq_jobs=prereq_jobs,
            return_code=data.get('return_code'),
            error_desc=data.get('error_desc'),
            restart_count=data.get('restart_count', 0),
            attempt_timestamp=datetime.fromisoformat(data['attempt_timestamp']) if data.get('attempt_timestamp') else None,
            complete_timestamp=datetime.fromisoformat(data['complete_timestamp']) if data.get('complete_timestamp') else None,
            records_read=data.get('records_read', 0),
            records_written=data.get('records_written', 0),
        )


class BatchControl(Base):
    """SQLAlchemy model for batch_control table - replaces VSAM BCHCTL"""
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
    records_read = Column(Integer, nullable=False, default=0)
    records_written = Column(Integer, nullable=False, default=0)

    __table_args__ = (
        UniqueConstraint('job_name', 'process_date', 'sequence_no', name='uix_batch_key'),
        Index('ix_batch_job', 'job_name'),
        Index('ix_batch_date', 'process_date'),
        Index('ix_batch_status', 'status'),
    )

    def to_record(self) -> BatchControlRecord:
        """Convert to BatchControlRecord dataclass"""
        return BatchControlRecord(
            job_name=self.job_name,
            process_date=self.process_date,
            sequence_no=self.sequence_no,
            status=BatchStatus(self.status),
            step_name=self.step_name,
            program_name=self.program_name,
            start_time=self.start_time,
            end_time=self.end_time,
            prereq_count=self.prereq_count,
            prereq_jobs=[],  # Would need separate table for prereqs
            return_code=self.return_code,
            error_desc=self.error_desc,
            restart_count=self.restart_count,
            attempt_timestamp=self.attempt_timestamp,
            complete_timestamp=self.complete_timestamp,
            records_read=self.records_read,
            records_written=self.records_written,
        )

    @classmethod
    def from_record(cls, record: BatchControlRecord) -> 'BatchControl':
        """Create from BatchControlRecord dataclass"""
        return cls(
            job_name=record.job_name,
            process_date=record.process_date,
            sequence_no=record.sequence_no,
            status=record.status.value,
            step_name=record.step_name,
            program_name=record.program_name,
            start_time=record.start_time,
            end_time=record.end_time,
            prereq_count=record.prereq_count,
            return_code=record.return_code,
            error_desc=record.error_desc,
            restart_count=record.restart_count,
            attempt_timestamp=record.attempt_timestamp,
            complete_timestamp=record.complete_timestamp,
            records_read=record.records_read,
            records_written=record.records_written,
        )

    def __repr__(self):
        return f"<BatchControl(job={self.job_name}, date={self.process_date}, status={self.status})>"

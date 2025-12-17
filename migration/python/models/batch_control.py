"""
Batch control data models - Python translation of BCHCTL.cpy

This module contains dataclasses that correspond to the COBOL copybook
BCHCTL.cpy, which defines the batch control record structure used for
job scheduling, dependency management, and checkpoint/restart.

Original COBOL Structure:
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
                    20  BCT-PREREQ-NAME  PIC X(8).
                    20  BCT-PREREQ-SEQ   PIC 9(4).
                    20  BCT-PREREQ-RC    PIC S9(4) COMP.
            10  BCT-RETURN-INFO.
                15  BCT-RETURN-CODE  PIC S9(4) COMP.
                15  BCT-ERROR-DESC   PIC X(80).
        05  BCT-STATISTICS.
            10  BCT-RESTART-COUNT  PIC 9(2) COMP.
            10  BCT-ATTEMPT-TS     PIC X(26).
            10  BCT-COMPLETE-TS    PIC X(26).
"""

from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional, Dict
from enum import Enum


class BatchStatus(str, Enum):
    """
    Batch status codes corresponding to BCT-STATUS 88-level conditions.
    
    88  BCT-STATUS-READY    VALUE 'R'.
    88  BCT-STATUS-ACTIVE   VALUE 'A'.
    88  BCT-STATUS-WAITING  VALUE 'W'.
    88  BCT-STATUS-DONE     VALUE 'D'.
    88  BCT-STATUS-ERROR    VALUE 'E'.
    """
    READY = 'R'
    ACTIVE = 'A'
    WAITING = 'W'
    DONE = 'D'
    ERROR = 'E'


# Return code constants from BCHCON.cpy
class ReturnCode:
    """
    Standard return codes used throughout the batch system.
    
    Corresponds to constants defined in BCHCON.cpy and ERRHAND.cpy.
    """
    SUCCESS = 0      # Successful completion
    WARNING = 4      # Warning, processing complete
    ERROR = 8        # Errors, processing complete
    SEVERE = 12      # Critical error, abend
    TERMINAL = 16    # Environment error


@dataclass
class BatchPrerequisite:
    """
    Prerequisite job definition corresponding to BCT-PREREQ-JOBS in BCHCTL.cpy.
    
    Defines a job that must complete successfully before the current job
    can execute.
    
    Attributes:
        prereq_name: Name of the prerequisite job (8 bytes)
        prereq_seq: Sequence number of the prerequisite job
        prereq_rc: Maximum acceptable return code from prerequisite
    """
    prereq_name: str
    prereq_seq: int
    prereq_rc: int = ReturnCode.WARNING  # Default: accept RC <= 4
    
    def __post_init__(self):
        """Validate prerequisite fields."""
        if len(self.prereq_name) > 8:
            raise ValueError(f"Prereq name max 8 characters, got {len(self.prereq_name)}")
        self.prereq_name = self.prereq_name.ljust(8)
    
    def is_satisfied(self, actual_rc: int) -> bool:
        """Check if the prerequisite is satisfied by the given return code."""
        return actual_rc <= self.prereq_rc


@dataclass
class BatchControlKey:
    """
    Batch control key structure corresponding to BCT-KEY in BCHCTL.cpy.
    
    This composite key uniquely identifies each batch control record.
    
    Attributes:
        job_name: Job name (8 bytes)
        process_date: Processing date in YYYYMMDD format (8 bytes)
        sequence_no: Sequence number within the job
    """
    job_name: str
    process_date: str
    sequence_no: int
    
    def __post_init__(self):
        """Validate key field lengths matching COBOL PIC definitions."""
        if len(self.job_name) > 8:
            raise ValueError(f"Job name max 8 characters, got {len(self.job_name)}")
        if len(self.process_date) != 8:
            raise ValueError(f"Process date must be 8 characters (YYYYMMDD), got {len(self.process_date)}")
        
        # Pad job name to match COBOL fixed-length format
        self.job_name = self.job_name.ljust(8)
    
    @property
    def composite_key(self) -> str:
        """Return the full composite key as a single string."""
        return f"{self.job_name}{self.process_date}{self.sequence_no:04d}"
    
    @property
    def as_date(self) -> datetime:
        """Convert process_date to Python datetime object."""
        return datetime.strptime(self.process_date, "%Y%m%d")


@dataclass
class BatchProcessControl:
    """
    Process control structure corresponding to BCT-PROCESS-CONTROL in BCHCTL.cpy.
    
    Contains information about the current execution step.
    
    Attributes:
        step_name: Current step name (8 bytes)
        program_name: Program being executed (8 bytes)
        start_time: Step start time
        end_time: Step end time
    """
    step_name: str
    program_name: str
    start_time: Optional[datetime] = None
    end_time: Optional[datetime] = None
    
    def __post_init__(self):
        """Validate process control fields."""
        if len(self.step_name) > 8:
            raise ValueError(f"Step name max 8 characters, got {len(self.step_name)}")
        if len(self.program_name) > 8:
            raise ValueError(f"Program name max 8 characters, got {len(self.program_name)}")
        
        self.step_name = self.step_name.ljust(8)
        self.program_name = self.program_name.ljust(8)
    
    @property
    def elapsed_seconds(self) -> Optional[float]:
        """Calculate elapsed time in seconds."""
        if self.start_time is None or self.end_time is None:
            return None
        return (self.end_time - self.start_time).total_seconds()
    
    def start(self):
        """Mark the step as started."""
        self.start_time = datetime.now()
    
    def complete(self):
        """Mark the step as completed."""
        self.end_time = datetime.now()


@dataclass
class BatchReturnInfo:
    """
    Return information structure corresponding to BCT-RETURN-INFO in BCHCTL.cpy.
    
    Contains the return code and error description from job execution.
    
    Attributes:
        return_code: Return code from execution (S9(4) COMP)
        error_desc: Error description (80 bytes)
    """
    return_code: int = ReturnCode.SUCCESS
    error_desc: str = ''
    
    def __post_init__(self):
        """Validate return info fields."""
        if len(self.error_desc) > 80:
            self.error_desc = self.error_desc[:80]
    
    @property
    def is_success(self) -> bool:
        """Check if return code indicates success."""
        return self.return_code == ReturnCode.SUCCESS
    
    @property
    def is_warning(self) -> bool:
        """Check if return code indicates warning."""
        return self.return_code == ReturnCode.WARNING
    
    @property
    def is_error(self) -> bool:
        """Check if return code indicates error."""
        return self.return_code >= ReturnCode.ERROR
    
    def set_success(self):
        """Set return code to success."""
        self.return_code = ReturnCode.SUCCESS
        self.error_desc = ''
    
    def set_warning(self, message: str = ''):
        """Set return code to warning."""
        self.return_code = ReturnCode.WARNING
        self.error_desc = message[:80] if message else ''
    
    def set_error(self, message: str):
        """Set return code to error."""
        self.return_code = ReturnCode.ERROR
        self.error_desc = message[:80]
    
    def set_severe(self, message: str):
        """Set return code to severe error."""
        self.return_code = ReturnCode.SEVERE
        self.error_desc = message[:80]


@dataclass
class BatchStatistics:
    """
    Statistics structure corresponding to BCT-STATISTICS in BCHCTL.cpy.
    
    Contains execution statistics for monitoring and reporting.
    
    Attributes:
        restart_count: Number of restart attempts (9(2) COMP)
        attempt_timestamp: Timestamp of last attempt (26 bytes)
        complete_timestamp: Timestamp of completion (26 bytes)
    """
    restart_count: int = 0
    attempt_timestamp: Optional[datetime] = None
    complete_timestamp: Optional[datetime] = None
    
    def record_attempt(self):
        """Record a new execution attempt."""
        self.restart_count += 1
        self.attempt_timestamp = datetime.now()
    
    def record_completion(self):
        """Record successful completion."""
        self.complete_timestamp = datetime.now()
    
    @property
    def elapsed_seconds(self) -> Optional[float]:
        """Calculate total elapsed time in seconds."""
        if self.attempt_timestamp is None or self.complete_timestamp is None:
            return None
        return (self.complete_timestamp - self.attempt_timestamp).total_seconds()


@dataclass
class BatchControlRecord:
    """
    Complete batch control record corresponding to BATCH-CONTROL-RECORD in BCHCTL.cpy.
    
    This is the main data structure used for batch job control throughout
    the system. It manages job scheduling, dependency checking, and
    checkpoint/restart functionality.
    
    Replaces the functionality of BCHCTL00.cbl for:
    - Process initialization (INIT)
    - Prerequisite checking (CHEK)
    - Status updates (UPDT)
    - Process termination (TERM)
    """
    key: BatchControlKey
    status: BatchStatus = BatchStatus.READY
    process_control: Optional[BatchProcessControl] = None
    prerequisites: List[BatchPrerequisite] = field(default_factory=list)
    return_info: BatchReturnInfo = field(default_factory=BatchReturnInfo)
    statistics: BatchStatistics = field(default_factory=BatchStatistics)
    
    def check_prerequisites_met(self, completed_jobs: Dict[str, int]) -> bool:
        """
        Check if all prerequisites are satisfied.
        
        This implements the logic from BCHCTL00's 2000-CHECK-PREREQUISITES
        and 2200-CHECK-DEPENDENCIES paragraphs.
        
        Args:
            completed_jobs: Dict mapping job_name to return_code
            
        Returns:
            True if all prerequisites are met
        """
        for prereq in self.prerequisites:
            prereq_key = prereq.prereq_name.strip()
            if prereq_key not in completed_jobs:
                return False
            if not prereq.is_satisfied(completed_jobs[prereq_key]):
                return False
        return True
    
    def initialize(self, step_name: str, program_name: str) -> 'BatchControlRecord':
        """
        Initialize the batch control record for execution.
        
        This implements the logic from BCHCTL00's 1000-PROCESS-INITIALIZE
        paragraph.
        
        Args:
            step_name: Name of the step being executed
            program_name: Name of the program being executed
            
        Returns:
            Self for method chaining
        """
        self.status = BatchStatus.ACTIVE
        self.process_control = BatchProcessControl(
            step_name=step_name,
            program_name=program_name
        )
        self.process_control.start()
        self.statistics.record_attempt()
        return self
    
    def update_status(self, new_status: BatchStatus) -> 'BatchControlRecord':
        """
        Update the batch control status.
        
        This implements the logic from BCHCTL00's 3000-UPDATE-STATUS
        paragraph.
        
        Args:
            new_status: New status to set
            
        Returns:
            Self for method chaining
        """
        self.status = new_status
        return self
    
    def terminate(self, return_code: int, error_desc: str = '') -> 'BatchControlRecord':
        """
        Terminate the batch control record.
        
        This implements the logic from BCHCTL00's 4000-PROCESS-TERMINATE
        paragraph.
        
        Args:
            return_code: Final return code
            error_desc: Error description if applicable
            
        Returns:
            Self for method chaining
        """
        if self.process_control:
            self.process_control.complete()
        
        self.return_info.return_code = return_code
        self.return_info.error_desc = error_desc[:80] if error_desc else ''
        
        if return_code <= ReturnCode.WARNING:
            self.status = BatchStatus.DONE
            self.statistics.record_completion()
        else:
            self.status = BatchStatus.ERROR
        
        return self
    
    def add_prerequisite(self, job_name: str, sequence_no: int = 1,
                         max_rc: int = ReturnCode.WARNING) -> 'BatchControlRecord':
        """
        Add a prerequisite job.
        
        Args:
            job_name: Name of the prerequisite job
            sequence_no: Sequence number of the prerequisite
            max_rc: Maximum acceptable return code
            
        Returns:
            Self for method chaining
        """
        self.prerequisites.append(BatchPrerequisite(
            prereq_name=job_name,
            prereq_seq=sequence_no,
            prereq_rc=max_rc
        ))
        return self
    
    @property
    def is_ready(self) -> bool:
        """Check if job is ready to run."""
        return self.status == BatchStatus.READY
    
    @property
    def is_active(self) -> bool:
        """Check if job is currently running."""
        return self.status == BatchStatus.ACTIVE
    
    @property
    def is_complete(self) -> bool:
        """Check if job has completed (successfully or with error)."""
        return self.status in (BatchStatus.DONE, BatchStatus.ERROR)
    
    @property
    def is_successful(self) -> bool:
        """Check if job completed successfully."""
        return self.status == BatchStatus.DONE and self.return_info.is_success
    
    @classmethod
    def create(cls, job_name: str, process_date: Optional[str] = None,
               sequence_no: int = 1) -> 'BatchControlRecord':
        """
        Create a new batch control record.
        
        Args:
            job_name: Name of the job
            process_date: Processing date (defaults to today)
            sequence_no: Sequence number
            
        Returns:
            New BatchControlRecord
        """
        if process_date is None:
            process_date = datetime.now().strftime("%Y%m%d")
        
        key = BatchControlKey(
            job_name=job_name,
            process_date=process_date,
            sequence_no=sequence_no
        )
        return cls(key=key)
    
    @classmethod
    def from_dict(cls, d: dict) -> 'BatchControlRecord':
        """Create BatchControlRecord from dictionary."""
        key = BatchControlKey(
            job_name=d['job_name'],
            process_date=d['process_date'],
            sequence_no=d['sequence_no']
        )
        
        process_control = None
        if 'step_name' in d and d['step_name']:
            process_control = BatchProcessControl(
                step_name=d['step_name'],
                program_name=d.get('program_name', ''),
                start_time=datetime.fromisoformat(d['start_time']) if d.get('start_time') else None,
                end_time=datetime.fromisoformat(d['end_time']) if d.get('end_time') else None
            )
        
        prerequisites = []
        for prereq in d.get('prerequisites', []):
            prerequisites.append(BatchPrerequisite(
                prereq_name=prereq['prereq_name'],
                prereq_seq=prereq['prereq_seq'],
                prereq_rc=prereq.get('prereq_rc', ReturnCode.WARNING)
            ))
        
        return cls(
            key=key,
            status=BatchStatus(d.get('status', 'R')),
            process_control=process_control,
            prerequisites=prerequisites,
            return_info=BatchReturnInfo(
                return_code=d.get('return_code', 0),
                error_desc=d.get('error_desc', '')
            ),
            statistics=BatchStatistics(
                restart_count=d.get('restart_count', 0),
                attempt_timestamp=datetime.fromisoformat(d['attempt_timestamp']) 
                    if d.get('attempt_timestamp') else None,
                complete_timestamp=datetime.fromisoformat(d['complete_timestamp'])
                    if d.get('complete_timestamp') else None
            )
        )
    
    def to_dict(self) -> dict:
        """Convert BatchControlRecord to dictionary for serialization."""
        result = {
            'job_name': self.key.job_name.strip(),
            'process_date': self.key.process_date,
            'sequence_no': self.key.sequence_no,
            'status': self.status.value,
            'return_code': self.return_info.return_code,
            'error_desc': self.return_info.error_desc,
            'restart_count': self.statistics.restart_count,
        }
        
        if self.process_control:
            result['step_name'] = self.process_control.step_name.strip()
            result['program_name'] = self.process_control.program_name.strip()
            result['start_time'] = self.process_control.start_time.isoformat() \
                if self.process_control.start_time else None
            result['end_time'] = self.process_control.end_time.isoformat() \
                if self.process_control.end_time else None
        
        if self.statistics.attempt_timestamp:
            result['attempt_timestamp'] = self.statistics.attempt_timestamp.isoformat()
        if self.statistics.complete_timestamp:
            result['complete_timestamp'] = self.statistics.complete_timestamp.isoformat()
        
        result['prerequisites'] = [
            {
                'prereq_name': p.prereq_name.strip(),
                'prereq_seq': p.prereq_seq,
                'prereq_rc': p.prereq_rc
            }
            for p in self.prerequisites
        ]
        
        return result

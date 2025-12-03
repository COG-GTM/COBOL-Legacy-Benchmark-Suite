"""
Batch Control Record Model

Corresponds to COBOL copybooks: BCHCTL.cpy and BCHCON.cpy
Defines the structure for batch job control and process sequencing.
"""

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import List, Optional


class BatchStatus(Enum):
    """Batch status codes matching COBOL 88-level conditions in BCHCTL.cpy."""
    READY = "R"
    ACTIVE = "A"
    WAITING = "W"
    DONE = "D"
    ERROR = "E"


class ReturnCode(Enum):
    """Return code thresholds matching COBOL constants in BCHCON.cpy."""
    SUCCESS = 0
    WARNING = 4
    ERROR = 8
    SEVERE = 12
    CRITICAL = 16


class ProcessType(Enum):
    """Process type codes matching COBOL constants in BCHCON.cpy."""
    INITIAL = "INI"
    UPDATE = "UPD"
    REPORT = "RPT"
    CLEANUP = "CLN"


class DependencyType(Enum):
    """Dependency type codes matching COBOL constants in BCHCON.cpy."""
    REQUIRED = "R"
    OPTIONAL = "O"
    EXCLUSIVE = "X"


@dataclass
class PrerequisiteJob:
    """
    Prerequisite job definition.
    
    Corresponds to BCT-PREREQ-JOBS in BCHCTL.cpy:
    - BCT-PREREQ-NAME: PIC X(8)
    - BCT-PREREQ-SEQ: PIC 9(4)
    - BCT-PREREQ-RC: PIC S9(4) COMP
    """
    name: str
    sequence: int = 0
    max_return_code: int = 0

    def __post_init__(self) -> None:
        self.name = str(self.name).ljust(8)[:8]


@dataclass
class BatchControlKey:
    """
    Batch control key structure.
    
    Corresponds to BCT-KEY in BCHCTL.cpy:
    - BCT-JOB-NAME: PIC X(8)
    - BCT-PROCESS-DATE: PIC X(8)
    - BCT-SEQUENCE-NO: PIC 9(4)
    """
    job_name: str
    process_date: str  # YYYYMMDD format
    sequence_no: int = 0

    def __post_init__(self) -> None:
        self.job_name = str(self.job_name).ljust(8)[:8]
        self.process_date = str(self.process_date).ljust(8)[:8]

    def to_string(self) -> str:
        """Convert key to string for comparison and storage."""
        return f"{self.job_name}{self.process_date}{self.sequence_no:04d}"

    @classmethod
    def from_string(cls, key_string: str) -> "BatchControlKey":
        """Parse key from string representation."""
        return cls(
            job_name=key_string[0:8],
            process_date=key_string[8:16],
            sequence_no=int(key_string[16:20]),
        )


@dataclass
class ProcessControl:
    """
    Process control structure.
    
    Corresponds to BCT-PROCESS-CONTROL in BCHCTL.cpy:
    - BCT-STEP-NAME: PIC X(8)
    - BCT-PROGRAM-NAME: PIC X(8)
    - BCT-START-TIME: PIC X(8)
    - BCT-END-TIME: PIC X(8)
    """
    step_name: str = ""
    program_name: str = ""
    start_time: str = ""
    end_time: str = ""

    def __post_init__(self) -> None:
        self.step_name = str(self.step_name).ljust(8)[:8]
        self.program_name = str(self.program_name).ljust(8)[:8]
        self.start_time = str(self.start_time).ljust(8)[:8]
        self.end_time = str(self.end_time).ljust(8)[:8]


@dataclass
class Dependencies:
    """
    Dependencies structure.
    
    Corresponds to BCT-DEPENDENCIES in BCHCTL.cpy:
    - BCT-PREREQ-COUNT: PIC 9(2) COMP
    - BCT-PREREQ-JOBS: OCCURS 10 TIMES
    """
    prereq_count: int = 0
    prereq_jobs: List[PrerequisiteJob] = field(default_factory=list)

    def add_prerequisite(
        self, name: str, sequence: int = 0, max_return_code: int = 0
    ) -> None:
        """Add a prerequisite job."""
        if self.prereq_count >= 10:
            raise ValueError("Maximum 10 prerequisites allowed")
        self.prereq_jobs.append(
            PrerequisiteJob(name=name, sequence=sequence, max_return_code=max_return_code)
        )
        self.prereq_count += 1


@dataclass
class ReturnInfo:
    """
    Return information structure.
    
    Corresponds to BCT-RETURN-INFO in BCHCTL.cpy:
    - BCT-RETURN-CODE: PIC S9(4) COMP
    - BCT-ERROR-DESC: PIC X(80)
    """
    return_code: int = 0
    error_desc: str = ""

    def __post_init__(self) -> None:
        self.error_desc = str(self.error_desc).ljust(80)[:80]


@dataclass
class Statistics:
    """
    Statistics structure.
    
    Corresponds to BCT-STATISTICS in BCHCTL.cpy:
    - BCT-RESTART-COUNT: PIC 9(2) COMP
    - BCT-ATTEMPT-TS: PIC X(26)
    - BCT-COMPLETE-TS: PIC X(26)
    """
    restart_count: int = 0
    attempt_ts: str = ""
    complete_ts: str = ""

    def __post_init__(self) -> None:
        self.attempt_ts = str(self.attempt_ts).ljust(26)[:26]
        self.complete_ts = str(self.complete_ts).ljust(26)[:26]


@dataclass
class BatchControlRecord:
    """
    Complete batch control record structure.
    
    Corresponds to BATCH-CONTROL-RECORD in BCHCTL.cpy.
    """
    key: BatchControlKey
    status: BatchStatus = BatchStatus.READY
    process_control: ProcessControl = field(default_factory=ProcessControl)
    dependencies: Dependencies = field(default_factory=Dependencies)
    return_info: ReturnInfo = field(default_factory=ReturnInfo)
    statistics: Statistics = field(default_factory=Statistics)
    filler: str = ""

    def __post_init__(self) -> None:
        self.filler = " " * 50
        if isinstance(self.status, str):
            self.status = BatchStatus(self.status)

    @property
    def job_name(self) -> str:
        return self.key.job_name

    @property
    def process_date(self) -> str:
        return self.key.process_date

    @property
    def sequence_no(self) -> int:
        return self.key.sequence_no

    @property
    def return_code(self) -> int:
        return self.return_info.return_code

    @return_code.setter
    def return_code(self, value: int) -> None:
        self.return_info.return_code = value

    def is_ready(self) -> bool:
        return self.status == BatchStatus.READY

    def is_active(self) -> bool:
        return self.status == BatchStatus.ACTIVE

    def is_waiting(self) -> bool:
        return self.status == BatchStatus.WAITING

    def is_done(self) -> bool:
        return self.status == BatchStatus.DONE

    def is_error(self) -> bool:
        return self.status == BatchStatus.ERROR

    def start_processing(self, program_name: str = "") -> None:
        """Mark the job as active and record start time."""
        self.status = BatchStatus.ACTIVE
        self.process_control.program_name = program_name
        self.process_control.start_time = datetime.now().strftime("%H%M%S%f")[:8]
        self.statistics.attempt_ts = datetime.now().isoformat()

    def complete_processing(self, return_code: int = 0, error_desc: str = "") -> None:
        """Mark the job as complete and record end time."""
        self.status = BatchStatus.DONE if return_code <= 4 else BatchStatus.ERROR
        self.process_control.end_time = datetime.now().strftime("%H%M%S%f")[:8]
        self.return_info.return_code = return_code
        self.return_info.error_desc = error_desc
        self.statistics.complete_ts = datetime.now().isoformat()

    def increment_restart(self) -> bool:
        """
        Increment restart count and check if max restarts exceeded.
        Returns True if restart is allowed, False otherwise.
        """
        self.statistics.restart_count += 1
        return self.statistics.restart_count <= 3

    def check_prerequisites(
        self, completed_jobs: dict[str, int]
    ) -> tuple[bool, str]:
        """
        Check if all prerequisites are satisfied.
        
        Args:
            completed_jobs: Dict mapping job names to their return codes
            
        Returns:
            Tuple of (satisfied, message)
        """
        for prereq in self.dependencies.prereq_jobs:
            prereq_name = prereq.name.strip()
            if prereq_name not in completed_jobs:
                return False, f"Prerequisite {prereq_name} not completed"
            if completed_jobs[prereq_name] > prereq.max_return_code:
                return (
                    False,
                    f"Prerequisite {prereq_name} return code "
                    f"{completed_jobs[prereq_name]} exceeds max {prereq.max_return_code}",
                )
        return True, "All prerequisites satisfied"

    def to_dict(self) -> dict:
        """Convert record to dictionary for serialization."""
        return {
            "key": {
                "job_name": self.key.job_name,
                "process_date": self.key.process_date,
                "sequence_no": self.key.sequence_no,
            },
            "status": self.status.value,
            "process_control": {
                "step_name": self.process_control.step_name,
                "program_name": self.process_control.program_name,
                "start_time": self.process_control.start_time,
                "end_time": self.process_control.end_time,
            },
            "dependencies": {
                "prereq_count": self.dependencies.prereq_count,
                "prereq_jobs": [
                    {
                        "name": p.name,
                        "sequence": p.sequence,
                        "max_return_code": p.max_return_code,
                    }
                    for p in self.dependencies.prereq_jobs
                ],
            },
            "return_info": {
                "return_code": self.return_info.return_code,
                "error_desc": self.return_info.error_desc,
            },
            "statistics": {
                "restart_count": self.statistics.restart_count,
                "attempt_ts": self.statistics.attempt_ts,
                "complete_ts": self.statistics.complete_ts,
            },
        }

    @classmethod
    def from_dict(cls, data: dict) -> "BatchControlRecord":
        """Create record from dictionary."""
        deps = Dependencies(prereq_count=data["dependencies"]["prereq_count"])
        for p in data["dependencies"]["prereq_jobs"]:
            deps.prereq_jobs.append(
                PrerequisiteJob(
                    name=p["name"],
                    sequence=p["sequence"],
                    max_return_code=p["max_return_code"],
                )
            )
        
        return cls(
            key=BatchControlKey(
                job_name=data["key"]["job_name"],
                process_date=data["key"]["process_date"],
                sequence_no=data["key"]["sequence_no"],
            ),
            status=BatchStatus(data["status"]),
            process_control=ProcessControl(
                step_name=data["process_control"]["step_name"],
                program_name=data["process_control"]["program_name"],
                start_time=data["process_control"]["start_time"],
                end_time=data["process_control"]["end_time"],
            ),
            dependencies=deps,
            return_info=ReturnInfo(
                return_code=data["return_info"]["return_code"],
                error_desc=data["return_info"]["error_desc"],
            ),
            statistics=Statistics(
                restart_count=data["statistics"]["restart_count"],
                attempt_ts=data["statistics"]["attempt_ts"],
                complete_ts=data["statistics"]["complete_ts"],
            ),
        )

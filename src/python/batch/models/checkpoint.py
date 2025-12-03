"""
Checkpoint/Restart Control Model

Corresponds to COBOL copybook: CKPRST.cpy
Defines the structure for checkpoint/restart processing in batch programs.
"""

from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import List, Optional


class CheckpointStatus(Enum):
    """Checkpoint status codes matching COBOL 88-level conditions."""
    INITIAL = "I"
    ACTIVE = "A"
    COMPLETE = "C"
    FAILED = "F"
    RESTARTED = "R"


class CheckpointPhase(Enum):
    """Checkpoint phase codes matching COBOL 88-level conditions."""
    INIT = "00"
    READ = "10"
    PROCESS = "20"
    UPDATE = "30"
    TERMINATE = "40"


class RestartMode(Enum):
    """Restart mode codes matching COBOL 88-level conditions."""
    NORMAL = "N"
    RESTART = "R"
    RECOVER = "C"


@dataclass
class CheckpointHeader:
    """
    Checkpoint header structure.
    
    Corresponds to CK-HEADER in CKPRST.cpy:
    - CK-PROGRAM-ID: PIC X(8)
    - CK-RUN-DATE: PIC X(8)
    - CK-RUN-TIME: PIC X(6)
    - CK-STATUS: PIC X(1)
    """
    program_id: str
    run_date: str  # YYYYMMDD format
    run_time: str  # HHMMSS format
    status: CheckpointStatus = CheckpointStatus.INITIAL

    def __post_init__(self) -> None:
        self.program_id = str(self.program_id).ljust(8)[:8]
        self.run_date = str(self.run_date).ljust(8)[:8]
        self.run_time = str(self.run_time).ljust(6)[:6]
        if isinstance(self.status, str):
            self.status = CheckpointStatus(self.status)


@dataclass
class CheckpointCounters:
    """
    Checkpoint counters structure.
    
    Corresponds to CK-COUNTERS in CKPRST.cpy:
    - CK-RECORDS-READ: PIC 9(9) COMP
    - CK-RECORDS-PROC: PIC 9(9) COMP
    - CK-RECORDS-ERROR: PIC 9(9) COMP
    - CK-RESTART-COUNT: PIC 9(2) COMP
    """
    records_read: int = 0
    records_processed: int = 0
    records_error: int = 0
    restart_count: int = 0


@dataclass
class CheckpointPosition:
    """
    Checkpoint position structure.
    
    Corresponds to CK-POSITION in CKPRST.cpy:
    - CK-LAST-KEY: PIC X(50)
    - CK-LAST-TIME: PIC X(26)
    - CK-PHASE: PIC X(2)
    """
    last_key: str = ""
    last_time: str = ""
    phase: CheckpointPhase = CheckpointPhase.INIT

    def __post_init__(self) -> None:
        self.last_key = str(self.last_key).ljust(50)[:50]
        if not self.last_time:
            self.last_time = datetime.now().isoformat()
        self.last_time = str(self.last_time).ljust(26)[:26]
        if isinstance(self.phase, str):
            self.phase = CheckpointPhase(self.phase)


@dataclass
class FileStatus:
    """
    File status structure for checkpoint tracking.
    
    Corresponds to CK-FILE-STATUS in CKPRST.cpy:
    - CK-FILE-NAME: PIC X(8)
    - CK-FILE-POS: PIC X(50)
    - CK-FILE-STATUS: PIC X(2)
    """
    file_name: str
    file_pos: str = ""
    file_status: str = "00"

    def __post_init__(self) -> None:
        self.file_name = str(self.file_name).ljust(8)[:8]
        self.file_pos = str(self.file_pos).ljust(50)[:50]
        self.file_status = str(self.file_status).ljust(2)[:2]


@dataclass
class CheckpointResources:
    """
    Checkpoint resources structure.
    
    Corresponds to CK-RESOURCES in CKPRST.cpy:
    - CK-FILE-STATUS OCCURS 5 TIMES
    """
    file_statuses: List[FileStatus] = field(default_factory=list)

    def __post_init__(self) -> None:
        while len(self.file_statuses) < 5:
            self.file_statuses.append(FileStatus(file_name=""))

    def set_file_status(
        self, index: int, file_name: str, file_pos: str = "", status: str = "00"
    ) -> None:
        """Set file status at given index (0-4)."""
        if 0 <= index < 5:
            self.file_statuses[index] = FileStatus(
                file_name=file_name, file_pos=file_pos, file_status=status
            )

    def get_file_status(self, file_name: str) -> Optional[FileStatus]:
        """Get file status by name."""
        for fs in self.file_statuses:
            if fs.file_name.strip() == file_name.strip():
                return fs
        return None


@dataclass
class CheckpointControlInfo:
    """
    Checkpoint control information structure.
    
    Corresponds to CK-CONTROL-INFO in CKPRST.cpy:
    - CK-COMMIT-FREQ: PIC 9(5) COMP VALUE 1000
    - CK-MAX-ERRORS: PIC 9(3) COMP VALUE 100
    - CK-MAX-RESTARTS: PIC 9(2) COMP VALUE 3
    - CK-RESTART-MODE: PIC X(1)
    """
    commit_freq: int = 1000
    max_errors: int = 100
    max_restarts: int = 3
    restart_mode: RestartMode = RestartMode.NORMAL

    def __post_init__(self) -> None:
        if isinstance(self.restart_mode, str):
            self.restart_mode = RestartMode(self.restart_mode)


@dataclass
class CheckpointControl:
    """
    Complete checkpoint control structure.
    
    Corresponds to CHECKPOINT-CONTROL in CKPRST.cpy.
    """
    header: CheckpointHeader
    counters: CheckpointCounters = field(default_factory=CheckpointCounters)
    position: CheckpointPosition = field(default_factory=CheckpointPosition)
    resources: CheckpointResources = field(default_factory=CheckpointResources)
    control_info: CheckpointControlInfo = field(default_factory=CheckpointControlInfo)

    @property
    def program_id(self) -> str:
        return self.header.program_id

    @property
    def run_date(self) -> str:
        return self.header.run_date

    @property
    def run_time(self) -> str:
        return self.header.run_time

    @property
    def status(self) -> CheckpointStatus:
        return self.header.status

    @status.setter
    def status(self, value: CheckpointStatus) -> None:
        self.header.status = value

    @property
    def records_read(self) -> int:
        return self.counters.records_read

    @records_read.setter
    def records_read(self, value: int) -> None:
        self.counters.records_read = value

    @property
    def records_processed(self) -> int:
        return self.counters.records_processed

    @records_processed.setter
    def records_processed(self, value: int) -> None:
        self.counters.records_processed = value

    @property
    def records_error(self) -> int:
        return self.counters.records_error

    @records_error.setter
    def records_error(self, value: int) -> None:
        self.counters.records_error = value

    @property
    def last_key(self) -> str:
        return self.position.last_key

    @last_key.setter
    def last_key(self, value: str) -> None:
        self.position.last_key = str(value).ljust(50)[:50]

    @property
    def phase(self) -> CheckpointPhase:
        return self.position.phase

    @phase.setter
    def phase(self, value: CheckpointPhase) -> None:
        self.position.phase = value

    @property
    def commit_freq(self) -> int:
        return self.control_info.commit_freq

    @property
    def max_errors(self) -> int:
        return self.control_info.max_errors

    @property
    def max_restarts(self) -> int:
        return self.control_info.max_restarts

    @property
    def restart_mode(self) -> RestartMode:
        return self.control_info.restart_mode

    @restart_mode.setter
    def restart_mode(self, value: RestartMode) -> None:
        self.control_info.restart_mode = value

    def is_initial(self) -> bool:
        return self.header.status == CheckpointStatus.INITIAL

    def is_active(self) -> bool:
        return self.header.status == CheckpointStatus.ACTIVE

    def is_complete(self) -> bool:
        return self.header.status == CheckpointStatus.COMPLETE

    def is_failed(self) -> bool:
        return self.header.status == CheckpointStatus.FAILED

    def is_restarted(self) -> bool:
        return self.header.status == CheckpointStatus.RESTARTED

    def is_restart_mode(self) -> bool:
        return self.control_info.restart_mode == RestartMode.RESTART

    def should_checkpoint(self) -> bool:
        """Check if a checkpoint should be taken based on commit frequency."""
        return self.counters.records_processed % self.control_info.commit_freq == 0

    def can_restart(self) -> bool:
        """Check if restart is allowed based on max restarts."""
        return self.counters.restart_count < self.control_info.max_restarts

    def has_exceeded_errors(self) -> bool:
        """Check if error count has exceeded maximum."""
        return self.counters.records_error > self.control_info.max_errors

    def increment_read(self) -> None:
        """Increment records read counter."""
        self.counters.records_read += 1

    def increment_processed(self) -> None:
        """Increment records processed counter."""
        self.counters.records_processed += 1

    def increment_error(self) -> None:
        """Increment error counter."""
        self.counters.records_error += 1

    def increment_restart(self) -> bool:
        """
        Increment restart counter and check if restart is allowed.
        Returns True if restart is allowed, False otherwise.
        """
        self.counters.restart_count += 1
        return self.can_restart()

    def start_processing(self) -> None:
        """Mark checkpoint as active and set phase to process."""
        self.header.status = CheckpointStatus.ACTIVE
        self.position.phase = CheckpointPhase.PROCESS
        self.position.last_time = datetime.now().isoformat()

    def complete_processing(self) -> None:
        """Mark checkpoint as complete."""
        self.header.status = CheckpointStatus.COMPLETE
        self.position.phase = CheckpointPhase.TERMINATE
        self.position.last_time = datetime.now().isoformat()

    def fail_processing(self) -> None:
        """Mark checkpoint as failed."""
        self.header.status = CheckpointStatus.FAILED
        self.position.last_time = datetime.now().isoformat()

    def prepare_restart(self) -> None:
        """Prepare for restart processing."""
        self.header.status = CheckpointStatus.RESTARTED
        self.control_info.restart_mode = RestartMode.RESTART
        self.position.last_time = datetime.now().isoformat()

    def take_checkpoint(self, last_key: str) -> None:
        """Take a checkpoint with the given last key."""
        self.position.last_key = str(last_key).ljust(50)[:50]
        self.position.last_time = datetime.now().isoformat()

    def to_dict(self) -> dict:
        """Convert control structure to dictionary for serialization."""
        return {
            "header": {
                "program_id": self.header.program_id,
                "run_date": self.header.run_date,
                "run_time": self.header.run_time,
                "status": self.header.status.value,
            },
            "counters": {
                "records_read": self.counters.records_read,
                "records_processed": self.counters.records_processed,
                "records_error": self.counters.records_error,
                "restart_count": self.counters.restart_count,
            },
            "position": {
                "last_key": self.position.last_key,
                "last_time": self.position.last_time,
                "phase": self.position.phase.value,
            },
            "resources": {
                "file_statuses": [
                    {
                        "file_name": fs.file_name,
                        "file_pos": fs.file_pos,
                        "file_status": fs.file_status,
                    }
                    for fs in self.resources.file_statuses
                ]
            },
            "control_info": {
                "commit_freq": self.control_info.commit_freq,
                "max_errors": self.control_info.max_errors,
                "max_restarts": self.control_info.max_restarts,
                "restart_mode": self.control_info.restart_mode.value,
            },
        }

    @classmethod
    def from_dict(cls, data: dict) -> "CheckpointControl":
        """Create control structure from dictionary."""
        resources = CheckpointResources()
        for i, fs_data in enumerate(data["resources"]["file_statuses"]):
            if i < 5:
                resources.file_statuses[i] = FileStatus(
                    file_name=fs_data["file_name"],
                    file_pos=fs_data["file_pos"],
                    file_status=fs_data["file_status"],
                )
        
        return cls(
            header=CheckpointHeader(
                program_id=data["header"]["program_id"],
                run_date=data["header"]["run_date"],
                run_time=data["header"]["run_time"],
                status=CheckpointStatus(data["header"]["status"]),
            ),
            counters=CheckpointCounters(
                records_read=data["counters"]["records_read"],
                records_processed=data["counters"]["records_processed"],
                records_error=data["counters"]["records_error"],
                restart_count=data["counters"]["restart_count"],
            ),
            position=CheckpointPosition(
                last_key=data["position"]["last_key"],
                last_time=data["position"]["last_time"],
                phase=CheckpointPhase(data["position"]["phase"]),
            ),
            resources=resources,
            control_info=CheckpointControlInfo(
                commit_freq=data["control_info"]["commit_freq"],
                max_errors=data["control_info"]["max_errors"],
                max_restarts=data["control_info"]["max_restarts"],
                restart_mode=RestartMode(data["control_info"]["restart_mode"]),
            ),
        )

    @classmethod
    def create_new(
        cls,
        program_id: str,
        commit_freq: int = 1000,
        max_errors: int = 100,
        max_restarts: int = 3,
    ) -> "CheckpointControl":
        """Factory method to create a new checkpoint control structure."""
        now = datetime.now()
        return cls(
            header=CheckpointHeader(
                program_id=program_id,
                run_date=now.strftime("%Y%m%d"),
                run_time=now.strftime("%H%M%S"),
                status=CheckpointStatus.INITIAL,
            ),
            counters=CheckpointCounters(),
            position=CheckpointPosition(
                last_key="",
                last_time=now.isoformat(),
                phase=CheckpointPhase.INIT,
            ),
            resources=CheckpointResources(),
            control_info=CheckpointControlInfo(
                commit_freq=commit_freq,
                max_errors=max_errors,
                max_restarts=max_restarts,
                restart_mode=RestartMode.NORMAL,
            ),
        )

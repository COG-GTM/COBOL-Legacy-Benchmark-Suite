"""Process Sequence Manager - migrated from PRCSEQ00.cbl.

Manages process sequencing, dependency resolution, and status tracking.
"""

import logging
from typing import Optional

from portfolio_management.models.process_sequence import (
    ProcessSequenceRecord,
    DependencyStrength,
)
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "PRCSEQ00"


class ProcessStatus:
    READY = "R"
    RUNNING = "X"
    COMPLETE = "C"
    FAILED = "F"
    WAITING = "W"
    SKIPPED = "S"


class ProcessSequenceManager:
    def __init__(self):
        self._sequences: dict[str, ProcessSequenceRecord] = {}
        self._process_status: dict[str, str] = {}
        self._process_rc: dict[str, int] = {}

    def initialize(self) -> int:
        self._sequences.clear()
        self._process_status.clear()
        self._process_rc.clear()
        logger.info("Process Sequence Manager initialized")
        return ReturnCode.SUCCESS

    def load_sequences(self, file_path: str) -> int:
        try:
            with open(file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue
                    parts = line.split("|")
                    if len(parts) >= 4:
                        record = ProcessSequenceRecord(
                            process_id=parts[0].strip(),
                            version=int(parts[1].strip()),
                            description=parts[2].strip(),
                            program=parts[3].strip(),
                        )
                        self._sequences[record.sequence_key] = record
                        self._process_status[record.process_id] = ProcessStatus.READY
            logger.info("Loaded %d process sequences", len(self._sequences))
            return ReturnCode.SUCCESS
        except FileNotFoundError:
            logger.warning("Sequence file not found: %s", file_path)
            return ReturnCode.WARNING
        except Exception as e:
            logger.error("Error loading sequences: %s", e)
            return ReturnCode.ERROR

    def add_sequence(self, record: ProcessSequenceRecord) -> int:
        key = record.sequence_key
        if key in self._sequences:
            logger.error("Sequence %s already exists", key)
            return ReturnCode.ERROR

        self._sequences[key] = record
        self._process_status[record.process_id] = ProcessStatus.READY
        return ReturnCode.SUCCESS

    def get_next_ready(self) -> Optional[ProcessSequenceRecord]:
        for seq in self._sequences.values():
            if self._process_status.get(seq.process_id) != ProcessStatus.READY:
                continue

            if self._check_dependencies(seq):
                return seq

        return None

    def _check_dependencies(self, sequence: ProcessSequenceRecord) -> bool:
        for i in range(sequence.dep_count):
            dep = sequence.dependencies[i]
            dep_status = self._process_status.get(dep.dep_id)

            if dep_status is None:
                if dep.dep_type == DependencyStrength.HARD:
                    return False
                continue

            if dep_status != ProcessStatus.COMPLETE:
                if dep.dep_type == DependencyStrength.HARD:
                    return False
                continue

            dep_rc = self._process_rc.get(dep.dep_id, 0)
            if dep_rc > dep.dep_rc and dep.dep_type == DependencyStrength.HARD:
                return False

        return True

    def mark_running(self, process_id: str) -> int:
        if process_id not in self._process_status:
            logger.error("Process %s not found", process_id)
            return ReturnCode.ERROR

        self._process_status[process_id] = ProcessStatus.RUNNING
        logger.info("Process %s marked as running", process_id)
        return ReturnCode.SUCCESS

    def mark_complete(self, process_id: str, return_code: int) -> int:
        if process_id not in self._process_status:
            logger.error("Process %s not found", process_id)
            return ReturnCode.ERROR

        self._process_status[process_id] = ProcessStatus.COMPLETE
        self._process_rc[process_id] = return_code
        logger.info("Process %s completed with RC=%d", process_id, return_code)
        return ReturnCode.SUCCESS

    def mark_failed(self, process_id: str, return_code: int) -> int:
        if process_id not in self._process_status:
            logger.error("Process %s not found", process_id)
            return ReturnCode.ERROR

        self._process_status[process_id] = ProcessStatus.FAILED
        self._process_rc[process_id] = return_code
        logger.error("Process %s failed with RC=%d", process_id, return_code)
        return ReturnCode.ERROR

    def get_status(self, process_id: str) -> Optional[str]:
        return self._process_status.get(process_id)

    def get_all_statuses(self) -> dict[str, str]:
        return dict(self._process_status)

    def terminate(self) -> int:
        completed = sum(1 for s in self._process_status.values() if s == ProcessStatus.COMPLETE)
        failed = sum(1 for s in self._process_status.values() if s == ProcessStatus.FAILED)
        logger.info(
            "Process Sequence Manager terminated - Completed: %d, Failed: %d",
            completed,
            failed,
        )
        return ReturnCode.SUCCESS

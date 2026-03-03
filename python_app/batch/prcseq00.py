"""Process Sequence Manager - replaces PRCSEQ00.cbl.

Manages process dependencies, execution order, and status tracking
for the batch pipeline. Tracks which processes have run and their
return codes to determine if dependent processes can execute.

COBOL program flow (EVALUATE LS-PSQ-FUNCTION):
- INIT: Initialize process sequence table
- SEQC: Sequence check - verify process can run
- UPDT: Update process status after execution
- STAT: Get sequence status summary
- TERM: Terminate and clean up
"""

import logging
from datetime import datetime
from enum import StrEnum
from typing import Any

logger = logging.getLogger("portfolio.batch.prcseq00")


class ProcessStatus(StrEnum):
    """Process status codes matching COBOL PSQ-PROC-STATUS."""

    PENDING = "P"
    RUNNING = "R"
    COMPLETE = "C"
    ERROR = "E"
    SKIPPED = "S"
    BYPASSED = "B"


class ProcessEntry:
    """A single process in the sequence table."""

    def __init__(
        self,
        process_name: str,
        sequence_no: int,
        dependencies: list[str] | None = None,
    ) -> None:
        self.process_name = process_name
        self.sequence_no = sequence_no
        self.dependencies = dependencies or []
        self.status = ProcessStatus.PENDING
        self.return_code = 0
        self.start_time = ""
        self.end_time = ""
        self.error_desc = ""


class ProcessSequenceManager:
    """Process sequence manager replacing PRCSEQ00.cbl.

    Manages the ordered execution of batch processes with dependency
    checking. Each process can depend on zero or more predecessor
    processes that must complete with RC <= 4.
    """

    def __init__(self) -> None:
        self.processes: dict[str, ProcessEntry] = {}
        self.sequence_order: list[str] = []
        self.initialized = False

    def initialize(self, process_definitions: list[dict[str, Any]]) -> None:
        """Initialize process sequence table - replaces P100-INITIALIZE.

        COBOL: Reads PRCCTL file and builds WS-SEQ-TABLE with up to
        50 entries (WS-MAX-PROCESSES = 50).

        Args:
            process_definitions: List of dicts with keys:
                - name: Process name (8 chars)
                - sequence: Sequence number
                - dependencies: List of prerequisite process names
        """
        self.processes.clear()
        self.sequence_order.clear()

        for defn in sorted(process_definitions, key=lambda d: d.get("sequence", 0)):
            name = defn["name"]
            entry = ProcessEntry(
                process_name=name,
                sequence_no=defn.get("sequence", 0),
                dependencies=defn.get("dependencies", []),
            )
            self.processes[name] = entry
            self.sequence_order.append(name)

        self.initialized = True
        logger.info(
            "PRCSEQ00 INIT: %d processes loaded - %s",
            len(self.processes),
            ", ".join(self.sequence_order),
        )

    def sequence_check(self, process_name: str) -> dict[str, Any]:
        """Check if a process can run - replaces P200-SEQUENCE-CHECK.

        COBOL: Verifies all dependencies are COMPLETE with RC <= 4.
        Returns whether the process can proceed.
        """
        entry = self.processes.get(process_name)
        if entry is None:
            return {"can_run": False, "reason": f"Process {process_name} not found"}

        if entry.status == ProcessStatus.COMPLETE:
            return {"can_run": False, "reason": "Process already completed"}

        if entry.status == ProcessStatus.RUNNING:
            return {"can_run": False, "reason": "Process already running"}

        # Check all dependencies
        failed_deps: list[str] = []
        pending_deps: list[str] = []

        for dep_name in entry.dependencies:
            dep = self.processes.get(dep_name)
            if dep is None:
                failed_deps.append(f"{dep_name}(NOT_FOUND)")
            elif dep.status == ProcessStatus.COMPLETE and dep.return_code <= 4:
                continue  # Dependency satisfied
            elif dep.status == ProcessStatus.COMPLETE and dep.return_code > 4:
                failed_deps.append(f"{dep_name}(RC={dep.return_code})")
            elif dep.status == ProcessStatus.ERROR:
                failed_deps.append(f"{dep_name}(ERROR)")
            elif dep.status in (ProcessStatus.SKIPPED, ProcessStatus.BYPASSED):
                continue  # Dependency bypassed, treat as satisfied
            else:
                pending_deps.append(dep_name)

        if failed_deps:
            return {
                "can_run": False,
                "reason": f"Failed dependencies: {', '.join(failed_deps)}",
            }
        if pending_deps:
            return {
                "can_run": False,
                "reason": f"Pending dependencies: {', '.join(pending_deps)}",
            }

        return {"can_run": True, "reason": "All dependencies satisfied"}

    def update_status(
        self,
        process_name: str,
        status: ProcessStatus,
        return_code: int = 0,
        error_desc: str = "",
    ) -> bool:
        """Update process status - replaces P300-UPDATE-STATUS.

        COBOL: Updates PSQ-PROC-STATUS and PSQ-PROC-RC.
        """
        entry = self.processes.get(process_name)
        if entry is None:
            logger.warning("PRCSEQ00 UPDT: process %s not found", process_name)
            return False

        entry.status = status
        entry.return_code = return_code
        entry.error_desc = error_desc

        if status == ProcessStatus.RUNNING:
            entry.start_time = datetime.now().isoformat()
        elif status in (ProcessStatus.COMPLETE, ProcessStatus.ERROR):
            entry.end_time = datetime.now().isoformat()

        logger.info(
            "PRCSEQ00 UPDT: process=%s, status=%s, rc=%d",
            process_name, status, return_code,
        )
        return True

    def get_status(self) -> dict[str, Any]:
        """Get sequence status summary - replaces P400-GET-STATUS."""
        total = len(self.processes)
        complete = sum(1 for p in self.processes.values() if p.status == ProcessStatus.COMPLETE)
        errors = sum(1 for p in self.processes.values() if p.status == ProcessStatus.ERROR)
        pending = sum(1 for p in self.processes.values() if p.status == ProcessStatus.PENDING)
        running = sum(1 for p in self.processes.values() if p.status == ProcessStatus.RUNNING)

        return {
            "total": total,
            "complete": complete,
            "errors": errors,
            "pending": pending,
            "running": running,
            "all_complete": complete == total,
            "has_errors": errors > 0,
            "processes": {
                name: {
                    "status": entry.status,
                    "return_code": entry.return_code,
                    "sequence": entry.sequence_no,
                }
                for name, entry in self.processes.items()
            },
        }

    def terminate(self) -> dict[str, Any]:
        """Terminate and return final summary - replaces P500-TERMINATE."""
        status = self.get_status()
        logger.info(
            "PRCSEQ00 TERM: total=%d, complete=%d, errors=%d",
            status["total"], status["complete"], status["errors"],
        )
        return status

    def get_next_process(self) -> str | None:
        """Get the next process that can run (not in COBOL, convenience method)."""
        for name in self.sequence_order:
            entry = self.processes[name]
            if entry.status == ProcessStatus.PENDING:
                check = self.sequence_check(name)
                if check["can_run"]:
                    return name
        return None

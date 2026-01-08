"""Process Sequencer - converted from PRCSEQ00.cbl.

This module provides process sequencing functionality similar to
the COBOL PRCSEQ00 batch program.

COBOL Program Reference (PRCSEQ00.cbl):
- Manages process sequence definitions
- Determines execution order based on dependencies
- Supports different sequence types (daily, weekly, monthly)
"""

from typing import Optional

from sqlalchemy.orm import Session

from app.database.models import BatchControl, ProcessDependency, ProcessSequence
from app.models.error import ErrorCategory, ReturnCode
from app.utils.error_handler import ErrorHandler
from app.utils.logging import get_logger

logger = get_logger(__name__)


class ProcessSequencer:
    """Process Sequencer - replaces PRCSEQ00 batch program.

    This class manages process sequence definitions and determines
    the execution order of batch jobs based on dependencies.
    """

    PROGRAM_NAME = "PRCSEQ00"

    SEQUENCE_TYPES = {
        "DLY": "Daily",
        "WKY": "Weekly",
        "MTH": "Monthly",
        "EOD": "End of Day",
        "EOM": "End of Month",
        "EOY": "End of Year",
    }

    def __init__(self, db: Session):
        self.db = db
        self.error_handler = ErrorHandler(db, self.PROGRAM_NAME)
        self.return_code = ReturnCode.SUCCESS

    def define_process(
        self,
        process_id: str,
        process_date: str,
        sequence_type: str,
        sequence_order: int,
        description: str = "",
        is_restartable: bool = True,
        max_restarts: int = 3,
    ) -> Optional[ProcessSequence]:
        """Define a process in the sequence.

        Args:
            process_id: Process identifier
            process_date: Process date
            sequence_type: Type of sequence (DLY, WKY, MTH, etc.)
            sequence_order: Order within sequence
            description: Process description
            is_restartable: Whether process can be restarted
            max_restarts: Maximum restart attempts

        Returns:
            ProcessSequence record if successful
        """
        try:
            existing = (
                self.db.query(ProcessSequence)
                .filter(
                    ProcessSequence.process_id == process_id,
                    ProcessSequence.process_date == process_date,
                )
                .first()
            )

            if existing:
                existing.sequence_type = sequence_type
                existing.sequence_order = sequence_order
                existing.description = description
                existing.is_restartable = is_restartable
                existing.max_restarts = max_restarts
                self.db.commit()
                return existing

            process_seq = ProcessSequence(
                process_id=process_id,
                process_date=process_date,
                sequence_type=sequence_type,
                sequence_order=sequence_order,
                description=description,
                is_restartable=is_restartable,
                max_restarts=max_restarts,
                dep_count=0,
            )

            self.db.add(process_seq)
            self.db.commit()

            logger.info(f"Process {process_id} defined in sequence")
            return process_seq

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="PS01", category=ErrorCategory.SYSTEM
            )
            return None

    def add_dependency(
        self,
        process_id: str,
        process_date: str,
        dep_process_id: str,
        dep_sequence: int = 1,
        dep_max_rc: int = 4,
        is_hard_dependency: bool = True,
    ) -> bool:
        """Add a dependency to a process.

        Args:
            process_id: Process identifier
            process_date: Process date
            dep_process_id: Dependent process identifier
            dep_sequence: Dependency sequence number
            dep_max_rc: Maximum acceptable return code
            is_hard_dependency: Whether dependency is required

        Returns:
            True if dependency added successfully
        """
        try:
            existing = (
                self.db.query(ProcessDependency)
                .filter(
                    ProcessDependency.process_id == process_id,
                    ProcessDependency.process_date == process_date,
                    ProcessDependency.dep_process_id == dep_process_id,
                )
                .first()
            )

            if existing:
                existing.dep_sequence = dep_sequence
                existing.dep_max_rc = dep_max_rc
                existing.is_hard_dependency = is_hard_dependency
            else:
                dependency = ProcessDependency(
                    process_id=process_id,
                    process_date=process_date,
                    dep_process_id=dep_process_id,
                    dep_sequence=dep_sequence,
                    dep_max_rc=dep_max_rc,
                    is_hard_dependency=is_hard_dependency,
                )
                self.db.add(dependency)

                process_seq = (
                    self.db.query(ProcessSequence)
                    .filter(
                        ProcessSequence.process_id == process_id,
                        ProcessSequence.process_date == process_date,
                    )
                    .first()
                )
                if process_seq:
                    process_seq.dep_count += 1

            self.db.commit()
            logger.info(f"Dependency {dep_process_id} added to {process_id}")
            return True

        except Exception as e:
            self.db.rollback()
            self.error_handler.handle_error(
                e, code="PS02", category=ErrorCategory.SYSTEM
            )
            return False

    def get_execution_order(
        self, process_date: str, sequence_type: str = None
    ) -> list[dict]:
        """Get processes in execution order.

        Args:
            process_date: Process date
            sequence_type: Optional sequence type filter

        Returns:
            List of processes in execution order
        """
        query = self.db.query(ProcessSequence).filter(
            ProcessSequence.process_date == process_date
        )

        if sequence_type:
            query = query.filter(ProcessSequence.sequence_type == sequence_type)

        processes = query.order_by(ProcessSequence.sequence_order).all()

        result = []
        for proc in processes:
            dependencies = (
                self.db.query(ProcessDependency)
                .filter(
                    ProcessDependency.process_id == proc.process_id,
                    ProcessDependency.process_date == process_date,
                )
                .all()
            )

            result.append({
                "process_id": proc.process_id,
                "sequence_type": proc.sequence_type,
                "sequence_order": proc.sequence_order,
                "description": proc.description,
                "is_restartable": proc.is_restartable,
                "max_restarts": proc.max_restarts,
                "dependencies": [
                    {
                        "dep_process_id": dep.dep_process_id,
                        "dep_max_rc": dep.dep_max_rc,
                        "is_hard": dep.is_hard_dependency,
                    }
                    for dep in dependencies
                ],
            })

        return result

    def get_ready_processes(self, process_date: str) -> list[str]:
        """Get processes that are ready to run.

        Args:
            process_date: Process date

        Returns:
            List of process IDs ready to run
        """
        ready = []

        processes = (
            self.db.query(ProcessSequence)
            .filter(ProcessSequence.process_date == process_date)
            .order_by(ProcessSequence.sequence_order)
            .all()
        )

        for proc in processes:
            batch_control = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == proc.process_id,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            if batch_control and batch_control.status in ("D", "A"):
                continue

            if self._check_dependencies_met(proc.process_id, process_date):
                ready.append(proc.process_id)

        return ready

    def _check_dependencies_met(self, process_id: str, process_date: str) -> bool:
        """Check if all dependencies are met for a process."""
        dependencies = (
            self.db.query(ProcessDependency)
            .filter(
                ProcessDependency.process_id == process_id,
                ProcessDependency.process_date == process_date,
            )
            .all()
        )

        for dep in dependencies:
            batch_control = (
                self.db.query(BatchControl)
                .filter(
                    BatchControl.job_name == dep.dep_process_id,
                    BatchControl.process_date == process_date,
                )
                .first()
            )

            if not batch_control:
                if dep.is_hard_dependency:
                    return False
                continue

            if batch_control.status != "D":
                if dep.is_hard_dependency:
                    return False
                continue

            if batch_control.return_code > dep.dep_max_rc:
                if dep.is_hard_dependency:
                    return False

        return True

    def setup_daily_sequence(self, process_date: str) -> bool:
        """Set up standard daily processing sequence.

        Args:
            process_date: Process date

        Returns:
            True if setup successful
        """
        try:
            daily_jobs = [
                ("TRNVAL00", 10, "Transaction Validation"),
                ("POSUPD00", 20, "Position Update"),
                ("HISTLD00", 30, "History Load"),
                ("RPTGEN00", 40, "Report Generation"),
            ]

            for job_name, order, desc in daily_jobs:
                self.define_process(
                    process_id=job_name,
                    process_date=process_date,
                    sequence_type="DLY",
                    sequence_order=order,
                    description=desc,
                )

            self.add_dependency("POSUPD00", process_date, "TRNVAL00")
            self.add_dependency("HISTLD00", process_date, "POSUPD00")
            self.add_dependency("RPTGEN00", process_date, "HISTLD00")

            logger.info(f"Daily sequence set up for {process_date}")
            return True

        except Exception as e:
            self.error_handler.handle_error(
                e, code="PS03", category=ErrorCategory.SYSTEM
            )
            return False

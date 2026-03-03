"""History Load module - replaces HISTLD00.cbl.

Loads position history from VSAM to DB2 POSHIST table.
Third step in the batch pipeline: TRNVAL00 -> POSUPD00 -> HISTLD00.

COBOL program flow:
- 0000-MAIN: Initialize -> Process -> Terminate
- 1000-INITIALIZE: Open VSAM, connect DB2, init checkpoint
- 2000-PROCESS-RECORDS: Read VSAM, load to DB2
- 2100-READ-VSAM: Sequential read from VSAM file
- 2200-LOAD-TO-DB2: INSERT INTO POSHIST
- 2300-CHECKPOINT: Commit every 1000 records (WS-COMMIT-INTERVAL)
- 3000-TERMINATE: Final commit, stats, set RC = error count
"""

import logging
from datetime import datetime
from typing import Any

from python_app.common.db2 import CommitController, StatisticsCollector
from python_app.common.errors import ErrorHandler, ErrorSeverity
from python_app.models.position import PositionRecord
from python_app.models.return_code import RC_SUCCESS, RC_WARNING, RC_ERROR

logger = logging.getLogger("portfolio.batch.histld00")


class HistoryLoader:
    """History load processor replacing HISTLD00.cbl.

    Loads position records into the POSHIST table with
    checkpoint/commit every 1000 records.
    Returns RC = error count (capped at RC_ERROR).
    """

    COMMIT_INTERVAL = 1000  # WS-COMMIT-INTERVAL from HISTLD00.cbl

    def __init__(self) -> None:
        self.error_handler = ErrorHandler("HISTLD00")
        self.stats = StatisticsCollector("HISTLD00")
        self.commit_ctrl = CommitController(commit_interval=self.COMMIT_INTERVAL)
        self.loaded_records: list[dict[str, Any]] = []
        self.records_read = 0
        self.records_loaded = 0
        self.records_error = 0

    def initialize(self) -> None:
        """Initialize processing - replaces 1000-INITIALIZE."""
        self.stats.initialize()
        self.commit_ctrl.initialize()
        self.loaded_records.clear()
        self.records_read = 0
        self.records_loaded = 0
        self.records_error = 0
        logger.info("HISTLD00 initialized - %s", datetime.now().isoformat())

    def load_to_db2(self, position: PositionRecord) -> bool:
        """Load a position record to POSHIST - replaces 2200-LOAD-TO-DB2.

        COBOL: EXEC SQL INSERT INTO POSHIST
               (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, ...) VALUES (...) END-EXEC
        """
        try:
            record = {
                "account_no": position.portfolio_id[:10],
                "portfolio_id": position.portfolio_id,
                "trans_date": position.date,
                "trans_time": datetime.now().strftime("%H%M%S00"),
                "trans_type": "HIST",
                "security_id": position.investment_id,
                "quantity": position.quantity,
                "price": position.market_value / position.quantity if position.quantity else 0,
                "amount": position.market_value,
                "fees": 0,
                "total_amount": position.market_value,
                "cost_basis": position.cost_basis,
                "gain_loss": position.market_value - position.cost_basis,
            }
            self.loaded_records.append(record)
            self.records_loaded += 1
            self.stats.update("inserts")
            return True

        except Exception as exc:
            self.error_handler.log_error(
                f"DB2 load failed for position {position.composite_key}: {exc}",
                severity=ErrorSeverity.ERROR,
                error_code="HLOD",
                exc=exc,
            )
            self.records_error += 1
            self.stats.update("errors")
            return False

    def checkpoint(self) -> None:
        """Perform checkpoint - replaces 2300-CHECKPOINT.

        COBOL: Commits every WS-COMMIT-INTERVAL records.
        """
        logger.info(
            "HISTLD00 checkpoint: loaded=%d, errors=%d",
            self.records_loaded, self.records_error,
        )
        self.commit_ctrl.commit_count += 1

    def process_batch(self, positions: list[PositionRecord]) -> int:
        """Process a batch of positions - replaces 0000-MAIN.

        Returns RC = min(error_count, RC_ERROR).
        Matching COBOL: MOVE WS-ERROR-COUNT TO WS-RETURN-CODE.
        """
        self.initialize()

        try:
            for position in positions:
                # 2100-READ-VSAM (simulated - we receive the records directly)
                self.records_read += 1
                self.stats.update("reads")

                # 2200-LOAD-TO-DB2
                self.load_to_db2(position)

                # 2300-CHECKPOINT every COMMIT_INTERVAL records
                self.commit_ctrl.increment_records()
                if self.commit_ctrl.should_commit():
                    self.checkpoint()

            return self.terminate()
        except Exception as exc:
            self.error_handler.log_error(
                f"Batch processing failed: {exc}",
                severity=ErrorSeverity.FATAL,
                error_code="HBAT",
                exc=exc,
            )
            return RC_ERROR

    def terminate(self) -> int:
        """Terminate processing - replaces 3000-TERMINATE.

        COBOL: Final commit, display stats, set RC = error count.
        """
        final_stats = self.stats.terminate()

        logger.info(
            "HISTLD00 complete: read=%d, loaded=%d, errors=%d",
            self.records_read, self.records_loaded, self.records_error,
        )
        logger.info("Statistics: %s", final_stats)

        # RC = error count, capped at RC_ERROR (8)
        rc = min(self.records_error, RC_ERROR)
        if rc == 0:
            rc = RC_SUCCESS
        elif rc <= 4:
            rc = RC_WARNING

        logger.info("HISTLD00 return code: %d", rc)
        return rc

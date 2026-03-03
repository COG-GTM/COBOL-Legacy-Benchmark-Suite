"""Job Orchestration Scheduler - replaces JCL batch job control scripts.

Converts src/jcl/batch/ job control (DAILYBCH.jcl, RPTJOB.jcl, etc.)
to Python orchestration. Provides a programmatic alternative to JCL
job scheduling with dependency management and error handling.

JCL Jobs replaced:
- DAILYBCH: Daily batch processing (TRNVAL00 -> POSUPD00 -> HISTLD00)
- RPTJOB: Report generation (RPTPOS00, RPTAUD00, RPTSTA00)
- UTILJOB: Utility maintenance jobs
"""

import logging
from datetime import datetime
from typing import Any

from python_app.batch.bchctl00 import BatchController
from python_app.batch.histld00 import HistoryLoader
from python_app.batch.pipeline import BatchPipeline
from python_app.batch.posupd00 import PositionUpdater
from python_app.batch.prcseq00 import ProcessSequenceManager, ProcessStatus
from python_app.batch.rptaud00 import AuditReportGenerator
from python_app.batch.rptpos00 import PositionReportGenerator
from python_app.batch.rptsta00 import StatisticsReportGenerator
from python_app.batch.trnval00 import TransactionValidator
from python_app.models.audit import AuditLogRecord
from python_app.models.position import PositionRecord
from python_app.models.transaction import TransactionRecord

logger = logging.getLogger("portfolio.orchestration.scheduler")


class DailyBatchJob:
    """Daily batch processing job - replaces DAILYBCH.jcl.

    JCL flow:
    //STEP010  EXEC PGM=TRNVAL00  (Validate transactions)
    //STEP020  EXEC PGM=POSUPD00  (Update positions)
    //STEP030  EXEC PGM=HISTLD00  (Load history)

    Each step is gated by the previous step's return code (RC <= 4).
    """

    def __init__(self) -> None:
        self.pipeline = BatchPipeline("DAILYBCH")
        self.validator = TransactionValidator()
        self.updater = PositionUpdater()
        self.loader = HistoryLoader()
        self.batch_ctrl = BatchController()

    def execute(
        self,
        transactions: list[TransactionRecord],
        existing_positions: list[PositionRecord] | None = None,
        process_date: str = "",
    ) -> dict[str, Any]:
        """Execute the daily batch pipeline.

        Returns pipeline execution summary with return codes.
        """
        if not process_date:
            process_date = datetime.now().strftime("%Y%m%d")

        logger.info("DAILYBCH starting for date %s with %d transactions", process_date, len(transactions))

        # Initialize batch control
        self.batch_ctrl.init_job("DAILYBCH", process_date, "TRNVAL00")

        # Step 1: Validate transactions (STEP010)
        self.pipeline.add_step(
            "TRNVAL00",
            self.validator.process_batch,
            args=(transactions,),
        )

        # Step 2: Update positions (STEP020)
        def run_posupd() -> int:
            return self.updater.process_batch(
                self.validator.valid_records,
                existing_positions,
            )

        self.pipeline.add_step("POSUPD00", run_posupd)

        # Step 3: Load history (STEP030)
        def run_histld() -> int:
            return self.loader.process_batch(self.updater.get_positions())

        self.pipeline.add_step("HISTLD00", run_histld)

        # Execute pipeline with RC gating
        highest_rc = self.pipeline.execute()

        # Terminate batch control
        self.batch_ctrl.terminate_job("DAILYBCH", process_date, highest_rc)

        summary = self.pipeline.get_summary()
        summary["process_date"] = process_date
        summary["validation_stats"] = {
            "valid": len(self.validator.valid_records),
            "errors": len(self.validator.error_records),
        }
        summary["position_count"] = len(self.updater.get_positions())

        logger.info("DAILYBCH completed: RC=%d", highest_rc)
        return summary


class ReportJob:
    """Report generation job - replaces RPTJOB.jcl.

    JCL flow:
    //STEP010  EXEC PGM=RPTPOS00  (Position report)
    //STEP020  EXEC PGM=RPTAUD00  (Audit report)
    //STEP030  EXEC PGM=RPTSTA00  (Statistics report)
    """

    def __init__(self) -> None:
        self.pipeline = BatchPipeline("RPTJOB")
        self.pos_report = PositionReportGenerator()
        self.aud_report = AuditReportGenerator()
        self.sta_report = StatisticsReportGenerator()

    def execute(
        self,
        positions: list[PositionRecord],
        audit_records: list[AuditLogRecord],
        processing_stats: dict[str, Any],
        error_stats: dict[str, Any],
        performance_stats: dict[str, Any],
        report_date: str = "",
    ) -> dict[str, Any]:
        """Execute the report generation pipeline."""
        if not report_date:
            report_date = datetime.now().strftime("%Y%m%d")

        logger.info("RPTJOB starting for date %s", report_date)

        self.pipeline.add_step(
            "RPTPOS00",
            self.pos_report.process_batch,
            args=(positions,),
            kwargs={"report_date": report_date},
        )
        self.pipeline.add_step(
            "RPTAUD00",
            self.aud_report.process_batch,
            args=(audit_records,),
            kwargs={"report_date": report_date},
        )
        self.pipeline.add_step(
            "RPTSTA00",
            self.sta_report.process_batch,
            args=(processing_stats, error_stats, performance_stats),
            kwargs={"report_date": report_date},
        )

        highest_rc = self.pipeline.execute()

        summary = self.pipeline.get_summary()
        summary["reports"] = {
            "position": self.pos_report.get_report_data(),
            "audit": self.aud_report.get_report_data(),
            "statistics": self.sta_report.get_report_data(),
        }

        logger.info("RPTJOB completed: RC=%d", highest_rc)
        return summary


class EndOfDayJob:
    """Complete end-of-day processing - combines DAILYBCH + RPTJOB.

    Orchestrates the full end-of-day workflow using the
    ProcessSequenceManager for dependency tracking.
    """

    def __init__(self) -> None:
        self.sequence_manager = ProcessSequenceManager()
        self.daily_batch = DailyBatchJob()
        self.report_job = ReportJob()

    def execute(
        self,
        transactions: list[TransactionRecord],
        existing_positions: list[PositionRecord] | None = None,
        audit_records: list[AuditLogRecord] | None = None,
        process_date: str = "",
    ) -> dict[str, Any]:
        """Execute complete end-of-day processing."""
        if not process_date:
            process_date = datetime.now().strftime("%Y%m%d")

        # Initialize process sequence
        self.sequence_manager.initialize([
            {"name": "DAILYBCH", "sequence": 1, "dependencies": []},
            {"name": "RPTJOB", "sequence": 2, "dependencies": ["DAILYBCH"]},
        ])

        results: dict[str, Any] = {"process_date": process_date}

        # Execute daily batch
        self.sequence_manager.update_status("DAILYBCH", ProcessStatus.RUNNING)
        batch_result = self.daily_batch.execute(transactions, existing_positions, process_date)
        batch_rc = batch_result.get("highest_rc", 0)
        self.sequence_manager.update_status(
            "DAILYBCH",
            ProcessStatus.COMPLETE if batch_rc <= 4 else ProcessStatus.ERROR,
            batch_rc,
        )
        results["daily_batch"] = batch_result

        # Execute reports if daily batch succeeded
        check = self.sequence_manager.sequence_check("RPTJOB")
        if check["can_run"]:
            self.sequence_manager.update_status("RPTJOB", ProcessStatus.RUNNING)

            processing_stats = {
                "transactions_read": len(transactions),
                "transactions_valid": len(self.daily_batch.validator.valid_records),
                "transactions_error": len(self.daily_batch.validator.error_records),
                "positions_updated": self.daily_batch.updater.records_updated,
                "positions_inserted": self.daily_batch.updater.records_inserted,
                "history_loaded": self.daily_batch.loader.records_loaded,
                "reports_generated": 3,
            }
            error_stats = {
                "total_errors": (
                    self.daily_batch.validator.records_error
                    + self.daily_batch.updater.records_error
                    + self.daily_batch.loader.records_error
                ),
                "total_records": len(transactions),
                "validation_errors": self.daily_batch.validator.records_error,
                "database_errors": self.daily_batch.loader.records_error,
                "processing_errors": self.daily_batch.updater.records_error,
            }
            performance_stats = {
                "elapsed_seconds": 0,
                "records_per_second": 0,
                "commits": 0,
                "rollbacks": 0,
                "connections_used": 1,
            }

            report_result = self.report_job.execute(
                self.daily_batch.updater.get_positions(),
                audit_records or [],
                processing_stats,
                error_stats,
                performance_stats,
                report_date=process_date,
            )
            report_rc = report_result.get("highest_rc", 0)
            self.sequence_manager.update_status(
                "RPTJOB",
                ProcessStatus.COMPLETE if report_rc <= 4 else ProcessStatus.ERROR,
                report_rc,
            )
            results["report_job"] = report_result

        results["sequence_status"] = self.sequence_manager.terminate()
        logger.info("End-of-day processing completed for %s", process_date)
        return results

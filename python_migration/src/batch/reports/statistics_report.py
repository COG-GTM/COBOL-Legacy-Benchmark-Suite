"""
Statistics Report Generator - Migrated from COBOL RPTSTA00 program.

This module generates system statistics reports for monitoring
batch processing performance and system health.

Original COBOL Program: src/programs/batch/RPTSTA00.cbl
"""

import logging
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from decimal import Decimal
from typing import List, Dict, Optional, TextIO
import io

from sqlalchemy.orm import Session
from sqlalchemy import func

from ...models.batch_control import BatchControl, BatchStatus
from ...models.transaction import Transaction
from ...models.position import Position
from ...models.history import History
from ...database.connection import get_session

logger = logging.getLogger(__name__)


@dataclass
class JobStatistics:
    """Statistics for a batch job"""
    job_name: str
    run_count: int
    success_count: int
    error_count: int
    avg_records_read: float
    avg_records_written: float
    avg_duration_seconds: float
    last_run_date: str
    last_return_code: int


@dataclass
class SystemStatistics:
    """Overall system statistics"""
    total_portfolios: int
    total_positions: int
    total_transactions: int
    total_history_records: int
    total_market_value: Decimal
    active_positions: int
    closed_positions: int


@dataclass
class StatisticsReportResult:
    """Result of statistics report generation"""
    report_date: str
    period_start: str
    period_end: str
    job_stats: List[JobStatistics]
    system_stats: SystemStatistics
    report_content: str
    return_code: int = 0


class StatisticsReportGenerator:
    """
    Statistics Report Generator - Migrated from COBOL RPTSTA00.
    
    Generates system statistics reports including:
    - Batch job execution statistics
    - System resource utilization
    - Data volume metrics
    - Performance trends
    
    Original COBOL program flow:
    1. 0000-MAIN: Main control
    2. 1000-INITIALIZE: Open files, set reporting period
    3. 2000-PROCESS: Gather statistics, calculate metrics
    4. 3000-TERMINATE: Print report, close files
    """
    
    def __init__(self, session: Session = None):
        """
        Initialize the report generator.
        
        Args:
            session: SQLAlchemy session (optional)
        """
        self._session = session
        self._owns_session = session is None
        
        # Report settings
        self.page_width = 132
        
        logger.info("StatisticsReportGenerator initialized")
    
    @property
    def session(self) -> Session:
        """Get or create database session"""
        if self._session is None:
            self._session = get_session()
        return self._session
    
    def generate_report(
        self,
        period_start: str = None,
        period_end: str = None,
        output_file: str = None
    ) -> StatisticsReportResult:
        """
        Generate statistics report.
        
        Args:
            period_start: Start date (YYYYMMDD)
            period_end: End date (YYYYMMDD)
            output_file: Output file path (optional)
            
        Returns:
            StatisticsReportResult with report content and statistics
        """
        # Default to last 30 days
        if period_end is None:
            period_end = datetime.now().strftime('%Y%m%d')
        if period_start is None:
            start_dt = datetime.now() - timedelta(days=30)
            period_start = start_dt.strftime('%Y%m%d')
        
        logger.info(f"Generating statistics report for {period_start} to {period_end}")
        
        # Gather statistics
        job_stats = self._get_job_statistics(period_start, period_end)
        system_stats = self._get_system_statistics()
        
        # Generate report content
        output = io.StringIO()
        self._print_report_header(output, period_start, period_end)
        self._print_job_statistics(output, job_stats)
        self._print_system_statistics(output, system_stats)
        self._print_report_footer(output)
        
        report_content = output.getvalue()
        
        # Write to file if specified
        if output_file:
            with open(output_file, 'w') as f:
                f.write(report_content)
            logger.info(f"Report written to: {output_file}")
        
        return StatisticsReportResult(
            report_date=datetime.now().strftime('%Y%m%d'),
            period_start=period_start,
            period_end=period_end,
            job_stats=job_stats,
            system_stats=system_stats,
            report_content=report_content,
            return_code=0
        )
    
    def _get_job_statistics(
        self, 
        period_start: str, 
        period_end: str
    ) -> List[JobStatistics]:
        """Get batch job statistics"""
        # Query batch control records
        query = self.session.query(
            BatchControl.job_name,
            func.count(BatchControl.id).label('run_count'),
            func.sum(
                func.cast(BatchControl.status == BatchStatus.DONE.value, type_=int)
            ).label('success_count'),
            func.sum(
                func.cast(BatchControl.status == BatchStatus.ERROR.value, type_=int)
            ).label('error_count'),
            func.avg(BatchControl.records_read).label('avg_records_read'),
            func.avg(BatchControl.records_written).label('avg_records_written'),
            func.max(BatchControl.process_date).label('last_run_date')
        ).filter(
            BatchControl.process_date >= period_start,
            BatchControl.process_date <= period_end
        ).group_by(BatchControl.job_name)
        
        job_stats = []
        for row in query.all():
            # Get last return code
            last_job = self.session.query(BatchControl).filter(
                BatchControl.job_name == row.job_name
            ).order_by(BatchControl.process_date.desc()).first()
            
            stats = JobStatistics(
                job_name=row.job_name,
                run_count=row.run_count or 0,
                success_count=row.success_count or 0,
                error_count=row.error_count or 0,
                avg_records_read=float(row.avg_records_read or 0),
                avg_records_written=float(row.avg_records_written or 0),
                avg_duration_seconds=0.0,  # Would need timestamp calculation
                last_run_date=row.last_run_date or '',
                last_return_code=last_job.return_code if last_job else 0
            )
            job_stats.append(stats)
        
        return job_stats
    
    def _get_system_statistics(self) -> SystemStatistics:
        """Get overall system statistics"""
        # Count distinct portfolios
        portfolio_count = self.session.query(
            func.count(func.distinct(Position.portfolio_id))
        ).scalar() or 0
        
        # Count positions
        total_positions = self.session.query(func.count(Position.id)).scalar() or 0
        active_positions = self.session.query(func.count(Position.id)).filter(
            Position.status == 'A'
        ).scalar() or 0
        closed_positions = total_positions - active_positions
        
        # Count transactions
        total_transactions = self.session.query(func.count(Transaction.id)).scalar() or 0
        
        # Count history records
        total_history = self.session.query(func.count(History.id)).scalar() or 0
        
        # Calculate total market value
        total_market_value = self.session.query(
            func.sum(Position.market_value)
        ).filter(Position.status == 'A').scalar() or Decimal('0')
        
        return SystemStatistics(
            total_portfolios=portfolio_count,
            total_positions=total_positions,
            total_transactions=total_transactions,
            total_history_records=total_history,
            total_market_value=Decimal(str(total_market_value)),
            active_positions=active_positions,
            closed_positions=closed_positions
        )
    
    def _print_report_header(self, output: TextIO, period_start: str, period_end: str):
        """Print report header"""
        output.write("=" * self.page_width + "\n")
        output.write(f"{'INVESTMENT PORTFOLIO MANAGEMENT SYSTEM':^{self.page_width}}\n")
        output.write(f"{'SYSTEM STATISTICS REPORT - RPTSTA00':^{self.page_width}}\n")
        output.write(f"{'Period: ' + period_start + ' to ' + period_end:^{self.page_width}}\n")
        output.write(f"{'Generated: ' + datetime.now().strftime('%Y-%m-%d %H:%M:%S'):^{self.page_width}}\n")
        output.write("=" * self.page_width + "\n")
        output.write("\n")
    
    def _print_job_statistics(self, output: TextIO, job_stats: List[JobStatistics]):
        """Print batch job statistics section"""
        output.write("-" * self.page_width + "\n")
        output.write("BATCH JOB STATISTICS\n")
        output.write("-" * self.page_width + "\n")
        
        if not job_stats:
            output.write("No batch job executions in reporting period.\n\n")
            return
        
        # Column headers
        output.write(f"{'Job Name':<12} {'Runs':>6} {'Success':>8} {'Errors':>8} "
                    f"{'Avg Read':>12} {'Avg Write':>12} {'Last Run':<10} {'RC':>4}\n")
        output.write("-" * self.page_width + "\n")
        
        for stats in job_stats:
            output.write(
                f"{stats.job_name:<12} "
                f"{stats.run_count:>6} "
                f"{stats.success_count:>8} "
                f"{stats.error_count:>8} "
                f"{stats.avg_records_read:>12,.0f} "
                f"{stats.avg_records_written:>12,.0f} "
                f"{stats.last_run_date:<10} "
                f"{stats.last_return_code:>4}\n"
            )
        
        output.write("\n")
    
    def _print_system_statistics(self, output: TextIO, stats: SystemStatistics):
        """Print system statistics section"""
        output.write("-" * self.page_width + "\n")
        output.write("SYSTEM DATA STATISTICS\n")
        output.write("-" * self.page_width + "\n")
        
        output.write(f"Total Portfolios:        {stats.total_portfolios:>15,}\n")
        output.write(f"Total Positions:         {stats.total_positions:>15,}\n")
        output.write(f"  Active Positions:      {stats.active_positions:>15,}\n")
        output.write(f"  Closed Positions:      {stats.closed_positions:>15,}\n")
        output.write(f"Total Transactions:      {stats.total_transactions:>15,}\n")
        output.write(f"Total History Records:   {stats.total_history_records:>15,}\n")
        output.write(f"Total Market Value:      ${stats.total_market_value:>14,.2f}\n")
        output.write("\n")
    
    def _print_report_footer(self, output: TextIO):
        """Print report footer"""
        output.write("=" * self.page_width + "\n")
        output.write(f"{'*** END OF REPORT ***':^{self.page_width}}\n")
    
    def close(self):
        """Close session if owned"""
        if self._owns_session and self._session:
            self._session.close()
            self._session = None

"""
Statistics Report Generator - Python implementation of RPTSTA00.cbl

This module implements the statistics reporting logic from the COBOL
program RPTSTA00, which generates system performance and processing
statistics reports.

Original COBOL Program: src/programs/batch/RPTSTA00.cbl

Key Functions:
- Generate batch processing statistics
- Track system performance metrics
- Produce volume and throughput reports
- Create trend analysis reports

Report Types:
- Processing Summary: Overview of batch processing
- Volume Statistics: Record counts and throughput
- Performance Metrics: Timing and efficiency
- Trend Analysis: Historical comparisons
"""

import logging
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import List, Optional, Dict, Any
from enum import Enum

from sqlalchemy import func, distinct

from migration.python.database.orm_models import (
    PortfolioMaster,
    TransactionHistory,
    PositionHistory,
    BatchControl,
    ErrorLog,
)
from migration.python.database.session import DatabaseManager

# Configure logging
logger = logging.getLogger(__name__)


class StatisticsReportType(str, Enum):
    """Type of statistics report."""
    PROCESSING_SUMMARY = 'processing_summary'
    VOLUME_STATISTICS = 'volume_statistics'
    PERFORMANCE_METRICS = 'performance_metrics'
    TREND_ANALYSIS = 'trend_analysis'
    SYSTEM_HEALTH = 'system_health'


@dataclass
class StatisticsReportConfig:
    """Configuration for statistics report generation."""
    report_type: StatisticsReportType = StatisticsReportType.PROCESSING_SUMMARY
    start_date: Optional[str] = None  # YYYYMMDD
    end_date: Optional[str] = None    # YYYYMMDD
    include_details: bool = True
    page_size: int = 60


@dataclass
class ProcessingStatistics:
    """Processing statistics for a time period."""
    period: str
    transactions_processed: int = 0
    positions_updated: int = 0
    history_records_created: int = 0
    errors_encountered: int = 0
    batch_jobs_run: int = 0
    batch_jobs_successful: int = 0
    batch_jobs_failed: int = 0
    average_processing_time: float = 0.0
    
    @property
    def success_rate(self) -> float:
        """Calculate batch job success rate."""
        if self.batch_jobs_run == 0:
            return 0.0
        return (self.batch_jobs_successful / self.batch_jobs_run) * 100


@dataclass
class VolumeStatistics:
    """Volume statistics for the system."""
    total_portfolios: int = 0
    total_positions: int = 0
    active_positions: int = 0
    closed_positions: int = 0
    total_transactions: int = 0
    pending_transactions: int = 0
    completed_transactions: int = 0
    failed_transactions: int = 0
    total_history_records: int = 0
    total_market_value: Decimal = Decimal('0')
    total_cost_basis: Decimal = Decimal('0')


class StatisticsReportGenerator:
    """
    Statistics Report Generator - Python implementation of RPTSTA00.cbl
    
    This class generates system performance and processing statistics reports,
    following the same business logic as the original COBOL program.
    
    The report generation follows the same flow as RPTSTA00:
    1. Initialize report parameters
    2. Gather statistics from various tables
    3. Calculate derived metrics
    4. Format and output report
    5. Generate summary and recommendations
    
    Usage:
        generator = StatisticsReportGenerator(db_manager)
        report = generator.generate_report(
            report_type=StatisticsReportType.PROCESSING_SUMMARY,
            start_date='20241201',
            end_date='20241215'
        )
        
        # Save to file
        generator.save_report(report, 'statistics_report.txt')
    """
    
    def __init__(self, db_manager: DatabaseManager,
                 config: Optional[StatisticsReportConfig] = None):
        """
        Initialize the statistics report generator.
        
        Args:
            db_manager: Database manager for data access
            config: Report configuration
        """
        self.db_manager = db_manager
        self.config = config or StatisticsReportConfig()
        self._page_number = 0
        self._line_count = 0
        self._report_lines: List[str] = []
    
    def generate_report(self, report_type: Optional[StatisticsReportType] = None,
                        start_date: Optional[str] = None,
                        end_date: Optional[str] = None) -> str:
        """
        Generate a statistics report.
        
        Args:
            report_type: Type of statistics report
            start_date: Start date filter (YYYYMMDD)
            end_date: End date filter (YYYYMMDD)
            
        Returns:
            Formatted report string
        """
        if report_type:
            self.config.report_type = report_type
        if start_date:
            self.config.start_date = start_date
        if end_date:
            self.config.end_date = end_date
        
        logger.info(f"Generating {self.config.report_type.value} statistics report")
        
        # Initialize report
        self._initialize_report()
        
        # Generate report based on type
        if self.config.report_type == StatisticsReportType.PROCESSING_SUMMARY:
            self._generate_processing_summary()
        elif self.config.report_type == StatisticsReportType.VOLUME_STATISTICS:
            self._generate_volume_statistics()
        elif self.config.report_type == StatisticsReportType.PERFORMANCE_METRICS:
            self._generate_performance_metrics()
        elif self.config.report_type == StatisticsReportType.TREND_ANALYSIS:
            self._generate_trend_analysis()
        elif self.config.report_type == StatisticsReportType.SYSTEM_HEALTH:
            self._generate_system_health()
        
        # Finalize report
        self._finalize_report()
        
        return "\n".join(self._report_lines)
    
    def get_volume_statistics(self) -> VolumeStatistics:
        """
        Get current volume statistics.
        
        Returns:
            VolumeStatistics object
        """
        stats = VolumeStatistics()
        
        with self.db_manager.session_scope() as session:
            # Portfolio and position counts
            stats.total_portfolios = session.query(
                func.count(distinct(PortfolioMaster.portfolio_id))
            ).scalar() or 0
            
            stats.total_positions = session.query(
                func.count(PortfolioMaster.id)
            ).scalar() or 0
            
            stats.active_positions = session.query(
                func.count(PortfolioMaster.id)
            ).filter(PortfolioMaster.status == 'A').scalar() or 0
            
            stats.closed_positions = session.query(
                func.count(PortfolioMaster.id)
            ).filter(PortfolioMaster.status == 'C').scalar() or 0
            
            # Transaction counts
            stats.total_transactions = session.query(
                func.count(TransactionHistory.id)
            ).scalar() or 0
            
            stats.pending_transactions = session.query(
                func.count(TransactionHistory.id)
            ).filter(TransactionHistory.status == 'P').scalar() or 0
            
            stats.completed_transactions = session.query(
                func.count(TransactionHistory.id)
            ).filter(TransactionHistory.status == 'D').scalar() or 0
            
            stats.failed_transactions = session.query(
                func.count(TransactionHistory.id)
            ).filter(TransactionHistory.status == 'F').scalar() or 0
            
            # History records
            stats.total_history_records = session.query(
                func.count(PositionHistory.id)
            ).scalar() or 0
            
            # Financial totals
            market_value = session.query(
                func.sum(PortfolioMaster.market_value)
            ).filter(PortfolioMaster.status == 'A').scalar()
            stats.total_market_value = Decimal(str(market_value or 0))
            
            cost_basis = session.query(
                func.sum(PortfolioMaster.cost_basis)
            ).filter(PortfolioMaster.status == 'A').scalar()
            stats.total_cost_basis = Decimal(str(cost_basis or 0))
        
        return stats
    
    def get_processing_statistics(self, start_date: Optional[str] = None,
                                   end_date: Optional[str] = None) -> ProcessingStatistics:
        """
        Get processing statistics for a date range.
        
        Args:
            start_date: Start date (YYYYMMDD)
            end_date: End date (YYYYMMDD)
            
        Returns:
            ProcessingStatistics object
        """
        start_date = start_date or self.config.start_date
        end_date = end_date or self.config.end_date
        
        period = f"{start_date or 'All'} to {end_date or 'All'}"
        stats = ProcessingStatistics(period=period)
        
        with self.db_manager.session_scope() as session:
            # Transaction query
            trans_query = session.query(TransactionHistory)
            if start_date:
                trans_query = trans_query.filter(TransactionHistory.trans_date >= start_date)
            if end_date:
                trans_query = trans_query.filter(TransactionHistory.trans_date <= end_date)
            
            stats.transactions_processed = trans_query.filter(
                TransactionHistory.status == 'D'
            ).count()
            
            # History records query
            hist_query = session.query(PositionHistory)
            if start_date:
                hist_query = hist_query.filter(PositionHistory.position_date >= start_date)
            if end_date:
                hist_query = hist_query.filter(PositionHistory.position_date <= end_date)
            
            stats.history_records_created = hist_query.count()
            stats.positions_updated = hist_query.filter(
                PositionHistory.action_code == 'C'
            ).count()
            
            # Batch job statistics
            batch_query = session.query(BatchControl)
            if start_date:
                batch_query = batch_query.filter(BatchControl.process_date >= start_date)
            if end_date:
                batch_query = batch_query.filter(BatchControl.process_date <= end_date)
            
            stats.batch_jobs_run = batch_query.count()
            stats.batch_jobs_successful = batch_query.filter(
                BatchControl.status == 'D'
            ).count()
            stats.batch_jobs_failed = batch_query.filter(
                BatchControl.status == 'E'
            ).count()
            
            # Error count
            error_query = session.query(ErrorLog)
            if start_date:
                start_dt = datetime.strptime(start_date, "%Y%m%d")
                error_query = error_query.filter(ErrorLog.error_timestamp >= start_dt)
            if end_date:
                end_dt = datetime.strptime(end_date, "%Y%m%d")
                error_query = error_query.filter(ErrorLog.error_timestamp <= end_dt)
            
            stats.errors_encountered = error_query.count()
        
        return stats
    
    def save_report(self, report: str, output_file: str):
        """
        Save report to a file.
        
        Args:
            report: Report content
            output_file: Output file path
        """
        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)
        
        with open(output_path, 'w') as f:
            f.write(report)
        
        logger.info(f"Statistics report saved to {output_file}")
    
    def _initialize_report(self):
        """Initialize report generation."""
        self._page_number = 0
        self._line_count = 0
        self._report_lines = []
    
    def _finalize_report(self):
        """Finalize report generation."""
        self._add_line("")
        self._add_line("=" * 80)
        self._add_line("END OF STATISTICS REPORT")
        self._add_line("=" * 80)
    
    def _generate_processing_summary(self):
        """
        Generate processing summary report.
        
        Shows overview of batch processing activity.
        """
        report_date = datetime.now().strftime("%Y%m%d")
        self._add_page_header("PROCESSING SUMMARY REPORT", report_date)
        
        stats = self.get_processing_statistics()
        
        self._add_line("")
        self._add_line("PROCESSING STATISTICS")
        self._add_line("=" * 50)
        self._add_line(f"Reporting Period: {stats.period}")
        self._add_line("")
        
        self._add_line("TRANSACTION PROCESSING")
        self._add_line("-" * 40)
        self._add_line(f"Transactions Processed:   {stats.transactions_processed:>12,}")
        self._add_line(f"Positions Updated:        {stats.positions_updated:>12,}")
        self._add_line(f"History Records Created:  {stats.history_records_created:>12,}")
        self._add_line("")
        
        self._add_line("BATCH JOB STATISTICS")
        self._add_line("-" * 40)
        self._add_line(f"Total Jobs Run:           {stats.batch_jobs_run:>12,}")
        self._add_line(f"Successful Jobs:          {stats.batch_jobs_successful:>12,}")
        self._add_line(f"Failed Jobs:              {stats.batch_jobs_failed:>12,}")
        self._add_line(f"Success Rate:             {stats.success_rate:>11.2f}%")
        self._add_line("")
        
        self._add_line("ERROR STATISTICS")
        self._add_line("-" * 40)
        self._add_line(f"Errors Encountered:       {stats.errors_encountered:>12,}")
        
        # Recommendations
        self._add_line("")
        self._add_line("RECOMMENDATIONS")
        self._add_line("-" * 40)
        
        if stats.success_rate < 95:
            self._add_line("- WARNING: Batch job success rate below 95%")
            self._add_line("  Review failed jobs and address root causes")
        
        if stats.errors_encountered > 100:
            self._add_line("- WARNING: High error count detected")
            self._add_line("  Review error log for patterns")
        
        if stats.success_rate >= 95 and stats.errors_encountered <= 100:
            self._add_line("- System operating within normal parameters")
    
    def _generate_volume_statistics(self):
        """
        Generate volume statistics report.
        
        Shows record counts and data volumes.
        """
        report_date = datetime.now().strftime("%Y%m%d")
        self._add_page_header("VOLUME STATISTICS REPORT", report_date)
        
        stats = self.get_volume_statistics()
        
        self._add_line("")
        self._add_line("PORTFOLIO STATISTICS")
        self._add_line("=" * 50)
        self._add_line(f"Total Portfolios:         {stats.total_portfolios:>12,}")
        self._add_line(f"Total Positions:          {stats.total_positions:>12,}")
        self._add_line(f"Active Positions:         {stats.active_positions:>12,}")
        self._add_line(f"Closed Positions:         {stats.closed_positions:>12,}")
        self._add_line("")
        
        self._add_line("TRANSACTION STATISTICS")
        self._add_line("-" * 40)
        self._add_line(f"Total Transactions:       {stats.total_transactions:>12,}")
        self._add_line(f"Pending Transactions:     {stats.pending_transactions:>12,}")
        self._add_line(f"Completed Transactions:   {stats.completed_transactions:>12,}")
        self._add_line(f"Failed Transactions:      {stats.failed_transactions:>12,}")
        self._add_line("")
        
        self._add_line("HISTORY STATISTICS")
        self._add_line("-" * 40)
        self._add_line(f"Total History Records:    {stats.total_history_records:>12,}")
        self._add_line("")
        
        self._add_line("FINANCIAL SUMMARY")
        self._add_line("-" * 40)
        self._add_line(f"Total Market Value:       {stats.total_market_value:>18,.2f}")
        self._add_line(f"Total Cost Basis:         {stats.total_cost_basis:>18,.2f}")
        
        unrealized_gl = stats.total_market_value - stats.total_cost_basis
        self._add_line(f"Total Unrealized G/L:     {unrealized_gl:>18,.2f}")
        
        if stats.total_cost_basis > 0:
            return_pct = (unrealized_gl / stats.total_cost_basis * 100).quantize(
                Decimal('0.01'), rounding=ROUND_HALF_UP)
            self._add_line(f"Overall Return:           {return_pct:>17.2f}%")
    
    def _generate_performance_metrics(self):
        """
        Generate performance metrics report.
        
        Shows timing and efficiency metrics.
        """
        report_date = datetime.now().strftime("%Y%m%d")
        self._add_page_header("PERFORMANCE METRICS REPORT", report_date)
        
        self._add_line("")
        self._add_line("BATCH JOB PERFORMANCE")
        self._add_line("=" * 60)
        
        with self.db_manager.session_scope() as session:
            # Get batch job timing data
            batch_query = session.query(BatchControl).filter(
                BatchControl.status == 'D',
                BatchControl.start_time.isnot(None),
                BatchControl.end_time.isnot(None)
            )
            
            if self.config.start_date:
                batch_query = batch_query.filter(
                    BatchControl.process_date >= self.config.start_date
                )
            if self.config.end_date:
                batch_query = batch_query.filter(
                    BatchControl.process_date <= self.config.end_date
                )
            
            jobs = batch_query.all()
            
            if jobs:
                self._add_line("")
                self._add_line(
                    f"{'Job Name':<12} {'Date':<10} {'Start':<10} "
                    f"{'End':<10} {'Duration':<12} {'Records':<10}"
                )
                self._add_line("-" * 70)
                
                total_duration = 0.0
                total_records = 0
                
                for job in jobs:
                    duration = (job.end_time - job.start_time).total_seconds()
                    total_duration += duration
                    records = (job.records_read or 0) + (job.records_written or 0)
                    total_records += records
                    
                    self._add_line(
                        f"{job.job_name:<12} "
                        f"{job.process_date:<10} "
                        f"{job.start_time.strftime('%H:%M:%S'):<10} "
                        f"{job.end_time.strftime('%H:%M:%S'):<10} "
                        f"{duration:>10.2f}s "
                        f"{records:>10,}"
                    )
                
                # Summary
                avg_duration = total_duration / len(jobs) if jobs else 0
                throughput = total_records / total_duration if total_duration > 0 else 0
                
                self._add_line("-" * 70)
                self._add_line("")
                self._add_line("PERFORMANCE SUMMARY")
                self._add_line("-" * 40)
                self._add_line(f"Total Jobs Analyzed:      {len(jobs):>12,}")
                self._add_line(f"Total Processing Time:    {total_duration:>11.2f}s")
                self._add_line(f"Average Job Duration:     {avg_duration:>11.2f}s")
                self._add_line(f"Total Records Processed:  {total_records:>12,}")
                self._add_line(f"Average Throughput:       {throughput:>10.2f}/s")
            else:
                self._add_line("")
                self._add_line("No completed batch jobs found for the specified period.")
    
    def _generate_trend_analysis(self):
        """
        Generate trend analysis report.
        
        Shows historical comparisons and trends.
        """
        report_date = datetime.now().strftime("%Y%m%d")
        self._add_page_header("TREND ANALYSIS REPORT", report_date)
        
        self._add_line("")
        self._add_line("DAILY TRANSACTION TRENDS")
        self._add_line("=" * 60)
        
        with self.db_manager.session_scope() as session:
            # Get daily transaction counts
            daily_counts = session.query(
                TransactionHistory.trans_date,
                func.count(TransactionHistory.id).label('count')
            ).group_by(
                TransactionHistory.trans_date
            ).order_by(
                TransactionHistory.trans_date.desc()
            ).limit(14).all()
            
            if daily_counts:
                self._add_line("")
                self._add_line(f"{'Date':<12} {'Transactions':>15} {'Graph':<30}")
                self._add_line("-" * 60)
                
                max_count = max(c[1] for c in daily_counts) if daily_counts else 1
                
                for date_str, count in reversed(daily_counts):
                    bar_length = int((count / max_count) * 25) if max_count > 0 else 0
                    bar = '*' * bar_length
                    
                    formatted_date = f"{date_str[:4]}-{date_str[4:6]}-{date_str[6:8]}"
                    self._add_line(
                        f"{formatted_date:<12} "
                        f"{count:>15,} "
                        f"{bar:<30}"
                    )
            else:
                self._add_line("No transaction data available for trend analysis.")
        
        # Position value trends
        self._add_line("")
        self._add_line("POSITION VALUE TRENDS")
        self._add_line("=" * 60)
        
        with self.db_manager.session_scope() as session:
            # Get daily position values from history
            daily_values = session.query(
                PositionHistory.position_date,
                func.sum(PositionHistory.market_value).label('total_value')
            ).filter(
                PositionHistory.record_type == 'PS'
            ).group_by(
                PositionHistory.position_date
            ).order_by(
                PositionHistory.position_date.desc()
            ).limit(14).all()
            
            if daily_values:
                self._add_line("")
                self._add_line(f"{'Date':<12} {'Market Value':>20}")
                self._add_line("-" * 35)
                
                for date_str, value in reversed(daily_values):
                    formatted_date = f"{date_str[:4]}-{date_str[4:6]}-{date_str[6:8]}"
                    self._add_line(
                        f"{formatted_date:<12} "
                        f"{value or 0:>20,.2f}"
                    )
            else:
                self._add_line("No position history data available for trend analysis.")
    
    def _generate_system_health(self):
        """
        Generate system health report.
        
        Shows overall system status and health indicators.
        """
        report_date = datetime.now().strftime("%Y%m%d")
        self._add_page_header("SYSTEM HEALTH REPORT", report_date)
        
        proc_stats = self.get_processing_statistics()
        vol_stats = self.get_volume_statistics()
        
        self._add_line("")
        self._add_line("SYSTEM HEALTH DASHBOARD")
        self._add_line("=" * 60)
        self._add_line("")
        
        # Health indicators
        indicators = []
        
        # Batch job success rate
        if proc_stats.success_rate >= 98:
            indicators.append(("Batch Processing", "HEALTHY", "Success rate >= 98%"))
        elif proc_stats.success_rate >= 95:
            indicators.append(("Batch Processing", "WARNING", "Success rate 95-98%"))
        else:
            indicators.append(("Batch Processing", "CRITICAL", "Success rate < 95%"))
        
        # Pending transactions
        pending_ratio = 0
        if vol_stats.total_transactions > 0:
            pending_ratio = vol_stats.pending_transactions / vol_stats.total_transactions * 100
        
        if pending_ratio <= 5:
            indicators.append(("Transaction Queue", "HEALTHY", f"{pending_ratio:.1f}% pending"))
        elif pending_ratio <= 15:
            indicators.append(("Transaction Queue", "WARNING", f"{pending_ratio:.1f}% pending"))
        else:
            indicators.append(("Transaction Queue", "CRITICAL", f"{pending_ratio:.1f}% pending"))
        
        # Error rate
        if proc_stats.errors_encountered <= 10:
            indicators.append(("Error Rate", "HEALTHY", f"{proc_stats.errors_encountered} errors"))
        elif proc_stats.errors_encountered <= 50:
            indicators.append(("Error Rate", "WARNING", f"{proc_stats.errors_encountered} errors"))
        else:
            indicators.append(("Error Rate", "CRITICAL", f"{proc_stats.errors_encountered} errors"))
        
        # Data integrity (active positions)
        if vol_stats.active_positions > 0:
            indicators.append(("Data Integrity", "HEALTHY", f"{vol_stats.active_positions} active positions"))
        else:
            indicators.append(("Data Integrity", "WARNING", "No active positions"))
        
        # Display indicators
        self._add_line(f"{'Component':<25} {'Status':<12} {'Details':<25}")
        self._add_line("-" * 65)
        
        for component, status, details in indicators:
            status_display = f"[{status}]"
            self._add_line(f"{component:<25} {status_display:<12} {details:<25}")
        
        # Overall health
        self._add_line("")
        self._add_line("=" * 65)
        
        critical_count = sum(1 for _, s, _ in indicators if s == "CRITICAL")
        warning_count = sum(1 for _, s, _ in indicators if s == "WARNING")
        
        if critical_count > 0:
            overall = "CRITICAL - Immediate attention required"
        elif warning_count > 0:
            overall = "WARNING - Review recommended"
        else:
            overall = "HEALTHY - All systems operational"
        
        self._add_line(f"OVERALL SYSTEM STATUS: {overall}")
        
        # Recommendations
        self._add_line("")
        self._add_line("RECOMMENDATIONS")
        self._add_line("-" * 40)
        
        if critical_count > 0:
            self._add_line("1. Address critical issues immediately")
            self._add_line("2. Review error logs for root cause analysis")
            self._add_line("3. Consider escalating to system administrators")
        elif warning_count > 0:
            self._add_line("1. Monitor warning indicators closely")
            self._add_line("2. Schedule maintenance window if needed")
            self._add_line("3. Review recent changes for potential issues")
        else:
            self._add_line("1. Continue regular monitoring")
            self._add_line("2. Maintain current operational procedures")
            self._add_line("3. Document any anomalies for future reference")
    
    def _add_page_header(self, title: str, report_date: str):
        """Add page header to report."""
        self._page_number += 1
        
        self._add_line("=" * 80)
        self._add_line(f"{title:^80}")
        self._add_line(f"{'Investment Portfolio Management System':^80}")
        self._add_line("=" * 80)
        self._add_line(
            f"Report Date: {report_date[:4]}-{report_date[4:6]}-{report_date[6:8]}"
            f"{' ' * 40}"
            f"Page: {self._page_number}"
        )
        self._add_line(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        self._add_line("")
    
    def _add_line(self, line: str):
        """Add a line to the report."""
        self._report_lines.append(line)
        self._line_count += 1
        
        if self._line_count >= self.config.page_size:
            self._line_count = 0

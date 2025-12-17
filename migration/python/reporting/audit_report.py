"""
Audit Report Generator - Python implementation of RPTAUD00.cbl

This module implements the audit reporting logic from the COBOL
program RPTAUD00, which generates audit trail and change tracking reports.

Original COBOL Program: src/programs/batch/RPTAUD00.cbl

Key Functions:
- Generate audit trail reports for position changes
- Track user activity and system changes
- Produce compliance and regulatory reports
- Create detailed change history listings

Report Types:
- Change History: All changes to positions
- User Activity: Changes grouped by user
- Daily Audit: Changes for a specific date
- Compliance: Formatted for regulatory requirements
"""

import logging
from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal
from pathlib import Path
from typing import List, Optional, Dict
from enum import Enum

from sqlalchemy import func

from migration.python.database.orm_models import PositionHistory, ErrorLog
from migration.python.database.session import DatabaseManager

# Configure logging
logger = logging.getLogger(__name__)


class AuditReportType(str, Enum):
    """Type of audit report."""
    CHANGE_HISTORY = 'change_history'
    USER_ACTIVITY = 'user_activity'
    DAILY_AUDIT = 'daily_audit'
    COMPLIANCE = 'compliance'
    ERROR_LOG = 'error_log'


@dataclass
class AuditReportConfig:
    """Configuration for audit report generation."""
    report_type: AuditReportType = AuditReportType.CHANGE_HISTORY
    start_date: Optional[str] = None  # YYYYMMDD
    end_date: Optional[str] = None    # YYYYMMDD
    user_filter: Optional[str] = None
    portfolio_filter: Optional[str] = None
    include_before_after: bool = True
    page_size: int = 60


@dataclass
class AuditEntry:
    """Single audit trail entry."""
    timestamp: datetime
    portfolio_id: str
    investment_id: str
    record_type: str
    action_code: str
    reason_code: str
    user_id: str
    before_image: Optional[str]
    after_image: Optional[str]


class AuditReportGenerator:
    """
    Audit Report Generator - Python implementation of RPTAUD00.cbl
    
    This class generates audit trail and change tracking reports,
    following the same business logic as the original COBOL program.
    
    The report generation follows the same flow as RPTAUD00:
    1. Initialize report parameters
    2. Read audit/history records from database
    3. Format audit entries
    4. Generate report with appropriate grouping
    5. Add control totals and summary
    
    Usage:
        generator = AuditReportGenerator(db_manager)
        report = generator.generate_report(
            report_type=AuditReportType.DAILY_AUDIT,
            start_date='20241215'
        )
        
        # Save to file
        generator.save_report(report, 'audit_report.txt')
    """
    
    def __init__(self, db_manager: DatabaseManager,
                 config: Optional[AuditReportConfig] = None):
        """
        Initialize the audit report generator.
        
        Args:
            db_manager: Database manager for data access
            config: Report configuration
        """
        self.db_manager = db_manager
        self.config = config or AuditReportConfig()
        self._page_number = 0
        self._line_count = 0
        self._report_lines: List[str] = []
    
    def generate_report(self, report_type: Optional[AuditReportType] = None,
                        start_date: Optional[str] = None,
                        end_date: Optional[str] = None,
                        user_id: Optional[str] = None,
                        portfolio_id: Optional[str] = None) -> str:
        """
        Generate an audit report.
        
        Args:
            report_type: Type of audit report
            start_date: Start date filter (YYYYMMDD)
            end_date: End date filter (YYYYMMDD)
            user_id: Filter by user ID
            portfolio_id: Filter by portfolio ID
            
        Returns:
            Formatted report string
        """
        if report_type:
            self.config.report_type = report_type
        if start_date:
            self.config.start_date = start_date
        if end_date:
            self.config.end_date = end_date
        if user_id:
            self.config.user_filter = user_id
        if portfolio_id:
            self.config.portfolio_filter = portfolio_id
        
        logger.info(f"Generating {self.config.report_type.value} audit report")
        
        # Initialize report
        self._initialize_report()
        
        # Generate report based on type
        if self.config.report_type == AuditReportType.CHANGE_HISTORY:
            self._generate_change_history_report()
        elif self.config.report_type == AuditReportType.USER_ACTIVITY:
            self._generate_user_activity_report()
        elif self.config.report_type == AuditReportType.DAILY_AUDIT:
            self._generate_daily_audit_report()
        elif self.config.report_type == AuditReportType.COMPLIANCE:
            self._generate_compliance_report()
        elif self.config.report_type == AuditReportType.ERROR_LOG:
            self._generate_error_log_report()
        
        # Finalize report
        self._finalize_report()
        
        return "\n".join(self._report_lines)
    
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
        
        logger.info(f"Audit report saved to {output_file}")
    
    def _initialize_report(self):
        """Initialize report generation."""
        self._page_number = 0
        self._line_count = 0
        self._report_lines = []
    
    def _finalize_report(self):
        """Finalize report generation."""
        self._add_line("")
        self._add_line("=" * 80)
        self._add_line("END OF AUDIT REPORT")
        self._add_line("=" * 80)
    
    def _get_audit_records(self) -> List[PositionHistory]:
        """
        Get audit records from database based on filters.
        
        Returns:
            List of PositionHistory records
        """
        with self.db_manager.session_scope() as session:
            query = session.query(PositionHistory)
            
            if self.config.start_date:
                query = query.filter(PositionHistory.position_date >= self.config.start_date)
            if self.config.end_date:
                query = query.filter(PositionHistory.position_date <= self.config.end_date)
            if self.config.user_filter:
                query = query.filter(PositionHistory.process_user == self.config.user_filter)
            if self.config.portfolio_filter:
                query = query.filter(PositionHistory.portfolio_id == self.config.portfolio_filter)
            
            query = query.order_by(
                PositionHistory.process_date.desc(),
                PositionHistory.portfolio_id
            )
            
            return query.all()
    
    def _get_error_records(self) -> List[ErrorLog]:
        """
        Get error log records from database.
        
        Returns:
            List of ErrorLog records
        """
        with self.db_manager.session_scope() as session:
            query = session.query(ErrorLog)
            
            if self.config.start_date:
                start_dt = datetime.strptime(self.config.start_date, "%Y%m%d")
                query = query.filter(ErrorLog.error_timestamp >= start_dt)
            if self.config.end_date:
                end_dt = datetime.strptime(self.config.end_date, "%Y%m%d")
                query = query.filter(ErrorLog.error_timestamp <= end_dt)
            if self.config.user_filter:
                query = query.filter(ErrorLog.user_id == self.config.user_filter)
            
            query = query.order_by(ErrorLog.error_timestamp.desc())
            
            return query.all()
    
    def _generate_change_history_report(self):
        """
        Generate change history report.
        
        Lists all changes to positions in chronological order.
        """
        report_date = datetime.now().strftime("%Y%m%d")
        self._add_page_header("CHANGE HISTORY REPORT", report_date)
        
        records = self._get_audit_records()
        
        if not records:
            self._add_line("")
            self._add_line("No audit records found for the specified criteria.")
            return
        
        self._add_line("")
        self._add_line(
            f"{'Timestamp':<20} {'Portfolio':<10} {'Investment':<12} "
            f"{'Action':<8} {'Reason':<6} {'User':<10}"
        )
        self._add_line("=" * 75)
        
        action_counts: Dict[str, int] = {'A': 0, 'C': 0, 'D': 0}
        
        for record in records:
            timestamp = record.process_date.strftime("%Y-%m-%d %H:%M:%S") if record.process_date else ""
            action_desc = self._get_action_description(record.action_code)
            
            self._add_line(
                f"{timestamp:<20} "
                f"{record.portfolio_id:<10} "
                f"{record.investment_id:<12} "
                f"{action_desc:<8} "
                f"{record.reason_code or '':<6} "
                f"{record.process_user or '':<10}"
            )
            
            if record.action_code in action_counts:
                action_counts[record.action_code] += 1
            
            # Show before/after if configured
            if self.config.include_before_after and (record.before_image or record.after_image):
                if record.before_image:
                    self._add_line(f"  Before: {record.before_image[:60]}...")
                if record.after_image:
                    self._add_line(f"  After:  {record.after_image[:60]}...")
        
        # Summary
        self._add_line("")
        self._add_line("=" * 75)
        self._add_line("SUMMARY")
        self._add_line("-" * 40)
        self._add_line(f"Total Records:    {len(records):>10}")
        self._add_line(f"Additions:        {action_counts['A']:>10}")
        self._add_line(f"Changes:          {action_counts['C']:>10}")
        self._add_line(f"Deletions:        {action_counts['D']:>10}")
    
    def _generate_user_activity_report(self):
        """
        Generate user activity report.
        
        Groups changes by user ID.
        """
        report_date = datetime.now().strftime("%Y%m%d")
        self._add_page_header("USER ACTIVITY REPORT", report_date)
        
        records = self._get_audit_records()
        
        if not records:
            self._add_line("")
            self._add_line("No audit records found for the specified criteria.")
            return
        
        # Group by user
        user_records: Dict[str, List[PositionHistory]] = {}
        for record in records:
            user = record.process_user or 'UNKNOWN'
            if user not in user_records:
                user_records[user] = []
            user_records[user].append(record)
        
        for user_id, user_recs in sorted(user_records.items()):
            self._add_line("")
            self._add_line(f"User: {user_id}")
            self._add_line("-" * 70)
            self._add_line(
                f"{'Timestamp':<20} {'Portfolio':<10} {'Investment':<12} "
                f"{'Action':<8} {'Reason':<6}"
            )
            self._add_line("-" * 70)
            
            for record in user_recs:
                timestamp = record.process_date.strftime("%Y-%m-%d %H:%M:%S") if record.process_date else ""
                action_desc = self._get_action_description(record.action_code)
                
                self._add_line(
                    f"{timestamp:<20} "
                    f"{record.portfolio_id:<10} "
                    f"{record.investment_id:<12} "
                    f"{action_desc:<8} "
                    f"{record.reason_code or '':<6}"
                )
            
            self._add_line(f"Total actions by {user_id}: {len(user_recs)}")
        
        # Summary
        self._add_line("")
        self._add_line("=" * 70)
        self._add_line("USER SUMMARY")
        self._add_line("-" * 40)
        for user_id, user_recs in sorted(user_records.items()):
            self._add_line(f"{user_id:<20} {len(user_recs):>10} actions")
    
    def _generate_daily_audit_report(self):
        """
        Generate daily audit report.
        
        Shows all changes for a specific date.
        """
        report_date = self.config.start_date or datetime.now().strftime("%Y%m%d")
        self._add_page_header("DAILY AUDIT REPORT", report_date)
        
        records = self._get_audit_records()
        
        if not records:
            self._add_line("")
            self._add_line(f"No audit records found for {report_date}.")
            return
        
        self._add_line("")
        self._add_line(f"Audit Date: {report_date[:4]}-{report_date[4:6]}-{report_date[6:8]}")
        self._add_line("")
        
        # Morning activities (before noon)
        morning_records = [r for r in records 
                          if r.process_date and r.process_date.hour < 12]
        afternoon_records = [r for r in records 
                            if r.process_date and r.process_date.hour >= 12]
        
        if morning_records:
            self._add_line("MORNING ACTIVITY (00:00 - 11:59)")
            self._add_line("-" * 60)
            self._format_audit_entries(morning_records)
        
        if afternoon_records:
            self._add_line("")
            self._add_line("AFTERNOON ACTIVITY (12:00 - 23:59)")
            self._add_line("-" * 60)
            self._format_audit_entries(afternoon_records)
        
        # Daily summary
        self._add_line("")
        self._add_line("=" * 60)
        self._add_line("DAILY SUMMARY")
        self._add_line("-" * 40)
        self._add_line(f"Total Changes:        {len(records):>10}")
        self._add_line(f"Morning Changes:      {len(morning_records):>10}")
        self._add_line(f"Afternoon Changes:    {len(afternoon_records):>10}")
    
    def _generate_compliance_report(self):
        """
        Generate compliance report.
        
        Formatted for regulatory requirements with full audit trail.
        """
        report_date = datetime.now().strftime("%Y%m%d")
        self._add_page_header("COMPLIANCE AUDIT REPORT", report_date)
        
        records = self._get_audit_records()
        
        self._add_line("")
        self._add_line("REGULATORY COMPLIANCE AUDIT TRAIL")
        self._add_line("=" * 80)
        self._add_line("")
        self._add_line("This report contains a complete audit trail of all position changes")
        self._add_line("for the specified period, formatted for regulatory compliance review.")
        self._add_line("")
        
        if self.config.start_date and self.config.end_date:
            self._add_line(f"Reporting Period: {self.config.start_date} to {self.config.end_date}")
        elif self.config.start_date:
            self._add_line(f"Reporting Period: From {self.config.start_date}")
        
        self._add_line(f"Report Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        self._add_line("")
        
        if not records:
            self._add_line("No audit records found for the specified period.")
            return
        
        # Detailed listing with sequence numbers
        self._add_line("DETAILED AUDIT TRAIL")
        self._add_line("-" * 80)
        
        for seq, record in enumerate(records, 1):
            self._add_line("")
            self._add_line(f"Sequence #: {seq:06d}")
            self._add_line(f"Timestamp:  {record.process_date.strftime('%Y-%m-%d %H:%M:%S.%f') if record.process_date else 'N/A'}")
            self._add_line(f"Portfolio:  {record.portfolio_id}")
            self._add_line(f"Investment: {record.investment_id}")
            self._add_line(f"Action:     {self._get_action_description(record.action_code)} ({record.action_code})")
            self._add_line(f"Reason:     {record.reason_code or 'N/A'}")
            self._add_line(f"User ID:    {record.process_user or 'SYSTEM'}")
            self._add_line(f"Batch ID:   {record.batch_id or 'N/A'}")
            
            if record.before_image:
                self._add_line(f"Before:     {record.before_image}")
            if record.after_image:
                self._add_line(f"After:      {record.after_image}")
            
            self._add_line("-" * 40)
        
        # Compliance attestation
        self._add_line("")
        self._add_line("=" * 80)
        self._add_line("COMPLIANCE ATTESTATION")
        self._add_line("")
        self._add_line(f"Total Records in Audit Trail: {len(records)}")
        self._add_line(f"Report Generation Timestamp:  {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
        self._add_line("")
        self._add_line("This report has been generated automatically by the Investment")
        self._add_line("Portfolio Management System audit subsystem.")
    
    def _generate_error_log_report(self):
        """
        Generate error log report.
        
        Lists all errors from the error log table.
        """
        report_date = datetime.now().strftime("%Y%m%d")
        self._add_page_header("ERROR LOG REPORT", report_date)
        
        records = self._get_error_records()
        
        if not records:
            self._add_line("")
            self._add_line("No error records found for the specified criteria.")
            return
        
        self._add_line("")
        self._add_line(
            f"{'Timestamp':<20} {'Program':<10} {'Sev':<4} "
            f"{'Code':<10} {'Message':<30}"
        )
        self._add_line("=" * 80)
        
        severity_counts: Dict[str, int] = {'I': 0, 'W': 0, 'E': 0, 'S': 0}
        
        for record in records:
            timestamp = record.error_timestamp.strftime("%Y-%m-%d %H:%M:%S") if record.error_timestamp else ""
            message = record.error_message[:30] if record.error_message else ""
            
            self._add_line(
                f"{timestamp:<20} "
                f"{record.program_name:<10} "
                f"{record.severity:<4} "
                f"{record.error_code:<10} "
                f"{message:<30}"
            )
            
            if record.severity in severity_counts:
                severity_counts[record.severity] += 1
        
        # Summary
        self._add_line("")
        self._add_line("=" * 80)
        self._add_line("ERROR SUMMARY")
        self._add_line("-" * 40)
        self._add_line(f"Total Errors:     {len(records):>10}")
        self._add_line(f"Info (I):         {severity_counts['I']:>10}")
        self._add_line(f"Warning (W):      {severity_counts['W']:>10}")
        self._add_line(f"Error (E):        {severity_counts['E']:>10}")
        self._add_line(f"Severe (S):       {severity_counts['S']:>10}")
    
    def _format_audit_entries(self, records: List[PositionHistory]):
        """Format a list of audit entries."""
        self._add_line(
            f"{'Time':<10} {'Portfolio':<10} {'Investment':<12} "
            f"{'Action':<8} {'User':<10}"
        )
        
        for record in records:
            time_str = record.process_date.strftime("%H:%M:%S") if record.process_date else ""
            action_desc = self._get_action_description(record.action_code)
            
            self._add_line(
                f"{time_str:<10} "
                f"{record.portfolio_id:<10} "
                f"{record.investment_id:<12} "
                f"{action_desc:<8} "
                f"{record.process_user or '':<10}"
            )
    
    def _get_action_description(self, action_code: str) -> str:
        """Get human-readable description for action code."""
        descriptions = {
            'A': 'ADD',
            'C': 'CHANGE',
            'D': 'DELETE'
        }
        return descriptions.get(action_code, action_code)
    
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

"""
Audit Report Generator - Migrated from COBOL RPTAUD00 program.

This module generates audit trail reports for tracking system changes,
user activities, and security events.

Original COBOL Program: src/programs/batch/RPTAUD00.cbl
"""

import logging
from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Dict, Optional, TextIO
import io
import json

from sqlalchemy.orm import Session

from ...models.history import History, HistoryRecordType, HistoryActionCode
from ...database.connection import get_session

logger = logging.getLogger(__name__)


@dataclass
class AuditEntry:
    """Single audit entry"""
    timestamp: str
    portfolio_id: str
    record_type: str
    action: str
    user: str
    before_summary: str
    after_summary: str


@dataclass
class AuditReportResult:
    """Result of audit report generation"""
    report_date: str
    start_date: str
    end_date: str
    entries_processed: int
    report_content: str
    return_code: int = 0


class AuditReportGenerator:
    """
    Audit Report Generator - Migrated from COBOL RPTAUD00.
    
    Generates audit trail reports including:
    - User activity tracking
    - Data change history
    - Security events
    - System operations
    
    Original COBOL program flow:
    1. 0000-MAIN: Main control
    2. 1000-INITIALIZE: Open files, set date range
    3. 2000-PROCESS: Read audit records, format output
    4. 3000-TERMINATE: Print summary, close files
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
        
        logger.info("AuditReportGenerator initialized")
    
    @property
    def session(self) -> Session:
        """Get or create database session"""
        if self._session is None:
            self._session = get_session()
        return self._session
    
    def generate_report(
        self,
        start_date: str = None,
        end_date: str = None,
        portfolio_ids: List[str] = None,
        record_types: List[HistoryRecordType] = None,
        users: List[str] = None,
        output_file: str = None
    ) -> AuditReportResult:
        """
        Generate audit report.
        
        Args:
            start_date: Start date (YYYYMMDD)
            end_date: End date (YYYYMMDD)
            portfolio_ids: Filter by portfolio IDs
            record_types: Filter by record types
            users: Filter by users
            output_file: Output file path (optional)
            
        Returns:
            AuditReportResult with report content and statistics
        """
        # Default date range to last 7 days
        if end_date is None:
            end_date = datetime.now().strftime('%Y%m%d')
        if start_date is None:
            start_dt = datetime.now()
            start_date = (start_dt.replace(day=start_dt.day - 7) 
                         if start_dt.day > 7 
                         else start_dt.replace(month=start_dt.month - 1, day=28)).strftime('%Y%m%d')
        
        logger.info(f"Generating audit report for {start_date} to {end_date}")
        
        # Get audit entries
        entries = self._get_audit_entries(
            start_date, end_date, portfolio_ids, record_types, users
        )
        
        # Generate report content
        output = io.StringIO()
        self._print_report_header(output, start_date, end_date)
        
        # Group by date
        entries_by_date: Dict[str, List[AuditEntry]] = {}
        for entry in entries:
            date = entry.timestamp[:8]
            if date not in entries_by_date:
                entries_by_date[date] = []
            entries_by_date[date].append(entry)
        
        # Print entries by date
        for date in sorted(entries_by_date.keys()):
            self._print_date_section(output, date, entries_by_date[date])
        
        self._print_report_footer(output, len(entries))
        
        report_content = output.getvalue()
        
        # Write to file if specified
        if output_file:
            with open(output_file, 'w') as f:
                f.write(report_content)
            logger.info(f"Report written to: {output_file}")
        
        return AuditReportResult(
            report_date=datetime.now().strftime('%Y%m%d'),
            start_date=start_date,
            end_date=end_date,
            entries_processed=len(entries),
            report_content=report_content,
            return_code=0
        )
    
    def _get_audit_entries(
        self,
        start_date: str,
        end_date: str,
        portfolio_ids: List[str] = None,
        record_types: List[HistoryRecordType] = None,
        users: List[str] = None
    ) -> List[AuditEntry]:
        """Get audit entries from database"""
        query = self.session.query(History).filter(
            History.date >= start_date,
            History.date <= end_date
        )
        
        if portfolio_ids:
            query = query.filter(History.portfolio_id.in_(portfolio_ids))
        
        if record_types:
            type_values = [rt.value for rt in record_types]
            query = query.filter(History.record_type.in_(type_values))
        
        if users:
            query = query.filter(History.process_user.in_(users))
        
        query = query.order_by(History.date, History.time)
        
        entries = []
        for record in query.all():
            # Parse before/after images for summary
            before_summary = self._summarize_image(record.before_image)
            after_summary = self._summarize_image(record.after_image)
            
            entry = AuditEntry(
                timestamp=f"{record.date} {record.time}",
                portfolio_id=record.portfolio_id,
                record_type=record.record_type,
                action=record.action_code,
                user=record.process_user or 'SYSTEM',
                before_summary=before_summary,
                after_summary=after_summary
            )
            entries.append(entry)
        
        return entries
    
    def _summarize_image(self, image: str, max_length: int = 50) -> str:
        """Summarize before/after image for display"""
        if not image:
            return '-'
        
        try:
            data = json.loads(image)
            if isinstance(data, dict):
                # Extract key fields
                summary_parts = []
                for key in ['investment_id', 'quantity', 'amount', 'status']:
                    if key in data:
                        summary_parts.append(f"{key}={data[key]}")
                return ', '.join(summary_parts[:3]) if summary_parts else str(data)[:max_length]
            return str(data)[:max_length]
        except json.JSONDecodeError:
            return image[:max_length] if len(image) > max_length else image
    
    def _print_report_header(self, output: TextIO, start_date: str, end_date: str):
        """Print report header"""
        output.write("=" * self.page_width + "\n")
        output.write(f"{'INVESTMENT PORTFOLIO MANAGEMENT SYSTEM':^{self.page_width}}\n")
        output.write(f"{'AUDIT TRAIL REPORT - RPTAUD00':^{self.page_width}}\n")
        output.write(f"{'Date Range: ' + start_date + ' to ' + end_date:^{self.page_width}}\n")
        output.write(f"{'Generated: ' + datetime.now().strftime('%Y-%m-%d %H:%M:%S'):^{self.page_width}}\n")
        output.write("=" * self.page_width + "\n")
        output.write("\n")
    
    def _print_date_section(self, output: TextIO, date: str, entries: List[AuditEntry]):
        """Print entries for a specific date"""
        output.write("-" * self.page_width + "\n")
        output.write(f"Date: {date}\n")
        output.write("-" * self.page_width + "\n")
        
        # Column headers
        output.write(f"{'Time':<8} {'Portfolio':<10} {'Type':<4} {'Action':<6} "
                    f"{'User':<10} {'Before':<40} {'After':<40}\n")
        output.write("-" * self.page_width + "\n")
        
        for entry in entries:
            time_str = entry.timestamp[9:15] if len(entry.timestamp) > 9 else entry.timestamp
            output.write(
                f"{time_str:<8} "
                f"{entry.portfolio_id:<10} "
                f"{entry.record_type:<4} "
                f"{entry.action:<6} "
                f"{entry.user:<10} "
                f"{entry.before_summary:<40} "
                f"{entry.after_summary:<40}\n"
            )
        
        output.write(f"  Entries for {date}: {len(entries)}\n")
        output.write("\n")
    
    def _print_report_footer(self, output: TextIO, total_entries: int):
        """Print report footer"""
        output.write("=" * self.page_width + "\n")
        output.write(f"{'REPORT SUMMARY':^{self.page_width}}\n")
        output.write("=" * self.page_width + "\n")
        output.write(f"Total Audit Entries: {total_entries}\n")
        output.write("=" * self.page_width + "\n")
        output.write(f"{'*** END OF REPORT ***':^{self.page_width}}\n")
    
    def close(self):
        """Close session if owned"""
        if self._owns_session and self._session:
            self._session.close()
            self._session = None

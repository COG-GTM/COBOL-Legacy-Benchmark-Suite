"""
Position Report Generator - Python implementation of RPTPOS00.cbl

This module implements the position reporting logic from the COBOL
program RPTPOS00, which generates portfolio position and valuation reports.

Original COBOL Program: src/programs/batch/RPTPOS00.cbl

Key Functions:
- Generate portfolio position summary reports
- Calculate portfolio valuations and performance
- Produce detailed position listings
- Create summary statistics by portfolio, investment type, etc.

Report Types:
- Position Summary: Overview of all positions
- Position Detail: Detailed listing with all fields
- Valuation Report: Market values and unrealized gains/losses
- Performance Report: Returns and performance metrics
"""

import logging
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path
from typing import List, Optional, Dict, Any
from enum import Enum

from sqlalchemy import func

from migration.python.database.orm_models import PortfolioMaster, PositionHistory
from migration.python.database.session import DatabaseManager

# Configure logging
logger = logging.getLogger(__name__)


class ReportFormat(str, Enum):
    """Output format for reports."""
    TEXT = 'text'
    CSV = 'csv'
    JSON = 'json'
    HTML = 'html'


class ReportType(str, Enum):
    """Type of position report."""
    SUMMARY = 'summary'
    DETAIL = 'detail'
    VALUATION = 'valuation'
    PERFORMANCE = 'performance'


@dataclass
class PositionSummary:
    """Summary data for a single position."""
    portfolio_id: str
    investment_id: str
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    currency: str
    unrealized_gain_loss: Decimal
    unrealized_gain_loss_pct: Decimal
    status: str
    
    @classmethod
    def from_db_record(cls, record: PortfolioMaster) -> 'PositionSummary':
        """Create from database record."""
        quantity = Decimal(str(record.quantity))
        cost_basis = Decimal(str(record.cost_basis))
        market_value = Decimal(str(record.market_value))
        
        unrealized_gl = market_value - cost_basis
        unrealized_gl_pct = Decimal('0')
        if cost_basis != 0:
            unrealized_gl_pct = (unrealized_gl / cost_basis * 100).quantize(
                Decimal('0.01'), rounding=ROUND_HALF_UP)
        
        return cls(
            portfolio_id=record.portfolio_id.strip(),
            investment_id=record.investment_id.strip(),
            quantity=quantity,
            cost_basis=cost_basis,
            market_value=market_value,
            currency=record.currency,
            unrealized_gain_loss=unrealized_gl,
            unrealized_gain_loss_pct=unrealized_gl_pct,
            status=record.status
        )


@dataclass
class PortfolioSummary:
    """Summary data for an entire portfolio."""
    portfolio_id: str
    position_count: int
    total_cost_basis: Decimal
    total_market_value: Decimal
    total_unrealized_gain_loss: Decimal
    total_unrealized_gain_loss_pct: Decimal
    positions: List[PositionSummary] = field(default_factory=list)


@dataclass
class ReportConfig:
    """Configuration for report generation."""
    report_type: ReportType = ReportType.SUMMARY
    output_format: ReportFormat = ReportFormat.TEXT
    include_closed_positions: bool = False
    group_by_portfolio: bool = True
    sort_by: str = 'portfolio_id'
    page_size: int = 60  # Lines per page for text reports
    report_date: Optional[str] = None  # YYYYMMDD


class PositionReportGenerator:
    """
    Position Report Generator - Python implementation of RPTPOS00.cbl
    
    This class generates portfolio position and valuation reports,
    following the same business logic as the original COBOL program.
    
    The report generation follows the same flow as RPTPOS00:
    1. Initialize report (set headers, page counters)
    2. Read position data from database
    3. Calculate summaries and totals
    4. Format and output report
    5. Generate control totals
    
    Usage:
        generator = PositionReportGenerator(db_manager)
        report = generator.generate_report(
            report_type=ReportType.SUMMARY,
            portfolio_id='PORT001'
        )
        
        # Save to file
        generator.save_report(report, 'position_report.txt')
    """
    
    def __init__(self, db_manager: DatabaseManager,
                 config: Optional[ReportConfig] = None):
        """
        Initialize the report generator.
        
        Args:
            db_manager: Database manager for data access
            config: Report configuration
        """
        self.db_manager = db_manager
        self.config = config or ReportConfig()
        self._page_number = 0
        self._line_count = 0
        self._report_lines: List[str] = []
    
    def generate_report(self, report_type: Optional[ReportType] = None,
                        portfolio_id: Optional[str] = None,
                        investment_id: Optional[str] = None,
                        report_date: Optional[str] = None) -> str:
        """
        Generate a position report.
        
        Args:
            report_type: Type of report to generate
            portfolio_id: Filter by portfolio ID
            investment_id: Filter by investment ID
            report_date: Report as-of date (YYYYMMDD)
            
        Returns:
            Formatted report string
        """
        if report_type:
            self.config.report_type = report_type
        if report_date:
            self.config.report_date = report_date
        
        logger.info(f"Generating {self.config.report_type.value} report")
        
        # Initialize report
        self._initialize_report()
        
        # Get position data
        positions = self._get_positions(portfolio_id, investment_id)
        
        # Generate report based on type
        if self.config.report_type == ReportType.SUMMARY:
            self._generate_summary_report(positions)
        elif self.config.report_type == ReportType.DETAIL:
            self._generate_detail_report(positions)
        elif self.config.report_type == ReportType.VALUATION:
            self._generate_valuation_report(positions)
        elif self.config.report_type == ReportType.PERFORMANCE:
            self._generate_performance_report(positions)
        
        # Finalize report
        self._finalize_report()
        
        return "\n".join(self._report_lines)
    
    def generate_portfolio_summary(self, portfolio_id: str) -> PortfolioSummary:
        """
        Generate a summary for a single portfolio.
        
        Args:
            portfolio_id: Portfolio identifier
            
        Returns:
            PortfolioSummary object
        """
        positions = self._get_positions(portfolio_id=portfolio_id)
        
        total_cost = Decimal('0')
        total_market = Decimal('0')
        position_summaries = []
        
        for pos in positions:
            summary = PositionSummary.from_db_record(pos)
            position_summaries.append(summary)
            total_cost += summary.cost_basis
            total_market += summary.market_value
        
        total_gl = total_market - total_cost
        total_gl_pct = Decimal('0')
        if total_cost != 0:
            total_gl_pct = (total_gl / total_cost * 100).quantize(
                Decimal('0.01'), rounding=ROUND_HALF_UP)
        
        return PortfolioSummary(
            portfolio_id=portfolio_id,
            position_count=len(position_summaries),
            total_cost_basis=total_cost,
            total_market_value=total_market,
            total_unrealized_gain_loss=total_gl,
            total_unrealized_gain_loss_pct=total_gl_pct,
            positions=position_summaries
        )
    
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
        
        logger.info(f"Report saved to {output_file}")
    
    def _initialize_report(self):
        """Initialize report generation."""
        self._page_number = 0
        self._line_count = 0
        self._report_lines = []
    
    def _finalize_report(self):
        """Finalize report generation."""
        self._add_line("")
        self._add_line("=" * 80)
        self._add_line("END OF REPORT")
        self._add_line("=" * 80)
    
    def _get_positions(self, portfolio_id: Optional[str] = None,
                       investment_id: Optional[str] = None) -> List[PortfolioMaster]:
        """
        Get position records from database.
        
        Args:
            portfolio_id: Filter by portfolio ID
            investment_id: Filter by investment ID
            
        Returns:
            List of PortfolioMaster records
        """
        with self.db_manager.session_scope() as session:
            query = session.query(PortfolioMaster)
            
            if not self.config.include_closed_positions:
                query = query.filter(PortfolioMaster.status == 'A')
            
            if portfolio_id:
                query = query.filter(PortfolioMaster.portfolio_id == portfolio_id)
            if investment_id:
                query = query.filter(PortfolioMaster.investment_id == investment_id)
            
            # Apply sorting
            if self.config.sort_by == 'portfolio_id':
                query = query.order_by(
                    PortfolioMaster.portfolio_id,
                    PortfolioMaster.investment_id
                )
            elif self.config.sort_by == 'investment_id':
                query = query.order_by(
                    PortfolioMaster.investment_id,
                    PortfolioMaster.portfolio_id
                )
            elif self.config.sort_by == 'market_value':
                query = query.order_by(PortfolioMaster.market_value.desc())
            
            return query.all()
    
    def _generate_summary_report(self, positions: List[PortfolioMaster]):
        """
        Generate position summary report.
        
        Corresponds to the summary report logic in RPTPOS00.
        """
        report_date = self.config.report_date or datetime.now().strftime("%Y%m%d")
        
        self._add_page_header("POSITION SUMMARY REPORT", report_date)
        
        # Group by portfolio if configured
        if self.config.group_by_portfolio:
            portfolios: Dict[str, List[PortfolioMaster]] = {}
            for pos in positions:
                pid = pos.portfolio_id.strip()
                if pid not in portfolios:
                    portfolios[pid] = []
                portfolios[pid].append(pos)
            
            grand_total_cost = Decimal('0')
            grand_total_market = Decimal('0')
            
            for portfolio_id, portfolio_positions in sorted(portfolios.items()):
                self._add_line("")
                self._add_line(f"Portfolio: {portfolio_id}")
                self._add_line("-" * 70)
                
                # Column headers
                self._add_line(
                    f"{'Investment':<12} {'Quantity':>12} {'Cost Basis':>14} "
                    f"{'Market Value':>14} {'Unreal G/L':>12} {'%':>7}"
                )
                self._add_line("-" * 70)
                
                portfolio_cost = Decimal('0')
                portfolio_market = Decimal('0')
                
                for pos in portfolio_positions:
                    summary = PositionSummary.from_db_record(pos)
                    
                    self._add_line(
                        f"{summary.investment_id:<12} "
                        f"{summary.quantity:>12.4f} "
                        f"{summary.cost_basis:>14.2f} "
                        f"{summary.market_value:>14.2f} "
                        f"{summary.unrealized_gain_loss:>12.2f} "
                        f"{summary.unrealized_gain_loss_pct:>7.2f}"
                    )
                    
                    portfolio_cost += summary.cost_basis
                    portfolio_market += summary.market_value
                
                # Portfolio totals
                portfolio_gl = portfolio_market - portfolio_cost
                portfolio_gl_pct = Decimal('0')
                if portfolio_cost != 0:
                    portfolio_gl_pct = (portfolio_gl / portfolio_cost * 100).quantize(
                        Decimal('0.01'), rounding=ROUND_HALF_UP)
                
                self._add_line("-" * 70)
                self._add_line(
                    f"{'Portfolio Total':<12} "
                    f"{'':>12} "
                    f"{portfolio_cost:>14.2f} "
                    f"{portfolio_market:>14.2f} "
                    f"{portfolio_gl:>12.2f} "
                    f"{portfolio_gl_pct:>7.2f}"
                )
                
                grand_total_cost += portfolio_cost
                grand_total_market += portfolio_market
            
            # Grand totals
            grand_gl = grand_total_market - grand_total_cost
            grand_gl_pct = Decimal('0')
            if grand_total_cost != 0:
                grand_gl_pct = (grand_gl / grand_total_cost * 100).quantize(
                    Decimal('0.01'), rounding=ROUND_HALF_UP)
            
            self._add_line("")
            self._add_line("=" * 70)
            self._add_line(
                f"{'GRAND TOTAL':<12} "
                f"{'':>12} "
                f"{grand_total_cost:>14.2f} "
                f"{grand_total_market:>14.2f} "
                f"{grand_gl:>12.2f} "
                f"{grand_gl_pct:>7.2f}"
            )
        else:
            # Flat listing
            self._add_line(
                f"{'Portfolio':<10} {'Investment':<12} {'Quantity':>12} "
                f"{'Cost Basis':>14} {'Market Value':>14}"
            )
            self._add_line("-" * 70)
            
            for pos in positions:
                summary = PositionSummary.from_db_record(pos)
                self._add_line(
                    f"{summary.portfolio_id:<10} "
                    f"{summary.investment_id:<12} "
                    f"{summary.quantity:>12.4f} "
                    f"{summary.cost_basis:>14.2f} "
                    f"{summary.market_value:>14.2f}"
                )
    
    def _generate_detail_report(self, positions: List[PortfolioMaster]):
        """
        Generate detailed position report.
        
        Includes all fields for each position.
        """
        report_date = self.config.report_date or datetime.now().strftime("%Y%m%d")
        
        self._add_page_header("POSITION DETAIL REPORT", report_date)
        
        for pos in positions:
            summary = PositionSummary.from_db_record(pos)
            
            self._add_line("")
            self._add_line("-" * 50)
            self._add_line(f"Portfolio ID:        {summary.portfolio_id}")
            self._add_line(f"Investment ID:       {summary.investment_id}")
            self._add_line(f"Status:              {summary.status}")
            self._add_line(f"Currency:            {summary.currency}")
            self._add_line(f"Quantity:            {summary.quantity:,.4f}")
            self._add_line(f"Cost Basis:          {summary.cost_basis:,.2f}")
            self._add_line(f"Market Value:        {summary.market_value:,.2f}")
            self._add_line(f"Unrealized G/L:      {summary.unrealized_gain_loss:,.2f}")
            self._add_line(f"Unrealized G/L %:    {summary.unrealized_gain_loss_pct:.2f}%")
            
            if pos.last_maint_date:
                self._add_line(f"Last Updated:        {pos.last_maint_date}")
            if pos.last_maint_user:
                self._add_line(f"Updated By:          {pos.last_maint_user}")
    
    def _generate_valuation_report(self, positions: List[PortfolioMaster]):
        """
        Generate valuation report focusing on market values and gains/losses.
        """
        report_date = self.config.report_date or datetime.now().strftime("%Y%m%d")
        
        self._add_page_header("PORTFOLIO VALUATION REPORT", report_date)
        
        # Calculate totals
        total_cost = Decimal('0')
        total_market = Decimal('0')
        gains = Decimal('0')
        losses = Decimal('0')
        
        self._add_line("")
        self._add_line(
            f"{'Portfolio':<10} {'Investment':<12} "
            f"{'Cost Basis':>14} {'Market Value':>14} {'Gain/Loss':>14}"
        )
        self._add_line("=" * 70)
        
        for pos in positions:
            summary = PositionSummary.from_db_record(pos)
            
            self._add_line(
                f"{summary.portfolio_id:<10} "
                f"{summary.investment_id:<12} "
                f"{summary.cost_basis:>14.2f} "
                f"{summary.market_value:>14.2f} "
                f"{summary.unrealized_gain_loss:>14.2f}"
            )
            
            total_cost += summary.cost_basis
            total_market += summary.market_value
            
            if summary.unrealized_gain_loss >= 0:
                gains += summary.unrealized_gain_loss
            else:
                losses += summary.unrealized_gain_loss
        
        total_gl = total_market - total_cost
        
        self._add_line("=" * 70)
        self._add_line("")
        self._add_line("VALUATION SUMMARY")
        self._add_line("-" * 40)
        self._add_line(f"Total Cost Basis:      {total_cost:>18,.2f}")
        self._add_line(f"Total Market Value:    {total_market:>18,.2f}")
        self._add_line(f"Net Unrealized G/L:    {total_gl:>18,.2f}")
        self._add_line("")
        self._add_line(f"Total Gains:           {gains:>18,.2f}")
        self._add_line(f"Total Losses:          {losses:>18,.2f}")
    
    def _generate_performance_report(self, positions: List[PortfolioMaster]):
        """
        Generate performance report with return calculations.
        """
        report_date = self.config.report_date or datetime.now().strftime("%Y%m%d")
        
        self._add_page_header("PORTFOLIO PERFORMANCE REPORT", report_date)
        
        # Group by portfolio
        portfolios: Dict[str, List[PositionSummary]] = {}
        for pos in positions:
            pid = pos.portfolio_id.strip()
            if pid not in portfolios:
                portfolios[pid] = []
            portfolios[pid].append(PositionSummary.from_db_record(pos))
        
        self._add_line("")
        self._add_line(
            f"{'Portfolio':<12} {'Positions':>10} {'Cost Basis':>14} "
            f"{'Market Value':>14} {'Return %':>10}"
        )
        self._add_line("=" * 65)
        
        grand_cost = Decimal('0')
        grand_market = Decimal('0')
        
        for portfolio_id, portfolio_positions in sorted(portfolios.items()):
            cost = sum(p.cost_basis for p in portfolio_positions)
            market = sum(p.market_value for p in portfolio_positions)
            
            return_pct = Decimal('0')
            if cost != 0:
                return_pct = ((market - cost) / cost * 100).quantize(
                    Decimal('0.01'), rounding=ROUND_HALF_UP)
            
            self._add_line(
                f"{portfolio_id:<12} "
                f"{len(portfolio_positions):>10} "
                f"{cost:>14.2f} "
                f"{market:>14.2f} "
                f"{return_pct:>10.2f}"
            )
            
            grand_cost += cost
            grand_market += market
        
        grand_return = Decimal('0')
        if grand_cost != 0:
            grand_return = ((grand_market - grand_cost) / grand_cost * 100).quantize(
                Decimal('0.01'), rounding=ROUND_HALF_UP)
        
        self._add_line("=" * 65)
        self._add_line(
            f"{'TOTAL':<12} "
            f"{len(positions):>10} "
            f"{grand_cost:>14.2f} "
            f"{grand_market:>14.2f} "
            f"{grand_return:>10.2f}"
        )
    
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
        
        # Check for page break
        if self._line_count >= self.config.page_size:
            self._line_count = 0

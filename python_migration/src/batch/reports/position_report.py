"""
Position Report Generator - Migrated from COBOL RPTPOS00 program.

This module generates portfolio position reports including valuations,
holdings summaries, and gain/loss analysis.

Original COBOL Program: src/programs/batch/RPTPOS00.cbl
"""

import logging
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal
from typing import List, Dict, Optional, TextIO
import io

from sqlalchemy.orm import Session

from ...models.position import Position, PositionRecord, PositionStatus
from ...database.connection import get_session

logger = logging.getLogger(__name__)


@dataclass
class PositionSummary:
    """Summary of a single position"""
    portfolio_id: str
    investment_id: str
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    unrealized_gain_loss: Decimal
    gain_loss_percent: Decimal


@dataclass
class PortfolioSummary:
    """Summary of a portfolio"""
    portfolio_id: str
    position_count: int
    total_cost_basis: Decimal
    total_market_value: Decimal
    total_unrealized_gain_loss: Decimal
    total_gain_loss_percent: Decimal
    positions: List[PositionSummary] = field(default_factory=list)


@dataclass
class ReportResult:
    """Result of report generation"""
    report_date: str
    portfolios_processed: int
    positions_processed: int
    total_market_value: Decimal
    report_content: str
    return_code: int = 0


class PositionReportGenerator:
    """
    Position Report Generator - Migrated from COBOL RPTPOS00.
    
    Generates portfolio position reports including:
    - Individual position details
    - Portfolio summaries
    - Gain/loss analysis
    - Total portfolio valuation
    
    Original COBOL program flow:
    1. 0000-MAIN: Main control
    2. 1000-INITIALIZE: Open files, print headers
    3. 2000-PROCESS: Read positions, calculate values, print details
    4. 3000-TERMINATE: Print totals, close files
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
        self.lines_per_page = 60
        self.current_line = 0
        self.current_page = 0
        
        logger.info("PositionReportGenerator initialized")
    
    @property
    def session(self) -> Session:
        """Get or create database session"""
        if self._session is None:
            self._session = get_session()
        return self._session
    
    def generate_report(
        self,
        portfolio_ids: List[str] = None,
        report_date: str = None,
        output_file: str = None
    ) -> ReportResult:
        """
        Generate position report.
        
        Args:
            portfolio_ids: List of portfolio IDs (None for all)
            report_date: Report date (defaults to today)
            output_file: Output file path (optional)
            
        Returns:
            ReportResult with report content and statistics
        """
        if report_date is None:
            report_date = datetime.now().strftime('%Y%m%d')
        
        logger.info(f"Generating position report for date: {report_date}")
        
        # Get positions
        portfolios = self._get_portfolio_data(portfolio_ids)
        
        # Generate report content
        output = io.StringIO()
        self._print_report_header(output, report_date)
        
        total_market_value = Decimal('0')
        positions_processed = 0
        
        for portfolio in portfolios:
            self._print_portfolio_section(output, portfolio)
            total_market_value += portfolio.total_market_value
            positions_processed += portfolio.position_count
        
        self._print_report_footer(output, total_market_value, len(portfolios))
        
        report_content = output.getvalue()
        
        # Write to file if specified
        if output_file:
            with open(output_file, 'w') as f:
                f.write(report_content)
            logger.info(f"Report written to: {output_file}")
        
        return ReportResult(
            report_date=report_date,
            portfolios_processed=len(portfolios),
            positions_processed=positions_processed,
            total_market_value=total_market_value,
            report_content=report_content,
            return_code=0
        )
    
    def _get_portfolio_data(self, portfolio_ids: List[str] = None) -> List[PortfolioSummary]:
        """Get portfolio data from database"""
        query = self.session.query(Position).filter(
            Position.status == PositionStatus.ACTIVE.value
        )
        
        if portfolio_ids:
            query = query.filter(Position.portfolio_id.in_(portfolio_ids))
        
        query = query.order_by(Position.portfolio_id, Position.investment_id)
        
        # Group by portfolio
        portfolios: Dict[str, PortfolioSummary] = {}
        
        for pos in query.all():
            portfolio_id = pos.portfolio_id
            
            if portfolio_id not in portfolios:
                portfolios[portfolio_id] = PortfolioSummary(
                    portfolio_id=portfolio_id,
                    position_count=0,
                    total_cost_basis=Decimal('0'),
                    total_market_value=Decimal('0'),
                    total_unrealized_gain_loss=Decimal('0'),
                    total_gain_loss_percent=Decimal('0')
                )
            
            portfolio = portfolios[portfolio_id]
            
            # Calculate position values
            cost_basis = Decimal(str(pos.cost_basis)) if pos.cost_basis else Decimal('0')
            market_value = Decimal(str(pos.market_value)) if pos.market_value else Decimal('0')
            unrealized_gl = market_value - cost_basis
            gl_percent = (unrealized_gl / cost_basis * 100) if cost_basis != 0 else Decimal('0')
            
            position_summary = PositionSummary(
                portfolio_id=portfolio_id,
                investment_id=pos.investment_id,
                quantity=Decimal(str(pos.quantity)) if pos.quantity else Decimal('0'),
                cost_basis=cost_basis,
                market_value=market_value,
                unrealized_gain_loss=unrealized_gl,
                gain_loss_percent=gl_percent
            )
            
            portfolio.positions.append(position_summary)
            portfolio.position_count += 1
            portfolio.total_cost_basis += cost_basis
            portfolio.total_market_value += market_value
            portfolio.total_unrealized_gain_loss += unrealized_gl
        
        # Calculate portfolio-level gain/loss percent
        for portfolio in portfolios.values():
            if portfolio.total_cost_basis != 0:
                portfolio.total_gain_loss_percent = (
                    portfolio.total_unrealized_gain_loss / portfolio.total_cost_basis * 100
                )
        
        return list(portfolios.values())
    
    def _print_report_header(self, output: TextIO, report_date: str):
        """Print report header"""
        self.current_page = 1
        self.current_line = 0
        
        output.write("=" * self.page_width + "\n")
        output.write(f"{'INVESTMENT PORTFOLIO MANAGEMENT SYSTEM':^{self.page_width}}\n")
        output.write(f"{'POSITION REPORT - RPTPOS00':^{self.page_width}}\n")
        output.write(f"{'Report Date: ' + report_date:^{self.page_width}}\n")
        output.write(f"{'Generated: ' + datetime.now().strftime('%Y-%m-%d %H:%M:%S'):^{self.page_width}}\n")
        output.write("=" * self.page_width + "\n")
        output.write("\n")
        
        self.current_line = 7
    
    def _print_portfolio_section(self, output: TextIO, portfolio: PortfolioSummary):
        """Print portfolio section"""
        # Portfolio header
        output.write("-" * self.page_width + "\n")
        output.write(f"Portfolio: {portfolio.portfolio_id}\n")
        output.write("-" * self.page_width + "\n")
        
        # Column headers
        output.write(f"{'Investment':<12} {'Quantity':>15} {'Cost Basis':>15} "
                    f"{'Market Value':>15} {'Unrealized G/L':>15} {'G/L %':>10}\n")
        output.write("-" * self.page_width + "\n")
        
        # Position details
        for pos in portfolio.positions:
            output.write(
                f"{pos.investment_id:<12} "
                f"{pos.quantity:>15,.4f} "
                f"{pos.cost_basis:>15,.2f} "
                f"{pos.market_value:>15,.2f} "
                f"{pos.unrealized_gain_loss:>15,.2f} "
                f"{pos.gain_loss_percent:>9,.2f}%\n"
            )
        
        # Portfolio totals
        output.write("-" * self.page_width + "\n")
        output.write(
            f"{'Portfolio Total':<12} "
            f"{'':>15} "
            f"{portfolio.total_cost_basis:>15,.2f} "
            f"{portfolio.total_market_value:>15,.2f} "
            f"{portfolio.total_unrealized_gain_loss:>15,.2f} "
            f"{portfolio.total_gain_loss_percent:>9,.2f}%\n"
        )
        output.write("\n")
    
    def _print_report_footer(
        self, 
        output: TextIO, 
        total_market_value: Decimal,
        portfolio_count: int
    ):
        """Print report footer"""
        output.write("=" * self.page_width + "\n")
        output.write(f"{'REPORT SUMMARY':^{self.page_width}}\n")
        output.write("=" * self.page_width + "\n")
        output.write(f"Total Portfolios Processed: {portfolio_count}\n")
        output.write(f"Total Market Value: ${total_market_value:,.2f}\n")
        output.write("=" * self.page_width + "\n")
        output.write(f"{'*** END OF REPORT ***':^{self.page_width}}\n")
    
    def close(self):
        """Close session if owned"""
        if self._owns_session and self._session:
            self._session.close()
            self._session = None

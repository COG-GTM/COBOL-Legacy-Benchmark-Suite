"""Position Report module - replaces RPTPOS00.cbl.

Generates daily position reports showing portfolio holdings,
market values, and gain/loss calculations.

COBOL program flow:
- 0000-MAIN: Initialize -> Process -> Terminate
- 1000-INITIALIZE: Open files, print headers
- 2000-PROCESS: Read positions, format report lines
- 2100-PRINT-DETAIL: Write detail line
- 2200-PRINT-SUMMARY: Write portfolio summary
- 3000-TERMINATE: Print totals, close files
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Any

from python_app.common.db2 import StatisticsCollector
from python_app.common.errors import ErrorHandler
from python_app.models.position import PositionRecord
from python_app.models.return_code import RC_SUCCESS

logger = logging.getLogger("portfolio.batch.rptpos00")


class PositionReportGenerator:
    """Position report generator replacing RPTPOS00.cbl."""

    REPORT_TITLE = "DAILY POSITION REPORT"
    LINE_WIDTH = 132

    def __init__(self) -> None:
        self.error_handler = ErrorHandler("RPTPOS00")
        self.stats = StatisticsCollector("RPTPOS00")
        self.report_lines: list[str] = []
        self.total_market_value = Decimal("0")
        self.total_cost_basis = Decimal("0")
        self.total_gain_loss = Decimal("0")
        self.records_processed = 0
        self.page_number = 0

    def initialize(self, report_date: str = "") -> None:
        """Initialize report - replaces 1000-INITIALIZE."""
        self.stats.initialize()
        self.report_lines.clear()
        self.total_market_value = Decimal("0")
        self.total_cost_basis = Decimal("0")
        self.total_gain_loss = Decimal("0")
        self.records_processed = 0
        self.page_number = 1

        if not report_date:
            report_date = datetime.now().strftime("%Y%m%d")

        self._print_header(report_date)

    def _print_header(self, report_date: str) -> None:
        """Print report header."""
        self.report_lines.extend([
            "=" * self.LINE_WIDTH,
            f" {self.REPORT_TITLE}".center(self.LINE_WIDTH),
            f" Report Date: {report_date}  Page: {self.page_number}".center(self.LINE_WIDTH),
            "=" * self.LINE_WIDTH,
            f" {'Portfolio':<10} {'Investment':<12} {'Quantity':>15} "
            f"{'Cost Basis':>15} {'Market Value':>15} {'Gain/Loss':>15} {'Status':<8}",
            "-" * self.LINE_WIDTH,
        ])

    def print_detail(self, position: PositionRecord) -> None:
        """Print detail line - replaces 2100-PRINT-DETAIL."""
        gain_loss = position.market_value - position.cost_basis
        line = (
            f" {position.portfolio_id:<10} {position.investment_id:<12} "
            f"{position.quantity:>15,.4f} {position.cost_basis:>15,.2f} "
            f"{position.market_value:>15,.2f} {gain_loss:>15,.2f} {position.status:<8}"
        )
        self.report_lines.append(line)

        self.total_market_value += position.market_value
        self.total_cost_basis += position.cost_basis
        self.total_gain_loss += gain_loss
        self.records_processed += 1
        self.stats.update("reads")

    def print_summary(self, portfolio_id: str, portfolio_positions: list[PositionRecord]) -> None:
        """Print portfolio summary - replaces 2200-PRINT-SUMMARY."""
        port_value = sum(p.market_value for p in portfolio_positions)
        port_cost = sum(p.cost_basis for p in portfolio_positions)
        port_gl = port_value - port_cost

        self.report_lines.extend([
            "-" * self.LINE_WIDTH,
            f" Portfolio {portfolio_id} Summary: "
            f"Positions={len(portfolio_positions):>5}  "
            f"Cost Basis={port_cost:>15,.2f}  "
            f"Market Value={port_value:>15,.2f}  "
            f"Gain/Loss={port_gl:>15,.2f}",
            "-" * self.LINE_WIDTH,
        ])

    def process_batch(self, positions: list[PositionRecord], report_date: str = "") -> int:
        """Generate the full position report - replaces 0000-MAIN."""
        self.initialize(report_date)

        # Group positions by portfolio
        portfolios: dict[str, list[PositionRecord]] = {}
        for pos in positions:
            portfolios.setdefault(pos.portfolio_id, []).append(pos)

        # Process each portfolio
        for port_id in sorted(portfolios.keys()):
            port_positions = portfolios[port_id]
            for pos in port_positions:
                self.print_detail(pos)
            self.print_summary(port_id, port_positions)

        return self.terminate()

    def terminate(self) -> int:
        """Print grand totals and finalize - replaces 3000-TERMINATE."""
        self.report_lines.extend([
            "=" * self.LINE_WIDTH,
            f" GRAND TOTALS: "
            f"Records={self.records_processed:>8}  "
            f"Cost Basis={self.total_cost_basis:>15,.2f}  "
            f"Market Value={self.total_market_value:>15,.2f}  "
            f"Gain/Loss={self.total_gain_loss:>15,.2f}",
            "=" * self.LINE_WIDTH,
            f" Report generated: {datetime.now().isoformat()}",
        ])

        self.stats.terminate()
        logger.info("RPTPOS00 complete: %d records processed", self.records_processed)
        return RC_SUCCESS

    def get_report(self) -> str:
        """Get the formatted report as a string."""
        return "\n".join(self.report_lines)

    def get_report_data(self) -> dict[str, Any]:
        """Get report data as structured dict (for JSON API)."""
        return {
            "title": self.REPORT_TITLE,
            "generated_at": datetime.now().isoformat(),
            "records_processed": self.records_processed,
            "totals": {
                "market_value": float(self.total_market_value),
                "cost_basis": float(self.total_cost_basis),
                "gain_loss": float(self.total_gain_loss),
            },
        }

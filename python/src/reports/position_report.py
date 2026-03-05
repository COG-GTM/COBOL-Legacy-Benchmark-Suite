"""
Position report generator translated from COBOL program RPTPOS00.cbl.

Translates:
- 2100-READ-POSITIONS: iterate through position master
- 2110-FORMAT-POSITION: format position details
- COMPUTE POS-CHANGE-PCT from previous/current values
"""

import csv
import io
import logging
from datetime import datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.db.repository import PortfolioRepository, PositionRepository

logger = logging.getLogger(__name__)


class PositionReportGenerator:
    """Generate position summary reports. Translates RPTPOS00.cbl."""

    def __init__(self, session: Session):
        self.session = session
        self.position_repo = PositionRepository(session)
        self.portfolio_repo = PortfolioRepository(session)

    def generate(
        self,
        portfolio_id: str | None = None,
        output_format: str = "json",
    ) -> str | dict:
        """
        Generate position report.
        Translates RPTPOS00.cbl 2000-PROCESS main loop.
        """
        # 2100-READ-POSITIONS
        if portfolio_id:
            positions = self.position_repo.list_by_portfolio(portfolio_id)
        else:
            positions = self.position_repo.list_all_active()

        # Format each position (2110-FORMAT-POSITION)
        details = []
        total_cost = Decimal("0.00")
        total_market = Decimal("0.00")
        exceptions = []

        for pos in positions:
            cost = Decimal(str(pos.cost_basis))
            market = Decimal(str(pos.market_value))
            quantity = Decimal(str(pos.quantity))

            # COMPUTE POS-CHANGE-PCT
            change_pct = Decimal("0.00")
            if cost > 0:
                change_pct = (((market - cost) / cost) * Decimal("100")).quantize(Decimal("0.01"))

            # Flag exceptions (e.g., large losses)
            if change_pct < Decimal("-10.00"):
                exceptions.append({
                    "portfolio_id": pos.portfolio_id,
                    "investment_id": pos.investment_id,
                    "change_pct": str(change_pct),
                })

            detail = {
                "portfolio_id": pos.portfolio_id,
                "investment_id": pos.investment_id,
                "position_date": str(pos.position_date),
                "quantity": str(quantity),
                "cost_basis": str(cost),
                "market_value": str(market),
                "unrealized_gain_loss": str((market - cost).quantize(Decimal("0.01"))),
                "change_pct": str(change_pct),
                "currency": pos.currency_code,
                "status": pos.status,
            }
            details.append(detail)
            total_cost += cost
            total_market += market

        total_gain_loss = (total_market - total_cost).quantize(Decimal("0.01"))
        total_change_pct = Decimal("0.00")
        if total_cost > 0:
            total_change_pct = (((total_market - total_cost) / total_cost) * Decimal("100")).quantize(Decimal("0.01"))

        report = {
            "report_title": "POSITION SUMMARY REPORT",
            "generated_at": datetime.now().isoformat(),
            "portfolio_id": portfolio_id or "ALL PORTFOLIOS",
            "positions": details,
            "exceptions": exceptions,
            "summary": {
                "total_positions": len(details),
                "total_cost_basis": str(total_cost),
                "total_market_value": str(total_market),
                "total_unrealized_gain_loss": str(total_gain_loss),
                "total_change_pct": str(total_change_pct),
                "exception_count": len(exceptions),
            },
        }

        if output_format == "csv":
            return self._to_csv(details)
        elif output_format == "json":
            return report
        else:
            return report

    def _to_csv(self, details: list[dict]) -> str:
        """Generate CSV output."""
        if not details:
            return ""
        output = io.StringIO()
        writer = csv.DictWriter(output, fieldnames=details[0].keys())
        writer.writeheader()
        writer.writerows(details)
        return output.getvalue()

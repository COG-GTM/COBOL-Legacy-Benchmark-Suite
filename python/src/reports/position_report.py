"""
Position report generator translated from COBOL program RPTPOS00.cbl.

Replaces:
  - RPTPOS00.cbl 1000-INITIALIZE-REPORT: Open files, print headers
  - RPTPOS00.cbl 2000-PROCESS-PORTFOLIOS: Iterate portfolios
  - RPTPOS00.cbl 2100-GET-POSITIONS: Read positions for portfolio
  - RPTPOS00.cbl 2200-PRINT-DETAIL: Print detail line
  - RPTPOS00.cbl 2300-PRINT-SUBTOTAL: Print portfolio subtotal
  - RPTPOS00.cbl 3000-PRINT-GRAND-TOTAL: Print grand total
  - RPTPOS00.cbl 4000-TERMINATE: Close files

Outputs reports as JSON, CSV, or formatted text.
"""

import csv
import io
import json
import logging
from datetime import date
from decimal import Decimal

from sqlalchemy.orm import Session

from src.db.repository import PortfolioRepository, PositionRepository

logger = logging.getLogger(__name__)


class PositionReportGenerator:
    """
    Position report generator.

    Translates RPTPOS00.cbl paragraph structure.
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._portfolio_repo = PortfolioRepository(session)
        self._position_repo = PositionRepository(session)

    def generate(
        self, report_date: date, output_format: str = "json"
    ) -> str:
        """
        Generate position summary report.

        Args:
            report_date: Date for position snapshot.
            output_format: Output format - 'json', 'csv', or 'text'.

        Returns:
            Formatted report string.
        """
        logger.info("Generating position report for %s", report_date)

        # 2000-PROCESS-PORTFOLIOS
        portfolios = self._portfolio_repo.list_by_status("A")
        report_data: list[dict[str, object]] = []
        grand_total_value = Decimal("0.00")
        grand_total_cost = Decimal("0.00")

        for portfolio in portfolios:
            # 2100-GET-POSITIONS
            positions = self._position_repo.list_by_portfolio(
                portfolio.portfolio_id, report_date
            )
            active_positions = [p for p in positions if p.status == "A"]

            portfolio_value = Decimal("0.00")
            portfolio_cost = Decimal("0.00")
            position_details: list[dict[str, object]] = []

            for pos in active_positions:
                gain_loss = pos.market_value - pos.cost_basis
                portfolio_value += pos.market_value
                portfolio_cost += pos.cost_basis
                position_details.append({
                    "investment_id": pos.investment_id,
                    "quantity": str(pos.quantity),
                    "cost_basis": str(pos.cost_basis),
                    "market_value": str(pos.market_value),
                    "gain_loss": str(gain_loss),
                })

            report_data.append({
                "portfolio_id": portfolio.portfolio_id,
                "portfolio_name": portfolio.portfolio_name,
                "client_name": portfolio.client_name,
                "position_count": len(active_positions),
                "total_value": str(portfolio_value),
                "total_cost": str(portfolio_cost),
                "total_gain_loss": str(portfolio_value - portfolio_cost),
                "positions": position_details,
            })

            grand_total_value += portfolio_value
            grand_total_cost += portfolio_cost

        report = {
            "report_type": "Position Summary",
            "report_date": report_date.isoformat(),
            "portfolio_count": len(report_data),
            "grand_total_value": str(grand_total_value),
            "grand_total_cost": str(grand_total_cost),
            "grand_total_gain_loss": str(grand_total_value - grand_total_cost),
            "portfolios": report_data,
        }

        match output_format.lower():
            case "csv":
                return self._format_csv(report_data, report_date)
            case "text":
                return self._format_text(report, report_date)
            case _:
                return json.dumps(report, indent=2)

    def _format_csv(self, data: list[dict[str, object]], report_date: date) -> str:
        """Format report as CSV."""
        output = io.StringIO()
        writer = csv.writer(output)
        writer.writerow([
            "Portfolio ID", "Portfolio Name", "Investment ID",
            "Quantity", "Cost Basis", "Market Value", "Gain/Loss",
        ])
        for portfolio in data:
            positions = portfolio.get("positions", [])
            if isinstance(positions, list):
                for pos in positions:
                    if isinstance(pos, dict):
                        writer.writerow([
                            portfolio["portfolio_id"],
                            portfolio["portfolio_name"],
                            pos.get("investment_id", ""),
                            pos.get("quantity", ""),
                            pos.get("cost_basis", ""),
                            pos.get("market_value", ""),
                            pos.get("gain_loss", ""),
                        ])
        return output.getvalue()

    def _format_text(self, report: dict[str, object], report_date: date) -> str:
        """Format report as text (replaces COBOL print lines)."""
        lines: list[str] = []
        lines.append("=" * 80)
        lines.append(f"  POSITION SUMMARY REPORT - {report_date}")
        lines.append("=" * 80)
        lines.append("")

        portfolios = report.get("portfolios", [])
        if isinstance(portfolios, list):
            for portfolio in portfolios:
                if isinstance(portfolio, dict):
                    lines.append(
                        f"  Portfolio: {portfolio.get('portfolio_id', '')} - "
                        f"{portfolio.get('portfolio_name', '')}"
                    )
                    lines.append(f"  Client:    {portfolio.get('client_name', '')}")
                    lines.append("-" * 70)
                    lines.append(
                        f"  {'Investment':<12} {'Quantity':>14} {'Cost Basis':>14} "
                        f"{'Market Value':>14} {'Gain/Loss':>14}"
                    )
                    lines.append("-" * 70)

                    positions = portfolio.get("positions", [])
                    if isinstance(positions, list):
                        for pos in positions:
                            if isinstance(pos, dict):
                                lines.append(
                                    f"  {str(pos.get('investment_id', '')):<12} "
                                    f"{str(pos.get('quantity', '')):>14} "
                                    f"{str(pos.get('cost_basis', '')):>14} "
                                    f"{str(pos.get('market_value', '')):>14} "
                                    f"{str(pos.get('gain_loss', '')):>14}"
                                )

                    lines.append("-" * 70)
                    lines.append(
                        f"  SUBTOTAL: Value={portfolio.get('total_value', '')} "
                        f"Cost={portfolio.get('total_cost', '')} "
                        f"G/L={portfolio.get('total_gain_loss', '')}"
                    )
                    lines.append("")

        lines.append("=" * 80)
        lines.append(
            f"  GRAND TOTAL: Value={report.get('grand_total_value', '')} "
            f"Cost={report.get('grand_total_cost', '')} "
            f"G/L={report.get('grand_total_gain_loss', '')}"
        )
        lines.append("=" * 80)
        return "\n".join(lines)

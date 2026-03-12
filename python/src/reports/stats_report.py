"""
Statistics report generator translated from COBOL program RPTSTA00.cbl.

Replaces:
  - RPTSTA00.cbl: Processing statistics and system summaries
"""

import json
import logging
from datetime import date

from sqlalchemy.orm import Session

from src.db.repository import PortfolioRepository, TransactionRepository

logger = logging.getLogger(__name__)


class StatsReportGenerator:
    """
    Statistics report generator.

    Translates RPTSTA00.cbl paragraph structure.
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._portfolio_repo = PortfolioRepository(session)
        self._transaction_repo = TransactionRepository(session)

    def generate(self, output_format: str = "json") -> str:
        """
        Generate processing statistics report.

        Returns:
            Formatted report string.
        """
        logger.info("Generating statistics report")

        total_portfolios = self._portfolio_repo.count()
        active_portfolios = self._portfolio_repo.count(status="A")
        closed_portfolios = self._portfolio_repo.count(status="C")
        suspended_portfolios = self._portfolio_repo.count(status="S")

        pending_txns = self._transaction_repo.count_by_status("P")
        completed_txns = self._transaction_repo.count_by_status("D")
        failed_txns = self._transaction_repo.count_by_status("F")
        reversed_txns = self._transaction_repo.count_by_status("R")

        report = {
            "report_type": "System Statistics",
            "report_date": date.today().isoformat(),
            "portfolios": {
                "total": total_portfolios,
                "active": active_portfolios,
                "closed": closed_portfolios,
                "suspended": suspended_portfolios,
            },
            "transactions": {
                "pending": pending_txns,
                "completed": completed_txns,
                "failed": failed_txns,
                "reversed": reversed_txns,
                "total": pending_txns + completed_txns + failed_txns + reversed_txns,
            },
        }

        if output_format.lower() == "text":
            return self._format_text(report)
        return json.dumps(report, indent=2)

    def _format_text(self, report: dict[str, object]) -> str:
        """Format report as text."""
        lines: list[str] = []
        lines.append("=" * 50)
        lines.append(f"  SYSTEM STATISTICS - {report.get('report_date', '')}")
        lines.append("=" * 50)
        lines.append("")

        portfolios = report.get("portfolios", {})
        if isinstance(portfolios, dict):
            lines.append("  PORTFOLIOS:")
            lines.append(f"    Total:      {portfolios.get('total', 0)}")
            lines.append(f"    Active:     {portfolios.get('active', 0)}")
            lines.append(f"    Closed:     {portfolios.get('closed', 0)}")
            lines.append(f"    Suspended:  {portfolios.get('suspended', 0)}")

        lines.append("")

        transactions = report.get("transactions", {})
        if isinstance(transactions, dict):
            lines.append("  TRANSACTIONS:")
            lines.append(f"    Pending:    {transactions.get('pending', 0)}")
            lines.append(f"    Completed:  {transactions.get('completed', 0)}")
            lines.append(f"    Failed:     {transactions.get('failed', 0)}")
            lines.append(f"    Reversed:   {transactions.get('reversed', 0)}")
            lines.append(f"    Total:      {transactions.get('total', 0)}")

        lines.append("")
        lines.append("=" * 50)
        return "\n".join(lines)

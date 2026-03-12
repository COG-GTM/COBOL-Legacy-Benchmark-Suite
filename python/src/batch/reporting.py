"""
Batch reporting translated from COBOL programs:
  - RPTPOS00.cbl: Position report
  - RPTAUD00.cbl: Audit report
  - RPTSTA00.cbl: Statistics report

These programs generate formatted reports from batch processing data.
"""

import logging
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.db.repository import (
    AuditRepository,
    PortfolioRepository,
    PositionRepository,
    TransactionRepository,
)

logger = logging.getLogger(__name__)


class PositionReportData:
    """Position report data container."""

    def __init__(self) -> None:
        self.report_date: date = date.today()
        self.portfolios: list[dict[str, object]] = []
        self.total_market_value: Decimal = Decimal("0.00")
        self.total_cost_basis: Decimal = Decimal("0.00")
        self.total_positions: int = 0


class AuditReportData:
    """Audit report data container."""

    def __init__(self) -> None:
        self.start_date: datetime = datetime.now()
        self.end_date: datetime = datetime.now()
        self.entries: list[dict[str, object]] = []
        self.total_entries: int = 0


class StatsReportData:
    """Statistics report data container."""

    def __init__(self) -> None:
        self.report_date: date = date.today()
        self.total_portfolios: int = 0
        self.active_portfolios: int = 0
        self.total_transactions: int = 0
        self.pending_transactions: int = 0
        self.completed_transactions: int = 0
        self.failed_transactions: int = 0
        self.reversed_transactions: int = 0
        self.archived_transactions: int = 0


class BatchReporting:
    """
    Batch reporting service.

    Translates:
      RPTPOS00.cbl -> generate_position_report()
      RPTAUD00.cbl -> generate_audit_report()
      RPTSTA00.cbl -> generate_stats_report()
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._portfolio_repo = PortfolioRepository(session)
        self._position_repo = PositionRepository(session)
        self._transaction_repo = TransactionRepository(session)
        self._audit_repo = AuditRepository(session)

    def generate_position_report(self, report_date: date) -> PositionReportData:
        """
        Generate position summary report.

        Translates RPTPOS00.cbl:
          1000-INITIALIZE-REPORT
          2000-PROCESS-PORTFOLIOS
          2100-GET-POSITIONS
          2200-CALCULATE-TOTALS
          3000-WRITE-REPORT
        """
        logger.info("Generating position report for %s", report_date)
        report = PositionReportData()
        report.report_date = report_date

        # 2000-PROCESS-PORTFOLIOS: Iterate all active portfolios
        portfolios = self._portfolio_repo.list_by_status("A")
        for portfolio in portfolios:
            # 2100-GET-POSITIONS
            positions = self._position_repo.list_by_portfolio(
                portfolio.portfolio_id, report_date
            )
            active_positions = [p for p in positions if p.status == "A"]

            portfolio_total_value = Decimal("0.00")
            portfolio_cost_basis = Decimal("0.00")
            position_details: list[dict[str, object]] = []

            for pos in active_positions:
                portfolio_total_value += pos.market_value
                portfolio_cost_basis += pos.cost_basis
                position_details.append({
                    "investment_id": pos.investment_id,
                    "quantity": pos.quantity,
                    "cost_basis": pos.cost_basis,
                    "market_value": pos.market_value,
                    "gain_loss": pos.market_value - pos.cost_basis,
                })

            # 2200-CALCULATE-TOTALS
            report.portfolios.append({
                "portfolio_id": portfolio.portfolio_id,
                "portfolio_name": portfolio.portfolio_name,
                "client_name": portfolio.client_name,
                "total_value": portfolio_total_value,
                "cost_basis": portfolio_cost_basis,
                "gain_loss": portfolio_total_value - portfolio_cost_basis,
                "position_count": len(active_positions),
                "positions": position_details,
            })

            report.total_market_value += portfolio_total_value
            report.total_cost_basis += portfolio_cost_basis
            report.total_positions += len(active_positions)

        logger.info(
            "Position report generated: %d portfolios, %d positions, total value %s",
            len(report.portfolios),
            report.total_positions,
            report.total_market_value,
        )
        return report

    def generate_audit_report(
        self, start_date: datetime, end_date: datetime
    ) -> AuditReportData:
        """
        Generate audit trail report.

        Translates RPTAUD00.cbl:
          1000-INITIALIZE-REPORT
          2000-READ-AUDIT-RECORDS
          3000-FORMAT-REPORT
        """
        logger.info(
            "Generating audit report from %s to %s", start_date, end_date
        )
        report = AuditReportData()
        report.start_date = start_date
        report.end_date = end_date

        # 2000-READ-AUDIT-RECORDS
        audit_records = self._audit_repo.list_by_date_range(start_date, end_date)

        for record in audit_records:
            report.entries.append({
                "timestamp": record.timestamp.isoformat(),
                "user_id": record.user_id,
                "action": record.action,
                "portfolio_id": record.portfolio_id,
                "status": record.status,
                "message": record.message,
            })

        report.total_entries = len(report.entries)

        logger.info("Audit report generated: %d entries", report.total_entries)
        return report

    def generate_stats_report(self) -> StatsReportData:
        """
        Generate processing statistics report.

        Translates RPTSTA00.cbl:
          1000-GATHER-STATISTICS
          2000-FORMAT-REPORT
        """
        logger.info("Generating statistics report")
        report = StatsReportData()
        report.report_date = date.today()

        # 1000-GATHER-STATISTICS
        report.total_portfolios = self._portfolio_repo.count()
        report.active_portfolios = self._portfolio_repo.count(status="A")
        report.pending_transactions = self._transaction_repo.count_by_status("P")
        report.completed_transactions = self._transaction_repo.count_by_status("D")
        report.failed_transactions = self._transaction_repo.count_by_status("F")
        report.reversed_transactions = self._transaction_repo.count_by_status("R")
        report.archived_transactions = self._transaction_repo.count_by_status("X")
        report.total_transactions = (
            report.pending_transactions
            + report.completed_transactions
            + report.failed_transactions
            + report.reversed_transactions
            + report.archived_transactions
        )

        logger.info(
            "Stats report: portfolios=%d (active=%d), transactions=%d",
            report.total_portfolios,
            report.active_portfolios,
            report.total_transactions,
        )
        return report

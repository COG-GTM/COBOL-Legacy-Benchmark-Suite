"""Daily Position Report Generator - migrated from RPTPOS00.cbl.

Generates daily position report including portfolio position summary,
transaction activity, exception reporting, and performance metrics.
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Optional

from portfolio_management.models.position import PositionRecord
from portfolio_management.models.transaction import TransactionRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "RPTPOS00"
REPORT_WIDTH = 132


class PositionReportGenerator:
    def __init__(self):
        self._report_lines: list[str] = []
        self._page_number = 0
        self._line_count = 0
        self._lines_per_page = 60
        self._total_positions = 0
        self._total_market_value = Decimal("0")
        self._total_cost_basis = Decimal("0")

    def generate(
        self,
        positions: list[PositionRecord],
        transactions: list[TransactionRecord],
        report_date: Optional[str] = None,
    ) -> int:
        if report_date is None:
            report_date = datetime.now().strftime("%Y-%m-%d")

        self._report_lines = []
        self._page_number = 0
        self._total_positions = 0
        self._total_market_value = Decimal("0")
        self._total_cost_basis = Decimal("0")

        self._write_report_header(report_date)
        self._write_position_summary(positions)
        self._write_transaction_activity(transactions)
        self._write_exception_report(positions)
        self._write_performance_metrics(positions)
        self._write_report_footer()

        return ReturnCode.SUCCESS

    def _write_report_header(self, report_date: str) -> None:
        self._new_page()
        self._write_line("=" * REPORT_WIDTH)
        self._write_line(
            f"{'DAILY POSITION REPORT':^{REPORT_WIDTH}}"
        )
        self._write_line(f"{'Report Date: ' + report_date:^{REPORT_WIDTH}}")
        self._write_line("=" * REPORT_WIDTH)
        self._write_line("")

    def _write_position_summary(self, positions: list[PositionRecord]) -> None:
        self._write_line("PORTFOLIO POSITION SUMMARY")
        self._write_line("-" * REPORT_WIDTH)
        self._write_line(
            f"{'Portfolio ID':<12} {'Investment':<12} {'Date':<12} "
            f"{'Quantity':>15} {'Cost Basis':>18} {'Market Value':>18}"
        )
        self._write_line("-" * REPORT_WIDTH)

        for pos in positions:
            self._total_positions += 1
            self._total_market_value += pos.market_value
            self._total_cost_basis += pos.cost_basis

            self._write_line(
                f"{pos.portfolio_id:<12} {pos.investment_id:<12} "
                f"{pos.position_date:<12} {pos.quantity:>15.4f} "
                f"{pos.cost_basis:>18.2f} {pos.market_value:>18.2f}"
            )

        self._write_line("-" * REPORT_WIDTH)
        self._write_line(
            f"{'TOTALS':<36} {'':>15} "
            f"{self._total_cost_basis:>18.2f} {self._total_market_value:>18.2f}"
        )
        self._write_line("")

    def _write_transaction_activity(self, transactions: list[TransactionRecord]) -> None:
        self._write_line("TRANSACTION ACTIVITY")
        self._write_line("-" * REPORT_WIDTH)

        buy_count = sell_count = transfer_count = fee_count = 0
        buy_amount = sell_amount = Decimal("0")

        for txn in transactions:
            if txn.trans_type == "BU":
                buy_count += 1
                buy_amount += txn.amount
            elif txn.trans_type == "SL":
                sell_count += 1
                sell_amount += txn.amount
            elif txn.trans_type == "TR":
                transfer_count += 1
            elif txn.trans_type == "FE":
                fee_count += 1

        self._write_line(f"  Buy Transactions:      {buy_count:>8}  Amount: {buy_amount:>18.2f}")
        self._write_line(f"  Sell Transactions:     {sell_count:>8}  Amount: {sell_amount:>18.2f}")
        self._write_line(f"  Transfer Transactions: {transfer_count:>8}")
        self._write_line(f"  Fee Transactions:      {fee_count:>8}")
        self._write_line(f"  Total Transactions:    {len(transactions):>8}")
        self._write_line("")

    def _write_exception_report(self, positions: list[PositionRecord]) -> None:
        self._write_line("EXCEPTION REPORT")
        self._write_line("-" * REPORT_WIDTH)

        exceptions_found = False
        for pos in positions:
            if pos.quantity < 0:
                self._write_line(
                    f"  NEGATIVE QUANTITY: {pos.portfolio_id} {pos.investment_id} "
                    f"Qty: {pos.quantity}"
                )
                exceptions_found = True
            if pos.market_value < 0:
                self._write_line(
                    f"  NEGATIVE VALUE: {pos.portfolio_id} {pos.investment_id} "
                    f"Value: {pos.market_value}"
                )
                exceptions_found = True

        if not exceptions_found:
            self._write_line("  No exceptions found")
        self._write_line("")

    def _write_performance_metrics(self, positions: list[PositionRecord]) -> None:
        self._write_line("PERFORMANCE METRICS")
        self._write_line("-" * REPORT_WIDTH)

        if self._total_cost_basis != 0:
            gain_loss = self._total_market_value - self._total_cost_basis
            pct_return = (gain_loss / self._total_cost_basis) * 100
            self._write_line(f"  Total Cost Basis:    {self._total_cost_basis:>18.2f}")
            self._write_line(f"  Total Market Value:  {self._total_market_value:>18.2f}")
            self._write_line(f"  Total Gain/Loss:     {gain_loss:>18.2f}")
            self._write_line(f"  Percentage Return:   {pct_return:>17.2f}%")
        else:
            self._write_line("  No positions to evaluate")
        self._write_line("")

    def _write_report_footer(self) -> None:
        self._write_line("=" * REPORT_WIDTH)
        self._write_line(
            f"Total Positions: {self._total_positions}  |  "
            f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"
        )
        self._write_line("=" * REPORT_WIDTH)

    def _new_page(self) -> None:
        self._page_number += 1
        self._line_count = 0

    def _write_line(self, text: str) -> None:
        self._report_lines.append(text)
        self._line_count += 1

    def save_report(self, file_path: str) -> int:
        try:
            with open(file_path, "w") as f:
                for line in self._report_lines:
                    f.write(line + "\n")
            return ReturnCode.SUCCESS
        except Exception as e:
            logger.error("Error saving report: %s", e)
            return ReturnCode.ERROR

    def get_report_text(self) -> str:
        return "\n".join(self._report_lines)

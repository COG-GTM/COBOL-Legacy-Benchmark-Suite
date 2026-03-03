"""Position Report Generator.

Replaces: RPTPOS00 (src/programs/batch/RPTPOS00.cbl)

Generates portfolio position reports using pandas + jinja2
instead of 132-byte fixed-width COBOL report lines.

Original COBOL flow (RPTPOS00.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE (open files, print headers)
    2000-PROCESS-RECORDS (loop)
      2100-READ-POSITION (read VSAM position file)
      2200-ACCUMULATE-TOTALS
      2300-PRINT-DETAIL-LINE (write 132-byte detail)
    3000-PRINT-SUMMARY
    4000-FINALIZE (close files, print footer)

Report layout (original 132-byte):
  Col 1-8:   Portfolio ID
  Col 10-19: Investment ID
  Col 21-28: Position Date
  Col 30-44: Quantity
  Col 46-60: Cost Basis
  Col 62-76: Market Value
  Col 78-92: Gain/Loss
  Col 94-96: Currency
  Col 98-98: Status
"""

from __future__ import annotations

import logging
from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal

import pandas as pd
from jinja2 import Template
from sqlalchemy import select
from sqlalchemy.orm import Session

from portfolio_manager.models.database import InvestmentPosition

logger = logging.getLogger(__name__)

# Jinja2 template for HTML report output
POSITION_REPORT_TEMPLATE = Template(
    """\
<!DOCTYPE html>
<html>
<head>
    <title>Portfolio Position Report - {{ report_date }}</title>
    <style>
        body { font-family: monospace; margin: 20px; }
        h1 { text-align: center; }
        .header { text-align: center; margin-bottom: 20px; }
        table { border-collapse: collapse; width: 100%; }
        th, td { border: 1px solid #ddd; padding: 6px; text-align: right; }
        th { background-color: #f2f2f2; }
        td:first-child, td:nth-child(2) { text-align: left; }
        .summary { margin-top: 20px; font-weight: bold; }
        .positive { color: green; }
        .negative { color: red; }
        .footer { margin-top: 30px; font-size: 0.9em; color: #666; }
    </style>
</head>
<body>
    <div class="header">
        <h1>INVESTMENT PORTFOLIO POSITION REPORT</h1>
        <p>Report Date: {{ report_date }} | Generated: {{ generated_at }}</p>
        {% if portfolio_id %}<p>Portfolio: {{ portfolio_id }}</p>{% endif %}
    </div>

    <table>
        <thead>
            <tr>
                <th>Portfolio</th>
                <th>Investment</th>
                <th>Date</th>
                <th>Quantity</th>
                <th>Cost Basis</th>
                <th>Market Value</th>
                <th>Gain/Loss</th>
                <th>Ccy</th>
                <th>G/L %</th>
            </tr>
        </thead>
        <tbody>
        {% for row in rows %}
            <tr>
                <td>{{ row.portfolio_id }}</td>
                <td>{{ row.investment_id }}</td>
                <td>{{ row.position_date }}</td>
                <td>{{ "{:,.4f}".format(row.quantity) }}</td>
                <td>{{ "{:,.2f}".format(row.cost_basis) }}</td>
                <td>{{ "{:,.2f}".format(row.market_value) }}</td>
                <td class="{{ 'positive' if row.gain_loss >= 0 else 'negative' }}">
                    {{ "{:,.2f}".format(row.gain_loss) }}
                </td>
                <td>{{ row.currency_code }}</td>
                <td class="{{ 'positive' if row.gain_loss_pct >= 0 else 'negative' }}">
                    {{ "{:.2f}%".format(row.gain_loss_pct) }}
                </td>
            </tr>
        {% endfor %}
        </tbody>
    </table>

    <div class="summary">
        <p>Total Positions: {{ stats.total_positions }}</p>
        <p>Total Cost Basis: {{ "{:,.2f}".format(stats.total_cost_basis) }}</p>
        <p>Total Market Value: {{ "{:,.2f}".format(stats.total_market_value) }}</p>
        <p>Total Gain/Loss:
            <span class="{{ 'positive' if stats.total_gain_loss >= 0 else 'negative' }}">
                {{ "{:,.2f}".format(stats.total_gain_loss) }}
                ({{ "{:.2f}%".format(stats.gain_loss_pct) }})
            </span>
        </p>
    </div>

    <div class="footer">
        <p>End of Report | Program: RPTPOS00 (Python) | Records: {{ stats.total_positions }}</p>
    </div>
</body>
</html>
"""
)

TEXT_REPORT_TEMPLATE = Template(
    """\
================================================================================
                    INVESTMENT PORTFOLIO POSITION REPORT
================================================================================
Report Date: {{ report_date }}    Generated: {{ generated_at }}
{% if portfolio_id %}Portfolio: {{ portfolio_id }}{% endif %}
--------------------------------------------------------------------------------
Portfolio  Investment  Date        Quantity       Cost Basis
---------- ----------  ----------  -------------- --------------
{% for row in rows %}
{{ "%-10s" | format(row.portfolio_id) }} {{ "%-10s" | format(row.investment_id) }}\
 {{ "%-10s" | format(row.position_date|string) }}\
 {{ "%14.4f" | format(row.quantity) }} {{ "%14.2f" | format(row.cost_basis) }}\
 {{ "%14.2f" | format(row.market_value) }} {{ "%14.2f" | format(row.gain_loss) }}\
 {{ row.currency_code }}  {{ "%7.2f%%" | format(row.gain_loss_pct) }}
{% endfor %}
--------------------------------------------------------------------------------
SUMMARY:
  Total Positions:    {{ stats.total_positions }}
  Total Cost Basis:   {{ "%14.2f" | format(stats.total_cost_basis) }}
  Total Market Value: {{ "%14.2f" | format(stats.total_market_value) }}
  Total Gain/Loss:    {{ "%14.2f" | format(stats.total_gain_loss) }}\
 ({{ "%.2f%%" | format(stats.gain_loss_pct) }})
================================================================================
End of Report | Program: RPTPOS00 (Python) | Records: {{ stats.total_positions }}
================================================================================
"""
)


@dataclass
class PositionRow:
    """A single position row for the report."""

    portfolio_id: str
    investment_id: str
    position_date: date
    quantity: Decimal
    cost_basis: Decimal
    market_value: Decimal
    gain_loss: Decimal
    currency_code: str
    gain_loss_pct: float


@dataclass
class ReportStats:
    """Summary statistics for the report."""

    total_positions: int = 0
    total_cost_basis: Decimal = Decimal("0")
    total_market_value: Decimal = Decimal("0")
    total_gain_loss: Decimal = Decimal("0")
    gain_loss_pct: float = 0.0


class PositionReportGenerator:
    """Generate portfolio position reports.

    Replaces RPTPOS00 (src/programs/batch/RPTPOS00.cbl).
    """

    PROGRAM_ID = "RPTPOS00"

    def __init__(self, session: Session):
        self._session = session

    def generate(
        self,
        portfolio_id: str | None = None,
        report_date: date | None = None,
        output_format: str = "html",
    ) -> str:
        """Generate the position report.

        Replaces 0000-MAIN-PROCESS flow.

        Args:
            portfolio_id: Optional filter by portfolio.
            report_date: Report date (defaults to today).
            output_format: 'html' or 'text'.

        Returns:
            Rendered report string.
        """
        rpt_date = report_date or date.today()

        logger.info(
            "%s: Generating position report for date=%s portfolio=%s",
            self.PROGRAM_ID,
            rpt_date,
            portfolio_id or "ALL",
        )

        # 2000-PROCESS-RECORDS: read positions
        query = select(InvestmentPosition).order_by(
            InvestmentPosition.portfolio_id,
            InvestmentPosition.investment_id,
        )
        if portfolio_id:
            query = query.where(
                InvestmentPosition.portfolio_id == portfolio_id
            )

        positions = self._session.execute(query).scalars().all()

        # Build rows and accumulate totals (2200-ACCUMULATE-TOTALS)
        rows: list[PositionRow] = []
        stats = ReportStats()

        for pos in positions:
            gain_loss = pos.market_value - pos.cost_basis
            gl_pct = (
                float(gain_loss / pos.cost_basis * 100)
                if pos.cost_basis != 0
                else 0.0
            )

            rows.append(
                PositionRow(
                    portfolio_id=pos.portfolio_id,
                    investment_id=pos.investment_id,
                    position_date=pos.position_date,
                    quantity=pos.quantity,
                    cost_basis=pos.cost_basis,
                    market_value=pos.market_value,
                    gain_loss=gain_loss,
                    currency_code=pos.currency_code,
                    gain_loss_pct=gl_pct,
                )
            )

            stats.total_positions += 1
            stats.total_cost_basis += pos.cost_basis
            stats.total_market_value += pos.market_value
            stats.total_gain_loss += gain_loss

        if stats.total_cost_basis != 0:
            stats.gain_loss_pct = float(
                stats.total_gain_loss / stats.total_cost_basis * 100
            )

        # 3000-PRINT-SUMMARY / render
        template_vars = {
            "report_date": rpt_date,
            "generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            "portfolio_id": portfolio_id,
            "rows": rows,
            "stats": stats,
        }

        template = (
            POSITION_REPORT_TEMPLATE
            if output_format == "html"
            else TEXT_REPORT_TEMPLATE
        )

        report = template.render(**template_vars)

        logger.info(
            "%s: Report generated — %d positions, format=%s",
            self.PROGRAM_ID,
            stats.total_positions,
            output_format,
        )

        return report

    def generate_dataframe(
        self,
        portfolio_id: str | None = None,
    ) -> pd.DataFrame:
        """Generate report data as a pandas DataFrame.

        Useful for further analysis or export to CSV/Excel.
        """
        query = select(InvestmentPosition).order_by(
            InvestmentPosition.portfolio_id,
            InvestmentPosition.investment_id,
        )
        if portfolio_id:
            query = query.where(
                InvestmentPosition.portfolio_id == portfolio_id
            )

        positions = self._session.execute(query).scalars().all()

        data = []
        for pos in positions:
            gain_loss = pos.market_value - pos.cost_basis
            gl_pct = (
                float(gain_loss / pos.cost_basis * 100)
                if pos.cost_basis != 0
                else 0.0
            )
            data.append(
                {
                    "portfolio_id": pos.portfolio_id,
                    "investment_id": pos.investment_id,
                    "position_date": pos.position_date,
                    "quantity": float(pos.quantity),
                    "cost_basis": float(pos.cost_basis),
                    "market_value": float(pos.market_value),
                    "gain_loss": float(gain_loss),
                    "currency_code": pos.currency_code,
                    "gain_loss_pct": gl_pct,
                }
            )

        return pd.DataFrame(data)

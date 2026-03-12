"""
Report generation tests.

Tests report generators from:
  - RPTPOS00.cbl: Position report
  - RPTAUD00.cbl: Audit report
  - RPTSTA00.cbl: Statistics report
"""

import json
from datetime import date, datetime
from decimal import Decimal

from sqlalchemy.orm import Session

from src.reports.audit_report import AuditReportGenerator
from src.reports.position_report import PositionReportGenerator
from src.reports.stats_report import StatsReportGenerator


class TestPositionReport:
    """Test PositionReportGenerator (RPTPOS00.cbl)."""

    def test_generate_json(
        self, session: Session, sample_portfolio, sample_position
    ):
        generator = PositionReportGenerator(session)
        output = generator.generate(date(2024, 1, 15), output_format="json")
        data = json.loads(output)
        assert data["report_type"] == "Position Summary"
        assert data["portfolio_count"] > 0
        assert Decimal(data["grand_total_value"]) > 0

    def test_generate_csv(
        self, session: Session, sample_portfolio, sample_position
    ):
        generator = PositionReportGenerator(session)
        output = generator.generate(date(2024, 1, 15), output_format="csv")
        assert "Portfolio ID" in output
        assert "PORT0001" in output

    def test_generate_text(
        self, session: Session, sample_portfolio, sample_position
    ):
        generator = PositionReportGenerator(session)
        output = generator.generate(date(2024, 1, 15), output_format="text")
        assert "POSITION SUMMARY REPORT" in output
        assert "PORT0001" in output

    def test_empty_report(self, session: Session):
        generator = PositionReportGenerator(session)
        output = generator.generate(date(2024, 1, 15), output_format="json")
        data = json.loads(output)
        assert data["portfolio_count"] == 0


class TestAuditReport:
    """Test AuditReportGenerator (RPTAUD00.cbl)."""

    def test_generate_empty(self, session: Session):
        generator = AuditReportGenerator(session)
        output = generator.generate(
            datetime(2024, 1, 1),
            datetime(2024, 12, 31),
            output_format="json",
        )
        data = json.loads(output)
        assert data["report_type"] == "Audit Trail"
        assert data["total_entries"] == 0

    def test_generate_text_format(self, session: Session):
        generator = AuditReportGenerator(session)
        output = generator.generate(
            datetime(2024, 1, 1),
            datetime(2024, 12, 31),
            output_format="text",
        )
        assert "AUDIT TRAIL REPORT" in output


class TestStatsReport:
    """Test StatsReportGenerator (RPTSTA00.cbl)."""

    def test_generate_json(self, session: Session, multiple_portfolios):
        generator = StatsReportGenerator(session)
        output = generator.generate(output_format="json")
        data = json.loads(output)
        assert data["report_type"] == "System Statistics"
        assert data["portfolios"]["total"] == 5
        assert data["portfolios"]["active"] == 3

    def test_generate_text(self, session: Session):
        generator = StatsReportGenerator(session)
        output = generator.generate(output_format="text")
        assert "SYSTEM STATISTICS" in output

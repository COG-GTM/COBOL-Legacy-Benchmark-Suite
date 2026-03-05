"""
Report generation tests translated from COBOL reporting programs.

Tests:
- Position report (RPTPOS00.cbl)
- Audit report (RPTAUD00.cbl)
- Statistics report (RPTSTA00.cbl)
"""

from datetime import date, datetime
from decimal import Decimal

from src.db.tables import (
    InvestmentPosition,
    PortfolioMaster,
)
from src.reports.audit_report import AuditReportGenerator
from src.reports.position_report import PositionReportGenerator
from src.reports.stats_report import StatsReportGenerator


class TestPositionReport:
    """Test position report. Translates RPTPOS00.cbl."""

    def test_generate_json(self, session, sample_portfolio, sample_position):
        gen = PositionReportGenerator(session)
        report = gen.generate(
            portfolio_id=sample_portfolio.portfolio_id,
            output_format="json",
        )
        assert report["report_title"] == "POSITION SUMMARY REPORT"
        assert len(report["positions"]) >= 1
        assert report["summary"]["total_positions"] >= 1

    def test_generate_csv(self, session, sample_portfolio, sample_position):
        gen = PositionReportGenerator(session)
        csv_output = gen.generate(
            portfolio_id=sample_portfolio.portfolio_id,
            output_format="csv",
        )
        assert isinstance(csv_output, str)
        assert "portfolio_id" in csv_output

    def test_change_pct_calculation(self, session, sample_portfolio, sample_position):
        """Verify COMPUTE POS-CHANGE-PCT."""
        gen = PositionReportGenerator(session)
        report = gen.generate(
            portfolio_id=sample_portfolio.portfolio_id,
            output_format="json",
        )
        pos = report["positions"][0]
        # cost=5000, market=5500 → change_pct=10.00%
        assert pos["change_pct"] == "10.00"
        assert pos["unrealized_gain_loss"] == "500.00"

    def test_empty_report(self, session):
        gen = PositionReportGenerator(session)
        report = gen.generate(portfolio_id="NOTEXIST", output_format="json")
        assert report["summary"]["total_positions"] == 0

    def test_exception_detection(self, session):
        """Positions with > 10% loss should be flagged as exceptions."""
        portfolio = PortfolioMaster(
            portfolio_id="PORT0099",
            account_type="IN",
            branch_id="01",
            client_id="CLIENT99",
            client_name="Exception Client",
            portfolio_name="Exception Test",
            currency_code="USD",
            risk_level="H",
            status="A",
            open_date=date(2024, 1, 1),
        )
        session.add(portfolio)

        pos = InvestmentPosition(
            portfolio_id="PORT0099",
            investment_id="FUND0099",
            position_date=date(2024, 1, 15),
            quantity=Decimal("100.0000"),
            cost_basis=Decimal("10000.00"),
            market_value=Decimal("8000.00"),  # -20% loss
            currency_code="USD",
            status="A",
        )
        session.add(pos)
        session.flush()

        gen = PositionReportGenerator(session)
        report = gen.generate(portfolio_id="PORT0099", output_format="json")
        assert len(report["exceptions"]) >= 1


class TestAuditReport:
    """Test audit report. Translates RPTAUD00.cbl."""

    def test_generate_report(self, session, sample_audit):
        gen = AuditReportGenerator(session)
        report = gen.generate(output_format="json")
        assert report["report_title"] == "AUDIT TRAIL REPORT"
        assert report["summary"]["total_entries"] >= 1

    def test_filter_by_type(self, session, sample_audit):
        gen = AuditReportGenerator(session)
        report = gen.generate(audit_type="TRAN", output_format="json")
        assert report["summary"]["total_entries"] >= 1

    def test_csv_output(self, session, sample_audit):
        gen = AuditReportGenerator(session)
        csv_output = gen.generate(output_format="csv")
        assert isinstance(csv_output, str)
        assert "timestamp" in csv_output

    def test_empty_report(self, session):
        gen = AuditReportGenerator(session)
        report = gen.generate(
            start=datetime(2099, 1, 1),
            end=datetime(2099, 12, 31),
            output_format="json",
        )
        assert report["summary"]["total_entries"] == 0


class TestStatsReport:
    """Test statistics report. Translates RPTSTA00.cbl."""

    def test_generate_report(self, session, sample_batch_control):
        gen = StatsReportGenerator(session)
        report = gen.generate(output_format="json")
        assert report["report_title"] == "BATCH PROCESSING STATISTICS"
        assert report["summary"]["total_jobs"] >= 1

    def test_filter_by_date(self, session, sample_batch_control):
        gen = StatsReportGenerator(session)
        report = gen.generate(process_date="20240115", output_format="json")
        assert report["summary"]["total_jobs"] >= 1

    def test_csv_output(self, session, sample_batch_control):
        gen = StatsReportGenerator(session)
        csv_output = gen.generate(output_format="csv")
        assert isinstance(csv_output, str)
        assert "job_name" in csv_output

    def test_empty_report(self, session):
        gen = StatsReportGenerator(session)
        report = gen.generate(process_date="99991231", output_format="json")
        assert report["summary"]["total_jobs"] == 0

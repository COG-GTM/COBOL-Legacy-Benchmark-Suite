"""Tests for PositionRecord models (POSREC copybook)."""

from decimal import Decimal

import pytest
from pydantic import ValidationError

from src.models.position import (
    PositionAudit,
    PositionData,
    PositionKey,
    PositionRecord,
)


class TestPositionKey:
    def test_valid_key(self):
        key = PositionKey(
            pos_portfolio_id="PORT0001",
            pos_date="20240315",
            pos_investment_id="INV0000001",
        )
        assert key.pos_portfolio_id == "PORT0001"
        assert key.pos_date == "20240315"

    def test_date_validation(self):
        with pytest.raises(ValidationError, match="pos_date must contain only digits"):
            PositionKey(
                pos_portfolio_id="PORT0001",
                pos_date="BAD-DATE",
                pos_investment_id="INV0000001",
            )


class TestPositionData:
    def test_valid_data(self):
        data = PositionData(
            pos_quantity=Decimal("1000.5000"),
            pos_cost_basis=Decimal("50000.00"),
            pos_market_value=Decimal("55000.00"),
            pos_currency="USD",
            pos_status="A",
        )
        assert data.pos_quantity == Decimal("1000.5000")
        assert data.pos_status == "A"

    def test_all_statuses(self):
        for s in ("A", "C", "P"):
            data = PositionData(
                pos_quantity=Decimal("0.0000"),
                pos_cost_basis=Decimal("0.00"),
                pos_market_value=Decimal("0.00"),
                pos_currency="EUR",
                pos_status=s,
            )
            assert data.pos_status == s

    def test_invalid_status(self):
        with pytest.raises(ValidationError, match="pos_status must be one of"):
            PositionData(
                pos_quantity=Decimal("0.0000"),
                pos_cost_basis=Decimal("0.00"),
                pos_market_value=Decimal("0.00"),
                pos_currency="EUR",
                pos_status="X",
            )


class TestPositionRecord:
    def test_full_record(self):
        record = PositionRecord(
            pos_key=PositionKey(
                pos_portfolio_id="PORT0001",
                pos_date="20240315",
                pos_investment_id="INV0000001",
            ),
            pos_data=PositionData(
                pos_quantity=Decimal("500.0000"),
                pos_cost_basis=Decimal("25000.00"),
                pos_market_value=Decimal("27500.00"),
                pos_currency="USD",
                pos_status="A",
            ),
            pos_audit=PositionAudit(
                pos_last_maint_date="2024-03-15T10:00:00.000000",
                pos_last_maint_user="ADMIN01",
            ),
        )
        assert record.pos_key.pos_portfolio_id == "PORT0001"
        assert record.pos_data.pos_market_value == Decimal("27500.00")
        assert record.pos_filler == ""

    def test_from_attributes_config(self):
        assert PositionRecord.model_config["from_attributes"] is True

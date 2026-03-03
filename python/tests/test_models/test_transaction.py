"""Tests for TransactionRecord models (TRNREC copybook)."""

from decimal import Decimal

import pytest
from pydantic import ValidationError

from src.models.transaction import (
    TransactionAudit,
    TransactionData,
    TransactionKey,
    TransactionRecord,
)


class TestTransactionKey:
    def test_valid_key(self):
        key = TransactionKey(
            trn_date="20240315",
            trn_time="143022",
            trn_portfolio_id="PORT0001",
            trn_sequence_no="000001",
        )
        assert key.trn_date == "20240315"
        assert key.trn_time == "143022"
        assert key.trn_portfolio_id == "PORT0001"
        assert key.trn_sequence_no == "000001"

    def test_date_validation_rejects_alpha(self):
        with pytest.raises(ValidationError, match="trn_date must contain only digits"):
            TransactionKey(
                trn_date="ABCD1234",
                trn_time="143022",
                trn_portfolio_id="PORT0001",
                trn_sequence_no="000001",
            )

    def test_time_validation_rejects_alpha(self):
        with pytest.raises(ValidationError, match="trn_time must contain only digits"):
            TransactionKey(
                trn_date="20240315",
                trn_time="14:30:",
                trn_portfolio_id="PORT0001",
                trn_sequence_no="000001",
            )

    def test_max_length_exceeded(self):
        with pytest.raises(ValidationError):
            TransactionKey(
                trn_date="202403150",  # 9 chars, max is 8
                trn_time="143022",
                trn_portfolio_id="PORT0001",
                trn_sequence_no="000001",
            )


class TestTransactionData:
    def test_valid_buy(self):
        data = TransactionData(
            trn_investment_id="INV0000001",
            trn_type="BU",
            trn_quantity=Decimal("100.5000"),
            trn_price=Decimal("45.2500"),
            trn_amount=Decimal("4547.63"),
            trn_currency="USD",
            trn_status="P",
        )
        assert data.trn_type == "BU"
        assert data.trn_status == "P"
        assert data.trn_quantity == Decimal("100.5000")

    def test_all_transaction_types(self):
        for t in ("BU", "SL", "TR", "FE"):
            data = TransactionData(
                trn_investment_id="INV0000001",
                trn_type=t,
                trn_quantity=Decimal("1.0000"),
                trn_price=Decimal("1.0000"),
                trn_amount=Decimal("1.00"),
                trn_currency="USD",
                trn_status="D",
            )
            assert data.trn_type == t

    def test_invalid_type(self):
        with pytest.raises(ValidationError, match="trn_type must be one of"):
            TransactionData(
                trn_investment_id="INV0000001",
                trn_type="XX",
                trn_quantity=Decimal("1.0000"),
                trn_price=Decimal("1.0000"),
                trn_amount=Decimal("1.00"),
                trn_currency="USD",
                trn_status="D",
            )

    def test_invalid_status(self):
        with pytest.raises(ValidationError, match="trn_status must be one of"):
            TransactionData(
                trn_investment_id="INV0000001",
                trn_type="BU",
                trn_quantity=Decimal("1.0000"),
                trn_price=Decimal("1.0000"),
                trn_amount=Decimal("1.00"),
                trn_currency="USD",
                trn_status="Z",
            )

    def test_all_statuses(self):
        for s in ("P", "D", "F", "R"):
            data = TransactionData(
                trn_investment_id="INV0000001",
                trn_type="BU",
                trn_quantity=Decimal("1.0000"),
                trn_price=Decimal("1.0000"),
                trn_amount=Decimal("1.00"),
                trn_currency="USD",
                trn_status=s,
            )
            assert data.trn_status == s


class TestTransactionRecord:
    def test_full_record(self):
        record = TransactionRecord(
            trn_key=TransactionKey(
                trn_date="20240315",
                trn_time="143022",
                trn_portfolio_id="PORT0001",
                trn_sequence_no="000001",
            ),
            trn_data=TransactionData(
                trn_investment_id="INV0000001",
                trn_type="BU",
                trn_quantity=Decimal("100.0000"),
                trn_price=Decimal("50.0000"),
                trn_amount=Decimal("5000.00"),
                trn_currency="USD",
                trn_status="D",
            ),
            trn_audit=TransactionAudit(
                trn_process_date="2024-03-15T14:30:22.000000",
                trn_process_user="ADMIN01",
            ),
        )
        assert record.trn_key.trn_date == "20240315"
        assert record.trn_data.trn_amount == Decimal("5000.00")
        assert record.trn_audit.trn_process_user == "ADMIN01"
        assert record.trn_filler == ""

    def test_from_attributes_config(self):
        assert TransactionRecord.model_config["from_attributes"] is True

    def test_filler_default(self):
        record = TransactionRecord(
            trn_key=TransactionKey(
                trn_date="20240315",
                trn_time="143022",
                trn_portfolio_id="PORT0001",
                trn_sequence_no="000001",
            ),
            trn_data=TransactionData(
                trn_investment_id="INV0000001",
                trn_type="BU",
                trn_quantity=Decimal("100.0000"),
                trn_price=Decimal("50.0000"),
                trn_amount=Decimal("5000.00"),
                trn_currency="USD",
                trn_status="D",
            ),
            trn_audit=TransactionAudit(
                trn_process_date="2024-03-15T14:30:22.000000",
                trn_process_user="ADMIN01",
            ),
        )
        assert record.trn_filler == ""

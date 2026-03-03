"""Tests for DB2 models (DBPROC, DBTBLS, SQLCA copybooks)."""

from decimal import Decimal

import pytest
from pydantic import ValidationError

from src.models.db2.db2_procedures import Db2ErrorHandling, Db2ErrorMessage
from src.models.db2.db2_tables import ErrorLogRecord, PositionHistoryRecord
from src.models.db2.sqlca import SqlStatusCodes


class TestDb2ErrorHandling:
    def test_defaults(self):
        eh = Db2ErrorHandling(
            db2_error_message=Db2ErrorMessage(
                db2_sqlcode_txt="-803",
                db2_state="23505",
                db2_error_text="Duplicate key",
            ),
            db2_save_status="23505",
        )
        assert eh.db2_retry_count == 0
        assert eh.db2_max_retries == 3
        assert eh.db2_retry_wait == 100

    def test_error_message_fields(self):
        msg = Db2ErrorMessage(
            db2_sqlcode_txt="-803",
            db2_state="23505",
            db2_error_text="Duplicate key violation",
        )
        assert msg.db2_sqlcode_txt == "-803"
        assert msg.db2_state == "23505"

    def test_from_attributes_config(self):
        assert Db2ErrorHandling.model_config["from_attributes"] is True


class TestPositionHistoryRecord:
    def test_valid_record(self):
        rec = PositionHistoryRecord(
            ph_account_no="ACCT0001",
            ph_portfolio_id="PORT000001",
            ph_trans_date="2024-03-15",
            ph_trans_time="14:30:22",
            ph_trans_type="BU",
            ph_security_id="SEC000000001",
            ph_quantity=Decimal("100.000"),
            ph_price=Decimal("45.250"),
            ph_amount=Decimal("4525.00"),
            ph_fees=Decimal("10.00"),
            ph_total_amount=Decimal("4535.00"),
            ph_cost_basis=Decimal("4535.00"),
            ph_gain_loss=Decimal("0.00"),
            ph_process_date="2024-03-15",
            ph_process_time="14:30:23",
            ph_program_id="HISTLD00",
            ph_user_id="BATCH01",
            ph_audit_timestamp="2024-03-15T14:30:23.000000",
        )
        assert rec.ph_account_no == "ACCT0001"
        assert rec.ph_quantity == Decimal("100.000")
        assert rec.ph_total_amount == Decimal("4535.00")

    def test_from_attributes_config(self):
        assert PositionHistoryRecord.model_config["from_attributes"] is True


class TestErrorLogRecord:
    def test_valid_record(self):
        rec = ErrorLogRecord(
            el_error_timestamp="2024-03-15T14:30:22.000000",
            el_program_id="TRNVAL00",
            el_error_type="A",
            el_error_severity=3,
            el_error_code="E001",
            el_error_message="Validation failed for portfolio",
            el_process_date="2024-03-15",
            el_process_time="14:30:22",
            el_user_id="BATCH01",
        )
        assert rec.el_error_type == "A"
        assert rec.el_error_severity == 3
        assert rec.el_additional_info == ""

    def test_all_error_types(self):
        for et in ("S", "A", "D"):
            rec = ErrorLogRecord(
                el_error_timestamp="ts",
                el_program_id="PGM",
                el_error_type=et,
                el_error_severity=1,
                el_error_code="E001",
                el_error_message="msg",
                el_process_date="date",
                el_process_time="time",
                el_user_id="user",
            )
            assert rec.el_error_type == et

    def test_invalid_error_type(self):
        with pytest.raises(ValidationError, match="el_error_type must be one of"):
            ErrorLogRecord(
                el_error_timestamp="ts",
                el_program_id="PGM",
                el_error_type="Z",
                el_error_severity=1,
                el_error_code="E001",
                el_error_message="msg",
                el_process_date="date",
                el_process_time="time",
                el_user_id="user",
            )

    def test_all_severities(self):
        for sev in (1, 2, 3, 4):
            rec = ErrorLogRecord(
                el_error_timestamp="ts",
                el_program_id="PGM",
                el_error_type="S",
                el_error_severity=sev,
                el_error_code="E001",
                el_error_message="msg",
                el_process_date="date",
                el_process_time="time",
                el_user_id="user",
            )
            assert rec.el_error_severity == sev

    def test_invalid_severity(self):
        with pytest.raises(ValidationError, match="el_error_severity must be one of"):
            ErrorLogRecord(
                el_error_timestamp="ts",
                el_program_id="PGM",
                el_error_type="S",
                el_error_severity=5,
                el_error_code="E001",
                el_error_message="msg",
                el_process_date="date",
                el_process_time="time",
                el_user_id="user",
            )


class TestSqlStatusCodes:
    def test_defaults(self):
        sc = SqlStatusCodes()
        assert sc.sql_success == "00000"
        assert sc.sql_not_found == "02000"
        assert sc.sql_dup_key == "23505"
        assert sc.sql_deadlock == "40001"
        assert sc.sql_timeout == "40003"
        assert sc.sql_connection_error == "08001"
        assert sc.sql_db_error == "58004"

    def test_from_attributes_config(self):
        assert SqlStatusCodes.model_config["from_attributes"] is True

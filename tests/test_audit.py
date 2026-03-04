"""
Tests for audit trail rules from PORTTRAN.cbl and AUDPROC.cbl.

These tests encode the business rules for audit record creation,
action mapping, status mapping, and failure handling.
"""

import pytest

from tests.business_rules.validators import (
    map_transaction_to_audit_action,
    map_file_status_to_audit_status,
    build_audit_record,
    handle_audit_write_failure,
    AUDIT_ACTION_MAP,
)


# =====================================================================
# Audit Action Mapping
# Reference: PORTTRAN.cbl 2300-UPDATE-AUDIT-TRAIL
# Rule: BU -> CREATE, SL -> DELETE, TR -> UPDATE, FE -> UPDATE
# =====================================================================
class TestAuditActionMapping:
    """Transaction type maps to audit action."""

    def test_buy_maps_to_create(self):
        assert map_transaction_to_audit_action("BU") == "CREATE"

    def test_sell_maps_to_delete(self):
        assert map_transaction_to_audit_action("SL") == "DELETE"

    def test_transfer_maps_to_update(self):
        assert map_transaction_to_audit_action("TR") == "UPDATE"

    def test_fee_maps_to_update(self):
        assert map_transaction_to_audit_action("FE") == "UPDATE"

    def test_unknown_type_returns_empty(self):
        assert map_transaction_to_audit_action("XX") == ""

    def test_all_mappings_present(self):
        assert AUDIT_ACTION_MAP == {
            "BU": "CREATE",
            "SL": "DELETE",
            "TR": "UPDATE",
            "FE": "UPDATE",
        }


# =====================================================================
# Audit Status Mapping
# Reference: PORTTRAN.cbl 2300-UPDATE-AUDIT-TRAIL
# Rule: File status '00' maps to 'SUCC', otherwise 'FAIL'
# =====================================================================
class TestAuditStatusMapping:
    """File status '00' -> SUCC, anything else -> FAIL."""

    def test_status_00_maps_to_succ(self):
        assert map_file_status_to_audit_status("00") == "SUCC"

    def test_status_10_maps_to_fail(self):
        assert map_file_status_to_audit_status("10") == "FAIL"

    def test_status_22_maps_to_fail(self):
        assert map_file_status_to_audit_status("22") == "FAIL"

    def test_status_23_maps_to_fail(self):
        assert map_file_status_to_audit_status("23") == "FAIL"

    def test_empty_status_maps_to_fail(self):
        assert map_file_status_to_audit_status("") == "FAIL"


# =====================================================================
# Audit Record Content
# Reference: PORTTRAN.cbl 2300-UPDATE-AUDIT-TRAIL
# Rule: Must include timestamp, program name ('PORTTRAN'), user ID,
#       transaction type, portfolio ID, account number, before-image,
#       and transaction details (type, amount, units)
# =====================================================================
class TestAuditRecordContent:
    """Audit record must include all required fields."""

    def test_audit_record_has_timestamp(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="BU",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert record.timestamp == "2024-03-20-10.30.00.000000"

    def test_audit_record_has_program_name(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="BU",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert record.program == "PORTTRAN"

    def test_audit_record_has_user_id(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="BU",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert record.user_id == "USER0001"

    def test_audit_record_has_transaction_type(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="BU",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert record.transaction_type == "BU"

    def test_audit_record_has_action_from_mapping(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="BU",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert record.action == "CREATE"

    def test_audit_record_has_portfolio_id(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="SL",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert record.portfolio_id == "PORT0001"

    def test_audit_record_has_account_number(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="SL",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert record.account_number == "1234567890"

    def test_audit_record_has_before_image(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="SL",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert record.before_image == "original_data"

    def test_audit_record_message_includes_details(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="BU",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert "BU" in record.message
        assert "500.0" in record.message
        assert "50.0" in record.message

    def test_sell_audit_record_action(self):
        record = build_audit_record(
            timestamp="2024-03-20-10.30.00.000000",
            program="PORTTRAN",
            user_id="USER0001",
            transaction_type="SL",
            portfolio_id="PORT0001",
            account_number="1234567890",
            before_image="original_data",
            amount=500.0,
            units=50.0,
        )
        assert record.action == "DELETE"


# =====================================================================
# Audit Write Failure
# Reference: PORTTRAN.cbl 2310-WRITE-AUDIT-RECORD
# Rule: If AUDPROC returns non-zero, an error routine is invoked
# =====================================================================
class TestAuditWriteFailure:
    """Non-zero AUDPROC return code triggers error routine."""

    def test_zero_rc_no_error(self):
        assert handle_audit_write_failure(0) is False

    def test_nonzero_rc_triggers_error(self):
        assert handle_audit_write_failure(8) is True

    def test_negative_rc_triggers_error(self):
        assert handle_audit_write_failure(-1) is True

    def test_rc_one_triggers_error(self):
        assert handle_audit_write_failure(1) is True

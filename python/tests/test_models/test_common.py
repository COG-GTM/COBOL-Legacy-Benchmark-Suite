"""Tests for common models (COMMON, AUDITLOG, ERRHAND, PORTFLIO, PORTVAL, RETHND, RTNCODE copybooks)."""

from decimal import Decimal

import pytest
from pydantic import ValidationError

from src.models.audit_log import AuditHeader, AuditKeyInfo, AuditRecord
from src.models.common import (
    AuditFields,
    CommonDatetime,
    CurrencyCodes,
    CurrentDate,
    CurrentTime,
    ErrorHandling,
    ReturnCodes,
    StatusCodes,
    TransactionTypes,
)
from src.models.error_handling import (
    ErrorCategories,
    ErrorMessage,
    ErrorReturnCodes,
    ErrorTimestamp,
    VsamMessages,
    VsamStatuses,
)
from src.models.portfolio import (
    PortfolioClientInfo,
    PortfolioFinancialInfo,
    PortfolioInfo,
    PortfolioKey,
    PortfolioRecord,
    PortfolioAuditInfo,
)
from src.models.portfolio_validation import (
    ValidationConstants,
    ValidationErrorMessages,
    ValidationReturnCodes,
    ValidationWorkAreas,
)
from src.models.return_handling import (
    ErrorInfo,
    ErrorLocation,
    ReturnActions,
    ReturnDetails,
    ReturnHandling,
    ReturnStatus,
    StandardErrorCodes,
    SystemInfo,
)
from src.models.return_code import (
    ReturnCodeAnalysisData,
    ReturnCodeArea,
    ReturnCodesArea,
    ReturnData,
)


class TestReturnCodes:
    def test_defaults(self):
        rc = ReturnCodes()
        assert rc.rc_success == 0
        assert rc.rc_warning == 4
        assert rc.rc_error == 8
        assert rc.rc_severe == 12
        assert rc.rc_critical == 16


class TestStatusCodes:
    def test_defaults(self):
        sc = StatusCodes()
        assert sc.status_active == "A"
        assert sc.status_closed == "C"
        assert sc.status_pending == "P"
        assert sc.status_suspended == "S"
        assert sc.status_failed == "F"
        assert sc.status_reversed == "R"


class TestTransactionTypes:
    def test_defaults(self):
        tt = TransactionTypes()
        assert tt.trn_type_buy == "BU"
        assert tt.trn_type_sell == "SL"
        assert tt.trn_type_transfer == "TR"
        assert tt.trn_type_fee == "FE"


class TestCurrencyCodes:
    def test_defaults(self):
        cc = CurrencyCodes()
        assert cc.curr_usd == "USD"
        assert cc.curr_eur == "EUR"
        assert cc.curr_gbp == "GBP"
        assert cc.curr_jpy == "JPY"
        assert cc.curr_cad == "CAD"


class TestCommonDatetime:
    def test_creation(self):
        dt = CommonDatetime(
            current_date=CurrentDate(curr_year="2024", curr_month="03", curr_day="15"),
            current_time=CurrentTime(
                curr_hour="14", curr_minute="30", curr_second="22", curr_msec="00"
            ),
        )
        assert dt.current_date.curr_year == "2024"
        assert dt.current_time.curr_hour == "14"


class TestAuditRecord:
    def test_valid_record(self):
        rec = AuditRecord(
            aud_header=AuditHeader(
                aud_timestamp="2024-03-15T14:30:22.000000",
                aud_system_id="SYS01",
                aud_user_id="USER01",
                aud_program="TRNVAL00",
                aud_terminal="TERM01",
            ),
            aud_type="TRAN",
            aud_action="CREATE",
            aud_status="SUCC",
            aud_key_info=AuditKeyInfo(
                aud_portfolio_id="PORT0001",
                aud_account_no="ACCT000001",
            ),
            aud_before_image="",
            aud_after_image="new record",
            aud_message="Transaction created",
        )
        assert rec.aud_type == "TRAN"
        assert rec.aud_action == "CREATE"
        assert rec.aud_status == "SUCC"

    def test_invalid_type(self):
        with pytest.raises(ValidationError, match="aud_type must be one of"):
            AuditRecord(
                aud_header=AuditHeader(
                    aud_timestamp="ts",
                    aud_system_id="SYS",
                    aud_user_id="USR",
                    aud_program="PGM",
                    aud_terminal="TRM",
                ),
                aud_type="XXXX",
                aud_action="CREATE",
                aud_status="SUCC",
                aud_key_info=AuditKeyInfo(
                    aud_portfolio_id="P", aud_account_no="A"
                ),
                aud_before_image="",
                aud_after_image="",
                aud_message="",
            )

    def test_invalid_status(self):
        with pytest.raises(ValidationError, match="aud_status must be one of"):
            AuditRecord(
                aud_header=AuditHeader(
                    aud_timestamp="ts",
                    aud_system_id="SYS",
                    aud_user_id="USR",
                    aud_program="PGM",
                    aud_terminal="TRM",
                ),
                aud_type="TRAN",
                aud_action="CREATE",
                aud_status="XXXX",
                aud_key_info=AuditKeyInfo(
                    aud_portfolio_id="P", aud_account_no="A"
                ),
                aud_before_image="",
                aud_after_image="",
                aud_message="",
            )

    def test_all_actions(self):
        for action in ("CREATE", "UPDATE", "DELETE", "INQUIRE", "LOGIN", "LOGOUT", "STARTUP", "SHUTDOWN"):
            rec = AuditRecord(
                aud_header=AuditHeader(
                    aud_timestamp="ts",
                    aud_system_id="SYS",
                    aud_user_id="USR",
                    aud_program="PGM",
                    aud_terminal="TRM",
                ),
                aud_type="USER",
                aud_action=action,
                aud_status="SUCC",
                aud_key_info=AuditKeyInfo(
                    aud_portfolio_id="P", aud_account_no="A"
                ),
                aud_before_image="",
                aud_after_image="",
                aud_message="",
            )
            assert rec.aud_action == action


class TestErrorHandlingModels:
    def test_error_categories_defaults(self):
        ec = ErrorCategories()
        assert ec.err_cat_vsam == "VS"
        assert ec.err_cat_valid == "VL"

    def test_error_return_codes_defaults(self):
        erc = ErrorReturnCodes()
        assert erc.err_success == 0
        assert erc.err_terminal == 16

    def test_error_message(self):
        msg = ErrorMessage(
            err_timestamp=ErrorTimestamp(err_date="2024-03-15", err_time="14:30:22"),
            err_program="TRNVAL00",
            err_category="VS",
            err_code="E001",
            err_severity=8,
            err_text="Record not found",
            err_details="VSAM key not found in PORTFILE",
        )
        assert msg.err_program == "TRNVAL00"
        assert msg.err_severity == 8

    def test_vsam_statuses_defaults(self):
        vs = VsamStatuses()
        assert vs.err_vsam_success == "00"
        assert vs.err_vsam_dupkey == "22"
        assert vs.err_vsam_notfnd == "23"
        assert vs.err_vsam_eof == "10"

    def test_vsam_messages_defaults(self):
        vm = VsamMessages()
        assert vm.err_vsam_22 == "Duplicate record key"
        assert vm.err_vsam_23 == "Record not found"


class TestPortfolioRecord:
    def test_valid_record(self):
        rec = PortfolioRecord(
            port_key=PortfolioKey(port_id="PORT0001", port_account_no="ACCT000001"),
            port_client_info=PortfolioClientInfo(
                port_client_name="John Doe",
                port_client_type="I",
            ),
            port_portfolio_info=PortfolioInfo(
                port_create_date=20240315,
                port_last_maint=20240315,
                port_status="A",
            ),
            port_financial_info=PortfolioFinancialInfo(
                port_total_value=Decimal("100000.00"),
                port_cash_balance=Decimal("25000.00"),
            ),
            port_audit_info=PortfolioAuditInfo(
                port_last_user="ADMIN01",
                port_last_trans=20240315,
            ),
        )
        assert rec.port_key.port_id == "PORT0001"
        assert rec.port_financial_info.port_total_value == Decimal("100000.00")

    def test_invalid_client_type(self):
        with pytest.raises(ValidationError, match="port_client_type must be one of"):
            PortfolioClientInfo(
                port_client_name="Test",
                port_client_type="Z",
            )

    def test_all_client_types(self):
        for ct in ("I", "C", "T"):
            info = PortfolioClientInfo(port_client_name="Test", port_client_type=ct)
            assert info.port_client_type == ct

    def test_invalid_status(self):
        with pytest.raises(ValidationError, match="port_status must be one of"):
            PortfolioInfo(
                port_create_date=20240101,
                port_last_maint=20240101,
                port_status="Z",
            )


class TestPortfolioValidation:
    def test_return_codes_defaults(self):
        vrc = ValidationReturnCodes()
        assert vrc.val_success == 0
        assert vrc.val_invalid_id == 1

    def test_error_messages_defaults(self):
        vem = ValidationErrorMessages()
        assert vem.val_err_id == "Invalid Portfolio ID format"

    def test_constants_defaults(self):
        vc = ValidationConstants()
        assert vc.val_min_amount == Decimal("-9999999999999.99")
        assert vc.val_max_amount == Decimal("9999999999999.99")
        assert vc.val_id_prefix == "PORT"

    def test_work_areas(self):
        wa = ValidationWorkAreas()
        assert wa.val_numeric_check == ""
        assert wa.val_temp_num == Decimal("0")


class TestReturnHandling:
    def test_full_structure(self):
        rh = ReturnHandling(
            return_status=ReturnStatus(
                return_code=8,
                reason_code=1,
                module_id="PORTMSTR",
                function_id="VALIDATE",
            ),
            return_details=ReturnDetails(
                error_location=ErrorLocation(
                    program_name="TRNVAL00",
                    paragraph_name="VAL-PORT",
                    error_routine="ERR-RTN",
                ),
                error_info=ErrorInfo(
                    error_type="V",
                    error_code="E001",
                    error_text="Invalid portfolio ID",
                ),
                system_info=SystemInfo(system_code="SYS1", system_msg=""),
            ),
            return_actions=ReturnActions(
                action_flag="A",
                retry_count=0,
                max_retries=3,
            ),
        )
        assert rh.return_status.return_code == 8
        assert rh.return_details.error_info.error_type == "V"
        assert rh.return_actions.action_flag == "A"

    def test_invalid_error_type(self):
        with pytest.raises(ValidationError, match="error_type must be one of"):
            ErrorInfo(error_type="Z", error_code="E001", error_text="test")

    def test_invalid_action_flag(self):
        with pytest.raises(ValidationError, match="action_flag must be one of"):
            ReturnActions(action_flag="Z", retry_count=0)

    def test_standard_error_codes(self):
        sec = StandardErrorCodes()
        assert sec.err_invalid_data == "E001"
        assert sec.err_timeout == "E010"


class TestReturnCodeArea:
    def test_valid_area(self):
        rca = ReturnCodeArea(
            rc_request_type="I",
            rc_program_id="TRNVAL00",
            rc_codes_area=ReturnCodesArea(
                rc_current_code=0,
                rc_highest_code=0,
                rc_new_code=0,
                rc_status="S",
            ),
            rc_message="Initialized",
            rc_response_code=0,
            rc_analysis_data=ReturnCodeAnalysisData(
                rc_start_time="2024-03-15T14:30:22.000000",
                rc_end_time="2024-03-15T14:30:23.000000",
                rc_total_codes=0,
                rc_max_code=0,
                rc_min_code=0,
            ),
            rc_return_data=ReturnData(
                rc_return_value=0,
                rc_highest_return=0,
                rc_return_status="S",
            ),
        )
        assert rca.rc_request_type == "I"

    def test_invalid_request_type(self):
        with pytest.raises(ValidationError, match="rc_request_type must be one of"):
            ReturnCodeArea(
                rc_request_type="Z",
                rc_program_id="TEST",
                rc_codes_area=ReturnCodesArea(
                    rc_current_code=0,
                    rc_highest_code=0,
                    rc_new_code=0,
                    rc_status="S",
                ),
                rc_message="",
                rc_response_code=0,
                rc_analysis_data=ReturnCodeAnalysisData(
                    rc_start_time="",
                    rc_end_time="",
                    rc_total_codes=0,
                    rc_max_code=0,
                    rc_min_code=0,
                ),
                rc_return_data=ReturnData(
                    rc_return_value=0,
                    rc_highest_return=0,
                    rc_return_status="S",
                ),
            )

    def test_invalid_rc_status(self):
        with pytest.raises(ValidationError, match="rc_status must be one of"):
            ReturnCodesArea(
                rc_current_code=0,
                rc_highest_code=0,
                rc_new_code=0,
                rc_status="Z",
            )

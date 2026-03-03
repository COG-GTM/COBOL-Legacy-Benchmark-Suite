"""Tests for online models (INQCOM, DB2REQ, ERRHND copybooks)."""

import pytest
from pydantic import ValidationError

from src.models.online.inquiry import InquiryCommunicationArea
from src.models.online.db2_request import Db2ErrorInfo, Db2RequestArea
from src.models.online.error_handler import ErrorTrace, OnlineErrorHandling


class TestInquiryCommunicationArea:
    def test_valid_menu(self):
        area = InquiryCommunicationArea(
            inqcom_function="MENU",
            inqcom_account_no="ACCT000001",
            inqcom_response_code=0,
        )
        assert area.inqcom_function == "MENU"

    def test_all_functions(self):
        for fn in ("MENU", "INQP", "INQH", "EXIT"):
            area = InquiryCommunicationArea(
                inqcom_function=fn,
                inqcom_account_no="ACCT000001",
                inqcom_response_code=0,
            )
            assert area.inqcom_function == fn

    def test_invalid_function(self):
        with pytest.raises(ValidationError, match="inqcom_function must be one of"):
            InquiryCommunicationArea(
                inqcom_function="XXXX",
                inqcom_account_no="ACCT000001",
                inqcom_response_code=0,
            )

    def test_error_msg_default(self):
        area = InquiryCommunicationArea(
            inqcom_function="MENU",
            inqcom_account_no="ACCT000001",
            inqcom_response_code=0,
        )
        assert area.inqcom_error_msg == ""

    def test_from_attributes_config(self):
        assert InquiryCommunicationArea.model_config["from_attributes"] is True


class TestDb2RequestArea:
    def test_valid_connect(self):
        area = Db2RequestArea(
            db2_request_type="C",
            db2_response_code=0,
            db2_connection_token="TOKEN123456789",
            db2_error_info=Db2ErrorInfo(db2_sqlcode=0),
        )
        assert area.db2_request_type == "C"
        assert area.db2_connection_token == "TOKEN123456789"

    def test_all_request_types(self):
        for rt in ("C", "D", "S"):
            area = Db2RequestArea(
                db2_request_type=rt,
                db2_response_code=0,
                db2_connection_token="",
                db2_error_info=Db2ErrorInfo(db2_sqlcode=0),
            )
            assert area.db2_request_type == rt

    def test_invalid_request_type(self):
        with pytest.raises(ValidationError, match="db2_request_type must be one of"):
            Db2RequestArea(
                db2_request_type="Z",
                db2_response_code=0,
                db2_connection_token="",
                db2_error_info=Db2ErrorInfo(db2_sqlcode=0),
            )

    def test_error_info(self):
        info = Db2ErrorInfo(db2_sqlcode=-803, db2_error_msg="Duplicate key")
        assert info.db2_sqlcode == -803
        assert info.db2_error_msg == "Duplicate key"


class TestOnlineErrorHandling:
    def test_valid_error(self):
        err = OnlineErrorHandling(
            err_program="INQONLN",
            err_paragraph="PROCESS-INQUIRY",
            err_sqlcode=0,
            err_cics_resp=0,
            err_cics_resp2=0,
            err_severity="I",
            err_message="Processing complete",
            err_action="R",
            err_trace=ErrorTrace(
                err_trace_id="TRACE00000001",
                err_timestamp="2024-03-15T14:30:22.000000",
            ),
        )
        assert err.err_program == "INQONLN"
        assert err.err_severity == "I"
        assert err.err_action == "R"

    def test_all_severities(self):
        for sev in ("F", "W", "I"):
            err = OnlineErrorHandling(
                err_program="TEST",
                err_paragraph="PARA",
                err_sqlcode=0,
                err_cics_resp=0,
                err_cics_resp2=0,
                err_severity=sev,
                err_message="",
                err_action="R",
                err_trace=ErrorTrace(err_trace_id="ID", err_timestamp="TS"),
            )
            assert err.err_severity == sev

    def test_invalid_severity(self):
        with pytest.raises(ValidationError, match="err_severity must be one of"):
            OnlineErrorHandling(
                err_program="TEST",
                err_paragraph="PARA",
                err_sqlcode=0,
                err_cics_resp=0,
                err_cics_resp2=0,
                err_severity="Z",
                err_message="",
                err_action="R",
                err_trace=ErrorTrace(err_trace_id="ID", err_timestamp="TS"),
            )

    def test_all_actions(self):
        for act in ("R", "C", "A"):
            err = OnlineErrorHandling(
                err_program="TEST",
                err_paragraph="PARA",
                err_sqlcode=0,
                err_cics_resp=0,
                err_cics_resp2=0,
                err_severity="I",
                err_message="",
                err_action=act,
                err_trace=ErrorTrace(err_trace_id="ID", err_timestamp="TS"),
            )
            assert err.err_action == act

    def test_invalid_action(self):
        with pytest.raises(ValidationError, match="err_action must be one of"):
            OnlineErrorHandling(
                err_program="TEST",
                err_paragraph="PARA",
                err_sqlcode=0,
                err_cics_resp=0,
                err_cics_resp2=0,
                err_severity="I",
                err_message="",
                err_action="Z",
                err_trace=ErrorTrace(err_trace_id="ID", err_timestamp="TS"),
            )

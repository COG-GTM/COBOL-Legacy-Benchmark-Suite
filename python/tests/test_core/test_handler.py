"""Tests for the centralized error handler."""

import logging
from unittest.mock import MagicMock

import pytest

from python.src.core.error_handling.exceptions import (
    CLBSError,
    ConnectionError,
    DataError,
    ErrorCategory,
    ErrorSeverity,
    ProcessingError,
    RecoveryAction,
    SecurityError,
    SystemError,
    ValidationError,
)
from python.src.core.error_handling.handler import ErrorHandler, ErrorLogStore


class MockErrorLogStore(ErrorLogStore):
    """Mock error log store for testing."""

    def __init__(self, should_fail: bool = False):
        self.records: list[dict] = []
        self.should_fail = should_fail

    def save_error_log(self, error_record: dict) -> bool:
        if self.should_fail:
            raise RuntimeError("Database insert failed")
        self.records.append(error_record)
        return True


class TestErrorHandlerInit:
    """Test ErrorHandler initialization."""

    def test_init_without_store(self):
        handler = ErrorHandler()
        assert handler._error_store is None

    def test_init_with_store(self):
        store = MockErrorLogStore()
        handler = ErrorHandler(error_store=store)
        assert handler._error_store is store


class TestErrorHandlerP100:
    """Test P100: Init error context."""

    def test_clbs_error_context(self):
        handler = ErrorHandler()
        err = DataError("test data error", program="TRNVAL00")
        result = handler.handle_error(err, program="TRNVAL00", paragraph="VALIDATE")

        assert result["program"] == "TRNVAL00"
        assert result["paragraph"] == "VALIDATE"
        assert result["message"] == "test data error"
        assert result["category"] == "VS"
        assert result["severity"] == ErrorSeverity.ERROR
        assert result["trace_id"] is not None
        assert len(result["trace_id"]) == 16

    def test_generic_exception_context(self):
        handler = ErrorHandler()
        err = ValueError("something went wrong")
        result = handler.handle_error(err, program="TESTPGM")

        assert result["program"] == "TESTPGM"
        assert result["message"] == "something went wrong"
        assert result["category"] == "SY"
        assert result["severity"] == ErrorSeverity.ERROR
        assert result["exception_type"] == "ValueError"

    def test_custom_trace_id(self):
        handler = ErrorHandler()
        err = CLBSError("test")
        result = handler.handle_error(err, trace_id="custom-trace-123")

        assert result["trace_id"] == "custom-trace-123"

    def test_auto_generated_trace_id(self):
        handler = ErrorHandler()
        err = CLBSError("test")
        result = handler.handle_error(err)

        assert result["trace_id"] is not None
        assert len(result["trace_id"]) == 16


class TestErrorHandlerP200:
    """Test P200: Log error."""

    def test_logs_to_store(self):
        store = MockErrorLogStore()
        handler = ErrorHandler(error_store=store)
        err = DataError("test error")
        handler.handle_error(err, program="PGM1")

        assert len(store.records) == 1
        record = store.records[0]
        assert record["program"] == "PGM1"
        assert record["message"] == "test error"
        assert record["trace_id"] is not None

    def test_store_failure_handled(self, caplog):
        """Mirrors ERRHNDL: if log INSERT fails, log 'Error logging failed'."""
        store = MockErrorLogStore(should_fail=True)
        handler = ErrorHandler(error_store=store)
        err = DataError("test error")

        with caplog.at_level(logging.ERROR, logger="clbs.error_handler"):
            result = handler.handle_error(err)

        # Should not raise, should log failure
        assert result is not None
        assert any("Error logging failed" in r.message for r in caplog.records)

    def test_logs_via_python_logging(self, caplog):
        handler = ErrorHandler()
        err = DataError("logged error")

        with caplog.at_level(logging.ERROR, logger="clbs.error_handler"):
            handler.handle_error(err)

        assert any("logged error" in r.message for r in caplog.records)


class TestErrorHandlerP300:
    """Test P300: Format error message."""

    def test_format_with_program(self):
        handler = ErrorHandler()
        err = CLBSError("connection lost")
        result = handler.handle_error(err, program="DB2ONLN")

        assert result["formatted_message"].startswith("Error in DB2ONLN")
        assert "connection lost" in result["formatted_message"]
        assert result["trace_id"] in result["formatted_message"]

    def test_format_without_program(self):
        handler = ErrorHandler()
        err = CLBSError("general error")
        result = handler.handle_error(err)

        assert not result["formatted_message"].startswith("Error in")
        assert "general error" in result["formatted_message"]


class TestErrorHandlerP400:
    """Test P400: Determine recovery action.

    Matches ERRHNDL EVALUATE:
      ERR-FATAL   -> ERR-ABEND
      ERR-WARNING -> ERR-CONTINUE
      OTHER       -> ERR-RETURN
    """

    def test_warning_continues(self):
        handler = ErrorHandler()
        err = CLBSError("minor issue", severity=ErrorSeverity.WARNING)
        result = handler.handle_error(err)
        assert result["recovery_action"] == RecoveryAction.CONTINUE.value

    def test_error_returns(self):
        handler = ErrorHandler()
        err = CLBSError("error", severity=ErrorSeverity.ERROR)
        result = handler.handle_error(err)
        assert result["recovery_action"] == RecoveryAction.RETURN.value

    def test_severe_returns(self):
        handler = ErrorHandler()
        err = CLBSError("severe", severity=ErrorSeverity.SEVERE)
        result = handler.handle_error(err)
        assert result["recovery_action"] == RecoveryAction.RETURN.value

    def test_terminal_abends(self):
        handler = ErrorHandler()
        err = SystemError("fatal crash")
        result = handler.handle_error(err)
        assert result["recovery_action"] == RecoveryAction.ABEND.value


class TestHandleException:
    """Test handle_exception alias."""

    def test_handle_exception_works(self):
        handler = ErrorHandler()
        err = RuntimeError("unexpected")
        result = handler.handle_exception(err, program="TEST")
        assert result["message"] == "unexpected"
        assert result["program"] == "TEST"

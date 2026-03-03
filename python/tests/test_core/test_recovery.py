"""Tests for retry/recovery logic."""

from unittest.mock import MagicMock, patch

import pytest
from tenacity import RetryError

from python.src.core.error_handling.recovery import (
    RecoveryManager,
    RetryConfig,
    create_retry_decorator,
    retry_database_operation,
    retry_external_call,
)


class TestRetryConfig:
    """Test RetryConfig defaults match DB2RECV WS-RECOVERY-STATS."""

    def test_default_values(self):
        config = RetryConfig()
        assert config.max_retries == 3  # WS-MAX-RETRIES
        assert config.wait_seconds == 2.0  # WS-RETRY-INTERVAL
        assert config.exponential_backoff is False
        assert config.backoff_multiplier == 1.0
        assert config.backoff_max == 60.0

    def test_custom_values(self):
        config = RetryConfig(
            max_retries=5,
            wait_seconds=1.0,
            exponential_backoff=True,
        )
        assert config.max_retries == 5
        assert config.wait_seconds == 1.0
        assert config.exponential_backoff is True


class TestCreateRetryDecorator:
    """Test create_retry_decorator function."""

    def test_retries_on_failure(self):
        call_count = 0

        config = RetryConfig(max_retries=3, wait_seconds=0)

        @create_retry_decorator(config)
        def flaky_fn():
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise ValueError("not yet")
            return "success"

        result = flaky_fn()
        assert result == "success"
        assert call_count == 3

    def test_exhausts_retries(self):
        call_count = 0

        config = RetryConfig(max_retries=3, wait_seconds=0)

        @create_retry_decorator(config)
        def always_fails():
            nonlocal call_count
            call_count += 1
            raise ValueError("always fails")

        with pytest.raises(ValueError, match="always fails"):
            always_fails()
        assert call_count == 3

    def test_succeeds_first_try(self):
        config = RetryConfig(max_retries=3, wait_seconds=0)

        @create_retry_decorator(config)
        def works_first_time():
            return 42

        assert works_first_time() == 42

    def test_exponential_backoff_config(self):
        config = RetryConfig(
            max_retries=2,
            wait_seconds=0,
            exponential_backoff=True,
            backoff_multiplier=0.01,
            backoff_max=1.0,
        )

        call_count = 0

        @create_retry_decorator(config)
        def fail_once():
            nonlocal call_count
            call_count += 1
            if call_count < 2:
                raise RuntimeError("fail")
            return "ok"

        result = fail_once()
        assert result == "ok"
        assert call_count == 2


class TestRetryDatabaseOperation:
    """Test retry_database_operation decorator."""

    def test_retries_database_call(self):
        call_count = 0

        @retry_database_operation(max_retries=3, wait_seconds=0)
        def db_query():
            nonlocal call_count
            call_count += 1
            if call_count < 2:
                raise ConnectionError("DB connection lost")
            return {"data": "result"}

        result = db_query()
        assert result == {"data": "result"}
        assert call_count == 2

    def test_database_retry_exhausted(self):
        @retry_database_operation(max_retries=2, wait_seconds=0)
        def db_query():
            raise TimeoutError("DB timeout")

        with pytest.raises(TimeoutError):
            db_query()


class TestRetryExternalCall:
    """Test retry_external_call decorator."""

    def test_retries_external_service(self):
        call_count = 0

        @retry_external_call(max_retries=3, wait_seconds=0)
        def call_service():
            nonlocal call_count
            call_count += 1
            if call_count < 3:
                raise OSError("Service unavailable")
            return "response"

        result = call_service()
        assert result == "response"
        assert call_count == 3


class TestRecoveryManager:
    """Test RecoveryManager mirroring DB2RECV patterns."""

    def test_init_defaults(self):
        mgr = RecoveryManager()
        assert mgr.max_retries == 3
        assert mgr.wait_seconds == 2.0
        assert mgr.retry_count == 0
        assert mgr.last_error is None

    def test_recover_connection_success(self):
        mgr = RecoveryManager(max_retries=3, wait_seconds=0)
        connect_fn = MagicMock(return_value="connected")

        result = mgr.recover_connection(connect_fn)

        assert result["status"] == "S"
        assert result["response_code"] == 0
        assert result["result"] == "connected"

    def test_recover_connection_failure(self):
        mgr = RecoveryManager(max_retries=2, wait_seconds=0)
        connect_fn = MagicMock(side_effect=RuntimeError("connection refused"))

        result = mgr.recover_connection(connect_fn)

        assert result["status"] == "F"
        assert result["response_code"] == -1
        assert "connection refused" in result["error_info"]
        assert mgr.last_error is not None

    def test_recover_connection_retry_then_success(self):
        mgr = RecoveryManager(max_retries=3, wait_seconds=0)
        call_count = 0

        def flaky_connect():
            nonlocal call_count
            call_count += 1
            if call_count < 2:
                raise RuntimeError("not ready")
            return "connected"

        result = mgr.recover_connection(flaky_connect)
        assert result["status"] == "S"
        assert result["response_code"] == 0

    def test_recover_transaction_success(self):
        mgr = RecoveryManager()
        rollback_fn = MagicMock(return_value=None)

        result = mgr.recover_transaction(rollback_fn)

        assert result["status"] == "S"
        assert result["response_code"] == 0
        rollback_fn.assert_called_once()

    def test_recover_transaction_failure(self):
        mgr = RecoveryManager()
        rollback_fn = MagicMock(side_effect=RuntimeError("rollback failed"))

        result = mgr.recover_transaction(rollback_fn)

        assert result["status"] == "F"
        assert result["response_code"] == -1
        assert mgr.last_error is not None

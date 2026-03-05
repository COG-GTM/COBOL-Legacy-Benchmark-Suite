"""
Batch controller tests translated from COBOL BCHCTL00.cbl.

Tests:
- INIT function (1100-INITIALIZE)
- CHEK function (1200-CHECK-PREREQUISITES)
- UPDT function (1300-UPDATE-STATUS)
- TERM function (1400-TERMINATE)
- Dispatch EVALUATE TRUE pattern
"""

import pytest

from src.batch.controller import BatchController
from src.common.constants import BatchStatus, ReturnCode
from src.db.repository import BatchControlRepository


class TestBatchControllerInit:
    """Test INIT function. Translates BCHCTL00.cbl 1100-INITIALIZE."""

    def test_initialize_new_job(self, session):
        ctrl = BatchController(session)
        rc = ctrl.dispatch("INIT", "TRNVAL", "20240115")
        assert rc == ReturnCode.SUCCESS

        repo = BatchControlRepository(session)
        rec = repo.get("TRNVAL", "20240115")
        assert rec is not None
        assert rec.status == BatchStatus.READY

    def test_initialize_existing_job_resets(self, session, sample_batch_control):
        """Re-initializing an existing job resets it to READY."""
        sample_batch_control.status = BatchStatus.ACTIVE
        session.flush()

        ctrl = BatchController(session)
        rc = ctrl.dispatch("INIT", "TRNVAL", "20240115")
        assert rc == ReturnCode.SUCCESS

        repo = BatchControlRepository(session)
        rec = repo.get("TRNVAL", "20240115")
        assert rec.status == BatchStatus.READY


class TestBatchControllerCheck:
    """Test CHEK function. Translates BCHCTL00.cbl 1200-CHECK-PREREQUISITES."""

    def test_check_ready_job(self, session, sample_batch_control):
        ctrl = BatchController(session)
        rc = ctrl.dispatch("CHEK", "TRNVAL", "20240115")
        assert rc == ReturnCode.SUCCESS


class TestBatchControllerUpdate:
    """Test UPDT function. Translates BCHCTL00.cbl 1300-UPDATE-STATUS."""

    def test_update_status(self, session, sample_batch_control):
        sample_batch_control.status = BatchStatus.ACTIVE
        session.flush()

        ctrl = BatchController(session)
        rc = ctrl.dispatch("UPDT", "TRNVAL", "20240115")
        assert rc == ReturnCode.SUCCESS


class TestBatchControllerTerminate:
    """Test TERM function. Translates BCHCTL00.cbl 1400-TERMINATE."""

    def test_terminate_active_job(self, session, sample_batch_control):
        sample_batch_control.status = BatchStatus.ACTIVE
        session.flush()

        ctrl = BatchController(session)
        rc = ctrl.dispatch("TERM", "TRNVAL", "20240115")
        assert rc == ReturnCode.SUCCESS

        repo = BatchControlRepository(session)
        rec = repo.get("TRNVAL", "20240115")
        assert rec.status == BatchStatus.DONE


class TestBatchControllerDispatch:
    """Test EVALUATE TRUE dispatch pattern."""

    def test_invalid_function(self, session):
        ctrl = BatchController(session)
        with pytest.raises(Exception):
            ctrl.dispatch("INVALID", "TRNVAL", "20240115")

"""
Batch controller tests translated from TSTVAL00.cbl.

Tests batch lifecycle from BCHCTL00.cbl:
  - Initialize batch job
  - Check prerequisites
  - Update status
  - Terminate batch job
"""

from datetime import date

from sqlalchemy.orm import Session

from src.batch.controller import BatchController
from src.common.constants import BatchProcessType, BatchStatus, ReturnCode
from src.models.batch_control import BatchParameters


class TestBatchController:
    """Test BatchController (BCHCTL00.cbl)."""

    def _make_params(self, batch_id: str = "BCH0115") -> BatchParameters:
        return BatchParameters(
            batch_id=batch_id,
            process_date=date(2024, 1, 15),
            process_type=BatchProcessType.INITIAL,
        )

    def test_initialize(self, session: Session):
        controller = BatchController(session)
        rc = controller.initialize(self._make_params())
        assert rc == ReturnCode.SUCCESS
        assert controller.status.status == BatchStatus.ACTIVE

    def test_dispatch_init(self, session: Session):
        controller = BatchController(session)
        rc = controller.dispatch("INIT", self._make_params())
        assert rc == ReturnCode.SUCCESS

    def test_dispatch_check(self, session: Session):
        controller = BatchController(session)
        controller.initialize(self._make_params())
        rc = controller.dispatch("CHEK", self._make_params())
        assert rc == ReturnCode.SUCCESS

    def test_terminate_success(self, session: Session):
        controller = BatchController(session)
        params = self._make_params()
        controller.initialize(params)
        rc = controller.terminate(params)
        assert rc == ReturnCode.SUCCESS
        assert controller.status.status == BatchStatus.DONE

    def test_terminate_with_errors(self, session: Session):
        controller = BatchController(session)
        params = self._make_params()
        controller.initialize(params)
        controller.increment_error("Test error")
        rc = controller.terminate(params)
        assert rc == ReturnCode.WARNING

    def test_counters(self, session: Session):
        controller = BatchController(session)
        controller.initialize(self._make_params())
        controller.increment_read()
        controller.increment_read()
        controller.increment_processed()
        controller.increment_error("err")
        assert controller.status.records_read == 2
        assert controller.status.records_processed == 1
        assert controller.status.error_count == 1

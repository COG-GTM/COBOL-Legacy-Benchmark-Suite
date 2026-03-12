"""
Admin router for batch processing and system management.

Provides endpoints for:
  - Triggering batch processing
  - Checking batch status
  - Health checks
"""

import logging
from datetime import datetime

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy import text
from sqlalchemy.orm import Session

from src.api.schemas import BatchRunRequest, BatchStatusResponse, HealthResponse
from src.api.security import require_admin_access
from src.common.constants import ReturnCode
from src.db.session import get_session

logger = logging.getLogger(__name__)
router = APIRouter()


@router.get("/health", response_model=HealthResponse)
def health_check(
    session: Session = Depends(get_session),
) -> HealthResponse:
    """
    System health check.

    Verifies database connectivity and returns system status.
    """
    db_status = "connected"
    try:
        session.execute(text("SELECT 1"))
    except Exception:
        db_status = "disconnected"

    return HealthResponse(
        status="healthy" if db_status == "connected" else "degraded",
        version="1.0.0",
        database=db_status,
        timestamp=datetime.now(),
    )


@router.post("/batch/run", response_model=BatchStatusResponse)
def trigger_batch(
    request: BatchRunRequest,
    session: Session = Depends(get_session),
    user_id: str = Depends(require_admin_access),
) -> BatchStatusResponse:
    """
    Trigger batch processing.

    Runs the batch cycle synchronously and returns the result.
    For production, this should be run asynchronously.
    """
    from src.batch.runner import run_full_cycle, run_single_step

    process_date = request.process_date

    if request.full_cycle:
        rc = run_full_cycle(process_date, restart=request.restart)
    elif request.step:
        rc = run_single_step(request.step, process_date)
    else:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Either full_cycle or step must be specified",
        )

    return BatchStatusResponse(
        batch_id=f"BCH{process_date.strftime('%m%d')}",
        status="completed" if rc <= ReturnCode.WARNING else "failed",
        start_time=datetime.now(),
        end_time=datetime.now(),
        records_read=0,
        records_processed=0,
        records_rejected=0,
        error_count=0 if rc == ReturnCode.SUCCESS else 1,
        return_code=rc,
    )


@router.get("/batch/status", response_model=BatchStatusResponse)
def get_batch_status(
    batch_id: str | None = None,
    session: Session = Depends(get_session),
    user_id: str = Depends(require_admin_access),
) -> BatchStatusResponse:
    """
    Get batch processing status.

    Returns the status of the most recent or specified batch job.
    """
    from src.db.repository import BatchControlRepository

    repo = BatchControlRepository(session)

    if batch_id:
        control = repo.get_by_id(batch_id)
    else:
        # Get most recent batch across all statuses
        all_batches = sorted(
            repo.list_by_status("A") + repo.list_by_status("D")
            + repo.list_by_status("E") + repo.list_by_status("R")
            + repo.list_by_status("W"),
            key=lambda b: b.schedule_date,
            reverse=True,
        )
        control = all_batches[0] if all_batches else None

    if control is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="No batch status found",
        )

    return BatchStatusResponse(
        batch_id=control.batch_id,
        status=control.batch_status,
        start_time=None,
        end_time=None,
        records_read=0,
        records_processed=0,
        records_rejected=0,
        error_count=0,
        return_code=control.last_run_rc,
    )

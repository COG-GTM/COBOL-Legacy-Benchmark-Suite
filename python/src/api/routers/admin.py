"""
Admin router for batch processing and health checks.
"""

import logging
from datetime import date, datetime

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import text
from sqlalchemy.orm import Session

from src.api.schemas import BatchRunRequest, BatchStatusResponse, HealthResponse
from src.api.security import validate_api_key
from src.db.repository import BatchControlRepository
from src.db.session import get_session_dependency

logger = logging.getLogger(__name__)
router = APIRouter()


@router.post("/batch/run", response_model=dict)
def run_batch(
    req: BatchRunRequest,
    user: str = Depends(validate_api_key),
):
    """POST /batch/run — trigger batch processing."""
    from src.batch.runner import run_full_cycle

    process_date = req.process_date or date.today().strftime("%Y%m%d")
    try:
        rc = run_full_cycle(process_date)
        return {"status": "completed", "return_code": rc.value, "process_date": process_date}
    except Exception as e:
        logger.error("Batch run failed: %s", e)
        raise HTTPException(status_code=500, detail=str(e))


@router.get("/batch/status", response_model=list[BatchStatusResponse])
def get_batch_status(
    process_date: str | None = None,
    session: Session = Depends(get_session_dependency),
    user: str = Depends(validate_api_key),
):
    """GET /batch/status — check batch status."""
    repo = BatchControlRepository(session)
    if process_date:
        records = repo.list_by_date(process_date)
    else:
        records = repo.list_all()

    return [
        BatchStatusResponse(
            job_name=r.job_name,
            process_date=r.process_date,
            status=r.status,
            return_code=r.return_code,
            records_read=r.records_read,
            records_written=r.records_written,
            error_count=r.error_count,
            start_time=r.start_time.isoformat() if r.start_time else None,
            end_time=r.end_time.isoformat() if r.end_time else None,
        )
        for r in records
    ]


@router.get("/health", response_model=HealthResponse)
def health_check(
    session: Session = Depends(get_session_dependency),
):
    """GET /health — health check."""
    db_status = "ok"
    try:
        session.execute(text("SELECT 1"))
    except Exception:
        db_status = "error"

    return HealthResponse(
        status="ok" if db_status == "ok" else "degraded",
        database=db_status,
        timestamp=datetime.now().isoformat(),
    )

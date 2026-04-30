from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from models.database import get_db
from schemas.report import PositionReportResponse, AuditReportResponse, StatisticsResponse
from services import report_service

router = APIRouter(prefix="/api/reports", tags=["reports"])


@router.get("/positions", response_model=PositionReportResponse)
def position_report(db: Session = Depends(get_db)):
    return report_service.generate_position_report(db)


@router.get("/audit", response_model=AuditReportResponse)
def audit_report(
    limit: int = Query(100, ge=1, le=1000),
    db: Session = Depends(get_db),
):
    return report_service.generate_audit_report(db, limit)


@router.get("/statistics", response_model=StatisticsResponse)
def statistics(db: Session = Depends(get_db)):
    return report_service.generate_statistics(db)

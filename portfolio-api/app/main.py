import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text

from app.database import engine
from app.routers import admin, inquiries, portfolios, reports, transactions

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    async with engine.connect() as conn:
        await conn.execute(text("SELECT 1"))
    logger.info("Database connectivity verified")
    yield


app = FastAPI(
    title="Investment Portfolio Management API",
    description="COBOL Legacy Benchmark Suite - Python FastAPI Migration",
    version="0.1.0",
    lifespan=lifespan,
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(portfolios.router)
app.include_router(transactions.router)
app.include_router(inquiries.router)
app.include_router(reports.router)
app.include_router(admin.router)


@app.get("/health")
async def health_check():
    return {"status": "ok"}

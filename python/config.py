"""Runtime configuration for the HISTLD00 Python migration.

Values are sourced from environment variables (mirroring the JCL DD names
used by the COBOL job), with sensible defaults suitable for tests and local
development.
"""

from __future__ import annotations

import logging
import os
from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional


@dataclass
class HistoryLoaderConfig:
    """Runtime configuration for :class:`HistoryLoader`.

    Attributes:
        db_url: SQLAlchemy URL for the destination database. Mirrors the
            COBOL ``CONNECT TO POSMVP`` directive.
        tranhist_path: Path to the transaction-history KSDS (DD ``TRANHIST``).
        bchctl_path: Path to the batch-control KSDS (DD ``BCHCTL``).
        errlog_path: Path to the sequential error log (DD ``ERRLOG``).
        commit_threshold: Number of records between commits. Mirrors
            ``WS-COMMIT-THRESHOLD VALUE 1000``.
        max_errors: Stop processing once this many errors accumulate. The
            COBOL program halts when ``WS-ERROR-COUNT > 100``.
        program_id: Program identifier stored in PH-PROGRAM-ID and
            ERR-PROGRAM (defaults to ``HISTLD00``).
        user_id: User identifier stored in PH-USER-ID.
        job_name: BCT-JOB-NAME used to look up the batch-control record.
        log_level: Python logging level for the loader.
    """

    db_url: str = field(
        default_factory=lambda: os.environ.get("POSHIST_DB_URL", "sqlite:///poshist.db")
    )
    tranhist_path: Path = field(
        default_factory=lambda: Path(os.environ.get("TRANHIST_PATH", "tranhist.db"))
    )
    bchctl_path: Path = field(
        default_factory=lambda: Path(os.environ.get("BCHCTL_PATH", "bchctl.db"))
    )
    errlog_path: Path = field(
        default_factory=lambda: Path(os.environ.get("ERRLOG_PATH", "errlog.txt"))
    )
    commit_threshold: int = field(
        default_factory=lambda: int(os.environ.get("COMMIT_THRESHOLD", "1000"))
    )
    max_errors: int = field(
        default_factory=lambda: int(os.environ.get("MAX_ERRORS", "100"))
    )
    program_id: str = "HISTLD00"
    user_id: str = field(default_factory=lambda: os.environ.get("USER_ID", "BATCH"))
    job_name: str = "HISTLD00"
    log_level: int = logging.INFO

    @classmethod
    def from_cli(cls, args) -> "HistoryLoaderConfig":
        """Build a config from an ``argparse`` namespace."""
        return cls(
            db_url=args.db_url,
            tranhist_path=Path(args.tranhist),
            bchctl_path=Path(args.bchctl),
            errlog_path=Path(args.errlog),
            commit_threshold=args.commit_threshold,
            max_errors=args.max_errors,
            program_id=args.program_id,
            user_id=args.user_id,
            job_name=args.job_name,
            log_level=getattr(logging, args.log_level.upper(), logging.INFO),
        )


def configure_logging(level: Optional[int] = None) -> None:
    """Configure stdout logging matching the COBOL DISPLAY style."""
    logging.basicConfig(
        level=level if level is not None else logging.INFO,
        format="%(asctime)s %(levelname)-7s %(name)s %(message)s",
    )

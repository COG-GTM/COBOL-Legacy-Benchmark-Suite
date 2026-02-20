"""Cursor Management for Online Programs - migrated from CURSMGR.cbl.

Manages cursor declarations and lifecycle, implements cursor optimization,
handles array fetching for performance, provides cursor status monitoring.
"""

import logging
from dataclasses import dataclass, field
from typing import Optional, Protocol

from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "CURSMGR"
DEFAULT_MAX_ROWS = 20


class DatabaseCursor(Protocol):
    def execute(self, sql: str, params: Optional[tuple] = None) -> None: ...
    def fetchone(self) -> Optional[tuple]: ...
    def fetchmany(self, size: int) -> list[tuple]: ...
    def close(self) -> None: ...


@dataclass
class CursorInfo:
    name: str = ""
    sql: str = ""
    status: str = "CLOSED"
    rows_fetched: int = 0
    cursor: Optional[DatabaseCursor] = field(default=None, repr=False)


class CursorManager:
    def __init__(self, max_rows: int = DEFAULT_MAX_ROWS):
        self._cursors: dict[str, CursorInfo] = {}
        self._max_rows = max_rows

    def declare(self, name: str, sql: str) -> int:
        if name in self._cursors:
            logger.warning("Cursor %s already declared, replacing", name)

        self._cursors[name] = CursorInfo(name=name, sql=sql, status="DECLARED")
        logger.debug("Cursor %s declared", name)
        return ReturnCode.SUCCESS

    def open(self, name: str, cursor: Optional[DatabaseCursor] = None) -> int:
        info = self._cursors.get(name)
        if info is None:
            logger.error("Cursor %s not declared", name)
            return ReturnCode.ERROR

        info.cursor = cursor
        info.status = "OPEN"
        info.rows_fetched = 0

        if cursor is not None:
            try:
                cursor.execute(info.sql)
            except Exception as e:
                logger.error("Error opening cursor %s: %s", name, e)
                info.status = "ERROR"
                return ReturnCode.ERROR

        logger.debug("Cursor %s opened", name)
        return ReturnCode.SUCCESS

    def fetch(self, name: str, max_rows: Optional[int] = None) -> tuple[int, list]:
        info = self._cursors.get(name)
        if info is None or info.status != "OPEN":
            logger.error("Cursor %s not open", name)
            return ReturnCode.ERROR, []

        fetch_size = max_rows if max_rows is not None else self._max_rows

        if info.cursor is not None:
            try:
                rows = info.cursor.fetchmany(fetch_size)
                info.rows_fetched += len(rows)
                if not rows:
                    return ReturnCode.WARNING, []
                return ReturnCode.SUCCESS, rows
            except Exception as e:
                logger.error("Error fetching from cursor %s: %s", name, e)
                return ReturnCode.ERROR, []

        return ReturnCode.WARNING, []

    def close(self, name: str) -> int:
        info = self._cursors.get(name)
        if info is None:
            logger.warning("Cursor %s not found", name)
            return ReturnCode.WARNING

        if info.cursor is not None:
            try:
                info.cursor.close()
            except Exception as e:
                logger.error("Error closing cursor %s: %s", name, e)

        info.status = "CLOSED"
        info.cursor = None
        logger.debug("Cursor %s closed (fetched %d rows)", name, info.rows_fetched)
        return ReturnCode.SUCCESS

    def get_status(self, name: str) -> Optional[str]:
        info = self._cursors.get(name)
        return info.status if info is not None else None

    def close_all(self) -> int:
        for name in list(self._cursors.keys()):
            self.close(name)
        return ReturnCode.SUCCESS

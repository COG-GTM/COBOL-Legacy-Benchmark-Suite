"""Cursor Manager module - replaces CURSMGR.cbl.

Provides standardized cursor management replacing the COBOL
declare/open/fetch/close pattern with Python's cursor.fetchmany(n).

COBOL CURSMGR functions (EVALUATE LS-CURS-FUNCTION):
- D: Declare cursor (P100-DECLARE-CURSOR)
- O: Open cursor (P200-OPEN-CURSOR)
- F: Fetch rows (P300-FETCH-ROWS) - array fetch, max 20 rows
- C: Close cursor (P400-CLOSE-CURSOR)
"""

import logging
from typing import Any, Generator

logger = logging.getLogger("portfolio.online.cursor_manager")

# Constants matching COBOL CURSMGR
MAX_ROWS = 20  # WS-MAX-ROWS from CURSMGR.cbl
DEFAULT_FETCH_SIZE = 10  # WS-FETCH-SIZE from INQHIST.cbl


class CursorManager:
    """Cursor manager replacing CURSMGR.cbl.

    Provides a Pythonic wrapper around database result pagination,
    replacing the COBOL declare/open/fetch/close cursor pattern.

    Usage:
        mgr = CursorManager(fetch_size=10)
        mgr.declare("history_cursor", query_func, params)
        mgr.open("history_cursor")
        rows = mgr.fetch("history_cursor")
        mgr.close("history_cursor")
    """

    def __init__(self, fetch_size: int = DEFAULT_FETCH_SIZE) -> None:
        self.fetch_size = min(fetch_size, MAX_ROWS)
        self.cursors: dict[str, dict[str, Any]] = {}

    def declare(
        self,
        cursor_name: str,
        data: list[Any],
    ) -> None:
        """Declare a cursor - replaces P100-DECLARE-CURSOR.

        COBOL: EXEC SQL DECLARE cursor-name CURSOR FOR SELECT ... END-EXEC
        Python: Stores the data source for later iteration.
        """
        self.cursors[cursor_name] = {
            "data": data,
            "position": 0,
            "is_open": False,
            "rows_fetched": 0,
            "end_of_data": False,
        }
        logger.debug("Cursor '%s' declared with %d rows", cursor_name, len(data))

    def open(self, cursor_name: str) -> bool:
        """Open a cursor - replaces P200-OPEN-CURSOR.

        COBOL: EXEC SQL OPEN cursor-name END-EXEC
        """
        cursor = self.cursors.get(cursor_name)
        if cursor is None:
            logger.error("Cursor '%s' not declared", cursor_name)
            return False

        cursor["is_open"] = True
        cursor["position"] = 0
        cursor["rows_fetched"] = 0
        cursor["end_of_data"] = False
        logger.debug("Cursor '%s' opened", cursor_name)
        return True

    def fetch(self, cursor_name: str, fetch_size: int | None = None) -> list[Any]:
        """Fetch rows from cursor - replaces P300-FETCH-ROWS.

        COBOL: EXEC SQL FETCH cursor-name INTO :host-vars END-EXEC
        Python: Returns up to fetch_size rows from current position.

        This maps directly to Python's cursor.fetchmany(n) pattern
        as noted in the migration guide.
        """
        cursor = self.cursors.get(cursor_name)
        if cursor is None or not cursor["is_open"]:
            logger.error("Cursor '%s' not open", cursor_name)
            return []

        size = min(fetch_size or self.fetch_size, MAX_ROWS)
        data = cursor["data"]
        pos = cursor["position"]

        # Fetch the next batch (equivalent to cursor.fetchmany(n))
        rows = data[pos : pos + size]
        cursor["position"] = pos + len(rows)
        cursor["rows_fetched"] += len(rows)

        if not rows or cursor["position"] >= len(data):
            cursor["end_of_data"] = True

        logger.debug(
            "Cursor '%s' fetched %d rows (total: %d)",
            cursor_name, len(rows), cursor["rows_fetched"],
        )
        return rows

    def close(self, cursor_name: str) -> bool:
        """Close a cursor - replaces P400-CLOSE-CURSOR.

        COBOL: EXEC SQL CLOSE cursor-name END-EXEC
        """
        cursor = self.cursors.get(cursor_name)
        if cursor is None:
            return False

        cursor["is_open"] = False
        logger.debug(
            "Cursor '%s' closed after %d rows",
            cursor_name, cursor["rows_fetched"],
        )
        return True

    def is_end_of_data(self, cursor_name: str) -> bool:
        """Check if cursor has reached end of data (SQLCODE = 100)."""
        cursor = self.cursors.get(cursor_name)
        if cursor is None:
            return True
        return cursor.get("end_of_data", True)

    def get_rows_fetched(self, cursor_name: str) -> int:
        """Get total rows fetched for a cursor."""
        cursor = self.cursors.get(cursor_name)
        if cursor is None:
            return 0
        return cursor.get("rows_fetched", 0)

    def iterate(
        self,
        cursor_name: str,
        fetch_size: int | None = None,
    ) -> Generator[list[Any], None, None]:
        """Iterate through cursor in batches (convenience method).

        Combines open/fetch/close into a single generator.
        """
        self.open(cursor_name)
        try:
            while not self.is_end_of_data(cursor_name):
                rows = self.fetch(cursor_name, fetch_size)
                if rows:
                    yield rows
                else:
                    break
        finally:
            self.close(cursor_name)

"""Cursor and pagination manager.

Replaces:
  - CURSMGR (src/programs/common/CURSMGR.cbl) — cursor management
  - HISTORY_CURSOR from INQHIST.cbl — cursor-based array fetching

Provides keyset and offset pagination via SQLAlchemy ORM queries.
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from typing import Generic, Sequence, TypeVar

from sqlalchemy import Select, func, select
from sqlalchemy.orm import Session

logger = logging.getLogger(__name__)

T = TypeVar("T")


@dataclass
class Page(Generic[T]):
    """Paginated result set.

    Replaces the COBOL cursor-based array fetch pattern where
    CURSMGR would return N records at a time with cursor position.
    """

    items: Sequence[T] = field(default_factory=list)
    total: int = 0
    page: int = 1
    page_size: int = 10
    has_next: bool = False
    has_prev: bool = False
    cursor: str | None = None

    @property
    def total_pages(self) -> int:
        """Calculate total number of pages."""
        if self.page_size <= 0:
            return 0
        return (self.total + self.page_size - 1) // self.page_size


class PaginationManager:
    """Manages paginated queries.

    Replaces CURSMGR (src/programs/common/CURSMGR.cbl).

    The original COBOL program managed DB2 cursors:
      - CMG-REQUEST-TYPE 'O' (Open cursor)   -> managed by SQLAlchemy
      - CMG-REQUEST-TYPE 'F' (Fetch records)  -> paginate()
      - CMG-REQUEST-TYPE 'C' (Close cursor)   -> managed by SQLAlchemy
      - CMG-REQUEST-TYPE 'R' (Reset cursor)   -> new query

    INQHIST.cbl fetched 10 records per screen via HISTORY_CURSOR
    with up to 3000 bytes of data. This is replicated via
    page_size=10 default.
    """

    DEFAULT_PAGE_SIZE = 10  # matches INQHIST 10 records per screen
    MAX_PAGE_SIZE = 100

    def __init__(self, session: Session):
        self._session = session

    def paginate(
        self,
        query: Select[tuple[T]],
        page: int = 1,
        page_size: int = DEFAULT_PAGE_SIZE,
    ) -> Page[T]:
        """Execute a paginated query using offset pagination.

        Replaces CURSMGR FETCH processing with LIMIT/OFFSET.

        Args:
            query: SQLAlchemy select statement.
            page: Page number (1-based).
            page_size: Number of records per page.

        Returns:
            Page object with results and pagination metadata.
        """
        page = max(1, page)
        page_size = min(max(1, page_size), self.MAX_PAGE_SIZE)
        offset = (page - 1) * page_size

        # Get total count
        count_query = select(func.count()).select_from(query.subquery())
        total = self._session.execute(count_query).scalar() or 0

        # Get page of results
        paginated_query = query.offset(offset).limit(page_size)
        results = self._session.execute(paginated_query).scalars().all()

        has_next = (offset + page_size) < total
        has_prev = page > 1

        logger.debug(
            "Paginate: page=%d size=%d total=%d has_next=%s",
            page,
            page_size,
            total,
            has_next,
        )

        return Page(
            items=results,
            total=total,
            page=page,
            page_size=page_size,
            has_next=has_next,
            has_prev=has_prev,
        )

    def keyset_paginate(
        self,
        query: Select[tuple[T]],
        cursor_column: str,
        cursor_value: str | None = None,
        page_size: int = DEFAULT_PAGE_SIZE,
        ascending: bool = True,
    ) -> Page[T]:
        """Execute a keyset-paginated query.

        More efficient than offset pagination for large datasets.
        Replaces CURSMGR's cursor-position-based fetching
        (CK-LAST-KEY from CKPRST.cpy pattern).

        Args:
            query: SQLAlchemy select statement.
            cursor_column: Column name to use as cursor.
            cursor_value: Last seen cursor value (None for first page).
            page_size: Number of records per page.
            ascending: Sort direction.

        Returns:
            Page object with results and next cursor.
        """
        page_size = min(max(1, page_size), self.MAX_PAGE_SIZE)

        # Get total count
        count_query = select(func.count()).select_from(query.subquery())
        total = self._session.execute(count_query).scalar() or 0

        # Apply cursor filter if cursor_value provided
        # Note: cursor_column filtering is applied at the caller level
        # by modifying the query before passing it here
        paginated_query = query.limit(page_size + 1)  # fetch one extra to check has_next
        results = list(self._session.execute(paginated_query).scalars().all())

        has_next = len(results) > page_size
        if has_next:
            results = results[:page_size]

        # Get the cursor value from the last result
        next_cursor = None
        if results and has_next:
            last_item = results[-1]
            next_cursor = str(getattr(last_item, cursor_column, ""))

        return Page(
            items=results,
            total=total,
            page=1,  # keyset pagination doesn't use page numbers
            page_size=page_size,
            has_next=has_next,
            has_prev=cursor_value is not None,
            cursor=next_cursor,
        )

"""
VSAM-like Data Access Layer for PostgreSQL/SQLAlchemy.

Provides operations that mirror VSAM file access patterns:
  - read_by_key()   → VSAM READ (keyed random access)
  - read_next()     → VSAM READ NEXT (sequential forward access)
  - write()         → VSAM WRITE (insert new record)
  - rewrite()       → VSAM REWRITE (update existing record in place)
  - delete_by_key() → VSAM DELETE (remove record by key)

This layer abstracts SQLAlchemy session management and provides a
familiar interface for code migrated from COBOL/VSAM programs.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any, Generic, Iterator, Optional, Sequence, Type, TypeVar

from sqlalchemy import inspect, select, tuple_
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session

from .audit_history import AuditHistory
from .base import Base
from .position_history import PositionHistory
from .position_master import PortfolioMaster
from .transaction_file import TransactionHistory

T = TypeVar("T", bound=Base)


# ---------------------------------------------------------------------------
# VSAM Status Codes (mirrors COBOL FILE STATUS values)
# ---------------------------------------------------------------------------
class VSAMStatus:
    """VSAM file status codes, analogous to COBOL FILE STATUS values."""

    SUCCESS = "00"
    DUPLICATE_KEY = "22"
    RECORD_NOT_FOUND = "23"
    END_OF_FILE = "10"
    SEQUENCE_ERROR = "21"
    LOGIC_ERROR = "92"


class VSAMError(Exception):
    """Exception raised for VSAM-like operation failures."""

    def __init__(self, status: str, message: str) -> None:
        self.status = status
        self.message = message
        super().__init__(f"VSAM status {status}: {message}")


# ---------------------------------------------------------------------------
# Sequential Read Cursor
# ---------------------------------------------------------------------------
@dataclass
class VSAMCursor(Generic[T]):
    """
    Cursor for sequential (READ NEXT) access to VSAM-equivalent tables.

    Mirrors VSAM sequential browse operations:
      - START positions the cursor at a key
      - READ NEXT retrieves the next record in key sequence

    Attributes:
        model: The SQLAlchemy model class being browsed.
        start_key: Optional starting key for positioned browse.
        _results: Internal iterator over query results.
        _exhausted: Whether the cursor has reached end-of-file.
    """

    model: Type[T]
    start_key: Optional[dict[str, Any]] = None
    _results: Optional[Iterator[T]] = field(default=None, repr=False)
    _exhausted: bool = field(default=False, repr=False)


# ---------------------------------------------------------------------------
# VSAM Data Access Object
# ---------------------------------------------------------------------------
class VSAMDataAccess(Generic[T]):
    """
    Generic VSAM-like data access layer for a single file (table).

    Provides CRUD operations that mirror VSAM file access verbs.
    Each instance is bound to one model class (one VSAM file).

    Usage::

        from sqlalchemy import create_engine
        from sqlalchemy.orm import Session

        engine = create_engine("postgresql://...")
        dao = VSAMDataAccess(PortfolioMaster, engine)

        # Keyed read
        record = dao.read_by_key(
            portfolio_id="PORT0001", account_type="IN", branch_id="01"
        )

        # Sequential browse
        cursor = dao.open_cursor(portfolio_id="PORT0001")
        while True:
            record = dao.read_next(cursor)
            if record is None:
                break
    """

    def __init__(self, model: Type[T], engine: Engine) -> None:
        self.model = model
        self.engine = engine
        self._pk_columns = [
            col.name for col in inspect(model).mapper.primary_key
        ]

    # -------------------------------------------------------------------
    # READ BY KEY  (VSAM READ — keyed random access)
    # -------------------------------------------------------------------
    def read_by_key(self, **key_values: Any) -> T:
        """
        Read a single record by its full composite primary key.

        Mirrors VSAM READ with KEY IS EQUAL TO.

        Args:
            **key_values: Primary key column values as keyword arguments.

        Returns:
            The matching model instance.

        Raises:
            VSAMError: With status '23' if the record is not found,
                       or '92' if key columns are incomplete.
        """
        self._validate_key(key_values)
        with Session(self.engine) as session:
            stmt = select(self.model)
            for col_name, value in key_values.items():
                stmt = stmt.where(
                    getattr(self.model, col_name) == value
                )
            result = session.execute(stmt).scalar_one_or_none()
            if result is None:
                raise VSAMError(
                    VSAMStatus.RECORD_NOT_FOUND,
                    f"No record found for key {key_values}",
                )
            session.expunge(result)
            return result

    # -------------------------------------------------------------------
    # READ NEXT  (VSAM READ NEXT — sequential forward access)
    # -------------------------------------------------------------------
    def open_cursor(
        self, start_key: Optional[dict[str, Any]] = None
    ) -> VSAMCursor[T]:
        """
        Open a cursor for sequential browsing, optionally starting at a key.

        Mirrors VSAM START followed by READ NEXT.

        Args:
            start_key: Optional dict of key column values to position
                       the cursor. Partial keys are allowed for prefix
                       browsing (e.g., browse all records for a portfolio).

        Returns:
            A VSAMCursor instance for use with read_next().
        """
        return VSAMCursor(model=self.model, start_key=start_key)

    def read_next(self, cursor: VSAMCursor[T]) -> Optional[T]:
        """
        Read the next record in key sequence from the cursor.

        Mirrors VSAM READ NEXT.

        Args:
            cursor: An open VSAMCursor from open_cursor().

        Returns:
            The next model instance, or None if end-of-file is reached.

        Raises:
            VSAMError: With status '10' can also be checked via return None.
        """
        if cursor._exhausted:
            return None

        # Lazy-initialize the result iterator on first call
        if cursor._results is None:
            cursor._results = self._execute_browse(cursor)

        try:
            return next(cursor._results)
        except StopIteration:
            cursor._exhausted = True
            return None

    def read_all(
        self, start_key: Optional[dict[str, Any]] = None
    ) -> Sequence[T]:
        """
        Read all records matching an optional partial key prefix.

        Convenience method combining open_cursor + read_next loop.

        Args:
            start_key: Optional partial key for filtering.

        Returns:
            List of matching model instances.
        """
        cursor = self.open_cursor(start_key)
        records: list[T] = []
        while True:
            record = self.read_next(cursor)
            if record is None:
                break
            records.append(record)
        return records

    # -------------------------------------------------------------------
    # WRITE  (VSAM WRITE — insert new record)
    # -------------------------------------------------------------------
    def write(self, record: T) -> str:
        """
        Insert a new record into the VSAM-equivalent table.

        Mirrors VSAM WRITE. Raises an error if a record with the same
        key already exists (duplicate key).

        Args:
            record: A model instance to insert.

        Returns:
            VSAMStatus.SUCCESS on successful insert.

        Raises:
            VSAMError: With status '22' if a duplicate key exists.
        """
        key_values = self._extract_key(record)
        with Session(self.engine, expire_on_commit=False) as session:
            # Check for duplicate key (VSAM would reject duplicates)
            existing = self._find_by_key(session, key_values)
            if existing is not None:
                raise VSAMError(
                    VSAMStatus.DUPLICATE_KEY,
                    f"Record already exists for key {key_values}",
                )
            session.add(record)
            session.commit()
            session.expunge(record)
        return VSAMStatus.SUCCESS

    # -------------------------------------------------------------------
    # REWRITE  (VSAM REWRITE — update existing record in place)
    # -------------------------------------------------------------------
    def rewrite(self, record: T) -> str:
        """
        Update an existing record in the VSAM-equivalent table.

        Mirrors VSAM REWRITE. The record must already exist; the
        primary key fields cannot be changed.

        Args:
            record: A model instance with updated field values.
                    Primary key fields must match an existing record.

        Returns:
            VSAMStatus.SUCCESS on successful update.

        Raises:
            VSAMError: With status '23' if the record does not exist.
        """
        key_values = self._extract_key(record)
        with Session(self.engine, expire_on_commit=False) as session:
            existing = self._find_by_key(session, key_values)
            if existing is None:
                raise VSAMError(
                    VSAMStatus.RECORD_NOT_FOUND,
                    f"No record found for key {key_values}",
                )
            # Update non-key columns
            mapper = inspect(self.model)
            for col in mapper.columns:
                if col.name not in self._pk_columns:
                    setattr(existing, col.name, getattr(record, col.name))
            session.commit()
        return VSAMStatus.SUCCESS

    # -------------------------------------------------------------------
    # DELETE BY KEY  (VSAM DELETE — remove record by key)
    # -------------------------------------------------------------------
    def delete_by_key(self, **key_values: Any) -> str:
        """
        Delete a record by its full composite primary key.

        Mirrors VSAM DELETE.

        Args:
            **key_values: Primary key column values as keyword arguments.

        Returns:
            VSAMStatus.SUCCESS on successful deletion.

        Raises:
            VSAMError: With status '23' if the record does not exist,
                       or '92' if key columns are incomplete.
        """
        self._validate_key(key_values)
        with Session(self.engine) as session:
            existing = self._find_by_key(session, key_values)
            if existing is None:
                raise VSAMError(
                    VSAMStatus.RECORD_NOT_FOUND,
                    f"No record found for key {key_values}",
                )
            session.delete(existing)
            session.commit()
        return VSAMStatus.SUCCESS

    # -------------------------------------------------------------------
    # Internal helpers
    # -------------------------------------------------------------------
    def _validate_key(self, key_values: dict[str, Any]) -> None:
        """Validate that all primary key columns are provided."""
        missing = set(self._pk_columns) - set(key_values.keys())
        if missing:
            raise VSAMError(
                VSAMStatus.LOGIC_ERROR,
                f"Missing primary key columns: {missing}",
            )

    def _extract_key(self, record: T) -> dict[str, Any]:
        """Extract primary key values from a model instance."""
        return {col: getattr(record, col) for col in self._pk_columns}

    def _find_by_key(
        self, session: Session, key_values: dict[str, Any]
    ) -> Optional[T]:
        """Find a record by composite key within an existing session."""
        stmt = select(self.model)
        for col_name, value in key_values.items():
            stmt = stmt.where(getattr(self.model, col_name) == value)
        return session.execute(stmt).scalar_one_or_none()

    def _execute_browse(self, cursor: VSAMCursor[T]) -> Iterator[T]:
        """Execute a sequential browse query and yield results."""
        with Session(self.engine) as session:
            # Order by primary key columns for key-sequenced access
            stmt = select(self.model)

            # Apply partial key filter if start_key is provided
            if cursor.start_key:
                # Separate PK columns from non-PK columns
                pk_cols_in_key = [
                    col for col in self._pk_columns
                    if col in cursor.start_key
                ]
                non_pk_cols = [
                    col for col in cursor.start_key
                    if col not in self._pk_columns
                ]

                # Use composite tuple comparison for PK columns
                # to correctly implement VSAM START semantics
                if pk_cols_in_key:
                    lhs = tuple_(
                        *[getattr(self.model, c) for c in pk_cols_in_key]
                    )
                    rhs = tuple_(
                        *[cursor.start_key[c] for c in pk_cols_in_key]
                    )
                    stmt = stmt.where(lhs >= rhs)

                # Non-PK columns use exact match
                for col_name in non_pk_cols:
                    stmt = stmt.where(
                        getattr(self.model, col_name) == cursor.start_key[col_name]
                    )

            # Order by PK columns for KSDS key-sequenced access
            for col_name in self._pk_columns:
                stmt = stmt.order_by(getattr(self.model, col_name))

            results = session.execute(stmt).scalars().all()
            for record in results:
                session.expunge(record)
                yield record


# ---------------------------------------------------------------------------
# Convenience factory functions
# ---------------------------------------------------------------------------
def create_portfolio_master_dao(engine: Engine) -> VSAMDataAccess[PortfolioMaster]:
    """Create a data access object for the VSAM Portfolio Master file."""
    return VSAMDataAccess(PortfolioMaster, engine)


def create_transaction_history_dao(engine: Engine) -> VSAMDataAccess[TransactionHistory]:
    """Create a data access object for the VSAM Transaction History file."""
    return VSAMDataAccess(TransactionHistory, engine)


def create_position_history_dao(engine: Engine) -> VSAMDataAccess[PositionHistory]:
    """Create a data access object for the VSAM Position History file."""
    return VSAMDataAccess(PositionHistory, engine)


def create_audit_history_dao(engine: Engine) -> VSAMDataAccess[AuditHistory]:
    """Create a data access object for the VSAM Audit History file."""
    return VSAMDataAccess(AuditHistory, engine)

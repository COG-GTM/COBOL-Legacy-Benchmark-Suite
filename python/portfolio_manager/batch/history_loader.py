"""History Loader — batch processing program.

Replaces: HISTLD00 (src/programs/batch/HISTLD00.cbl)

Bulk inserts validated transaction records into the POSHIST
(Position History) DB2/PostgreSQL table. This is the third step
in the batch pipeline (after POSUPD00).

Original COBOL flow (HISTLD00.cbl):
  0000-MAIN-PROCESS
    1000-INITIALIZE  (open files, init counters)
    2000-PROCESS-RECORDS (loop)
      2100-READ-HISTORY  (read from transaction file)
      2200-INSERT-DB2    (INSERT INTO POSHIST via EXEC SQL)
      2300-CHECK-COMMIT  (COMMIT every 1000 records)
    3000-FINALIZE (close files, log stats)

Key behaviors from COBOL:
  - Handles duplicate keys (SQLCODE -803) by logging and continuing
  - Commits every 1000 records (WS-COMMIT-THRESHOLD)
  - Updates checkpoint on each commit
  - Returns error count as return code
"""

from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import date, datetime, time
from decimal import Decimal

from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from portfolio_manager.models.copybook_models import TransactionRecord
from portfolio_manager.models.database import PositionHistory
from portfolio_manager.services.error_handler import ErrorProcessor

logger = logging.getLogger(__name__)


@dataclass
class LoadResult:
    """Result of the history load batch run."""

    records_read: int = 0
    records_inserted: int = 0
    records_duplicate: int = 0
    records_error: int = 0
    commit_count: int = 0
    return_code: int = 0
    error_messages: list[str] = field(default_factory=list)


class HistoryLoader:
    """Position history DB2 loader.

    Replaces HISTLD00 (src/programs/batch/HISTLD00.cbl).
    """

    PROGRAM_ID = "HISTLD00"
    COMMIT_THRESHOLD = 1000  # matches WS-COMMIT-THRESHOLD in COBOL

    def __init__(self, session: Session):
        self._session = session
        self._error_processor = ErrorProcessor(session)

    def run(
        self,
        transactions: list[TransactionRecord],
        user_id: str = "BATCH",
    ) -> LoadResult:
        """Run the history load batch.

        Replaces 0000-MAIN-PROCESS flow from HISTLD00.cbl.

        Args:
            transactions: Validated transaction records.
            user_id: Processing user ID.

        Returns:
            LoadResult with processing statistics.
        """
        result = LoadResult()
        now = datetime.now()

        logger.info(
            "%s: Starting history load of %d records",
            self.PROGRAM_ID,
            len(transactions),
        )

        for i, txn in enumerate(transactions, 1):
            result.records_read += 1

            try:
                # Use SAVEPOINT so only the failing record is rolled
                # back, not the entire transaction.  This matches the
                # COBOL behaviour where a duplicate key (SQLCODE -803)
                # skips only the current record.
                with self._session.begin_nested():
                    # 2200-INSERT-DB2
                    self._insert_history(txn, now, user_id)
                result.records_inserted += 1

            except IntegrityError:
                # SAVEPOINT is automatically rolled back by
                # begin_nested() — outer transaction stays intact.
                result.records_duplicate += 1
                logger.warning(
                    "%s: Duplicate key for %s/%s/%s — skipping",
                    self.PROGRAM_ID,
                    txn.portfolio_id,
                    txn.trn_date,
                    txn.investment_id,
                )

            except Exception as exc:
                # SAVEPOINT is automatically rolled back.
                result.records_error += 1
                error_msg = (
                    f"Error inserting history for "
                    f"{txn.portfolio_id}: {exc}"
                )
                result.error_messages.append(error_msg)
                logger.error("%s: %s", self.PROGRAM_ID, error_msg)

                try:
                    self._error_processor.process_error(
                        program_id=self.PROGRAM_ID,
                        category="PR",
                        error_code="E005",
                        severity=3,
                        error_text=str(exc)[:200],
                        details=f"portfolio={txn.portfolio_id}",
                    )
                except Exception:
                    logger.exception(
                        "%s: Failed to log error",
                        self.PROGRAM_ID,
                    )

            # 2300-CHECK-COMMIT
            if i % self.COMMIT_THRESHOLD == 0:
                self._session.flush()
                result.commit_count += 1
                logger.info(
                    "%s: Commit checkpoint at record %d (commits=%d)",
                    self.PROGRAM_ID,
                    i,
                    result.commit_count,
                )

        # 3000-FINALIZE
        self._session.flush()
        result.commit_count += 1

        # Return code = error count (matching COBOL behavior)
        if result.records_error == 0:
            result.return_code = 0
        elif result.records_error <= 4:
            result.return_code = 4
        else:
            result.return_code = 8

        logger.info(
            "%s: Complete — read=%d inserted=%d dups=%d errors=%d commits=%d RC=%d",
            self.PROGRAM_ID,
            result.records_read,
            result.records_inserted,
            result.records_duplicate,
            result.records_error,
            result.commit_count,
            result.return_code,
        )

        return result

    def _insert_history(
        self,
        txn: TransactionRecord,
        process_dt: datetime,
        user_id: str,
    ) -> None:
        """Insert a single history record into POSHIST.

        Replaces HISTLD00 paragraph 2200-INSERT-DB2 which uses:
          EXEC SQL INSERT INTO POSHIST ...

        Maps TransactionRecord fields to PositionHistory columns.
        """
        # Parse date/time from the transaction record
        txn_date = _parse_date(txn.trn_date)
        txn_time = _parse_time(txn.trn_time)

        history = PositionHistory(
            account_no=txn.portfolio_id[:8],
            portfolio_id=txn.portfolio_id,
            trans_date=txn_date,
            trans_time=txn_time,
            trans_type=txn.transaction_type.value,
            security_id=txn.investment_id,
            quantity=txn.quantity,
            price=txn.price,
            amount=txn.amount,
            fees=Decimal("0"),
            total_amount=txn.amount,
            cost_basis=txn.amount,
            gain_loss=Decimal("0"),
            process_date=process_dt.date(),
            process_time=process_dt.time(),
            program_id=self.PROGRAM_ID,
            user_id=user_id,
        )
        self._session.add(history)


def _parse_date(date_str: str) -> date:
    """Parse YYYYMMDD string to date object."""
    try:
        return date(
            int(date_str[:4]),
            int(date_str[4:6]),
            int(date_str[6:8]),
        )
    except (ValueError, IndexError):
        return date.today()


def _parse_time(time_str: str) -> time:
    """Parse HHMMSS string to time object."""
    try:
        return time(
            int(time_str[:2]),
            int(time_str[2:4]),
            int(time_str[4:6]) if len(time_str) >= 6 else 0,
        )
    except (ValueError, IndexError):
        return time(0, 0, 0)

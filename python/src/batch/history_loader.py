"""
History loader translated from COBOL program HISTLD00.cbl.

Replaces:
  - HISTLD00.cbl 1000-INITIALIZE: Open files, initialize counters
  - HISTLD00.cbl 2000-PROCESS-RECORDS: Read VSAM, insert DB2
  - HISTLD00.cbl 2100-READ-VSAM-RECORD: Read next transaction
  - HISTLD00.cbl 2200-INSERT-DB2-RECORD: Insert into history table
  - HISTLD00.cbl 3000-TERMINATE: Close files, report totals

Commit threshold: 1000 records (from HISTLD00.cbl).
"""

import logging
from datetime import date

from sqlalchemy.orm import Session

from src.common.constants import COMMIT_THRESHOLD, ReturnCode, TransactionStatus
from src.db.repository import TransactionRepository

logger = logging.getLogger(__name__)


class HistoryLoader:
    """
    History loader - loads processed transactions into history tables.

    Translates HISTLD00.cbl paragraph structure.
    In the original COBOL, this moved data from VSAM to DB2.
    In Python, this archives completed transactions.
    """

    def __init__(self, session: Session) -> None:
        self._session = session
        self._transaction_repo = TransactionRepository(session)
        # Counters from HISTLD00.cbl WS-COUNTERS
        self.records_read = 0
        self.records_inserted = 0
        self.records_error = 0
        self.commit_count = 0

    def process(self, process_date: date) -> ReturnCode:
        """
        Load transaction history.

        Translates HISTLD00.cbl main flow:
          PERFORM 1000-INITIALIZE
          PERFORM 2000-PROCESS-RECORDS UNTIL END-OF-FILE
          PERFORM 3000-TERMINATE
        """
        logger.info("Starting history load for date %s", process_date)

        # 1000-INITIALIZE
        self._initialize()

        # 2000-PROCESS-RECORDS
        rc = self._process_records(process_date)

        # 3000-TERMINATE
        self._terminate()

        return rc

    def _initialize(self) -> None:
        """
        Initialize history load.

        Translates HISTLD00.cbl 1000-INITIALIZE.
        """
        self.records_read = 0
        self.records_inserted = 0
        self.records_error = 0
        self.commit_count = 0
        logger.info("History loader initialized")

    def _process_records(self, process_date: date) -> ReturnCode:
        """
        Process completed transaction records.

        Translates HISTLD00.cbl 2000-PROCESS-RECORDS.
        Reads completed transactions and marks them as archived.
        """
        # Filter for done transactions on or before process_date
        done_transactions = [
            t for t in self._get_all_done_transactions()
            if t.trn_date <= process_date
        ]

        logger.info("Found %d completed transactions to archive", len(done_transactions))

        for txn in done_transactions:
            self.records_read += 1
            # Use SAVEPOINT so a failure only rolls back this single
            # record, not all uncommitted successful work.
            nested = self._session.begin_nested()
            try:
                # 2200-INSERT-DB2-RECORD: In Python, the transaction is already
                # in the database. Mark as archived so it won't be reprocessed.
                txn.status = TransactionStatus.ARCHIVED.value  # 'X' = archived
                self._transaction_repo.update(txn)
                nested.commit()
                self.records_inserted += 1

                # Commit at threshold (from HISTLD00.cbl)
                if self.records_inserted % COMMIT_THRESHOLD == 0:
                    self._session.commit()
                    self.commit_count += 1
                    logger.info(
                        "Commit at record %d (commit #%d)",
                        self.records_inserted,
                        self.commit_count,
                    )

            except Exception as exc:
                self.records_error += 1
                # Rollback only this SAVEPOINT; prior successful work is preserved
                nested.rollback()
                logger.error(
                    "Error archiving transaction %s: %s",
                    txn.transaction_id,
                    exc,
                )

        # Final commit
        self._session.commit()
        self.commit_count += 1

        if self.records_error > 0:
            return ReturnCode.WARNING
        return ReturnCode.SUCCESS

    def _get_all_done_transactions(self) -> list[object]:
        """Get all completed transactions across all portfolios."""
        from sqlalchemy import select

        from src.db.tables import TransactionHistory

        stmt = (
            select(TransactionHistory)
            .where(TransactionHistory.status == TransactionStatus.DONE.value)
            .order_by(TransactionHistory.trn_date, TransactionHistory.trn_time)
        )
        return list(self._session.execute(stmt).scalars().all())

    def _terminate(self) -> None:
        """
        Finalize history load.

        Translates HISTLD00.cbl 3000-TERMINATE.
        """
        logger.info(
            "History load complete: read=%d, inserted=%d, errors=%d, commits=%d",
            self.records_read,
            self.records_inserted,
            self.records_error,
            self.commit_count,
        )

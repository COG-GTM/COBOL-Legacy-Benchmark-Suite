"""
History Load Batch Processor - migrated from HISTLD00.cbl.

Original COBOL Program: HISTLD00.cbl
Purpose: Loads transaction history from VSAM files to DB2 POSHIST table

Key Functions:
- P100-INIT: Initialize program, connect to DB2
- P200-PROCESS: Main processing loop
- P300-READ-HISTORY: Read history record from VSAM
- P400-INSERT-DB2: Insert record into DB2
- P500-HANDLE-DUPLICATE: Handle duplicate key
- P900-TERMINATE: Disconnect from DB2, write statistics
- 9000-ERROR-ROUTINE: Error handling
- 9100-CHECKPOINT: Checkpoint with commit

Commit Frequency: Every 1000 records (configurable)
"""

from datetime import date, datetime
from decimal import Decimal

import pandas as pd
from sqlalchemy import and_
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.batch.base import BatchProcessor
from app.models.database import PositionHistory, TransactionHistory
from app.utils.exceptions import DatabaseError


class HistoryLoader(BatchProcessor):
    """
    History load batch processor.
    Replaces HISTLD00.cbl functionality.

    Loads transaction history data into the POSHIST table for reporting.
    """

    def __init__(
        self,
        db: Session,
        input_data: pd.DataFrame | list[dict] | None = None,
        process_date: date | None = None,
        load_from_transactions: bool = True,
    ):
        super().__init__(
            db=db,
            job_name="HISTLD",
            program_name="HISTLD00",
            process_date=process_date,
        )

        if input_data is None:
            self.input_df = pd.DataFrame()
        elif isinstance(input_data, pd.DataFrame):
            self.input_df = input_data
        else:
            self.input_df = pd.DataFrame(input_data)

        self.load_from_transactions = load_from_transactions
        self.records_inserted = 0
        self.records_duplicate = 0
        self.records_updated = 0

    def initialize(self) -> None:
        """
        Initialize history load processing.
        Replaces P100-INIT in HISTLD00.cbl.

        Original COBOL:
        EXEC SQL CONNECT TO :WS-DB2-SUBSYS END-EXEC
        """
        self.logger.info(
            "Initializing history load processing",
            load_from_transactions=self.load_from_transactions,
            input_count=len(self.input_df) if not self.input_df.empty else "N/A",
        )

    def process(self) -> None:
        """
        Main processing loop.
        Replaces P200-PROCESS in HISTLD00.cbl.
        """
        if self.load_from_transactions:
            self._load_from_transaction_history()
        else:
            self._load_from_input_data()

    def _load_from_transaction_history(self) -> None:
        """
        Load history from TransactionHistory table.
        Processes completed transactions that haven't been loaded to history.
        """
        transactions = self.db.query(TransactionHistory).filter(
            TransactionHistory.status == "D",
            TransactionHistory.transaction_date <= self.process_date,
        ).order_by(
            TransactionHistory.transaction_date,
            TransactionHistory.transaction_time,
        ).all()

        self.logger.info(
            "Loading from transaction history",
            transaction_count=len(transactions),
        )

        for txn in transactions:
            self.records_read += 1

            if self.restart_key and txn.transaction_id <= self.restart_key:
                continue

            try:
                self._insert_history_record(txn)
                self.records_processed += 1

                if self.should_checkpoint():
                    self.checkpoint(txn.transaction_id)

            except DatabaseError as e:
                self.error_logger.log_db_error(
                    message=str(e),
                    sqlcode=e.sqlcode or -1,
                )
                if not self.increment_error_count():
                    raise

    def _load_from_input_data(self) -> None:
        """
        Load history from input DataFrame.
        Replaces P300-READ-HISTORY in HISTLD00.cbl.
        """
        for idx, row in self.input_df.iterrows():
            self.records_read += 1

            if self.restart_key and str(idx) <= self.restart_key:
                continue

            try:
                self._insert_history_from_dict(row.to_dict())
                self.records_processed += 1

                if self.should_checkpoint():
                    self.checkpoint(str(idx))

            except DatabaseError as e:
                self.error_logger.log_db_error(
                    message=str(e),
                    sqlcode=e.sqlcode or -1,
                )
                if not self.increment_error_count():
                    raise

    def _insert_history_record(self, txn: TransactionHistory) -> None:
        """
        Insert history record from transaction.
        Replaces P400-INSERT-DB2 in HISTLD00.cbl.

        Original COBOL:
        EXEC SQL
            INSERT INTO POSHIST
            (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME,
             TRANS_TYPE, SECURITY_ID, QUANTITY, PRICE, AMOUNT,
             FEES, TOTAL_AMOUNT, COST_BASIS, GAIN_LOSS,
             PROCESS_DATE, PROCESS_TIME, PROGRAM_ID, USER_ID)
            VALUES
            (:WS-ACCOUNT-NO, :WS-PORTFOLIO-ID, ...)
        END-EXEC
        """
        existing = self.db.query(PositionHistory).filter(
            and_(
                PositionHistory.account_no == txn.portfolio_id,
                PositionHistory.portfolio_id == txn.portfolio_id,
                PositionHistory.trans_date == txn.transaction_date,
                PositionHistory.trans_time == txn.transaction_time,
            )
        ).first()

        if existing:
            self._handle_duplicate(existing, txn)
            return

        history = PositionHistory(
            account_no=txn.portfolio_id,
            portfolio_id=txn.portfolio_id,
            trans_date=txn.transaction_date,
            trans_time=txn.transaction_time,
            trans_type=txn.transaction_type,
            security_id=txn.investment_id,
            quantity=txn.quantity,
            price=txn.price,
            amount=txn.amount,
            fees=txn.fees or Decimal("0"),
            total_amount=txn.total_amount,
            cost_basis=Decimal("0"),
            gain_loss=Decimal("0"),
            process_date=date.today(),
            process_time=datetime.utcnow().time(),
            program_id="HISTLD00",
            user_id="BATCH",
            audit_timestamp=datetime.utcnow(),
        )

        try:
            self.db.add(history)
            self.db.flush()
            self.records_inserted += 1
            self.records_written += 1
        except IntegrityError:
            self.db.rollback()
            self.records_duplicate += 1
            self.logger.warning(
                "Duplicate history record",
                portfolio_id=txn.portfolio_id,
                trans_date=str(txn.transaction_date),
            )

    def _insert_history_from_dict(self, record: dict) -> None:
        """
        Insert history record from dictionary.
        """
        account_no = str(record.get("account_no", record.get("portfolio_id", ""))).strip()
        portfolio_id = str(record.get("portfolio_id", "")).strip()
        trans_date = record.get("trans_date", date.today())
        trans_time = record.get("trans_time", datetime.utcnow().time())

        existing = self.db.query(PositionHistory).filter(
            and_(
                PositionHistory.account_no == account_no,
                PositionHistory.portfolio_id == portfolio_id,
                PositionHistory.trans_date == trans_date,
                PositionHistory.trans_time == trans_time,
            )
        ).first()

        if existing:
            self.records_duplicate += 1
            return

        history = PositionHistory(
            account_no=account_no,
            portfolio_id=portfolio_id,
            trans_date=trans_date,
            trans_time=trans_time,
            trans_type=str(record.get("trans_type", "")).strip(),
            security_id=str(record.get("security_id", "")).strip(),
            quantity=Decimal(str(record.get("quantity", 0))),
            price=Decimal(str(record.get("price", 0))),
            amount=Decimal(str(record.get("amount", 0))),
            fees=Decimal(str(record.get("fees", 0))),
            total_amount=Decimal(str(record.get("total_amount", 0))),
            cost_basis=Decimal(str(record.get("cost_basis", 0))),
            gain_loss=Decimal(str(record.get("gain_loss", 0))),
            process_date=date.today(),
            process_time=datetime.utcnow().time(),
            program_id="HISTLD00",
            user_id="BATCH",
            audit_timestamp=datetime.utcnow(),
        )

        try:
            self.db.add(history)
            self.db.flush()
            self.records_inserted += 1
            self.records_written += 1
        except IntegrityError:
            self.db.rollback()
            self.records_duplicate += 1

    def _handle_duplicate(
        self,
        existing: PositionHistory,
        txn: TransactionHistory,
    ) -> None:
        """
        Handle duplicate key.
        Replaces P500-HANDLE-DUPLICATE in HISTLD00.cbl.

        Original COBOL:
        IF SQLCODE = -803
            ADD 1 TO WS-DUP-COUNT
            PERFORM P510-UPDATE-EXISTING
        END-IF
        """
        existing.quantity = txn.quantity
        existing.price = txn.price
        existing.amount = txn.amount
        existing.fees = txn.fees or Decimal("0")
        existing.total_amount = txn.total_amount
        existing.audit_timestamp = datetime.utcnow()

        self.records_updated += 1
        self.records_duplicate += 1

    def terminate(self) -> None:
        """
        Terminate history load processing.
        Replaces P900-TERMINATE in HISTLD00.cbl.

        Original COBOL:
        EXEC SQL COMMIT END-EXEC
        EXEC SQL DISCONNECT END-EXEC
        """
        self.db.commit()

        self.logger.info(
            "History load complete",
            records_read=self.records_read,
            records_inserted=self.records_inserted,
            records_updated=self.records_updated,
            records_duplicate=self.records_duplicate,
            records_error=self.records_error,
        )

        if self.records_duplicate > 0:
            self.return_code = max(self.return_code, 4)

    def get_results(self) -> dict:
        """Get processing results."""
        return {
            "records_read": self.records_read,
            "records_processed": self.records_processed,
            "records_inserted": self.records_inserted,
            "records_updated": self.records_updated,
            "records_duplicate": self.records_duplicate,
            "records_error": self.records_error,
            "return_code": self.return_code,
        }

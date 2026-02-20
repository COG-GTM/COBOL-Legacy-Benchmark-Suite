"""Position History DB2 Load Program - migrated from HISTLD00.cbl.

Reads transaction history from file and loads into database with
commit frequency control and error handling.
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Optional, Protocol

from portfolio_management.models.db2_tables import PosHistRecord
from portfolio_management.models.common import ReturnCode

logger = logging.getLogger(__name__)

PROGRAM_ID = "HISTLD00"
DEFAULT_COMMIT_FREQUENCY = 100


class DatabaseWriter(Protocol):
    def insert(self, table: str, record: dict) -> int: ...
    def commit(self) -> None: ...
    def rollback(self) -> None: ...


class HistoryLoader:
    def __init__(self):
        self._records_read = 0
        self._records_written = 0
        self._records_skipped = 0
        self._error_count = 0
        self._commit_frequency = DEFAULT_COMMIT_FREQUENCY

    def load(
        self,
        input_file_path: str,
        db_writer: Optional[DatabaseWriter] = None,
        commit_frequency: int = DEFAULT_COMMIT_FREQUENCY,
    ) -> int:
        self._commit_frequency = commit_frequency
        self._records_read = 0
        self._records_written = 0
        self._records_skipped = 0
        self._error_count = 0

        try:
            with open(input_file_path, "r") as f:
                for line in f:
                    line = line.strip()
                    if not line:
                        continue

                    self._records_read += 1
                    record = self._parse_record(line)

                    if record is None:
                        self._error_count += 1
                        continue

                    rc = self._load_to_db2(record, db_writer)
                    if rc != 0:
                        self._error_count += 1

                    if (
                        self._records_written > 0
                        and self._records_written % self._commit_frequency == 0
                    ):
                        if db_writer is not None:
                            db_writer.commit()
                        logger.info(
                            "Commit at %d records written", self._records_written
                        )

            if db_writer is not None:
                db_writer.commit()

        except FileNotFoundError:
            logger.error("Input file not found: %s", input_file_path)
            return ReturnCode.ERROR
        except Exception as e:
            logger.error("Error processing history file: %s", e)
            if db_writer is not None:
                db_writer.rollback()
            return ReturnCode.ERROR

        self._display_statistics()

        if self._error_count > 0:
            return ReturnCode.WARNING
        return ReturnCode.SUCCESS

    def _parse_record(self, line: str) -> Optional[PosHistRecord]:
        parts = line.split("|")
        if len(parts) < 13:
            logger.warning("Invalid record format: %s", line[:50])
            return None

        try:
            return PosHistRecord(
                account_no=parts[0].strip(),
                portfolio_id=parts[1].strip(),
                trans_date=parts[2].strip(),
                trans_time=parts[3].strip(),
                trans_type=parts[4].strip(),
                security_id=parts[5].strip(),
                quantity=Decimal(parts[6].strip()),
                price=Decimal(parts[7].strip()),
                amount=Decimal(parts[8].strip()),
                fees=Decimal(parts[9].strip()),
                total_amount=Decimal(parts[10].strip()),
                cost_basis=Decimal(parts[11].strip()),
                gain_loss=Decimal(parts[12].strip()),
                process_date=datetime.now().strftime("%Y-%m-%d"),
                process_time=datetime.now().strftime("%H:%M:%S"),
                program_id=PROGRAM_ID,
                user_id="BATCH",
                audit_timestamp=datetime.now().strftime("%Y-%m-%d-%H.%M.%S.%f"),
            )
        except (ValueError, IndexError) as e:
            logger.warning("Error parsing record: %s", e)
            return None

    def _load_to_db2(
        self, record: PosHistRecord, db_writer: Optional[DatabaseWriter]
    ) -> int:
        if db_writer is None:
            self._records_written += 1
            return 0

        rc = db_writer.insert(
            "POSHIST",
            {
                "ACCOUNT_NO": record.account_no,
                "PORTFOLIO_ID": record.portfolio_id,
                "TRANS_DATE": record.trans_date,
                "TRANS_TIME": record.trans_time,
                "TRANS_TYPE": record.trans_type,
                "SECURITY_ID": record.security_id,
                "QUANTITY": record.quantity,
                "PRICE": record.price,
                "AMOUNT": record.amount,
                "FEES": record.fees,
                "TOTAL_AMOUNT": record.total_amount,
                "COST_BASIS": record.cost_basis,
                "GAIN_LOSS": record.gain_loss,
                "PROCESS_DATE": record.process_date,
                "PROCESS_TIME": record.process_time,
                "PROGRAM_ID": record.program_id,
                "USER_ID": record.user_id,
                "AUDIT_TIMESTAMP": record.audit_timestamp,
            },
        )

        if rc == 0:
            self._records_written += 1
        elif rc == -803:
            self._records_skipped += 1
        else:
            return rc

        return 0

    def _display_statistics(self) -> None:
        logger.info(
            "History Load Statistics:\n"
            "  Records Read:    %d\n"
            "  Records Written: %d\n"
            "  Records Skipped: %d\n"
            "  Errors:          %d",
            self._records_read,
            self._records_written,
            self._records_skipped,
            self._error_count,
        )

    @property
    def records_read(self) -> int:
        return self._records_read

    @property
    def records_written(self) -> int:
        return self._records_written

    @property
    def error_count(self) -> int:
        return self._error_count

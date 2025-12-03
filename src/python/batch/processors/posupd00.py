"""
Position Updater (POSUPD00)

Python implementation of the COBOL POSUPD00 program.
Updates portfolio positions based on validated transactions.

Processing Flow:
1. Read validated transactions from TRNVAL00 output
2. For each transaction:
   - Read current position (or create new)
   - Apply transaction to position
   - Write updated position
   - Create history record
3. Commit changes at checkpoint intervals

Position Update Rules (from data-dictionary.md):
- Share balance must not go negative
- Cost basis must be updated for every BUY/SELL
- Average cost must be recalculated for buys
- Position status must be Active for transactions
"""

import logging
from datetime import datetime
from decimal import Decimal
from typing import Any, Dict, Iterator, List, Optional

from ..checkpoint.storage import CheckpointStorage
from ..database.connection import DatabaseConnection
from ..database.models import ErrorLog
from ..models.batch_control import ReturnCode
from ..models.history import HistoryActionCode, HistoryRecord
from ..models.position import PositionData, PositionKey, PositionRecord, PositionStatus
from ..models.transaction import TransactionRecord, TransactionType
from .base import BatchProcessor, ProcessingResult

logger = logging.getLogger(__name__)


class PositionUpdater(BatchProcessor):
    """
    Position Updater batch processor.
    
    Corresponds to COBOL program POSUPD00.
    Updates positions based on validated transactions.
    """
    
    PROGRAM_ID = "POSUPD00"
    COMMIT_FREQ = 500
    MAX_ERRORS = 100
    MAX_RESTARTS = 3
    
    def __init__(
        self,
        input_transactions: Optional[List[TransactionRecord]] = None,
        position_master: Optional[Dict[str, PositionRecord]] = None,
        db_connection: Optional[DatabaseConnection] = None,
        checkpoint_storage: Optional[CheckpointStorage] = None,
        restart: bool = False,
    ):
        """
        Initialize position updater.
        
        Args:
            input_transactions: List of validated transactions
            position_master: Dictionary of positions keyed by position key string
            db_connection: Database connection for error logging
            checkpoint_storage: Storage backend for checkpoints
            restart: If True, attempt to restart from last checkpoint
        """
        super().__init__(checkpoint_storage=checkpoint_storage, restart=restart)
        self.input_transactions = input_transactions or []
        self.position_master = position_master or {}
        self.db_connection = db_connection
        self.history_records: List[HistoryRecord] = []
        self.updated_positions: Dict[str, PositionRecord] = {}
        self._transaction_iter: Optional[Iterator[TransactionRecord]] = None
        self._current_index = 0
        self._skip_to_index = 0
    
    def initialize(self) -> None:
        """
        Initialize position update processing.
        
        Corresponds to 1000-INITIALIZE in COBOL:
        - Open position master file
        - Open transaction history file
        - Initialize counters
        """
        logger.info("Opening position master file")
        self._transaction_iter = iter(self.input_transactions)
        self._current_index = 0
        self.history_records = []
        self.updated_positions = {}
        
        if self.db_connection:
            logger.info("Database connection established")
    
    def read_next_record(self) -> Optional[TransactionRecord]:
        """
        Read the next transaction record.
        
        Corresponds to 2100-READ-TRANSACTION in COBOL.
        
        Returns:
            Next transaction or None if end of input
        """
        if self._transaction_iter is None:
            return None
        
        try:
            record = next(self._transaction_iter)
            self._current_index += 1
            
            if self._current_index <= self._skip_to_index:
                return self.read_next_record()
            
            return record
        except StopIteration:
            return None
    
    def process_record(self, record: TransactionRecord) -> bool:
        """
        Process a single transaction and update position.
        
        Corresponds to 2200-UPDATE-POSITION in COBOL.
        
        Args:
            record: Transaction to process
            
        Returns:
            True if position was updated successfully
        """
        try:
            position = self._get_or_create_position(record)
            before_image = position.to_dict()
            
            self._apply_transaction(position, record)
            
            after_image = position.to_dict()
            self._create_history_record(record, before_image, after_image)
            
            position_key = position.key.to_string()
            self.updated_positions[position_key] = position
            self.position_master[position_key] = position
            
            self.result.records_written += 1
            logger.debug(f"Updated position: {position_key}")
            return True
            
        except Exception as e:
            logger.warning(f"Error updating position: {e}")
            self._log_error(record, str(e))
            return False
    
    def _get_or_create_position(self, record: TransactionRecord) -> PositionRecord:
        """
        Get existing position or create new one.
        
        Corresponds to 2210-READ-POSITION in COBOL.
        """
        position_key = PositionKey(
            portfolio_id=record.portfolio_id,
            date=record.date,
            investment_id=record.investment_id,
        )
        key_string = position_key.to_string()
        
        if key_string in self.position_master:
            return self.position_master[key_string]
        
        base_key = f"{record.portfolio_id.strip()}{record.investment_id.strip()}"
        for existing_key, existing_pos in self.position_master.items():
            if existing_key.startswith(base_key[:18]):
                return PositionRecord(
                    key=position_key,
                    data=PositionData(
                        quantity=existing_pos.quantity,
                        cost_basis=existing_pos.cost_basis,
                        market_value=existing_pos.market_value,
                        currency=existing_pos.currency,
                        status=existing_pos.status,
                    ),
                )
        
        return PositionRecord(
            key=position_key,
            data=PositionData(
                quantity=Decimal("0"),
                cost_basis=Decimal("0"),
                market_value=Decimal("0"),
                currency=record.currency,
                status=PositionStatus.ACTIVE,
            ),
        )
    
    def _apply_transaction(
        self, position: PositionRecord, transaction: TransactionRecord
    ) -> None:
        """
        Apply transaction to position.
        
        Corresponds to 2220-APPLY-TRANSACTION in COBOL.
        """
        if not position.is_active():
            raise ValueError("Position is not active")
        
        if transaction.is_buy():
            self._apply_buy(position, transaction)
        elif transaction.is_sell():
            self._apply_sell(position, transaction)
        elif transaction.is_transfer():
            self._apply_transfer(position, transaction)
        elif transaction.is_fee():
            self._apply_fee(position, transaction)
        else:
            raise ValueError(f"Unknown transaction type: {transaction.transaction_type}")
    
    def _apply_buy(
        self, position: PositionRecord, transaction: TransactionRecord
    ) -> None:
        """
        Apply buy transaction to position.
        
        Updates:
        - Quantity: increased by transaction quantity
        - Cost basis: increased by transaction amount
        - Average cost: recalculated
        """
        position.update_for_buy(
            quantity=transaction.quantity,
            price=transaction.price,
            user=self.PROGRAM_ID,
        )
        logger.debug(
            f"BUY: qty={transaction.quantity}, price={transaction.price}, "
            f"new_qty={position.quantity}, new_cost={position.cost_basis}"
        )
    
    def _apply_sell(
        self, position: PositionRecord, transaction: TransactionRecord
    ) -> None:
        """
        Apply sell transaction to position.
        
        Updates:
        - Quantity: decreased by transaction quantity
        - Cost basis: decreased proportionally
        - Gain/loss: calculated and recorded
        """
        if transaction.quantity > position.quantity:
            raise ValueError(
                f"Insufficient position balance: "
                f"have {position.quantity}, need {transaction.quantity}"
            )
        
        gain_loss = position.update_for_sell(
            quantity=transaction.quantity,
            price=transaction.price,
            user=self.PROGRAM_ID,
        )
        logger.debug(
            f"SELL: qty={transaction.quantity}, price={transaction.price}, "
            f"gain_loss={gain_loss}, new_qty={position.quantity}"
        )
    
    def _apply_transfer(
        self, position: PositionRecord, transaction: TransactionRecord
    ) -> None:
        """
        Apply transfer transaction to position.
        
        Transfers adjust quantity without affecting cost basis per share.
        """
        if transaction.quantity < 0:
            if abs(transaction.quantity) > position.quantity:
                raise ValueError("Insufficient position balance for transfer out")
        
        position.quantity += transaction.quantity
        
        if transaction.quantity > 0:
            position.cost_basis += transaction.amount
        else:
            ratio = abs(transaction.quantity) / (position.quantity + abs(transaction.quantity))
            position.cost_basis -= position.cost_basis * Decimal(str(ratio))
        
        position.audit.last_maint_date = datetime.now().isoformat()
        position.audit.last_maint_user = self.PROGRAM_ID
        
        logger.debug(f"TRANSFER: qty={transaction.quantity}, new_qty={position.quantity}")
    
    def _apply_fee(
        self, position: PositionRecord, transaction: TransactionRecord
    ) -> None:
        """
        Apply fee transaction to position.
        
        Fees increase the cost basis without changing quantity.
        """
        position.cost_basis += abs(transaction.amount)
        position.audit.last_maint_date = datetime.now().isoformat()
        position.audit.last_maint_user = self.PROGRAM_ID
        
        logger.debug(f"FEE: amount={transaction.amount}, new_cost={position.cost_basis}")
    
    def _create_history_record(
        self,
        transaction: TransactionRecord,
        before_image: dict,
        after_image: dict,
    ) -> None:
        """
        Create history record for audit trail.
        
        Corresponds to 2230-WRITE-HISTORY in COBOL.
        """
        import json
        
        action = HistoryActionCode.CHANGE
        if before_image["data"]["quantity"] == "0":
            action = HistoryActionCode.ADD
        
        history = HistoryRecord.create_for_position(
            portfolio_id=transaction.portfolio_id,
            action=action,
            before_image=json.dumps(before_image)[:400],
            after_image=json.dumps(after_image)[:400],
            reason_code=transaction.transaction_type.value,
            user=self.PROGRAM_ID,
        )
        self.history_records.append(history)
    
    def _log_error(self, record: TransactionRecord, error_message: str) -> None:
        """Log error to database."""
        if self.db_connection:
            try:
                with self.db_connection.transaction() as session:
                    error_log = ErrorLog.create_error(
                        program_id=self.PROGRAM_ID,
                        error_type="A",
                        error_severity=3,
                        error_code="E100",
                        error_message=error_message[:200],
                        user_id="SYSTEM",
                        additional_info=f"Transaction: {record.key.to_string()}",
                    )
                    session.add(error_log)
            except Exception as e:
                logger.error(f"Failed to log error to database: {e}")
    
    def get_record_key(self, record: TransactionRecord) -> str:
        """Get transaction key for checkpoint tracking."""
        return record.key.to_string()
    
    def skip_to_key(self, key: str) -> None:
        """Skip to a specific transaction for restart."""
        for i, trans in enumerate(self.input_transactions):
            if trans.key.to_string() == key:
                self._skip_to_index = i + 1
                logger.info(f"Restart: skipping to index {self._skip_to_index}")
                return
        logger.warning(f"Restart key not found: {key}")
    
    def terminate(self) -> None:
        """
        Terminate position update processing.
        
        Corresponds to 3000-TERMINATE in COBOL:
        - Close files
        - Final commit
        - Display statistics
        """
        logger.info("Closing position files")
        
        if self.result.records_error > 0:
            self.result.return_code = ReturnCode.WARNING.value
    
    def get_updated_positions(self) -> Dict[str, PositionRecord]:
        """Get dictionary of updated positions."""
        return self.updated_positions
    
    def get_history_records(self) -> List[HistoryRecord]:
        """Get list of history records created during processing."""
        return self.history_records


def update_positions(
    transactions: List[TransactionRecord],
    position_master: Optional[Dict[str, PositionRecord]] = None,
    db_connection: Optional[DatabaseConnection] = None,
    checkpoint_storage: Optional[CheckpointStorage] = None,
    restart: bool = False,
) -> tuple[ProcessingResult, Dict[str, PositionRecord], List[HistoryRecord]]:
    """
    Convenience function to update positions.
    
    Args:
        transactions: List of validated transactions
        position_master: Existing positions
        db_connection: Optional database connection
        checkpoint_storage: Optional checkpoint storage
        restart: If True, attempt restart from last checkpoint
        
    Returns:
        Tuple of (ProcessingResult, updated positions dict, history records)
    """
    updater = PositionUpdater(
        input_transactions=transactions,
        position_master=position_master,
        db_connection=db_connection,
        checkpoint_storage=checkpoint_storage,
        restart=restart,
    )
    result = updater.run()
    return result, updater.get_updated_positions(), updater.get_history_records()

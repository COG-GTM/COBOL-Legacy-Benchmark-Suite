"""
Position Manager - Migrated from COBOL POSUPD00 program.

This module implements the position update logic from the original
COBOL program, including portfolio position calculations and updates.

Original COBOL Program: src/programs/batch/POSUPD00.cbl (referenced in architecture)
"""

import logging
from dataclasses import dataclass, field
from datetime import datetime
from decimal import Decimal, ROUND_HALF_UP
from typing import List, Dict, Optional, Tuple
from enum import Enum

from sqlalchemy.orm import Session

from ..models.position import PositionRecord, Position, PositionStatus
from ..models.transaction import TransactionRecord, TransactionType, TransactionStatus
from ..models.history import HistoryRecord, HistoryRecordType, HistoryActionCode
from ..database.connection import session_scope, get_session

logger = logging.getLogger(__name__)


class UpdateAction(str, Enum):
    """Position update action types"""
    ADD = 'ADD'
    UPDATE = 'UPDATE'
    DELETE = 'DELETE'


@dataclass
class PositionUpdate:
    """Represents a position update operation"""
    action: UpdateAction
    position: PositionRecord
    transaction: TransactionRecord
    before_position: Optional[PositionRecord] = None
    success: bool = False
    error_message: str = ""


@dataclass
class BatchUpdateResult:
    """Result of batch position update"""
    total_transactions: int = 0
    positions_added: int = 0
    positions_updated: int = 0
    positions_deleted: int = 0
    errors: int = 0
    updates: List[PositionUpdate] = field(default_factory=list)
    return_code: int = 0  # 0=success, 4=warnings, 8=errors, 12=critical

    def add_update(self, update: PositionUpdate):
        """Add an update result"""
        self.total_transactions += 1
        self.updates.append(update)
        
        if update.success:
            if update.action == UpdateAction.ADD:
                self.positions_added += 1
            elif update.action == UpdateAction.UPDATE:
                self.positions_updated += 1
            elif update.action == UpdateAction.DELETE:
                self.positions_deleted += 1
        else:
            self.errors += 1
            self.return_code = max(self.return_code, 8)


class PositionManager:
    """
    Position Manager - Migrated from COBOL POSUPD00.
    
    Manages portfolio positions by processing validated transactions:
    - BUY: Increase position quantity, update cost basis
    - SELL: Decrease position quantity, calculate realized gain/loss
    - TRANSFER: Move positions between portfolios
    - FEE: Apply fees to positions
    
    Original COBOL program flow:
    1. 0000-MAIN: Main control
    2. 1000-INITIALIZE: Open files, initialize working storage
    3. 2000-PROCESS: Process transactions and update positions
    4. 3000-TERMINATE: Close files, display statistics
    """
    
    def __init__(self, session: Session = None):
        """
        Initialize the position manager.
        
        Args:
            session: SQLAlchemy session (optional, will create if not provided)
        """
        self._session = session
        self._owns_session = session is None
        
        # Position cache (maps to COBOL POSITION-TABLE)
        self.position_cache: Dict[str, PositionRecord] = {}
        
        # Statistics (maps to COBOL WS-COUNTERS)
        self.records_read = 0
        self.records_processed = 0
        self.records_error = 0
        
        logger.info("PositionManager initialized")
    
    @property
    def session(self) -> Session:
        """Get or create database session"""
        if self._session is None:
            self._session = get_session()
        return self._session
    
    def process_transactions(self, transactions: List[TransactionRecord]) -> BatchUpdateResult:
        """
        Process a list of validated transactions and update positions.
        Implements COBOL 2000-PROCESS paragraph logic.
        
        Args:
            transactions: List of validated TransactionRecord objects
            
        Returns:
            BatchUpdateResult containing update statistics
        """
        logger.info(f"Starting position update for {len(transactions)} transactions")
        
        result = BatchUpdateResult()
        
        for transaction in transactions:
            self.records_read += 1
            
            try:
                update = self._process_single_transaction(transaction)
                result.add_update(update)
                
                if update.success:
                    self.records_processed += 1
                else:
                    self.records_error += 1
                    
            except Exception as e:
                logger.error(f"Error processing transaction: {e}")
                self.records_error += 1
                result.errors += 1
                result.return_code = max(result.return_code, 8)
        
        # Commit all changes
        if self._owns_session:
            try:
                self.session.commit()
            except Exception as e:
                logger.error(f"Error committing changes: {e}")
                self.session.rollback()
                result.return_code = 12
        
        self._display_statistics(result)
        return result
    
    def _process_single_transaction(self, transaction: TransactionRecord) -> PositionUpdate:
        """
        Process a single transaction and update position.
        
        Args:
            transaction: TransactionRecord to process
            
        Returns:
            PositionUpdate with result
        """
        # Get position key
        position_key = self._get_position_key(
            transaction.portfolio_id,
            transaction.investment_id,
            transaction.date
        )
        
        # Get or create position
        position, is_new = self._get_or_create_position(
            transaction.portfolio_id,
            transaction.investment_id,
            transaction.date
        )
        
        # Store before image for history
        before_position = None if is_new else PositionRecord(
            portfolio_id=position.portfolio_id,
            date=position.date,
            investment_id=position.investment_id,
            quantity=position.quantity,
            cost_basis=position.cost_basis,
            market_value=position.market_value,
            currency=position.currency,
            status=position.status
        )
        
        # Determine action
        action = UpdateAction.ADD if is_new else UpdateAction.UPDATE
        
        # Process based on transaction type
        try:
            if transaction.transaction_type == TransactionType.BUY:
                self._process_buy(position, transaction)
            elif transaction.transaction_type == TransactionType.SELL:
                self._process_sell(position, transaction)
            elif transaction.transaction_type == TransactionType.TRANSFER:
                self._process_transfer(position, transaction)
            elif transaction.transaction_type == TransactionType.FEE:
                self._process_fee(position, transaction)
            
            # Update maintenance info
            position.last_maint_date = datetime.now()
            position.last_maint_user = transaction.process_user or 'SYSTEM'
            
            # Save position
            self._save_position(position)
            
            # Update transaction status
            transaction.status = TransactionStatus.DONE
            transaction.process_date = datetime.now()
            
            return PositionUpdate(
                action=action,
                position=position,
                transaction=transaction,
                before_position=before_position,
                success=True
            )
            
        except Exception as e:
            logger.error(f"Error processing transaction {transaction.transaction_id}: {e}")
            transaction.status = TransactionStatus.FAILED
            
            return PositionUpdate(
                action=action,
                position=position,
                transaction=transaction,
                before_position=before_position,
                success=False,
                error_message=str(e)
            )
    
    def _process_buy(self, position: PositionRecord, transaction: TransactionRecord):
        """
        Process BUY transaction - Increase position.
        Implements COBOL 2100-PROCESS-BUY paragraph.
        
        Cost basis calculation:
        new_cost_basis = old_cost_basis + (quantity * price)
        """
        logger.debug(f"Processing BUY: {transaction.quantity} @ {transaction.price}")
        
        # Calculate transaction amount
        transaction_amount = transaction.quantity * transaction.price
        
        # Update position
        position.quantity += transaction.quantity
        position.cost_basis += transaction_amount
        
        # Update market value (using transaction price as current price)
        position.market_value = position.quantity * transaction.price
    
    def _process_sell(self, position: PositionRecord, transaction: TransactionRecord):
        """
        Process SELL transaction - Decrease position.
        Implements COBOL 2200-PROCESS-SELL paragraph.
        
        Cost basis calculation (FIFO):
        cost_per_share = old_cost_basis / old_quantity
        new_cost_basis = old_cost_basis - (quantity * cost_per_share)
        """
        logger.debug(f"Processing SELL: {transaction.quantity} @ {transaction.price}")
        
        # Validate sufficient quantity
        if position.quantity < transaction.quantity:
            raise ValueError(
                f"Insufficient position: have {position.quantity}, need {transaction.quantity}"
            )
        
        # Calculate cost per share (average cost method)
        if position.quantity > 0:
            cost_per_share = position.cost_basis / position.quantity
        else:
            cost_per_share = Decimal('0')
        
        # Calculate cost basis reduction
        cost_reduction = transaction.quantity * cost_per_share
        
        # Update position
        position.quantity -= transaction.quantity
        position.cost_basis -= cost_reduction
        
        # Round cost basis to 2 decimal places
        position.cost_basis = position.cost_basis.quantize(
            Decimal('0.01'), rounding=ROUND_HALF_UP
        )
        
        # Update market value
        position.market_value = position.quantity * transaction.price
        
        # Check if position is now zero
        if position.quantity == 0:
            position.status = PositionStatus.CLOSED
    
    def _process_transfer(self, position: PositionRecord, transaction: TransactionRecord):
        """
        Process TRANSFER transaction.
        Implements COBOL 2300-PROCESS-TRANSFER paragraph.
        
        For transfers, we reduce the source position and create/update
        the destination position (handled separately).
        """
        logger.debug(f"Processing TRANSFER: {transaction.quantity}")
        
        # For the source position, treat like a sell without price impact
        if position.quantity < transaction.quantity:
            raise ValueError(
                f"Insufficient position for transfer: have {position.quantity}, need {transaction.quantity}"
            )
        
        # Calculate proportional cost basis
        if position.quantity > 0:
            cost_per_share = position.cost_basis / position.quantity
        else:
            cost_per_share = Decimal('0')
        
        cost_reduction = transaction.quantity * cost_per_share
        
        # Update position
        position.quantity -= transaction.quantity
        position.cost_basis -= cost_reduction
        
        # Round cost basis
        position.cost_basis = position.cost_basis.quantize(
            Decimal('0.01'), rounding=ROUND_HALF_UP
        )
        
        # Update market value proportionally
        if position.quantity > 0:
            position.market_value = (position.market_value * position.quantity / 
                                     (position.quantity + transaction.quantity))
        else:
            position.market_value = Decimal('0')
            position.status = PositionStatus.CLOSED
    
    def _process_fee(self, position: PositionRecord, transaction: TransactionRecord):
        """
        Process FEE transaction.
        Implements COBOL 2400-PROCESS-FEE paragraph.
        
        Fees reduce the cost basis (increase effective cost).
        """
        logger.debug(f"Processing FEE: {transaction.amount}")
        
        # Add fee to cost basis (fees increase cost)
        position.cost_basis += transaction.amount
    
    def _get_position_key(self, portfolio_id: str, investment_id: str, date: str) -> str:
        """Generate position cache key"""
        return f"{portfolio_id}|{investment_id}|{date}"
    
    def _get_or_create_position(
        self, 
        portfolio_id: str, 
        investment_id: str, 
        date: str
    ) -> Tuple[PositionRecord, bool]:
        """
        Get existing position or create new one.
        
        Returns:
            Tuple of (PositionRecord, is_new)
        """
        position_key = self._get_position_key(portfolio_id, investment_id, date)
        
        # Check cache first
        if position_key in self.position_cache:
            return self.position_cache[position_key], False
        
        # Query database
        db_position = self.session.query(Position).filter(
            Position.portfolio_id == portfolio_id,
            Position.investment_id == investment_id,
            Position.date == date
        ).first()
        
        if db_position:
            position = db_position.to_record()
            self.position_cache[position_key] = position
            return position, False
        
        # Create new position
        position = PositionRecord(
            portfolio_id=portfolio_id,
            date=date,
            investment_id=investment_id,
            quantity=Decimal('0'),
            cost_basis=Decimal('0'),
            market_value=Decimal('0'),
            currency='USD',
            status=PositionStatus.ACTIVE
        )
        self.position_cache[position_key] = position
        return position, True
    
    def _save_position(self, position: PositionRecord):
        """Save position to database"""
        position_key = self._get_position_key(
            position.portfolio_id, 
            position.investment_id, 
            position.date
        )
        
        # Check if exists in database
        db_position = self.session.query(Position).filter(
            Position.portfolio_id == position.portfolio_id,
            Position.investment_id == position.investment_id,
            Position.date == position.date
        ).first()
        
        if db_position:
            # Update existing
            db_position.quantity = position.quantity
            db_position.cost_basis = position.cost_basis
            db_position.market_value = position.market_value
            db_position.currency = position.currency
            db_position.status = position.status.value
            db_position.last_maint_date = position.last_maint_date
            db_position.last_maint_user = position.last_maint_user
        else:
            # Insert new
            db_position = Position.from_record(position)
            self.session.add(db_position)
        
        # Update cache
        self.position_cache[position_key] = position
    
    def get_position(
        self, 
        portfolio_id: str, 
        investment_id: str, 
        date: str = None
    ) -> Optional[PositionRecord]:
        """
        Get a position by portfolio, investment, and date.
        
        Args:
            portfolio_id: Portfolio identifier
            investment_id: Investment identifier
            date: Position date (defaults to today)
            
        Returns:
            PositionRecord if found, None otherwise
        """
        if date is None:
            date = datetime.now().strftime('%Y%m%d')
        
        position_key = self._get_position_key(portfolio_id, investment_id, date)
        
        if position_key in self.position_cache:
            return self.position_cache[position_key]
        
        db_position = self.session.query(Position).filter(
            Position.portfolio_id == portfolio_id,
            Position.investment_id == investment_id,
            Position.date == date
        ).first()
        
        if db_position:
            position = db_position.to_record()
            self.position_cache[position_key] = position
            return position
        
        return None
    
    def get_portfolio_positions(self, portfolio_id: str) -> List[PositionRecord]:
        """
        Get all positions for a portfolio.
        
        Args:
            portfolio_id: Portfolio identifier
            
        Returns:
            List of PositionRecord objects
        """
        db_positions = self.session.query(Position).filter(
            Position.portfolio_id == portfolio_id,
            Position.status == PositionStatus.ACTIVE.value
        ).all()
        
        return [pos.to_record() for pos in db_positions]
    
    def calculate_portfolio_value(self, portfolio_id: str) -> Dict[str, Decimal]:
        """
        Calculate total portfolio value.
        
        Args:
            portfolio_id: Portfolio identifier
            
        Returns:
            Dictionary with total_cost_basis, total_market_value, unrealized_gain_loss
        """
        positions = self.get_portfolio_positions(portfolio_id)
        
        total_cost_basis = sum(p.cost_basis for p in positions)
        total_market_value = sum(p.market_value for p in positions)
        unrealized_gain_loss = total_market_value - total_cost_basis
        
        return {
            'total_cost_basis': total_cost_basis,
            'total_market_value': total_market_value,
            'unrealized_gain_loss': unrealized_gain_loss,
            'position_count': len(positions)
        }
    
    def _display_statistics(self, result: BatchUpdateResult):
        """Display processing statistics - maps to COBOL 3400-DISPLAY-STATS"""
        logger.info("=" * 60)
        logger.info("POSUPD00 Processing Statistics:")
        logger.info(f"  Transactions Processed: {result.total_transactions}")
        logger.info(f"  Positions Added:        {result.positions_added}")
        logger.info(f"  Positions Updated:      {result.positions_updated}")
        logger.info(f"  Positions Deleted:      {result.positions_deleted}")
        logger.info(f"  Errors:                 {result.errors}")
        logger.info(f"  Return Code:            {result.return_code}")
        logger.info("=" * 60)
    
    def close(self):
        """Close session if owned"""
        if self._owns_session and self._session:
            self._session.close()
            self._session = None

"""
Workflow Tasks

Defines the individual tasks for the batch processing workflow.
These tasks wrap the batch processors for use with workflow orchestration tools.
"""

import logging
from datetime import datetime
from typing import Any, Dict, List, Optional

from ..checkpoint.storage import DatabaseCheckpointStorage, FileCheckpointStorage
from ..database.connection import DatabaseConnection, get_database_url
from ..models.history import HistoryRecord
from ..models.position import PositionRecord
from ..models.transaction import TransactionRecord
from ..processors.histld00 import HistoryLoader
from ..processors.posupd00 import PositionUpdater
from ..processors.trnval00 import TransactionValidator

logger = logging.getLogger(__name__)


def run_transaction_validation(
    transactions: List[TransactionRecord],
    use_database: bool = True,
    restart: bool = False,
    **kwargs,
) -> Dict[str, Any]:
    """
    Run transaction validation task.
    
    Corresponds to TRNVAL00 step in JCL batch job.
    
    Args:
        transactions: List of transactions to validate
        use_database: Whether to use database for error logging
        restart: Whether to attempt restart from checkpoint
        **kwargs: Additional arguments (for Airflow compatibility)
        
    Returns:
        Dictionary with results and valid transactions
    """
    logger.info(f"Starting transaction validation with {len(transactions)} transactions")
    
    db_connection = None
    checkpoint_storage = None
    
    if use_database:
        try:
            db_connection = DatabaseConnection()
            checkpoint_storage = DatabaseCheckpointStorage(db_connection.SessionLocal)
        except Exception as e:
            logger.warning(f"Database connection failed, using file storage: {e}")
            checkpoint_storage = FileCheckpointStorage()
    else:
        checkpoint_storage = FileCheckpointStorage()
    
    try:
        validator = TransactionValidator(
            input_transactions=transactions,
            db_connection=db_connection,
            checkpoint_storage=checkpoint_storage,
            restart=restart,
        )
        result = validator.run()
        
        return {
            "return_code": result.return_code,
            "records_read": result.records_read,
            "records_processed": result.records_processed,
            "records_written": result.records_written,
            "records_error": result.records_error,
            "valid_transactions": [t.to_dict() for t in validator.get_valid_transactions()],
            "error_messages": result.error_messages[:100],
        }
    finally:
        if db_connection:
            db_connection.close()


def run_position_update(
    transactions: List[TransactionRecord],
    position_master: Optional[Dict[str, PositionRecord]] = None,
    use_database: bool = True,
    restart: bool = False,
    **kwargs,
) -> Dict[str, Any]:
    """
    Run position update task.
    
    Corresponds to POSUPD00 step in JCL batch job.
    
    Args:
        transactions: List of validated transactions
        position_master: Existing positions (optional)
        use_database: Whether to use database for error logging
        restart: Whether to attempt restart from checkpoint
        **kwargs: Additional arguments (for Airflow compatibility)
        
    Returns:
        Dictionary with results, updated positions, and history records
    """
    logger.info(f"Starting position update with {len(transactions)} transactions")
    
    db_connection = None
    checkpoint_storage = None
    
    if use_database:
        try:
            db_connection = DatabaseConnection()
            checkpoint_storage = DatabaseCheckpointStorage(db_connection.SessionLocal)
        except Exception as e:
            logger.warning(f"Database connection failed, using file storage: {e}")
            checkpoint_storage = FileCheckpointStorage()
    else:
        checkpoint_storage = FileCheckpointStorage()
    
    try:
        updater = PositionUpdater(
            input_transactions=transactions,
            position_master=position_master or {},
            db_connection=db_connection,
            checkpoint_storage=checkpoint_storage,
            restart=restart,
        )
        result = updater.run()
        
        return {
            "return_code": result.return_code,
            "records_read": result.records_read,
            "records_processed": result.records_processed,
            "records_written": result.records_written,
            "records_error": result.records_error,
            "updated_positions": {
                k: v.to_dict() for k, v in updater.get_updated_positions().items()
            },
            "history_records": [h.to_dict() for h in updater.get_history_records()],
            "error_messages": result.error_messages[:100],
        }
    finally:
        if db_connection:
            db_connection.close()


def run_history_load(
    history_records: Optional[List[HistoryRecord]] = None,
    transactions: Optional[List[TransactionRecord]] = None,
    use_database: bool = True,
    restart: bool = False,
    **kwargs,
) -> Dict[str, Any]:
    """
    Run history load task.
    
    Corresponds to HISTLD00 step in JCL batch job.
    
    Args:
        history_records: List of history records to load
        transactions: List of transactions (alternative input)
        use_database: Whether to use database
        restart: Whether to attempt restart from checkpoint
        **kwargs: Additional arguments (for Airflow compatibility)
        
    Returns:
        Dictionary with results
    """
    record_count = len(history_records or []) + len(transactions or [])
    logger.info(f"Starting history load with {record_count} records")
    
    db_connection = None
    checkpoint_storage = None
    
    if use_database:
        try:
            db_connection = DatabaseConnection()
            checkpoint_storage = DatabaseCheckpointStorage(db_connection.SessionLocal)
        except Exception as e:
            logger.warning(f"Database connection failed, using file storage: {e}")
            checkpoint_storage = FileCheckpointStorage()
    else:
        checkpoint_storage = FileCheckpointStorage()
    
    try:
        loader = HistoryLoader(
            history_records=history_records,
            transactions=transactions,
            db_connection=db_connection,
            checkpoint_storage=checkpoint_storage,
            restart=restart,
        )
        result = loader.run()
        
        return {
            "return_code": result.return_code,
            "records_read": result.records_read,
            "records_processed": result.records_processed,
            "records_written": result.records_written,
            "records_error": result.records_error,
            "error_messages": result.error_messages[:100],
        }
    finally:
        if db_connection:
            db_connection.close()


def run_full_batch_pipeline(
    transactions: List[TransactionRecord],
    position_master: Optional[Dict[str, PositionRecord]] = None,
    use_database: bool = True,
) -> Dict[str, Any]:
    """
    Run the complete batch processing pipeline.
    
    Executes all three steps in sequence:
    1. TRNVAL00 - Transaction Validation
    2. POSUPD00 - Position Update
    3. HISTLD00 - History Load
    
    Args:
        transactions: List of transactions to process
        position_master: Existing positions (optional)
        use_database: Whether to use database
        
    Returns:
        Dictionary with results from all steps
    """
    logger.info("Starting full batch pipeline")
    start_time = datetime.now()
    
    results = {
        "pipeline_start": start_time.isoformat(),
        "steps": {},
        "overall_return_code": 0,
    }
    
    validation_result = run_transaction_validation(
        transactions=transactions,
        use_database=use_database,
    )
    results["steps"]["TRNVAL00"] = validation_result
    
    if validation_result["return_code"] > 4:
        logger.error("Transaction validation failed, aborting pipeline")
        results["overall_return_code"] = validation_result["return_code"]
        results["pipeline_end"] = datetime.now().isoformat()
        return results
    
    valid_transactions = [
        TransactionRecord.from_dict(t) for t in validation_result["valid_transactions"]
    ]
    
    position_result = run_position_update(
        transactions=valid_transactions,
        position_master=position_master,
        use_database=use_database,
    )
    results["steps"]["POSUPD00"] = position_result
    
    if position_result["return_code"] > 4:
        logger.error("Position update failed, aborting pipeline")
        results["overall_return_code"] = position_result["return_code"]
        results["pipeline_end"] = datetime.now().isoformat()
        return results
    
    history_result = run_history_load(
        transactions=valid_transactions,
        use_database=use_database,
    )
    results["steps"]["HISTLD00"] = history_result
    
    results["overall_return_code"] = max(
        validation_result["return_code"],
        position_result["return_code"],
        history_result["return_code"],
    )
    
    results["pipeline_end"] = datetime.now().isoformat()
    
    logger.info(f"Pipeline complete with return code {results['overall_return_code']}")
    return results

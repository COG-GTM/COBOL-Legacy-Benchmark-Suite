"""
Apache Airflow DAG Definition

Replaces JCL orchestration with modern Python-based workflow.
Defines the batch processing DAG for the Investment Portfolio Management System.

The DAG implements the same sequential processing as the JCL:
1. TRNVAL00 - Transaction Validation
2. POSUPD00 - Position Update  
3. HISTLD00 - History Load

Dependencies (from data-dictionary.md):
- TRNVAL00: No prerequisites
- POSUPD00: Requires TRNVAL00 with RC <= 4
- HISTLD00: Requires POSUPD00 with RC <= 4
"""

from datetime import datetime, timedelta
from typing import Any, Dict

try:
    from airflow import DAG
    from airflow.operators.python import PythonOperator
    from airflow.utils.dates import days_ago
    AIRFLOW_AVAILABLE = True
except ImportError:
    AIRFLOW_AVAILABLE = False
    DAG = None
    PythonOperator = None


DEFAULT_ARGS = {
    "owner": "batch_processing",
    "depends_on_past": False,
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 3,
    "retry_delay": timedelta(minutes=5),
    "execution_timeout": timedelta(hours=2),
}

DAG_CONFIG = {
    "dag_id": "portfolio_batch_processing",
    "description": "Investment Portfolio Management System - Daily Batch Processing",
    "schedule_interval": "0 18 * * 1-5",
    "start_date": datetime(2024, 1, 1),
    "catchup": False,
    "max_active_runs": 1,
    "tags": ["portfolio", "batch", "financial"],
}


def _validate_transactions(**context) -> Dict[str, Any]:
    """
    Airflow task for transaction validation.
    
    Corresponds to TRNVAL00 step in JCL.
    """
    from .tasks import run_transaction_validation
    
    ti = context["ti"]
    transactions = ti.xcom_pull(task_ids="load_transactions", key="transactions") or []
    
    result = run_transaction_validation(
        transactions=transactions,
        use_database=True,
        restart=False,
    )
    
    ti.xcom_push(key="validation_result", value=result)
    ti.xcom_push(key="valid_transactions", value=result["valid_transactions"])
    
    if result["return_code"] > 4:
        raise Exception(f"Transaction validation failed with RC={result['return_code']}")
    
    return result


def _update_positions(**context) -> Dict[str, Any]:
    """
    Airflow task for position updates.
    
    Corresponds to POSUPD00 step in JCL.
    """
    from ..models.transaction import TransactionRecord
    from .tasks import run_position_update
    
    ti = context["ti"]
    valid_transactions_data = ti.xcom_pull(
        task_ids="validate_transactions", key="valid_transactions"
    ) or []
    
    transactions = [TransactionRecord.from_dict(t) for t in valid_transactions_data]
    
    result = run_position_update(
        transactions=transactions,
        use_database=True,
        restart=False,
    )
    
    ti.xcom_push(key="position_result", value=result)
    ti.xcom_push(key="history_records", value=result["history_records"])
    
    if result["return_code"] > 4:
        raise Exception(f"Position update failed with RC={result['return_code']}")
    
    return result


def _load_history(**context) -> Dict[str, Any]:
    """
    Airflow task for history loading.
    
    Corresponds to HISTLD00 step in JCL.
    """
    from ..models.transaction import TransactionRecord
    from .tasks import run_history_load
    
    ti = context["ti"]
    valid_transactions_data = ti.xcom_pull(
        task_ids="validate_transactions", key="valid_transactions"
    ) or []
    
    transactions = [TransactionRecord.from_dict(t) for t in valid_transactions_data]
    
    result = run_history_load(
        transactions=transactions,
        use_database=True,
        restart=False,
    )
    
    ti.xcom_push(key="history_result", value=result)
    
    if result["return_code"] > 4:
        raise Exception(f"History load failed with RC={result['return_code']}")
    
    return result


def _load_transactions(**context) -> Dict[str, Any]:
    """
    Airflow task to load transactions from source.
    
    This is a placeholder that should be customized based on
    the actual transaction source (file, queue, API, etc.).
    """
    ti = context["ti"]
    
    transactions = []
    
    ti.xcom_push(key="transactions", value=transactions)
    
    return {"transaction_count": len(transactions)}


def _send_notifications(**context) -> None:
    """
    Airflow task to send completion notifications.
    
    Sends summary of batch processing results.
    """
    ti = context["ti"]
    
    validation_result = ti.xcom_pull(
        task_ids="validate_transactions", key="validation_result"
    ) or {}
    position_result = ti.xcom_pull(
        task_ids="update_positions", key="position_result"
    ) or {}
    history_result = ti.xcom_pull(
        task_ids="load_history", key="history_result"
    ) or {}
    
    summary = f"""
    Batch Processing Complete
    ========================
    
    Transaction Validation (TRNVAL00):
      - Records Read: {validation_result.get('records_read', 0)}
      - Records Processed: {validation_result.get('records_processed', 0)}
      - Records Error: {validation_result.get('records_error', 0)}
      - Return Code: {validation_result.get('return_code', 0)}
    
    Position Update (POSUPD00):
      - Records Read: {position_result.get('records_read', 0)}
      - Records Processed: {position_result.get('records_processed', 0)}
      - Records Error: {position_result.get('records_error', 0)}
      - Return Code: {position_result.get('return_code', 0)}
    
    History Load (HISTLD00):
      - Records Read: {history_result.get('records_read', 0)}
      - Records Written: {history_result.get('records_written', 0)}
      - Records Error: {history_result.get('records_error', 0)}
      - Return Code: {history_result.get('return_code', 0)}
    """
    
    print(summary)


def create_batch_processing_dag() -> "DAG":
    """
    Create the batch processing DAG.
    
    Returns:
        Configured Airflow DAG
        
    Raises:
        ImportError: If Airflow is not installed
    """
    if not AIRFLOW_AVAILABLE:
        raise ImportError(
            "Apache Airflow is not installed. "
            "Install with: pip install apache-airflow"
        )
    
    dag = DAG(
        dag_id=DAG_CONFIG["dag_id"],
        description=DAG_CONFIG["description"],
        schedule_interval=DAG_CONFIG["schedule_interval"],
        start_date=DAG_CONFIG["start_date"],
        catchup=DAG_CONFIG["catchup"],
        max_active_runs=DAG_CONFIG["max_active_runs"],
        tags=DAG_CONFIG["tags"],
        default_args=DEFAULT_ARGS,
    )
    
    with dag:
        load_transactions = PythonOperator(
            task_id="load_transactions",
            python_callable=_load_transactions,
            provide_context=True,
        )
        
        validate_transactions = PythonOperator(
            task_id="validate_transactions",
            python_callable=_validate_transactions,
            provide_context=True,
        )
        
        update_positions = PythonOperator(
            task_id="update_positions",
            python_callable=_update_positions,
            provide_context=True,
        )
        
        load_history = PythonOperator(
            task_id="load_history",
            python_callable=_load_history,
            provide_context=True,
        )
        
        send_notifications = PythonOperator(
            task_id="send_notifications",
            python_callable=_send_notifications,
            provide_context=True,
            trigger_rule="all_done",
        )
        
        load_transactions >> validate_transactions >> update_positions >> load_history >> send_notifications
    
    return dag


if AIRFLOW_AVAILABLE:
    batch_processing_dag = create_batch_processing_dag()

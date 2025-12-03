# Python Batch Processing Module

This module provides Python implementations of the COBOL batch processing programs from the Investment Portfolio Management System.

## Overview

The batch processing layer has been converted from COBOL to Python, maintaining the same sequential processing logic and reliability patterns:

1. **TRNVAL00** - Transaction Validation (entry point)
2. **POSUPD00** - Position Updates
3. **HISTLD00** - History Loading

## Architecture

### Components

- **models/** - Data models corresponding to COBOL copybooks
- **processors/** - Batch processing programs (TRNVAL00, POSUPD00, HISTLD00)
- **checkpoint/** - Checkpoint/restart framework
- **database/** - PostgreSQL database models and connection management
- **workflow/** - Apache Airflow DAG for orchestration
- **utils/** - Logging and configuration utilities

### Data Flow

```
Transactions → TRNVAL00 → Valid Transactions → POSUPD00 → Updated Positions → HISTLD00 → Database
                  ↓                                ↓                              ↓
              Error Log                      History Records                  POSHIST Table
```

## Installation

```bash
pip install -r requirements.txt
```

## Configuration

Configuration can be provided via environment variables or a configuration file:

### Environment Variables

```bash
# Database
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=portfolio
export DB_USER=postgres
export DB_PASSWORD=postgres

# Checkpoint
export CHECKPOINT_COMMIT_FREQ=1000
export CHECKPOINT_MAX_ERRORS=100
export CHECKPOINT_MAX_RESTARTS=3

# Processing
export PROCESS_DATE=20241201
export RESTART_MODE=false
export LOG_LEVEL=INFO
```

### Configuration File

```yaml
database:
  host: localhost
  port: 5432
  name: portfolio
  user: postgres
  password: postgres

checkpoint:
  commit_freq: 1000
  max_errors: 100
  max_restarts: 3

processing:
  process_date: "20241201"
  restart_mode: false
  log_level: INFO
```

## Usage

### Running Individual Programs

```python
from batch.processors import TransactionValidator, PositionUpdater, HistoryLoader
from batch.models import TransactionRecord

# Transaction Validation
validator = TransactionValidator(input_transactions=transactions)
result = validator.run()
valid_transactions = validator.get_valid_transactions()

# Position Update
updater = PositionUpdater(input_transactions=valid_transactions)
result = updater.run()
updated_positions = updater.get_updated_positions()

# History Load
loader = HistoryLoader(transactions=valid_transactions)
result = loader.run()
```

### Running the Full Pipeline

```python
from batch.workflow.tasks import run_full_batch_pipeline

results = run_full_batch_pipeline(
    transactions=transactions,
    use_database=True,
)
```

### Using Apache Airflow

The module includes an Airflow DAG for orchestration. Deploy the DAG to your Airflow instance:

```python
from batch.workflow.dag import batch_processing_dag
```

## Checkpoint/Restart

The checkpoint framework provides reliability similar to the COBOL implementation:

- Checkpoints are taken every N records (configurable)
- On failure, processing can restart from the last checkpoint
- Maximum restart attempts are configurable

```python
from batch.checkpoint import CheckpointManager

manager = CheckpointManager(
    program_id="TRNVAL00",
    commit_freq=1000,
    max_errors=100,
    max_restarts=3,
)

# Initialize (or restart from last checkpoint)
manager.initialize(restart=True)
```

## Database Schema

The module uses PostgreSQL with the following tables:

- **poshist** - Position history (replaces DB2 POSHIST)
- **errlog** - Error logging (replaces DB2 ERRLOG)
- **batch_control** - Batch job control (replaces VSAM BCHCTL)
- **checkpoint** - Checkpoint data (replaces VSAM checkpoint file)

## Mapping from COBOL

| COBOL Component | Python Component |
|-----------------|------------------|
| TRNVAL00.cbl | processors/trnval00.py |
| POSUPD00.cbl | processors/posupd00.py |
| HISTLD00.cbl | processors/histld00.py |
| CKPRST.cbl | checkpoint/manager.py |
| PRCSEQ00.cbl | workflow/dag.py |
| TRNREC.cpy | models/transaction.py |
| POSREC.cpy | models/position.py |
| HISTREC.cpy | models/history.py |
| BCHCTL.cpy | models/batch_control.py |
| CKPRST.cpy | models/checkpoint.py |
| DBTBLS.cpy | database/models.py |
| JCL scripts | workflow/dag.py |

## Return Codes

| Code | Description |
|------|-------------|
| 0 | Success |
| 4 | Warning (processing complete with warnings) |
| 8 | Error (processing complete with errors) |
| 12 | Severe error |
| 16 | Critical/environment error |

## License

See the main repository LICENSE file.

"""Python migration of HISTLD00 COBOL batch program (Position History DB2 Loader).

Mirrors the structure and behavior of ``src/programs/batch/HISTLD00.cbl`` and
its supporting copybooks, providing a modern Python-based ETL pipeline for
loading transaction history records into a ``POSHIST`` table with commit
checkpointing and error handling.
"""

__version__ = "1.0.0"

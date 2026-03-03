"""Support services for the Investment Portfolio Management System.

This package replaces COBOL support programs:
  - ERRPROC  (batch error processor)     -> error_handler.py
  - ERRHNDL  (online error handler)      -> error_handler.py
  - DB2ERR   (DB2 error handler)         -> db2_error_handler.py
  - SECMGR   (RACF/CICS security)       -> security.py
  - DB2ONLN  (DB2 connection pool)       -> database.py
  - CURSMGR  (cursor/pagination manager) -> pagination.py
"""

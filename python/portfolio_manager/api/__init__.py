"""FastAPI REST API layer.

Replaces the CICS online transaction processing layer:
  - INQONLN (main controller)  -> router
  - INQPORT (portfolio inquiry) -> portfolio endpoints
  - INQHIST (history inquiry)   -> history endpoints
  - BMS screen maps             -> JSON API responses
"""

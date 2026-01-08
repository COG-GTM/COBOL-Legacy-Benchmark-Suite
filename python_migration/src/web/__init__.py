"""
Web module - Flask application replacing CICS online transaction processing.
Migrated from COBOL CICS programs.
"""

from .app import create_app

__all__ = ['create_app']

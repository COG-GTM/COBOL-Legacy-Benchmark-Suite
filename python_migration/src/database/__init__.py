"""
Database module for the Investment Portfolio Management System.
Provides SQLAlchemy connection management and session handling.
"""

from .connection import DatabaseConnection, get_session, create_tables

__all__ = ['DatabaseConnection', 'get_session', 'create_tables']

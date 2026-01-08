"""
Route blueprints for the Flask application.
"""

from .portfolio import portfolio_bp
from .history import history_bp
from .reports import reports_bp
from .health import health_bp

__all__ = ['portfolio_bp', 'history_bp', 'reports_bp', 'health_bp']

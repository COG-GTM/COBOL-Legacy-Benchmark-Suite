"""
Flask Application - Replaces CICS online transaction processing.

This module creates the Flask application that replaces the CICS
online inquiry system (INQONLN, INQPORT, INQHIST).

Original CICS Programs:
- INQONLN: Main online controller
- INQPORT: Portfolio position inquiry
- INQHIST: Transaction history inquiry
"""

import os
import logging
from flask import Flask
from flask_cors import CORS

from .routes import portfolio_bp, history_bp, reports_bp, health_bp
from .security import security_bp, init_security
from ..database.connection import get_database

logger = logging.getLogger(__name__)


def create_app(config: dict = None) -> Flask:
    """
    Create and configure the Flask application.
    
    This replaces the CICS INQONLN main controller program.
    
    Args:
        config: Optional configuration dictionary
        
    Returns:
        Configured Flask application
    """
    app = Flask(__name__)
    
    # Load configuration
    app.config['SECRET_KEY'] = os.getenv('SECRET_KEY', 'dev-secret-key-change-in-production')
    app.config['DATABASE_URL'] = os.getenv('DATABASE_URL', 'sqlite:///portfolio.db')
    app.config['SESSION_TIMEOUT'] = int(os.getenv('SESSION_TIMEOUT', '1800'))  # 30 minutes
    app.config['MAX_LOGIN_ATTEMPTS'] = int(os.getenv('MAX_LOGIN_ATTEMPTS', '3'))
    
    if config:
        app.config.update(config)
    
    # Enable CORS
    CORS(app, resources={
        r"/api/*": {
            "origins": "*",
            "methods": ["GET", "POST", "PUT", "DELETE"],
            "allow_headers": ["Content-Type", "Authorization"]
        }
    })
    
    # Initialize database
    db = get_database(app.config['DATABASE_URL'])
    db.create_all_tables()
    
    # Initialize security
    init_security(app)
    
    # Register blueprints (replaces CICS program routing)
    app.register_blueprint(health_bp, url_prefix='/api')
    app.register_blueprint(portfolio_bp, url_prefix='/api/portfolio')
    app.register_blueprint(history_bp, url_prefix='/api/history')
    app.register_blueprint(reports_bp, url_prefix='/api/reports')
    app.register_blueprint(security_bp, url_prefix='/api/auth')
    
    # Error handlers
    @app.errorhandler(400)
    def bad_request(error):
        return {'error': 'Bad Request', 'message': str(error)}, 400
    
    @app.errorhandler(401)
    def unauthorized(error):
        return {'error': 'Unauthorized', 'message': 'Authentication required'}, 401
    
    @app.errorhandler(403)
    def forbidden(error):
        return {'error': 'Forbidden', 'message': 'Access denied'}, 403
    
    @app.errorhandler(404)
    def not_found(error):
        return {'error': 'Not Found', 'message': 'Resource not found'}, 404
    
    @app.errorhandler(500)
    def internal_error(error):
        logger.error(f"Internal server error: {error}")
        return {'error': 'Internal Server Error', 'message': 'An unexpected error occurred'}, 500
    
    logger.info("Flask application created successfully")
    return app


def run_app(host: str = '0.0.0.0', port: int = 5000, debug: bool = False):
    """
    Run the Flask application.
    
    Args:
        host: Host to bind to
        port: Port to listen on
        debug: Enable debug mode
    """
    app = create_app()
    app.run(host=host, port=port, debug=debug)


if __name__ == '__main__':
    logging.basicConfig(level=logging.INFO)
    run_app(debug=True)

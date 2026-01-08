"""
Authentication Routes and Decorators - Migrated from COBOL SECMGR.

This module provides Flask authentication endpoints and decorators
for securing API routes.

Original CICS Program: src/programs/online/SECMGR.cbl
"""

import logging
from functools import wraps
from flask import Blueprint, jsonify, request, g, current_app

from .user_manager import UserManager, User, Permission
from .audit_logger import get_audit_logger, AuditEventType

logger = logging.getLogger(__name__)

security_bp = Blueprint('security', __name__)

# Global user manager instance
_user_manager: UserManager = None


def init_security(app):
    """Initialize security module with Flask app"""
    global _user_manager
    _user_manager = UserManager(
        max_login_attempts=app.config.get('MAX_LOGIN_ATTEMPTS', 3),
        session_timeout=app.config.get('SESSION_TIMEOUT', 1800)
    )
    logger.info("Security module initialized")


def get_user_manager() -> UserManager:
    """Get user manager instance"""
    global _user_manager
    if _user_manager is None:
        _user_manager = UserManager()
    return _user_manager


def get_current_user() -> User:
    """Get current authenticated user from request context"""
    return getattr(g, 'current_user', None)


def require_auth(f):
    """
    Decorator to require authentication.
    Implements COBOL SECMGR authentication check.
    """
    @wraps(f)
    def decorated(*args, **kwargs):
        # Get session token from header
        auth_header = request.headers.get('Authorization', '')
        
        if not auth_header.startswith('Bearer '):
            return jsonify({
                'error': 'Unauthorized',
                'message': 'Missing or invalid authorization header'
            }), 401
        
        session_id = auth_header[7:]  # Remove 'Bearer ' prefix
        
        # Validate session
        user_manager = get_user_manager()
        user = user_manager.validate_session(session_id)
        
        if not user:
            return jsonify({
                'error': 'Unauthorized',
                'message': 'Invalid or expired session'
            }), 401
        
        # Store user in request context
        g.current_user = user
        g.session_id = session_id
        
        return f(*args, **kwargs)
    
    return decorated


def require_permission(permission: Permission):
    """
    Decorator to require specific permission.
    Implements COBOL SECMGR authorization check.
    """
    def decorator(f):
        @wraps(f)
        def decorated(*args, **kwargs):
            user = get_current_user()
            
            if not user:
                return jsonify({
                    'error': 'Unauthorized',
                    'message': 'Authentication required'
                }), 401
            
            if not user.has_permission(permission):
                audit_logger = get_audit_logger()
                audit_logger.log_access_denied(
                    user_id=user.user_id,
                    resource=request.path,
                    action=request.method,
                    reason=f"Missing permission: {permission.value}"
                )
                
                return jsonify({
                    'error': 'Forbidden',
                    'message': f'Permission denied: {permission.value}'
                }), 403
            
            return f(*args, **kwargs)
        
        return decorated
    return decorator


# Authentication endpoints

@security_bp.route('/login', methods=['POST'])
def login():
    """
    User login endpoint.
    Replaces CICS SECMGR login transaction.
    
    Request body:
        user_id: User identifier
        password: User password
        
    Returns:
        Session token on success
    """
    data = request.get_json()
    
    if not data:
        return jsonify({
            'error': 'Bad Request',
            'message': 'Missing request body'
        }), 400
    
    user_id = data.get('user_id', '')
    password = data.get('password', '')
    
    if not user_id or not password:
        return jsonify({
            'error': 'Bad Request',
            'message': 'User ID and password are required'
        }), 400
    
    # Get client info
    ip_address = request.remote_addr or ''
    user_agent = request.headers.get('User-Agent', '')
    
    # Authenticate
    user_manager = get_user_manager()
    session, error = user_manager.authenticate(
        user_id=user_id,
        password=password,
        ip_address=ip_address,
        user_agent=user_agent
    )
    
    audit_logger = get_audit_logger()
    
    if not session:
        audit_logger.log_login_failure(
            user_id=user_id,
            reason=error,
            ip_address=ip_address
        )
        return jsonify({
            'error': 'Unauthorized',
            'message': error
        }), 401
    
    # Log successful login
    audit_logger.log_login_success(
        user_id=user_id,
        ip_address=ip_address,
        session_id=session.session_id
    )
    
    # Get user for response
    user = user_manager.get_user(user_id)
    
    return jsonify({
        'session_id': session.session_id,
        'user': {
            'user_id': user.user_id,
            'username': user.username,
            'role': user.role.value,
            'permissions': [p.value for p in user.permissions]
        },
        'expires_at': session.expires_at.isoformat()
    }), 200


@security_bp.route('/logout', methods=['POST'])
@require_auth
def logout():
    """
    User logout endpoint.
    
    Returns:
        Success message
    """
    session_id = g.get('session_id')
    user = get_current_user()
    
    user_manager = get_user_manager()
    user_manager.logout(session_id)
    
    audit_logger = get_audit_logger()
    audit_logger.log_logout(
        user_id=user.user_id,
        session_id=session_id
    )
    
    return jsonify({
        'message': 'Logged out successfully'
    }), 200


@security_bp.route('/session', methods=['GET'])
@require_auth
def get_session_info():
    """
    Get current session information.
    
    Returns:
        Session and user information
    """
    user = get_current_user()
    
    return jsonify({
        'user': {
            'user_id': user.user_id,
            'username': user.username,
            'role': user.role.value,
            'permissions': [p.value for p in user.permissions],
            'portfolios': user.portfolios
        }
    }), 200


@security_bp.route('/users', methods=['GET'])
@require_auth
@require_permission(Permission.ADMIN_USERS)
def list_users():
    """
    List all users (admin only).
    
    Returns:
        List of users
    """
    user_manager = get_user_manager()
    users = user_manager.list_users()
    
    user_list = []
    for u in users:
        user_list.append({
            'user_id': u.user_id,
            'username': u.username,
            'role': u.role.value,
            'status': u.status,
            'last_login': u.last_login.isoformat() if u.last_login else None
        })
    
    return jsonify({
        'users': user_list,
        'count': len(user_list)
    }), 200


@security_bp.route('/audit', methods=['GET'])
@require_auth
@require_permission(Permission.ADMIN_SYSTEM)
def get_audit_log():
    """
    Get audit log (admin only).
    
    Query Parameters:
        user_id: Filter by user ID
        limit: Maximum records (default 100)
        
    Returns:
        List of audit events
    """
    user_id = request.args.get('user_id')
    limit = int(request.args.get('limit', 100))
    
    audit_logger = get_audit_logger()
    events = audit_logger.get_events(user_id=user_id, limit=limit)
    
    return jsonify({
        'events': [e.to_dict() for e in events],
        'count': len(events)
    }), 200

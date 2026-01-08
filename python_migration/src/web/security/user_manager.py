"""
User Manager - Migrated from COBOL SECMGR program.

This module implements user management functionality including
authentication, authorization, and session management.

Original COBOL Program: src/programs/online/SECMGR.cbl
"""

import logging
import hashlib
import secrets
from dataclasses import dataclass, field
from datetime import datetime, timedelta
from typing import Optional, Dict, List, Set
from enum import Enum

logger = logging.getLogger(__name__)


class UserRole(str, Enum):
    """User roles - maps to COBOL SEC-USER-ROLE"""
    ADMIN = 'ADMIN'
    MANAGER = 'MANAGER'
    ANALYST = 'ANALYST'
    VIEWER = 'VIEWER'


class Permission(str, Enum):
    """Permissions - maps to COBOL SEC-PERMISSION"""
    VIEW_PORTFOLIO = 'VIEW_PORTFOLIO'
    EDIT_PORTFOLIO = 'EDIT_PORTFOLIO'
    VIEW_TRANSACTIONS = 'VIEW_TRANSACTIONS'
    PROCESS_TRANSACTIONS = 'PROCESS_TRANSACTIONS'
    VIEW_REPORTS = 'VIEW_REPORTS'
    GENERATE_REPORTS = 'GENERATE_REPORTS'
    ADMIN_USERS = 'ADMIN_USERS'
    ADMIN_SYSTEM = 'ADMIN_SYSTEM'


# Role to permissions mapping
ROLE_PERMISSIONS: Dict[UserRole, Set[Permission]] = {
    UserRole.ADMIN: {
        Permission.VIEW_PORTFOLIO,
        Permission.EDIT_PORTFOLIO,
        Permission.VIEW_TRANSACTIONS,
        Permission.PROCESS_TRANSACTIONS,
        Permission.VIEW_REPORTS,
        Permission.GENERATE_REPORTS,
        Permission.ADMIN_USERS,
        Permission.ADMIN_SYSTEM,
    },
    UserRole.MANAGER: {
        Permission.VIEW_PORTFOLIO,
        Permission.EDIT_PORTFOLIO,
        Permission.VIEW_TRANSACTIONS,
        Permission.PROCESS_TRANSACTIONS,
        Permission.VIEW_REPORTS,
        Permission.GENERATE_REPORTS,
    },
    UserRole.ANALYST: {
        Permission.VIEW_PORTFOLIO,
        Permission.VIEW_TRANSACTIONS,
        Permission.VIEW_REPORTS,
        Permission.GENERATE_REPORTS,
    },
    UserRole.VIEWER: {
        Permission.VIEW_PORTFOLIO,
        Permission.VIEW_TRANSACTIONS,
        Permission.VIEW_REPORTS,
    },
}


@dataclass
class User:
    """
    User record - maps to COBOL SEC-USER-RECORD
    
    Original COBOL structure:
    01  SEC-USER-RECORD.
        05  SEC-USER-ID        PIC X(08).
        05  SEC-USER-NAME      PIC X(30).
        05  SEC-PASSWORD-HASH  PIC X(64).
        05  SEC-USER-ROLE      PIC X(08).
        05  SEC-USER-STATUS    PIC X(01).
        05  SEC-LAST-LOGIN     PIC X(26).
        05  SEC-LOGIN-ATTEMPTS PIC 9(02).
        05  SEC-LOCKED-UNTIL   PIC X(26).
    """
    user_id: str
    username: str
    password_hash: str
    role: UserRole
    status: str = 'A'  # A=Active, I=Inactive, L=Locked
    last_login: Optional[datetime] = None
    login_attempts: int = 0
    locked_until: Optional[datetime] = None
    created_at: datetime = field(default_factory=datetime.now)
    portfolios: List[str] = field(default_factory=list)  # Authorized portfolios

    @property
    def is_active(self) -> bool:
        return self.status == 'A'

    @property
    def is_locked(self) -> bool:
        if self.status == 'L':
            if self.locked_until and datetime.now() > self.locked_until:
                return False
            return True
        return False

    @property
    def permissions(self) -> Set[Permission]:
        return ROLE_PERMISSIONS.get(self.role, set())

    def has_permission(self, permission: Permission) -> bool:
        return permission in self.permissions

    def has_portfolio_access(self, portfolio_id: str) -> bool:
        if self.role == UserRole.ADMIN:
            return True
        return portfolio_id in self.portfolios or '*' in self.portfolios


@dataclass
class Session:
    """User session"""
    session_id: str
    user_id: str
    created_at: datetime
    expires_at: datetime
    ip_address: str = ''
    user_agent: str = ''

    @property
    def is_expired(self) -> bool:
        return datetime.now() > self.expires_at


class UserManager:
    """
    User Manager - Migrated from COBOL SECMGR.
    
    Provides:
    - User authentication (replaces RACF integration)
    - Session management
    - Authorization checks
    - Account lockout handling
    
    Original COBOL program flow:
    1. 1000-VALIDATE-USER: Validate user credentials
    2. 2000-CHECK-AUTHORIZATION: Check user permissions
    3. 3000-CREATE-SESSION: Create user session
    4. 4000-AUDIT-ACCESS: Log access attempts
    """
    
    def __init__(self, max_login_attempts: int = 3, session_timeout: int = 1800):
        """
        Initialize the user manager.
        
        Args:
            max_login_attempts: Maximum failed login attempts before lockout
            session_timeout: Session timeout in seconds (default 30 minutes)
        """
        self.max_login_attempts = max_login_attempts
        self.session_timeout = session_timeout
        
        # In-memory storage (would be database in production)
        self._users: Dict[str, User] = {}
        self._sessions: Dict[str, Session] = {}
        
        # Create default admin user
        self._create_default_users()
        
        logger.info("UserManager initialized")
    
    def _create_default_users(self):
        """Create default system users"""
        # Admin user
        self.create_user(
            user_id='ADMIN',
            username='System Administrator',
            password='admin123',  # Should be changed in production
            role=UserRole.ADMIN,
            portfolios=['*']
        )
        
        # Demo users for testing
        self.create_user(
            user_id='MANAGER1',
            username='Portfolio Manager',
            password='manager123',
            role=UserRole.MANAGER,
            portfolios=['PORT001', 'PORT002']
        )
        
        self.create_user(
            user_id='ANALYST1',
            username='Investment Analyst',
            password='analyst123',
            role=UserRole.ANALYST,
            portfolios=['PORT001']
        )
        
        self.create_user(
            user_id='VIEWER1',
            username='Report Viewer',
            password='viewer123',
            role=UserRole.VIEWER,
            portfolios=['PORT001']
        )
    
    def _hash_password(self, password: str) -> str:
        """Hash password using SHA-256"""
        return hashlib.sha256(password.encode()).hexdigest()
    
    def _generate_session_id(self) -> str:
        """Generate secure session ID"""
        return secrets.token_hex(32)
    
    def create_user(
        self,
        user_id: str,
        username: str,
        password: str,
        role: UserRole,
        portfolios: List[str] = None
    ) -> User:
        """
        Create a new user.
        
        Args:
            user_id: Unique user identifier
            username: User display name
            password: Plain text password (will be hashed)
            role: User role
            portfolios: List of authorized portfolio IDs
            
        Returns:
            Created User object
        """
        user = User(
            user_id=user_id.upper(),
            username=username,
            password_hash=self._hash_password(password),
            role=role,
            portfolios=portfolios or []
        )
        self._users[user.user_id] = user
        logger.info(f"User created: {user_id}")
        return user
    
    def authenticate(
        self,
        user_id: str,
        password: str,
        ip_address: str = '',
        user_agent: str = ''
    ) -> tuple[Optional[Session], str]:
        """
        Authenticate user and create session.
        Implements COBOL SECMGR 1000-VALIDATE-USER paragraph.
        
        Args:
            user_id: User identifier
            password: Plain text password
            ip_address: Client IP address
            user_agent: Client user agent
            
        Returns:
            Tuple of (Session if successful, error message)
        """
        user_id = user_id.upper()
        
        # Check if user exists
        user = self._users.get(user_id)
        if not user:
            logger.warning(f"Authentication failed: user not found - {user_id}")
            return None, "Invalid user ID or password"
        
        # Check if account is locked
        if user.is_locked:
            logger.warning(f"Authentication failed: account locked - {user_id}")
            return None, "Account is locked. Please contact administrator."
        
        # Check if account is active
        if not user.is_active:
            logger.warning(f"Authentication failed: account inactive - {user_id}")
            return None, "Account is inactive"
        
        # Verify password
        if self._hash_password(password) != user.password_hash:
            user.login_attempts += 1
            
            if user.login_attempts >= self.max_login_attempts:
                user.status = 'L'
                user.locked_until = datetime.now() + timedelta(minutes=30)
                logger.warning(f"Account locked due to failed attempts: {user_id}")
                return None, "Account locked due to too many failed attempts"
            
            logger.warning(f"Authentication failed: invalid password - {user_id}")
            return None, "Invalid user ID or password"
        
        # Successful authentication
        user.login_attempts = 0
        user.last_login = datetime.now()
        
        # Create session
        session = Session(
            session_id=self._generate_session_id(),
            user_id=user_id,
            created_at=datetime.now(),
            expires_at=datetime.now() + timedelta(seconds=self.session_timeout),
            ip_address=ip_address,
            user_agent=user_agent
        )
        self._sessions[session.session_id] = session
        
        logger.info(f"User authenticated: {user_id}")
        return session, ""
    
    def validate_session(self, session_id: str) -> Optional[User]:
        """
        Validate session and return user.
        
        Args:
            session_id: Session identifier
            
        Returns:
            User if session is valid, None otherwise
        """
        session = self._sessions.get(session_id)
        if not session:
            return None
        
        if session.is_expired:
            del self._sessions[session_id]
            return None
        
        user = self._users.get(session.user_id)
        if not user or not user.is_active:
            return None
        
        # Extend session
        session.expires_at = datetime.now() + timedelta(seconds=self.session_timeout)
        
        return user
    
    def logout(self, session_id: str) -> bool:
        """
        Logout user and invalidate session.
        
        Args:
            session_id: Session identifier
            
        Returns:
            True if session was found and removed
        """
        if session_id in self._sessions:
            user_id = self._sessions[session_id].user_id
            del self._sessions[session_id]
            logger.info(f"User logged out: {user_id}")
            return True
        return False
    
    def check_permission(
        self,
        user: User,
        permission: Permission,
        portfolio_id: str = None
    ) -> bool:
        """
        Check if user has permission.
        Implements COBOL SECMGR 2000-CHECK-AUTHORIZATION paragraph.
        
        Args:
            user: User object
            permission: Required permission
            portfolio_id: Portfolio ID for portfolio-specific checks
            
        Returns:
            True if user has permission
        """
        if not user.has_permission(permission):
            return False
        
        if portfolio_id and not user.has_portfolio_access(portfolio_id):
            return False
        
        return True
    
    def get_user(self, user_id: str) -> Optional[User]:
        """Get user by ID"""
        return self._users.get(user_id.upper())
    
    def list_users(self) -> List[User]:
        """List all users"""
        return list(self._users.values())
    
    def update_password(self, user_id: str, new_password: str) -> bool:
        """Update user password"""
        user = self._users.get(user_id.upper())
        if user:
            user.password_hash = self._hash_password(new_password)
            logger.info(f"Password updated for user: {user_id}")
            return True
        return False
    
    def unlock_account(self, user_id: str) -> bool:
        """Unlock user account"""
        user = self._users.get(user_id.upper())
        if user:
            user.status = 'A'
            user.login_attempts = 0
            user.locked_until = None
            logger.info(f"Account unlocked: {user_id}")
            return True
        return False

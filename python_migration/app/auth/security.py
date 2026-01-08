"""Security module - converted from SECMGR.cbl.

This module provides JWT-based authentication and authorization,
replacing the COBOL SECMGR program and RACF integration.

COBOL Program Reference (SECMGR.cbl):
- Manages user authentication
- Handles authorization checks
- Logs security events to audit trail
"""

import os
from datetime import datetime, timedelta
from typing import Optional

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jose import JWTError, jwt
from passlib.context import CryptContext
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.database.connection import get_db
from app.database.models import AuditLog, AuthFile, User
from app.utils.constants import SecurityConstants
from app.utils.logging import get_logger

logger = get_logger(__name__)

SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-change-in-production")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = SecurityConstants.TOKEN_EXPIRE_MINUTES

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")
security = HTTPBearer(auto_error=False)


class Token(BaseModel):
    """Token response model."""

    access_token: str
    token_type: str
    expires_in: int


class TokenData(BaseModel):
    """Token payload data."""

    user_id: Optional[str] = None
    username: Optional[str] = None
    is_admin: bool = False
    exp: Optional[datetime] = None


class UserCreate(BaseModel):
    """User creation request."""

    user_id: str
    username: str
    password: str
    full_name: Optional[str] = None
    email: Optional[str] = None
    is_admin: bool = False


class UserResponse(BaseModel):
    """User response model."""

    user_id: str
    username: str
    full_name: Optional[str]
    email: Optional[str]
    is_active: bool
    is_admin: bool
    created_date: datetime
    last_login: Optional[datetime]

    class Config:
        from_attributes = True


class LoginRequest(BaseModel):
    """Login request model."""

    username: str
    password: str


class SecurityManager:
    """Security Manager - replaces SECMGR program.

    This class provides authentication and authorization functionality
    similar to the COBOL SECMGR program that managed user validation
    and RACF integration.
    """

    PROGRAM_NAME = "SECMGR"

    def __init__(self, db: Session):
        self.db = db

    def verify_password(self, plain_password: str, hashed_password: str) -> bool:
        """Verify password against hash."""
        return pwd_context.verify(plain_password, hashed_password)

    def get_password_hash(self, password: str) -> str:
        """Generate password hash."""
        return pwd_context.hash(password)

    def authenticate_user(self, username: str, password: str) -> Optional[User]:
        """Authenticate user - similar to SECMGR 2000-VALIDATE-USER.

        Args:
            username: Username
            password: Password

        Returns:
            User if authenticated, None otherwise
        """
        user = (
            self.db.query(User)
            .filter(User.username == username)
            .first()
        )

        if not user:
            self._log_security_event(username, "LOGIN", "FAIL", "User not found")
            return None

        if user.locked_until and user.locked_until > datetime.now():
            self._log_security_event(
                username, "LOGIN", "FAIL", "Account locked"
            )
            return None

        if not user.is_active:
            self._log_security_event(
                username, "LOGIN", "FAIL", "Account inactive"
            )
            return None

        if not self.verify_password(password, user.hashed_password):
            user.failed_attempts += 1

            if user.failed_attempts >= SecurityConstants.MAX_LOGIN_ATTEMPTS:
                user.locked_until = datetime.now() + timedelta(
                    minutes=SecurityConstants.LOCKOUT_DURATION_MINUTES
                )
                self._log_security_event(
                    username, "LOGIN", "FAIL", "Account locked due to failed attempts"
                )
            else:
                self._log_security_event(
                    username, "LOGIN", "FAIL", "Invalid password"
                )

            self.db.commit()
            return None

        user.failed_attempts = 0
        user.locked_until = None
        user.last_login = datetime.now()
        self.db.commit()

        self._log_security_event(username, "LOGIN", "SUCC", "Login successful")
        return user

    def create_access_token(
        self, user: User, expires_delta: Optional[timedelta] = None
    ) -> Token:
        """Create JWT access token.

        Args:
            user: Authenticated user
            expires_delta: Token expiration time

        Returns:
            Token with access token and metadata
        """
        if expires_delta:
            expire = datetime.utcnow() + expires_delta
        else:
            expire = datetime.utcnow() + timedelta(
                minutes=ACCESS_TOKEN_EXPIRE_MINUTES
            )

        to_encode = {
            "sub": user.username,
            "user_id": user.user_id,
            "is_admin": user.is_admin,
            "exp": expire,
        }

        encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

        return Token(
            access_token=encoded_jwt,
            token_type="bearer",
            expires_in=ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        )

    def verify_token(self, token: str) -> Optional[TokenData]:
        """Verify JWT token.

        Args:
            token: JWT token string

        Returns:
            TokenData if valid, None otherwise
        """
        try:
            payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
            username: str = payload.get("sub")
            user_id: str = payload.get("user_id")
            is_admin: bool = payload.get("is_admin", False)

            if username is None:
                return None

            return TokenData(
                username=username,
                user_id=user_id,
                is_admin=is_admin,
            )
        except JWTError:
            return None

    def check_authorization(
        self, user_id: str, resource: str, access_type: str
    ) -> bool:
        """Check user authorization - similar to SECMGR 3000-CHECK-AUTH.

        Args:
            user_id: User ID
            resource: Resource to access
            access_type: Type of access (READ, WRITE, DELETE)

        Returns:
            True if authorized, False otherwise
        """
        auth = (
            self.db.query(AuthFile)
            .filter(
                AuthFile.user_id == user_id,
                AuthFile.resource == resource,
                AuthFile.access_type == access_type,
                AuthFile.is_active.is_(True),
            )
            .first()
        )

        if auth:
            if auth.expiry_date and auth.expiry_date < datetime.now().date():
                self._log_security_event(
                    user_id, "AUTH", "FAIL", f"Authorization expired for {resource}"
                )
                return False

            self._log_security_event(
                user_id, "AUTH", "SUCC", f"Authorized for {resource}/{access_type}"
            )
            return True

        self._log_security_event(
            user_id, "AUTH", "FAIL", f"Not authorized for {resource}/{access_type}"
        )
        return False

    def create_user(self, user_data: UserCreate) -> User:
        """Create a new user.

        Args:
            user_data: User creation data

        Returns:
            Created user
        """
        existing = (
            self.db.query(User)
            .filter(
                (User.user_id == user_data.user_id)
                | (User.username == user_data.username)
            )
            .first()
        )

        if existing:
            raise ValueError("User already exists")

        user = User(
            user_id=user_data.user_id,
            username=user_data.username,
            hashed_password=self.get_password_hash(user_data.password),
            full_name=user_data.full_name,
            email=user_data.email,
            is_admin=user_data.is_admin,
            is_active=True,
            created_date=datetime.now(),
        )

        self.db.add(user)
        self.db.commit()
        self.db.refresh(user)

        self._log_security_event(
            user_data.user_id, "CREATE", "SUCC", f"User created: {user_data.username}"
        )

        return user

    def grant_authorization(
        self,
        user_id: str,
        resource: str,
        access_type: str,
        granted_by: str,
        expiry_date: Optional[datetime] = None,
    ) -> AuthFile:
        """Grant authorization to a user.

        Args:
            user_id: User ID
            resource: Resource to grant access to
            access_type: Type of access
            granted_by: User granting the access
            expiry_date: Optional expiration date

        Returns:
            AuthFile record
        """
        auth = AuthFile(
            user_id=user_id,
            resource=resource,
            access_type=access_type,
            granted_date=datetime.now(),
            granted_by=granted_by,
            expiry_date=expiry_date,
            is_active=True,
        )

        self.db.add(auth)
        self.db.commit()

        self._log_security_event(
            user_id,
            "GRANT",
            "SUCC",
            f"Authorization granted: {resource}/{access_type}",
        )

        return auth

    def _log_security_event(
        self, user_id: str, event_type: str, status: str, message: str
    ) -> None:
        """Log security event to audit log."""
        try:
            audit = AuditLog(
                timestamp=datetime.now(),
                user_id=user_id,
                program=self.PROGRAM_NAME,
                access_type=event_type,
                action_status=status,
                message=message,
            )
            self.db.add(audit)
            self.db.commit()
        except Exception as e:
            logger.warning(f"Failed to log security event: {e}")
            self.db.rollback()


async def get_current_user(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security),
    db: Session = Depends(get_db),
) -> Optional[TokenData]:
    """Get current user from JWT token - FastAPI dependency.

    Args:
        credentials: HTTP Bearer credentials
        db: Database session

    Returns:
        TokenData if authenticated

    Raises:
        HTTPException if not authenticated
    """
    if not credentials:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated",
            headers={"WWW-Authenticate": "Bearer"},
        )

    security_manager = SecurityManager(db)
    token_data = security_manager.verify_token(credentials.credentials)

    if not token_data:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
            headers={"WWW-Authenticate": "Bearer"},
        )

    return token_data


async def get_current_user_optional(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(security),
    db: Session = Depends(get_db),
) -> Optional[TokenData]:
    """Get current user if authenticated, None otherwise."""
    if not credentials:
        return None

    security_manager = SecurityManager(db)
    return security_manager.verify_token(credentials.credentials)


async def require_admin(
    current_user: TokenData = Depends(get_current_user),
) -> TokenData:
    """Require admin user - FastAPI dependency."""
    if not current_user.is_admin:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Admin access required",
        )
    return current_user

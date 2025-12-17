"""
Authentication Service - migrated from SECMGR.cbl.
Handles user authentication, authorization, and audit logging.
Replaces CICS security with JWT-based authentication.
"""

from datetime import datetime, timedelta

from jose import JWTError, jwt
from passlib.context import CryptContext
from sqlalchemy.orm import Session

from app.config import get_settings
from app.models.database import AuditLog, AuthFile, User
from app.models.domain import AuditAction, AuditStatus, AuditType
from app.utils.exceptions import (
    AuthenticationError,
    UserNotFoundError,
)

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


class AuthService:
    """
    Authentication and authorization service.
    Replaces SECMGR.cbl functionality:
    - P100-VALIDATE-USER: User validation
    - P200-CHECK-AUTH: Authorization check
    - P300-LOG-ACCESS: Audit logging
    """

    def __init__(self, db: Session):
        self.db = db
        self.settings = get_settings()

    def verify_password(self, plain_password: str, hashed_password: str) -> bool:
        """Verify a password against its hash."""
        return pwd_context.verify(plain_password, hashed_password)

    def get_password_hash(self, password: str) -> str:
        """Generate password hash."""
        return pwd_context.hash(password)

    def validate_user(self, username: str, password: str) -> User:
        """
        Validate user credentials.
        Replaces P100-VALIDATE-USER in SECMGR.cbl.

        Original COBOL logic:
        - EXEC CICS ASSIGN USERID(WS-USER-ID)
        - Compare SEC-USER-ID with WS-USER-ID
        """
        user = self.db.query(User).filter(
            User.username == username,
            User.is_active,
        ).first()

        if not user:
            self._log_access(
                user_id=username,
                action=AuditAction.LOGIN,
                status=AuditStatus.FAILURE,
                message="User not found",
            )
            raise AuthenticationError("Invalid username or password")

        if not self.verify_password(password, user.hashed_password):
            self._log_access(
                user_id=user.user_id,
                action=AuditAction.LOGIN,
                status=AuditStatus.FAILURE,
                message="Invalid password",
            )
            raise AuthenticationError("Invalid username or password")

        user.last_login = datetime.utcnow()

        self._log_access(
            user_id=user.user_id,
            action=AuditAction.LOGIN,
            status=AuditStatus.SUCCESS,
            message="Login successful",
        )

        return user

    def check_authorization(
        self,
        user_id: str,
        resource: str,
        access_type: str,
    ) -> bool:
        """
        Check if user is authorized for a resource.
        Replaces P200-CHECK-AUTH in SECMGR.cbl.

        Original COBOL logic:
        SELECT COUNT(*) INTO :WS-DB2-AREA
        FROM AUTHFILE
        WHERE USER_ID = :SEC-USER-ID
          AND RESOURCE = :SEC-RESOURCE-NAME
          AND ACCESS_TYPE = :SEC-ACCESS-TYPE
        """
        auth = self.db.query(AuthFile).filter(
            AuthFile.user_id == user_id,
            AuthFile.resource == resource,
            AuthFile.access_type == access_type,
            AuthFile.is_active,
        ).first()

        if auth:
            if auth.expiry_date and auth.expiry_date < datetime.utcnow():
                self._log_access(
                    user_id=user_id,
                    action=AuditAction.INQUIRE,
                    status=AuditStatus.FAILURE,
                    message=f"Authorization expired for {resource}",
                )
                return False

            self._log_access(
                user_id=user_id,
                action=AuditAction.INQUIRE,
                status=AuditStatus.SUCCESS,
                message=f"Authorized for {resource}/{access_type}",
            )
            return True

        self._log_access(
            user_id=user_id,
            action=AuditAction.INQUIRE,
            status=AuditStatus.FAILURE,
            message=f"Access denied for {resource}/{access_type}",
        )
        return False

    def create_access_token(
        self,
        user_id: str,
        expires_delta: timedelta | None = None,
    ) -> str:
        """
        Create a JWT access token.
        Replaces CICS session management.
        """
        if expires_delta:
            expire = datetime.utcnow() + expires_delta
        else:
            expire = datetime.utcnow() + timedelta(
                minutes=self.settings.access_token_expire_minutes
            )

        to_encode = {
            "sub": user_id,
            "exp": expire,
            "iat": datetime.utcnow(),
        }

        encoded_jwt = jwt.encode(
            to_encode,
            self.settings.secret_key,
            algorithm=self.settings.algorithm,
        )

        return encoded_jwt

    def verify_token(self, token: str) -> str:
        """
        Verify a JWT token and return the user_id.
        """
        try:
            payload = jwt.decode(
                token,
                self.settings.secret_key,
                algorithms=[self.settings.algorithm],
            )
            user_id: str = payload.get("sub")
            if user_id is None:
                raise AuthenticationError("Invalid token")
            return user_id
        except JWTError:
            raise AuthenticationError("Invalid token")

    def get_user(self, user_id: str) -> User:
        """Get user by user_id."""
        user = self.db.query(User).filter(
            User.user_id == user_id,
            User.is_active,
        ).first()

        if not user:
            raise UserNotFoundError(f"User not found: {user_id}")

        return user

    def get_user_by_username(self, username: str) -> User:
        """Get user by username."""
        user = self.db.query(User).filter(
            User.username == username,
            User.is_active,
        ).first()

        if not user:
            raise UserNotFoundError(f"User not found: {username}")

        return user

    def create_user(
        self,
        user_id: str,
        username: str,
        email: str,
        password: str,
        full_name: str | None = None,
        is_superuser: bool = False,
    ) -> User:
        """Create a new user."""
        user = User(
            user_id=user_id,
            username=username,
            email=email,
            hashed_password=self.get_password_hash(password),
            full_name=full_name,
            is_active=True,
            is_superuser=is_superuser,
            created_at=datetime.utcnow(),
        )

        self.db.add(user)

        self._log_access(
            user_id=user_id,
            action=AuditAction.CREATE,
            status=AuditStatus.SUCCESS,
            message=f"User created: {username}",
        )

        return user

    def grant_authorization(
        self,
        user_id: str,
        resource: str,
        access_type: str,
        granted_by: str,
        expiry_date: datetime | None = None,
    ) -> AuthFile:
        """Grant authorization to a user."""
        auth = AuthFile(
            user_id=user_id,
            resource=resource,
            access_type=access_type,
            granted_date=datetime.utcnow(),
            granted_by=granted_by,
            expiry_date=expiry_date,
            is_active=True,
        )

        self.db.add(auth)

        self._log_access(
            user_id=granted_by,
            action=AuditAction.CREATE,
            status=AuditStatus.SUCCESS,
            message=f"Granted {access_type} on {resource} to {user_id}",
        )

        return auth

    def _log_access(
        self,
        user_id: str,
        action: AuditAction,
        status: AuditStatus,
        message: str | None = None,
    ) -> None:
        """
        Log access attempt.
        Replaces P300-LOG-ACCESS in SECMGR.cbl.

        Original COBOL logic:
        INSERT INTO AUDITLOG
        (TIMESTAMP, USER_ID, TERMINAL_ID, TRANS_ID, PROGRAM, ACCESS_TYPE)
        VALUES (...)
        """
        audit = AuditLog(
            timestamp=datetime.utcnow(),
            system_id="PORTMGMT",
            user_id=user_id,
            program="SECMGR",
            audit_type=AuditType.USER_ACTION.value,
            action=action.value,
            status=status.value,
            message=message,
        )
        self.db.add(audit)

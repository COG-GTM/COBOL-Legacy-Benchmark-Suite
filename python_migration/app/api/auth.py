"""Authentication API endpoints - converted from SECMGR.cbl.

This module provides REST API endpoints for authentication,
replacing the CICS SECMGR program functionality.
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth.security import (
    LoginRequest,
    SecurityManager,
    Token,
    TokenData,
    UserCreate,
    UserResponse,
    get_current_user,
    require_admin,
)
from app.database.connection import get_db
from app.database.models import User
from app.utils.logging import get_logger

logger = get_logger(__name__)

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post(
    "/login",
    response_model=Token,
    summary="User login",
    description="Authenticate user and return JWT token - replaces SECMGR login",
)
async def login(
    request: LoginRequest,
    db: Session = Depends(get_db),
) -> Token:
    """Authenticate user and return JWT token.

    This endpoint replaces the CICS SECMGR program's user
    authentication functionality.
    """
    security_manager = SecurityManager(db)
    user = security_manager.authenticate_user(request.username, request.password)

    if not user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )

    token = security_manager.create_access_token(user)
    logger.info(f"User logged in: {request.username}")

    return token


@router.post(
    "/logout",
    summary="User logout",
    description="Log out user (client should discard token)",
)
async def logout(
    current_user: TokenData = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> dict:
    """Log out user.

    Note: JWT tokens are stateless, so logout is handled client-side
    by discarding the token. This endpoint logs the logout event.
    """
    security_manager = SecurityManager(db)
    security_manager._log_security_event(
        current_user.user_id, "LOGOUT", "SUCC", "User logged out"
    )

    logger.info(f"User logged out: {current_user.username}")

    return {"message": "Logged out successfully"}


@router.get(
    "/me",
    response_model=UserResponse,
    summary="Get current user",
    description="Get current authenticated user details",
)
async def get_me(
    current_user: TokenData = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> UserResponse:
    """Get current user details."""
    user = (
        db.query(User)
        .filter(User.user_id == current_user.user_id)
        .first()
    )

    if not user:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="User not found",
        )

    return UserResponse(
        user_id=user.user_id,
        username=user.username,
        full_name=user.full_name,
        email=user.email,
        is_active=user.is_active,
        is_admin=user.is_admin,
        created_date=user.created_date,
        last_login=user.last_login,
    )


@router.post(
    "/users",
    response_model=UserResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Create user",
    description="Create a new user (admin only)",
)
async def create_user(
    user_data: UserCreate,
    current_user: TokenData = Depends(require_admin),
    db: Session = Depends(get_db),
) -> UserResponse:
    """Create a new user (admin only)."""
    security_manager = SecurityManager(db)

    try:
        user = security_manager.create_user(user_data)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )

    logger.info(f"User created: {user_data.username} by {current_user.username}")

    return UserResponse(
        user_id=user.user_id,
        username=user.username,
        full_name=user.full_name,
        email=user.email,
        is_active=user.is_active,
        is_admin=user.is_admin,
        created_date=user.created_date,
        last_login=user.last_login,
    )


@router.get(
    "/users",
    response_model=list[UserResponse],
    summary="List users",
    description="List all users (admin only)",
)
async def list_users(
    current_user: TokenData = Depends(require_admin),
    db: Session = Depends(get_db),
) -> list[UserResponse]:
    """List all users (admin only)."""
    users = db.query(User).order_by(User.user_id).all()

    return [
        UserResponse(
            user_id=u.user_id,
            username=u.username,
            full_name=u.full_name,
            email=u.email,
            is_active=u.is_active,
            is_admin=u.is_admin,
            created_date=u.created_date,
            last_login=u.last_login,
        )
        for u in users
    ]


@router.post(
    "/grant",
    summary="Grant authorization",
    description="Grant resource authorization to a user (admin only)",
)
async def grant_authorization(
    user_id: str,
    resource: str,
    access_type: str,
    current_user: TokenData = Depends(require_admin),
    db: Session = Depends(get_db),
) -> dict:
    """Grant authorization to a user (admin only)."""
    security_manager = SecurityManager(db)

    security_manager.grant_authorization(
        user_id=user_id,
        resource=resource,
        access_type=access_type,
        granted_by=current_user.user_id,
    )

    logger.info(
        f"Authorization granted: {user_id} -> {resource}/{access_type} by {current_user.username}"
    )

    return {
        "message": "Authorization granted",
        "user_id": user_id,
        "resource": resource,
        "access_type": access_type,
    }


@router.get(
    "/check",
    summary="Check authorization",
    description="Check if current user is authorized for a resource",
)
async def check_authorization(
    resource: str,
    access_type: str,
    current_user: TokenData = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> dict:
    """Check if current user is authorized for a resource."""
    security_manager = SecurityManager(db)

    is_authorized = security_manager.check_authorization(
        current_user.user_id, resource, access_type
    )

    return {
        "authorized": is_authorized,
        "user_id": current_user.user_id,
        "resource": resource,
        "access_type": access_type,
    }

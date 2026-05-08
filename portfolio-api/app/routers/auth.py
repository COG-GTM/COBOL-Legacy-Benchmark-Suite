"""Auth router: login, refresh, logout endpoints.

Replaces COBOL SECMGR's V (validate) phase with JWT-based authentication.
"""

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.middleware.auth import get_current_user
from app.models.user import User
from app.schemas.auth import LoginRequest, LogoutRequest, RefreshRequest, TokenResponse, UserRead
from app.services.auth_service import login, logout, refresh_tokens

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post("/login", response_model=TokenResponse)
async def login_endpoint(
    request: LoginRequest,
    db: AsyncSession = Depends(get_db),
):
    """Authenticate user credentials and return JWT tokens.

    Maps to SECMGR P100-VALIDATE-USER.
    """
    result = await login(db, request.username, request.password)
    if result is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return result


@router.post("/refresh", response_model=TokenResponse)
async def refresh_endpoint(
    request: RefreshRequest,
    db: AsyncSession = Depends(get_db),
):
    """Issue new token pair from a valid refresh token."""
    result = await refresh_tokens(db, request.refresh_token)
    if result is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired refresh token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    return result


@router.post("/logout", status_code=status.HTTP_200_OK)
async def logout_endpoint(request: LogoutRequest):
    """Invalidate the refresh token."""
    await logout(request.refresh_token)
    return {"detail": "Successfully logged out"}


@router.get("/me", response_model=UserRead)
async def get_me(current_user: User = Depends(get_current_user)):
    """Return the authenticated user's profile.

    Used to verify middleware token validation works correctly.
    """
    return current_user

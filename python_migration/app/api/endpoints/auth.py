"""
Authentication endpoints - migrated from SECMGR.cbl.
Replaces CICS security with JWT-based authentication.
"""

from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.services.auth import AuthService
from app.services.database import get_db
from app.utils.exceptions import AuthenticationError, UserNotFoundError

router = APIRouter()
oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/v1/auth/token")


class Token(BaseModel):
    """Token response model."""
    access_token: str
    token_type: str


class TokenData(BaseModel):
    """Token data model."""
    user_id: str | None = None


class UserCreate(BaseModel):
    """User creation request model."""
    user_id: str
    username: str
    email: str
    password: str
    full_name: str | None = None


class UserResponse(BaseModel):
    """User response model."""
    user_id: str
    username: str
    email: str
    full_name: str | None
    is_active: bool

    class Config:
        from_attributes = True


async def get_current_user(
    token: Annotated[str, Depends(oauth2_scheme)],
    db: Session = Depends(get_db),
) -> UserResponse:
    """
    Get current authenticated user from token.
    Replaces CICS ASSIGN USERID functionality.
    """
    auth_service = AuthService(db)

    try:
        user_id = auth_service.verify_token(token)
        user = auth_service.get_user(user_id)
        return UserResponse(
            user_id=user.user_id,
            username=user.username,
            email=user.email,
            full_name=user.full_name,
            is_active=user.is_active,
        )
    except (AuthenticationError, UserNotFoundError) as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(e),
            headers={"WWW-Authenticate": "Bearer"},
        )


@router.post("/token", response_model=Token)
async def login(
    form_data: Annotated[OAuth2PasswordRequestForm, Depends()],
    db: Session = Depends(get_db),
) -> Token:
    """
    Authenticate user and return access token.
    Replaces P100-VALIDATE-USER in SECMGR.cbl.
    """
    auth_service = AuthService(db)

    try:
        user = auth_service.validate_user(form_data.username, form_data.password)
        access_token = auth_service.create_access_token(user.user_id)
        return Token(access_token=access_token, token_type="bearer")
    except AuthenticationError as e:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(e),
            headers={"WWW-Authenticate": "Bearer"},
        )


@router.post("/register", response_model=UserResponse)
async def register(
    user_data: UserCreate,
    db: Session = Depends(get_db),
) -> UserResponse:
    """Register a new user."""
    auth_service = AuthService(db)

    try:
        user = auth_service.create_user(
            user_id=user_data.user_id,
            username=user_data.username,
            email=user_data.email,
            password=user_data.password,
            full_name=user_data.full_name,
        )
        db.commit()
        return UserResponse(
            user_id=user.user_id,
            username=user.username,
            email=user.email,
            full_name=user.full_name,
            is_active=user.is_active,
        )
    except Exception as e:
        db.rollback()
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(e),
        )


@router.get("/me", response_model=UserResponse)
async def get_me(
    current_user: Annotated[UserResponse, Depends(get_current_user)],
) -> UserResponse:
    """Get current user information."""
    return current_user

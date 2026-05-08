"""Unit tests for JWT authentication module.

Covers all acceptance criteria from COG-299:
- Login with valid/invalid credentials
- Refresh token flow
- Logout invalidation
- Auth middleware validation/rejection
"""

import pytest
import pytest_asyncio
from httpx import ASGITransport, AsyncClient

from app.core.security import hash_password, create_access_token, create_refresh_token
from app.models.user import User
from app.services.auth_service import _refresh_token_blacklist
from tests.conftest import TestingSessionLocal


@pytest_asyncio.fixture(autouse=True)
async def clear_blacklist():
    """Clear the refresh token blacklist between tests."""
    _refresh_token_blacklist.clear()
    yield
    _refresh_token_blacklist.clear()


@pytest_asyncio.fixture
async def test_user():
    """Create a test user in the database."""
    async with TestingSessionLocal() as session:
        user = User(
            username="testuser",
            password_hash=hash_password("testpassword123"),
            roles=["user"],
            status="active",
        )
        session.add(user)
        await session.commit()
        await session.refresh(user)
        yield user
        # Cleanup
        await session.delete(user)
        await session.commit()


@pytest_asyncio.fixture
async def inactive_user():
    """Create an inactive test user."""
    async with TestingSessionLocal() as session:
        user = User(
            username="inactiveuser",
            password_hash=hash_password("testpassword123"),
            roles=["user"],
            status="suspended",
        )
        session.add(user)
        await session.commit()
        await session.refresh(user)
        yield user
        await session.delete(user)
        await session.commit()


# --- Login Tests ---


@pytest.mark.asyncio
async def test_login_valid_credentials(client, test_user):
    """POST /api/auth/login authenticates valid credentials and returns JWT tokens."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"


@pytest.mark.asyncio
async def test_login_invalid_password(client, test_user):
    """POST /api/auth/login returns 401 for invalid credentials."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "wrongpassword"},
    )
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid credentials"


@pytest.mark.asyncio
async def test_login_nonexistent_user(client):
    """POST /api/auth/login returns 401 for nonexistent user."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "nonexistent", "password": "anypassword"},
    )
    assert response.status_code == 401
    assert response.json()["detail"] == "Invalid credentials"


@pytest.mark.asyncio
async def test_login_inactive_user(client, inactive_user):
    """POST /api/auth/login returns 401 for inactive user."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "inactiveuser", "password": "testpassword123"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_login_empty_username(client):
    """POST /api/auth/login rejects empty username."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "", "password": "anypassword"},
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_login_empty_password(client):
    """POST /api/auth/login rejects empty password."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": ""},
    )
    assert response.status_code == 422


# --- Refresh Tests ---


@pytest.mark.asyncio
async def test_refresh_valid_token(client, test_user):
    """POST /api/auth/refresh issues new access token from valid refresh token."""
    # Login first to get tokens
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    refresh_token = login_response.json()["refresh_token"]

    # Refresh
    response = await client.post(
        "/api/auth/refresh",
        json={"refresh_token": refresh_token},
    )
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"
    # New refresh token should be different (rotation)
    assert data["refresh_token"] != refresh_token


@pytest.mark.asyncio
async def test_refresh_invalid_token(client):
    """POST /api/auth/refresh returns 401 for invalid refresh token."""
    response = await client.post(
        "/api/auth/refresh",
        json={"refresh_token": "invalid-token"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_refresh_with_access_token(client, test_user):
    """POST /api/auth/refresh rejects access tokens used as refresh tokens."""
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    access_token = login_response.json()["access_token"]

    response = await client.post(
        "/api/auth/refresh",
        json={"refresh_token": access_token},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_refresh_blacklisted_token(client, test_user):
    """POST /api/auth/refresh rejects blacklisted refresh tokens."""
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    refresh_token = login_response.json()["refresh_token"]

    # Use the refresh token once (it gets blacklisted after rotation)
    await client.post(
        "/api/auth/refresh",
        json={"refresh_token": refresh_token},
    )

    # Try to use the old refresh token again
    response = await client.post(
        "/api/auth/refresh",
        json={"refresh_token": refresh_token},
    )
    assert response.status_code == 401


# --- Logout Tests ---


@pytest.mark.asyncio
async def test_logout_invalidates_refresh_token(client, test_user):
    """POST /api/auth/logout invalidates the refresh token."""
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    refresh_token = login_response.json()["refresh_token"]

    # Logout
    response = await client.post(
        "/api/auth/logout",
        json={"refresh_token": refresh_token},
    )
    assert response.status_code == 200

    # Try to refresh with the logged-out token
    refresh_response = await client.post(
        "/api/auth/refresh",
        json={"refresh_token": refresh_token},
    )
    assert refresh_response.status_code == 401


# --- Middleware Tests ---


@pytest.mark.asyncio
async def test_middleware_valid_token(client, test_user):
    """Auth middleware correctly validates JWT and attaches user to request."""
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    access_token = login_response.json()["access_token"]

    # Access a protected endpoint (health check is unprotected, so test via /api/auth/me)
    response = await client.get(
        "/api/auth/me",
        headers={"Authorization": f"Bearer {access_token}"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["username"] == "testuser"


@pytest.mark.asyncio
async def test_middleware_missing_token(client):
    """Auth middleware returns 401 for missing token."""
    response = await client.get("/api/auth/me")
    assert response.status_code == 403  # HTTPBearer returns 403 when no credentials


@pytest.mark.asyncio
async def test_middleware_invalid_token(client):
    """Auth middleware returns 401 for invalid token."""
    response = await client.get(
        "/api/auth/me",
        headers={"Authorization": "Bearer invalid-token"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_middleware_expired_token(client, test_user):
    """Auth middleware returns 401 for expired token."""
    from datetime import timedelta

    expired_token = create_access_token(
        user_id=test_user.id,
        username=test_user.username,
        roles=test_user.roles,
        expires_delta=timedelta(seconds=-1),
    )
    response = await client.get(
        "/api/auth/me",
        headers={"Authorization": f"Bearer {expired_token}"},
    )
    assert response.status_code == 401


# --- Password Hashing Tests ---


def test_password_hashing():
    """Passwords stored as bcrypt hashes (never plaintext)."""
    from app.core.security import hash_password, verify_password

    password = "mysecretpassword"
    hashed = hash_password(password)

    assert hashed != password
    assert hashed.startswith("$2b$")
    assert verify_password(password, hashed)
    assert not verify_password("wrongpassword", hashed)

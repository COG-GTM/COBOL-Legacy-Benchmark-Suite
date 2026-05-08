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


@pytest.mark.asyncio
async def test_login_whitespace_username(client):
    """POST /api/auth/login rejects whitespace-only username."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "   ", "password": "anypassword"},
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_login_whitespace_password(client):
    """POST /api/auth/login rejects whitespace-only password."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "   "},
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_login_updates_last_login(client, test_user):
    """Successful login updates the user's last_login timestamp."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    assert response.status_code == 200

    async with TestingSessionLocal() as session:
        from sqlalchemy import select
        result = await session.execute(
            select(User).where(User.username == "testuser")
        )
        user = result.scalar_one()
        assert user.last_login is not None


@pytest.mark.asyncio
async def test_login_returns_valid_jwt_claims(client, test_user):
    """Access token contains expected claims (sub, username, roles, type)."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    assert response.status_code == 200

    from app.core.security import decode_access_token
    token = response.json()["access_token"]
    payload = decode_access_token(token)
    assert payload["sub"] == test_user.id
    assert payload["username"] == "testuser"
    assert payload["roles"] == ["user"]
    assert payload["type"] == "access"
    assert "iat" in payload
    assert "exp" in payload


@pytest.mark.asyncio
async def test_login_invalid_credentials_has_www_authenticate(client):
    """401 response includes WWW-Authenticate: Bearer header."""
    response = await client.post(
        "/api/auth/login",
        json={"username": "noone", "password": "nope"},
    )
    assert response.status_code == 401
    assert response.headers.get("www-authenticate") == "Bearer"


@pytest.mark.asyncio
async def test_login_missing_fields(client):
    """POST /api/auth/login rejects request with missing fields."""
    response = await client.post("/api/auth/login", json={})
    assert response.status_code == 422

    response = await client.post("/api/auth/login", json={"username": "user"})
    assert response.status_code == 422

    response = await client.post("/api/auth/login", json={"password": "pass"})
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
    """Auth middleware rejects requests without Authorization header."""
    response = await client.get("/api/auth/me")
    assert response.status_code in (401, 403)


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


@pytest.mark.asyncio
async def test_middleware_refresh_token_as_access(client, test_user):
    """Auth middleware rejects refresh tokens used as access tokens."""
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    refresh_token = login_response.json()["refresh_token"]

    response = await client.get(
        "/api/auth/me",
        headers={"Authorization": f"Bearer {refresh_token}"},
    )
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_refresh_inactive_user(client, test_user):
    """POST /api/auth/refresh rejects token for a user that became inactive."""
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    refresh_token = login_response.json()["refresh_token"]

    # Suspend the user
    async with TestingSessionLocal() as session:
        from sqlalchemy import update
        await session.execute(
            update(User).where(User.username == "testuser").values(status="suspended")
        )
        await session.commit()

    response = await client.post(
        "/api/auth/refresh",
        json={"refresh_token": refresh_token},
    )
    assert response.status_code == 401

    # Restore user status for other tests
    async with TestingSessionLocal() as session:
        from sqlalchemy import update
        await session.execute(
            update(User).where(User.username == "testuser").values(status="active")
        )
        await session.commit()


@pytest.mark.asyncio
async def test_refresh_empty_token(client):
    """POST /api/auth/refresh rejects empty refresh token."""
    response = await client.post(
        "/api/auth/refresh",
        json={"refresh_token": ""},
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_logout_empty_token(client):
    """POST /api/auth/logout rejects empty refresh token."""
    response = await client.post(
        "/api/auth/logout",
        json={"refresh_token": ""},
    )
    assert response.status_code == 422


@pytest.mark.asyncio
async def test_logout_returns_success_message(client, test_user):
    """POST /api/auth/logout returns success detail message."""
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    refresh_token = login_response.json()["refresh_token"]

    response = await client.post(
        "/api/auth/logout",
        json={"refresh_token": refresh_token},
    )
    assert response.status_code == 200
    assert response.json()["detail"] == "Successfully logged out"


@pytest.mark.asyncio
async def test_middleware_inactive_user(client, test_user):
    """Auth middleware rejects tokens for inactive users."""
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    access_token = login_response.json()["access_token"]

    # Suspend the user after login
    async with TestingSessionLocal() as session:
        from sqlalchemy import update
        await session.execute(
            update(User).where(User.username == "testuser").values(status="suspended")
        )
        await session.commit()

    response = await client.get(
        "/api/auth/me",
        headers={"Authorization": f"Bearer {access_token}"},
    )
    assert response.status_code == 401

    # Restore user status
    async with TestingSessionLocal() as session:
        from sqlalchemy import update
        await session.execute(
            update(User).where(User.username == "testuser").values(status="active")
        )
        await session.commit()


@pytest.mark.asyncio
async def test_middleware_user_profile_fields(client, test_user):
    """GET /api/auth/me returns all expected user profile fields."""
    login_response = await client.post(
        "/api/auth/login",
        json={"username": "testuser", "password": "testpassword123"},
    )
    access_token = login_response.json()["access_token"]

    response = await client.get(
        "/api/auth/me",
        headers={"Authorization": f"Bearer {access_token}"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["username"] == "testuser"
    assert data["roles"] == ["user"]
    assert data["status"] == "active"
    assert "id" in data
    assert "created_at" in data
    assert "updated_at" in data


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

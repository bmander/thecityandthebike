import pytest
from fastapi.testclient import TestClient

from app.config import settings
from app.database import get_db
from app.models.orm import LoginAttempt
from app.rate_limit import AccountLockout, get_account_lockout, limiter
from tests.conftest import test_app, get_test_db, TestingSessionLocal


@pytest.fixture()
def rate_limited_client(setup_database, monkeypatch):
    """Client with low rate limits for testing rate limiting behaviour."""
    monkeypatch.setattr(settings, "LOGIN_RATE_LIMIT", "2/minute")
    monkeypatch.setattr(settings, "REGISTER_RATE_LIMIT", "2/minute")
    limiter.reset()

    test_app.dependency_overrides[get_db] = get_test_db
    test_app.dependency_overrides[get_account_lockout] = lambda: AccountLockout()
    with TestClient(test_app) as c:
        yield c
    test_app.dependency_overrides.clear()


@pytest.fixture()
def lockout_client(setup_database):
    """Client with low account lockout threshold for testing lockout."""
    lockout = AccountLockout(max_attempts=3, duration=300)
    limiter.reset()

    test_app.dependency_overrides[get_db] = get_test_db
    test_app.dependency_overrides[get_account_lockout] = lambda: lockout
    with TestClient(test_app) as c:
        yield c
    test_app.dependency_overrides.clear()


def _register_user(client, username="locktest", email="lock@example.com", password="password123"):
    resp = client.post(
        "/auth/register",
        json={"username": username, "email": email, "password": password},
    )
    assert resp.status_code == 201


class TestLoginRateLimit:
    """Tests for login endpoint rate limiting."""

    def test_login_rate_limit_returns_429(self, rate_limited_client):
        """Exceeding login rate limit should return 429."""
        _register_user(rate_limited_client)

        # First 2 requests should succeed (limit is 2/minute)
        for _ in range(2):
            resp = rate_limited_client.post(
                "/auth/login",
                json={"username": "locktest", "password": "password123"},
            )
            assert resp.status_code == 200

        # 3rd request should be rate limited
        resp = rate_limited_client.post(
            "/auth/login",
            json={"username": "locktest", "password": "password123"},
        )
        assert resp.status_code == 429


class TestRegisterRateLimit:
    """Tests for register endpoint rate limiting."""

    def test_register_rate_limit_returns_429(self, rate_limited_client):
        """Exceeding register rate limit should return 429."""
        # First 2 registrations should succeed (limit is 2/minute)
        for i in range(2):
            resp = rate_limited_client.post(
                "/auth/register",
                json={
                    "username": f"user{i}",
                    "email": f"user{i}@example.com",
                    "password": "password123",
                },
            )
            assert resp.status_code == 201

        # 3rd registration should be rate limited
        resp = rate_limited_client.post(
            "/auth/register",
            json={
                "username": "user2",
                "email": "user2@example.com",
                "password": "password123",
            },
        )
        assert resp.status_code == 429

    def test_register_returns_201_under_limit(self, rate_limited_client):
        """Registration under the rate limit should succeed."""
        response = rate_limited_client.post(
            "/auth/register",
            json={
                "username": "newuser",
                "email": "new@example.com",
                "password": "password123",
            },
        )
        assert response.status_code == 201


class Test429ResponseFormat:
    """Tests for the 429 response body and headers."""

    def test_429_response_format(self, rate_limited_client):
        """429 response should have correct JSON body and Retry-After header."""
        _register_user(rate_limited_client)

        # Exhaust the limit
        for _ in range(2):
            rate_limited_client.post(
                "/auth/login",
                json={"username": "locktest", "password": "password123"},
            )

        resp = rate_limited_client.post(
            "/auth/login",
            json={"username": "locktest", "password": "password123"},
        )
        assert resp.status_code == 429
        assert resp.json() == {"msg": "Rate limit exceeded. Try again later."}
        assert "Retry-After" in resp.headers


class TestAccountLockout:
    """Integration tests for account lockout on login."""

    def test_locks_after_failed_attempts(self, lockout_client):
        """Account should lock after max failed login attempts."""
        _register_user(lockout_client)

        # Fail 3 times (lockout threshold is 3 in this fixture)
        for _ in range(3):
            resp = lockout_client.post(
                "/auth/login",
                json={"username": "locktest", "password": "wrongpassword"},
            )
            assert resp.status_code == 401

        # Next attempt should be locked
        resp = lockout_client.post(
            "/auth/login",
            json={"username": "locktest", "password": "password123"},
        )
        assert resp.status_code == 423
        assert "locked" in resp.json()["detail"]["msg"].lower()

    def test_successful_login_resets_lockout(self, lockout_client):
        """Successful login should clear the failure counter."""
        _register_user(lockout_client)

        # Fail twice (below threshold of 3)
        for _ in range(2):
            lockout_client.post(
                "/auth/login",
                json={"username": "locktest", "password": "wrongpassword"},
            )

        # Succeed - this should clear the counter
        resp = lockout_client.post(
            "/auth/login",
            json={"username": "locktest", "password": "password123"},
        )
        assert resp.status_code == 200

        # Fail twice more - should NOT lock since counter was reset
        for _ in range(2):
            lockout_client.post(
                "/auth/login",
                json={"username": "locktest", "password": "wrongpassword"},
            )

        resp = lockout_client.post(
            "/auth/login",
            json={"username": "locktest", "password": "password123"},
        )
        assert resp.status_code == 200

    def test_lockout_per_username(self, lockout_client):
        """Lockout should be per-username, not global."""
        _register_user(lockout_client, username="user_a", email="a@example.com")
        _register_user(lockout_client, username="user_b", email="b@example.com")

        # Lock out user_a
        for _ in range(3):
            lockout_client.post(
                "/auth/login",
                json={"username": "user_a", "password": "wrong"},
            )

        # user_a is locked
        resp = lockout_client.post(
            "/auth/login",
            json={"username": "user_a", "password": "password123"},
        )
        assert resp.status_code == 423

        # user_b should still work
        resp = lockout_client.post(
            "/auth/login",
            json={"username": "user_b", "password": "password123"},
        )
        assert resp.status_code == 200

    def test_foreign_ip_failures_dont_lock_current_client(self, lockout_client):
        """Failed attempts from a foreign IP should not lock the test client."""
        _register_user(lockout_client)

        # Insert login failures from a foreign IP directly in the DB
        db = TestingSessionLocal()
        try:
            for _ in range(5):
                db.add(LoginAttempt(username="locktest", ip_address="99.99.99.99"))
            db.commit()
        finally:
            db.close()

        # The test client (IP "testclient") should still be able to log in
        resp = lockout_client.post(
            "/auth/login",
            json={"username": "locktest", "password": "password123"},
        )
        assert resp.status_code == 200

import logging
from datetime import datetime, timedelta, timezone

import jwt

from app.config import settings
from app.dependencies import ALGORITHM
from app.models import RefreshToken


class TestRegister:
    """Tests for POST /auth/register endpoint."""

    def test_register_success(self, client):
        """Successful registration should return 201 with success message."""
        response = client.post(
            "/auth/register",
            json={
                "username": "newuser",
                "password": "password123",
            },
        )
        assert response.status_code == 201
        assert response.json() == {"msg": "User created"}

    def test_register_duplicate_username(self, client, test_user, test_user_data):
        """Registration with existing username should return 409."""
        response = client.post(
            "/auth/register",
            json={
                "username": test_user_data["username"],
                "password": "password123",
            },
        )
        assert response.status_code == 409
        assert "already exists" in response.json()["detail"]["msg"]

    def test_register_missing_username(self, client):
        """Registration without username should return 422."""
        response = client.post(
            "/auth/register",
            json={
                "password": "password123",
            },
        )
        assert response.status_code == 422

    def test_register_missing_password(self, client):
        """Registration without password should return 422."""
        response = client.post(
            "/auth/register",
            json={
                "username": "testuser",
            },
        )
        assert response.status_code == 422

    def test_register_password_too_short(self, client):
        """Registration with password shorter than 8 characters should return 422."""
        response = client.post(
            "/auth/register",
            json={
                "username": "newuser",
                "password": "short",
            },
        )
        assert response.status_code == 422

    def test_register_password_exactly_8_chars(self, client):
        """Registration with exactly 8-character password should succeed."""
        response = client.post(
            "/auth/register",
            json={
                "username": "newuser",
                "password": "exactly8",
            },
        )
        assert response.status_code == 201

    def test_register_username_too_short(self, client):
        """Registration with username shorter than 3 characters should return 422."""
        response = client.post(
            "/auth/register",
            json={
                "username": "ab",
                "password": "password123",
            },
        )
        assert response.status_code == 422

    def test_register_username_too_long(self, client):
        """Registration with username longer than 50 characters should return 422."""
        response = client.post(
            "/auth/register",
            json={
                "username": "a" * 51,
                "password": "password123",
            },
        )
        assert response.status_code == 422

    def test_register_username_invalid_characters(self, client):
        """Registration with special characters in username should return 422."""
        response = client.post(
            "/auth/register",
            json={
                "username": "user@name!",
                "password": "password123",
            },
        )
        assert response.status_code == 422

    def test_register_username_with_spaces(self, client):
        """Registration with spaces in username should return 422."""
        response = client.post(
            "/auth/register",
            json={
                "username": "user name",
                "password": "password123",
            },
        )
        assert response.status_code == 422

    def test_register_username_with_underscores(self, client):
        """Registration with underscores in username should succeed."""
        response = client.post(
            "/auth/register",
            json={
                "username": "new_user_1",
                "password": "password123",
            },
        )
        assert response.status_code == 201

    def test_register_username_exactly_3_chars(self, client):
        """Registration with exactly 3-character username should succeed."""
        response = client.post(
            "/auth/register",
            json={
                "username": "abc",
                "password": "password123",
            },
        )
        assert response.status_code == 201

    def test_register_username_exactly_50_chars(self, client):
        """Registration with exactly 50-character username should succeed."""
        response = client.post(
            "/auth/register",
            json={
                "username": "a" * 50,
                "password": "password123",
            },
        )
        assert response.status_code == 201


class TestRegisterValidationLogging:
    """Tests for validation error logging on registration endpoint."""

    def test_validation_error_is_logged(self, client, caplog):
        """Validation errors on registration should produce a warning log."""
        with caplog.at_level(logging.WARNING, logger="app.main"):
            client.post(
                "/auth/register",
                json={
                    "username": "ab",
                    "password": "password123",
                },
            )
        assert any(
            "Validation error on POST /auth/register" in record.message
            for record in caplog.records
        )

    def test_validation_log_contains_field_name(self, client, caplog):
        """Logged validation error should identify the failing field."""
        with caplog.at_level(logging.WARNING, logger="app.main"):
            client.post(
                "/auth/register",
                json={
                    "username": "ab",
                    "password": "password123",
                },
            )
        log_messages = " ".join(r.message for r in caplog.records)
        assert "username" in log_messages

    def test_validation_log_redacts_password(self, client, caplog):
        """Logged validation error must not include the raw password value."""
        with caplog.at_level(logging.WARNING, logger="app.main"):
            client.post(
                "/auth/register",
                json={
                    "username": "validuser",
                    "password": "x9y8z7",
                },
            )
        log_messages = " ".join(r.message for r in caplog.records)
        assert "x9y8z7" not in log_messages
        assert "[REDACTED]" in log_messages

    def test_validation_response_redacts_password(self, client):
        """422 response body must not include the raw password value."""
        response = client.post(
            "/auth/register",
            json={
                "username": "validuser",
                "password": "x9y8z7",
            },
        )
        assert response.status_code == 422
        body = response.text
        assert "x9y8z7" not in body

    def test_validation_log_does_not_redact_non_sensitive_fields(self, client, caplog):
        """Non-sensitive field values (like username) should appear in the log."""
        with caplog.at_level(logging.WARNING, logger="app.main"):
            client.post(
                "/auth/register",
                json={
                    "username": "a!",
                    "password": "password123",
                },
            )
        log_messages = " ".join(r.message for r in caplog.records)
        assert "a!" in log_messages

    def test_validation_response_redacts_password(self, client):
        """Validation error response must not leak the raw password value."""
        response = client.post(
            "/auth/register",
            json={
                "username": "validuser",
                "password": "x9y8z7",
            },
        )
        assert response.status_code == 422
        body = response.text
        assert "x9y8z7" not in body
        assert "[REDACTED]" in body

    def test_validation_error_still_returns_422(self, client, caplog):
        """Validation errors should still return 422 with detail body."""
        with caplog.at_level(logging.WARNING, logger="app.main"):
            response = client.post(
                "/auth/register",
                json={
                    "username": "ab",
                    "password": "password123",
                },
            )
        assert response.status_code == 422
        assert "detail" in response.json()


class TestLogin:
    """Tests for POST /auth/login endpoint."""

    def test_login_success(self, client, test_user, test_user_data):
        """Successful login should return 200 with valid token pair."""
        response = client.post(
            "/auth/login",
            json={
                "username": test_user_data["username"],
                "password": test_user_data["password"],
            },
        )
        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert "refresh_token" in data
        assert data["token_type"] == "bearer"

    def test_login_returns_valid_jwt(self, client, test_user, test_user_data):
        """Login should return a JWT with correct user_id."""
        response = client.post(
            "/auth/login",
            json={
                "username": test_user_data["username"],
                "password": test_user_data["password"],
            },
        )
        token = response.json()["access_token"]
        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[ALGORITHM])
        assert payload["sub"] == str(test_user.user_id)

    def test_login_wrong_password(self, client, test_user, test_user_data):
        """Login with wrong password should return 401."""
        response = client.post(
            "/auth/login",
            json={
                "username": test_user_data["username"],
                "password": "wrongpassword",
            },
        )
        assert response.status_code == 401
        assert "Bad username or password" in response.json()["detail"]["msg"]

    def test_login_nonexistent_user(self, client):
        """Login with nonexistent username should return 401."""
        response = client.post(
            "/auth/login",
            json={
                "username": "doesnotexist",
                "password": "password123",
            },
        )
        assert response.status_code == 401
        assert "Bad username or password" in response.json()["detail"]["msg"]

    def test_login_missing_username(self, client):
        """Login without username should return 422."""
        response = client.post(
            "/auth/login",
            json={
                "password": "password123",
            },
        )
        assert response.status_code == 422

    def test_login_missing_password(self, client):
        """Login without password should return 422."""
        response = client.post(
            "/auth/login",
            json={
                "username": "testuser",
            },
        )
        assert response.status_code == 422

    def test_login_stores_refresh_token_in_database(
        self, client, test_user, test_user_data, db_session
    ):
        """Login should persist a RefreshToken row with matching token and user_id."""
        response = client.post(
            "/auth/login",
            json={
                "username": test_user_data["username"],
                "password": test_user_data["password"],
            },
        )
        assert response.status_code == 200
        refresh_value = response.json()["refresh_token"]

        row = (
            db_session.query(RefreshToken)
            .filter(RefreshToken.token == refresh_value)
            .first()
        )
        assert row is not None
        assert row.user_id == test_user.user_id


class TestRefresh:
    """Tests for POST /auth/refresh endpoint."""

    def _login(self, client, test_user_data):
        response = client.post(
            "/auth/login",
            json={
                "username": test_user_data["username"],
                "password": test_user_data["password"],
            },
        )
        return response.json()

    def test_refresh_success(self, client, test_user, test_user_data):
        """Refresh should return a new token pair."""
        login_data = self._login(client, test_user_data)
        response = client.post(
            "/auth/refresh",
            json={"refresh_token": login_data["refresh_token"]},
        )
        assert response.status_code == 200
        data = response.json()
        assert "access_token" in data
        assert "refresh_token" in data
        assert data["token_type"] == "bearer"

    def test_refresh_rotation(self, client, test_user, test_user_data):
        """After refresh, old refresh token should fail with 401."""
        login_data = self._login(client, test_user_data)
        old_refresh = login_data["refresh_token"]
        client.post("/auth/refresh", json={"refresh_token": old_refresh})
        response = client.post("/auth/refresh", json={"refresh_token": old_refresh})
        assert response.status_code == 401

    def test_refresh_invalid_token(self, client):
        """Random token should return 401."""
        response = client.post(
            "/auth/refresh",
            json={"refresh_token": "totally-invalid-token"},
        )
        assert response.status_code == 401

    def test_refresh_expired_token(self, client, test_user, db_session):
        """Expired refresh token should return 401."""
        expired_token = RefreshToken(
            token="expired-token-value",
            user_id=test_user.user_id,
            expires_at=datetime.now(timezone.utc) - timedelta(days=1),
        )
        db_session.add(expired_token)
        db_session.commit()

        response = client.post(
            "/auth/refresh",
            json={"refresh_token": "expired-token-value"},
        )
        assert response.status_code == 401

    def test_refresh_returns_working_access_token(self, client, test_user, test_user_data):
        """New access token from refresh should work for authenticated endpoints."""
        login_data = self._login(client, test_user_data)
        refresh_response = client.post(
            "/auth/refresh",
            json={"refresh_token": login_data["refresh_token"]},
        )
        new_access = refresh_response.json()["access_token"]
        me_response = client.get(
            "/users/me",
            headers={"Authorization": f"Bearer {new_access}"},
        )
        assert me_response.status_code == 200


class TestLogout:
    """Tests for POST /auth/logout endpoint."""

    def _login(self, client, test_user_data):
        response = client.post(
            "/auth/login",
            json={
                "username": test_user_data["username"],
                "password": test_user_data["password"],
            },
        )
        return response.json()

    def test_logout_revokes_refresh_token(self, client, test_user, test_user_data):
        """After logout, refresh token should be revoked."""
        login_data = self._login(client, test_user_data)
        logout_response = client.post(
            "/auth/logout",
            json={"refresh_token": login_data["refresh_token"]},
        )
        assert logout_response.status_code == 200
        assert logout_response.json() == {"msg": "Logged out"}

        refresh_response = client.post(
            "/auth/refresh",
            json={"refresh_token": login_data["refresh_token"]},
        )
        assert refresh_response.status_code == 401

    def test_logout_invalid_token_still_200(self, client):
        """Logout with random token should still return 200."""
        response = client.post(
            "/auth/logout",
            json={"refresh_token": "nonexistent-token"},
        )
        assert response.status_code == 200
        assert response.json() == {"msg": "Logged out"}

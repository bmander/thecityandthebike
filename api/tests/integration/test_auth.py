from jose import jwt

from app.config import settings
from app.dependencies import ALGORITHM


class TestRegister:
    """Tests for POST /auth/register endpoint."""

    def test_register_success(self, client):
        """Successful registration should return 201 with success message."""
        response = client.post(
            "/auth/register",
            json={
                "username": "newuser",
                "email": "newuser@example.com",
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
                "email": "different@example.com",
                "password": "password123",
            },
        )
        assert response.status_code == 409
        assert "already exists" in response.json()["detail"]["msg"]

    def test_register_duplicate_email(self, client, test_user, test_user_data):
        """Registration with existing email should return 409."""
        response = client.post(
            "/auth/register",
            json={
                "username": "differentuser",
                "email": test_user_data["email"],
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
                "email": "test@example.com",
                "password": "password123",
            },
        )
        assert response.status_code == 422

    def test_register_missing_email(self, client):
        """Registration without email should return 422."""
        response = client.post(
            "/auth/register",
            json={
                "username": "testuser",
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
                "email": "test@example.com",
            },
        )
        assert response.status_code == 422

    def test_register_password_too_short(self, client):
        """Registration with password shorter than 8 characters should return 422."""
        response = client.post(
            "/auth/register",
            json={
                "username": "newuser",
                "email": "new@example.com",
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
                "email": "new@example.com",
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
                "email": "new@example.com",
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
                "email": "new@example.com",
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
                "email": "new@example.com",
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
                "email": "new@example.com",
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
                "email": "new@example.com",
                "password": "password123",
            },
        )
        assert response.status_code == 201


class TestLogin:
    """Tests for POST /auth/login endpoint."""

    def test_login_success(self, client, test_user, test_user_data):
        """Successful login should return 200 with valid token."""
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
        assert payload["sub"] == test_user.user_id

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

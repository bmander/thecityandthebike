from datetime import datetime, timedelta

from jose import jwt

from app.config import settings
from app.dependencies import (
    ALGORITHM,
    ACCESS_TOKEN_EXPIRE_MINUTES,
    get_password_hash,
    verify_password,
    create_access_token,
)


class TestPasswordHashing:
    """Tests for password hashing functions."""

    def test_hash_generates_different_values(self):
        """Password hash should be different from the original password."""
        password = "mysecretpassword"
        hashed = get_password_hash(password)
        assert hashed != password
        assert len(hashed) > 0

    def test_hash_is_different_each_time(self):
        """Same password should produce different hashes (due to salt)."""
        password = "mysecretpassword"
        hash1 = get_password_hash(password)
        hash2 = get_password_hash(password)
        assert hash1 != hash2

    def test_verify_password_correct(self):
        """Correct password should verify successfully."""
        password = "mysecretpassword"
        hashed = get_password_hash(password)
        assert verify_password(password, hashed) is True

    def test_verify_password_incorrect(self):
        """Incorrect password should fail verification."""
        password = "mysecretpassword"
        hashed = get_password_hash(password)
        assert verify_password("wrongpassword", hashed) is False

    def test_unicode_password(self):
        """Password with unicode characters should work correctly."""
        password = "p@$$w0rd!*&^%$#"
        hashed = get_password_hash(password)
        assert verify_password(password, hashed) is True
        assert verify_password("wrong", hashed) is False

    def test_empty_password(self):
        """Empty password should be hashable and verifiable."""
        password = ""
        hashed = get_password_hash(password)
        assert verify_password(password, hashed) is True
        assert verify_password("notempty", hashed) is False


class TestJWTTokens:
    """Tests for JWT token creation and validation."""

    def test_create_access_token_returns_string(self):
        """Token creation should return a non-empty string."""
        token = create_access_token(subject="user123")
        assert isinstance(token, str)
        assert len(token) > 0

    def test_token_contains_correct_subject(self):
        """Token should contain the correct subject claim."""
        user_id = "user-uuid-123"
        token = create_access_token(subject=user_id)
        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[ALGORITHM])
        assert payload["sub"] == user_id

    def test_token_contains_expiration(self):
        """Token should contain an expiration claim."""
        token = create_access_token(subject="user123")
        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[ALGORITHM])
        assert "exp" in payload
        # Expiration should be in the future
        exp_datetime = datetime.utcfromtimestamp(payload["exp"])
        assert exp_datetime > datetime.utcnow()

    def test_token_expiration_is_correct_duration(self):
        """Token expiration should match ACCESS_TOKEN_EXPIRE_MINUTES."""
        before = datetime.utcnow()
        token = create_access_token(subject="user123")
        after = datetime.utcnow()

        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[ALGORITHM])
        exp_datetime = datetime.utcfromtimestamp(payload["exp"])

        # Expiration should be approximately ACCESS_TOKEN_EXPIRE_MINUTES from now
        # Add 1 second tolerance for timing variations
        expected_min = before + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES) - timedelta(seconds=1)
        expected_max = after + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES) + timedelta(seconds=1)

        assert expected_min <= exp_datetime <= expected_max

    def test_token_uses_correct_algorithm(self):
        """Token should use the HS256 algorithm."""
        token = create_access_token(subject="user123")
        header = jwt.get_unverified_header(token)
        assert header["alg"] == "HS256"

    def test_different_users_get_different_tokens(self):
        """Different user IDs should produce different tokens."""
        token1 = create_access_token(subject="user1")
        token2 = create_access_token(subject="user2")
        assert token1 != token2

import uuid
from datetime import datetime, timedelta, timezone

import jwt
import pytest
from fastapi import HTTPException

from app.config import settings
from app.dependencies import (
    ALGORITHM,
    ACCESS_TOKEN_EXPIRE_MINUTES,
    REFRESH_TOKEN_EXPIRE_DAYS,
    get_password_hash,
    verify_password,
    create_access_token,
    get_current_user,
    get_current_user_optional,
    create_refresh_token,
    rotate_refresh_token,
)
from app.models import User, RefreshToken


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
        exp_datetime = datetime.fromtimestamp(payload["exp"], tz=timezone.utc)
        assert exp_datetime > datetime.now(timezone.utc)

    def test_token_expiration_is_correct_duration(self):
        """Token expiration should match ACCESS_TOKEN_EXPIRE_MINUTES."""
        before = datetime.now(timezone.utc)
        token = create_access_token(subject="user123")
        after = datetime.now(timezone.utc)

        payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[ALGORITHM])
        exp_datetime = datetime.fromtimestamp(payload["exp"], tz=timezone.utc)

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


class TestGetCurrentUser:
    """Tests for get_current_user dependency."""

    def test_valid_token_returns_user(self, db_session, test_user):
        token = create_access_token(subject=str(test_user.user_id))
        user = get_current_user(token=token, db=db_session)
        assert user.user_id == test_user.user_id

    def test_missing_sub_claim_raises_401(self, db_session):
        token = jwt.encode(
            {"exp": datetime.now(timezone.utc) + timedelta(minutes=15)},
            settings.JWT_SECRET_KEY,
            algorithm=ALGORITHM,
        )
        with pytest.raises(HTTPException) as exc_info:
            get_current_user(token=token, db=db_session)
        assert exc_info.value.status_code == 401

    def test_invalid_uuid_in_sub_raises_401(self, db_session):
        token = jwt.encode(
            {"sub": "not-a-uuid", "exp": datetime.now(timezone.utc) + timedelta(minutes=15)},
            settings.JWT_SECRET_KEY,
            algorithm=ALGORITHM,
        )
        with pytest.raises(HTTPException) as exc_info:
            get_current_user(token=token, db=db_session)
        assert exc_info.value.status_code == 401

    def test_expired_token_raises_401(self, db_session):
        token = jwt.encode(
            {"sub": str(uuid.uuid4()), "exp": datetime.now(timezone.utc) - timedelta(minutes=1)},
            settings.JWT_SECRET_KEY,
            algorithm=ALGORITHM,
        )
        with pytest.raises(HTTPException) as exc_info:
            get_current_user(token=token, db=db_session)
        assert exc_info.value.status_code == 401

    def test_wrong_secret_raises_401(self, db_session):
        token = jwt.encode(
            {"sub": str(uuid.uuid4()), "exp": datetime.now(timezone.utc) + timedelta(minutes=15)},
            "wrong-secret-key",
            algorithm=ALGORITHM,
        )
        with pytest.raises(HTTPException) as exc_info:
            get_current_user(token=token, db=db_session)
        assert exc_info.value.status_code == 401

    def test_deleted_user_raises_401(self, db_session):
        nonexistent_id = uuid.uuid4()
        token = create_access_token(subject=str(nonexistent_id))
        with pytest.raises(HTTPException) as exc_info:
            get_current_user(token=token, db=db_session)
        assert exc_info.value.status_code == 401


class TestGetCurrentUserOptional:
    """Tests for get_current_user_optional dependency."""

    def test_no_token_returns_none(self, db_session):
        result = get_current_user_optional(token=None, db=db_session)
        assert result is None

    def test_invalid_token_returns_none(self, db_session):
        result = get_current_user_optional(token="garbage.token.value", db=db_session)
        assert result is None

    def test_missing_sub_returns_none(self, db_session):
        token = jwt.encode(
            {"exp": datetime.now(timezone.utc) + timedelta(minutes=15)},
            settings.JWT_SECRET_KEY,
            algorithm=ALGORITHM,
        )
        result = get_current_user_optional(token=token, db=db_session)
        assert result is None

    def test_invalid_uuid_sub_returns_none(self, db_session):
        token = jwt.encode(
            {"sub": "not-a-uuid", "exp": datetime.now(timezone.utc) + timedelta(minutes=15)},
            settings.JWT_SECRET_KEY,
            algorithm=ALGORITHM,
        )
        result = get_current_user_optional(token=token, db=db_session)
        assert result is None

    def test_deleted_user_returns_none(self, db_session):
        nonexistent_id = uuid.uuid4()
        token = create_access_token(subject=str(nonexistent_id))
        result = get_current_user_optional(token=token, db=db_session)
        assert result is None

    def test_valid_token_returns_user(self, db_session, test_user):
        token = create_access_token(subject=str(test_user.user_id))
        user = get_current_user_optional(token=token, db=db_session)
        assert user is not None
        assert user.user_id == test_user.user_id


class TestCreateRefreshToken:
    """Tests for create_refresh_token."""

    def test_returns_hex_string(self, db_session, test_user):
        token = create_refresh_token(test_user.user_id, db_session)
        assert isinstance(token, str)
        int(token, 16)  # raises ValueError if not valid hex

    def test_creates_record_in_db(self, db_session, test_user):
        token = create_refresh_token(test_user.user_id, db_session)
        record = db_session.query(RefreshToken).filter(RefreshToken.token == token).first()
        assert record is not None
        assert record.user_id == test_user.user_id

    def test_commit_true_persists(self, db_session, test_user):
        token = create_refresh_token(test_user.user_id, db_session, commit=True)
        # After commit, a new session should see the record
        record = db_session.query(RefreshToken).filter(RefreshToken.token == token).first()
        assert record is not None

    def test_commit_false_does_not_commit(self, db_session, test_user):
        token = create_refresh_token(test_user.user_id, db_session, commit=False)
        # Rollback the uncommitted work
        db_session.rollback()
        record = db_session.query(RefreshToken).filter(RefreshToken.token == token).first()
        assert record is None

    def test_expiration_set_correctly(self, db_session, test_user):
        before = datetime.now(timezone.utc)
        token = create_refresh_token(test_user.user_id, db_session)
        after = datetime.now(timezone.utc)

        record = db_session.query(RefreshToken).filter(RefreshToken.token == token).first()
        expires_at = record.expires_at
        if expires_at.tzinfo is None:
            expires_at = expires_at.replace(tzinfo=timezone.utc)

        expected_min = before + timedelta(days=REFRESH_TOKEN_EXPIRE_DAYS) - timedelta(seconds=1)
        expected_max = after + timedelta(days=REFRESH_TOKEN_EXPIRE_DAYS) + timedelta(seconds=1)
        assert expected_min <= expires_at <= expected_max


class TestRotateRefreshToken:
    """Tests for rotate_refresh_token."""

    def test_nonexistent_token_raises_401(self, db_session):
        with pytest.raises(HTTPException) as exc_info:
            rotate_refresh_token("nonexistent_token", db_session)
        assert exc_info.value.status_code == 401

    def test_expired_token_raises_401_and_deletes(self, db_session, test_user):
        # Insert an expired refresh token
        record = RefreshToken(
            token="expired_token_hex",
            user_id=test_user.user_id,
            expires_at=datetime.now(timezone.utc) - timedelta(days=1),
        )
        db_session.add(record)
        db_session.commit()

        with pytest.raises(HTTPException) as exc_info:
            rotate_refresh_token("expired_token_hex", db_session)
        assert exc_info.value.status_code == 401

        # Token should be deleted
        assert db_session.query(RefreshToken).filter(RefreshToken.token == "expired_token_hex").first() is None

    def test_valid_token_returns_new_tokens(self, db_session, test_user):
        old_token = create_refresh_token(test_user.user_id, db_session)

        access_token, new_refresh_token = rotate_refresh_token(old_token, db_session)

        # Old token should be deleted
        assert db_session.query(RefreshToken).filter(RefreshToken.token == old_token).first() is None
        # New refresh token should exist
        assert db_session.query(RefreshToken).filter(RefreshToken.token == new_refresh_token).first() is not None
        # Access token should be a valid JWT for the same user
        payload = jwt.decode(access_token, settings.JWT_SECRET_KEY, algorithms=[ALGORITHM])
        assert payload["sub"] == str(test_user.user_id)

import pytest
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.database import Base
from app.rate_limit import AccountLockout


@pytest.fixture()
def db():
    engine = create_engine(
        "sqlite:///:memory:",
        connect_args={"check_same_thread": False},
        poolclass=StaticPool,
    )
    Base.metadata.create_all(bind=engine)
    Session = sessionmaker(bind=engine)
    session = Session()
    try:
        yield session
    finally:
        session.close()


class TestAccountLockout:
    """Tests for AccountLockout tracking."""

    def test_not_locked_initially(self, db):
        lockout = AccountLockout(max_attempts=3, duration=60)
        assert not lockout.is_locked("alice", db)

    def test_locks_after_max_attempts(self, db):
        lockout = AccountLockout(max_attempts=3, duration=60)
        for _ in range(3):
            lockout.record_failure("alice", db)
        assert lockout.is_locked("alice", db)

    def test_not_locked_below_max(self, db):
        lockout = AccountLockout(max_attempts=3, duration=60)
        for _ in range(2):
            lockout.record_failure("alice", db)
        assert not lockout.is_locked("alice", db)

    def test_clear_resets_lockout(self, db):
        lockout = AccountLockout(max_attempts=3, duration=60)
        for _ in range(3):
            lockout.record_failure("alice", db)
        assert lockout.is_locked("alice", db)
        lockout.clear("alice", db)
        assert not lockout.is_locked("alice", db)

    def test_users_are_independent(self, db):
        lockout = AccountLockout(max_attempts=3, duration=60)
        for _ in range(3):
            lockout.record_failure("alice", db)
        assert lockout.is_locked("alice", db)
        assert not lockout.is_locked("bob", db)

    def test_clear_nonexistent_user_is_noop(self, db):
        lockout = AccountLockout(max_attempts=3, duration=60)
        lockout.clear("nobody", db)  # should not raise


class TestRateLimitExceededHandler:
    """Tests for the rate_limit_exceeded_handler function."""

    def test_handler_returns_429_when_view_rate_limit_is_none(self):
        import asyncio
        from unittest.mock import MagicMock
        from app.rate_limit import rate_limit_exceeded_handler

        request = MagicMock()
        request.state.view_rate_limit = None
        exc = MagicMock()

        response = asyncio.run(rate_limit_exceeded_handler(request, exc))
        assert response.status_code == 429
        assert "Retry-After" not in response.headers


class TestGetAccountLockout:
    """Tests for the get_account_lockout factory function."""

    def test_returns_lockout_with_settings_values(self, monkeypatch):
        from app.rate_limit import get_account_lockout
        from app import config

        monkeypatch.setattr(config.settings, "ACCOUNT_LOCKOUT_ATTEMPTS", 5)
        monkeypatch.setattr(config.settings, "ACCOUNT_LOCKOUT_DURATION", 600)

        lockout = get_account_lockout()
        assert lockout.max_attempts == 5
        assert lockout.duration == 600

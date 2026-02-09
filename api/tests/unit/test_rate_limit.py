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

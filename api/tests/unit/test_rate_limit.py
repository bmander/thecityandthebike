from app.rate_limit import AccountLockout


class TestAccountLockout:
    """Tests for AccountLockout tracking."""

    def test_not_locked_initially(self):
        lockout = AccountLockout(max_attempts=3, duration=60)
        assert not lockout.is_locked("alice")

    def test_locks_after_max_attempts(self):
        lockout = AccountLockout(max_attempts=3, duration=60)
        for _ in range(3):
            lockout.record_failure("alice")
        assert lockout.is_locked("alice")

    def test_not_locked_below_max(self):
        lockout = AccountLockout(max_attempts=3, duration=60)
        for _ in range(2):
            lockout.record_failure("alice")
        assert not lockout.is_locked("alice")

    def test_clear_resets_lockout(self):
        lockout = AccountLockout(max_attempts=3, duration=60)
        for _ in range(3):
            lockout.record_failure("alice")
        assert lockout.is_locked("alice")
        lockout.clear("alice")
        assert not lockout.is_locked("alice")

    def test_users_are_independent(self):
        lockout = AccountLockout(max_attempts=3, duration=60)
        for _ in range(3):
            lockout.record_failure("alice")
        assert lockout.is_locked("alice")
        assert not lockout.is_locked("bob")

    def test_clear_nonexistent_user_is_noop(self):
        lockout = AccountLockout(max_attempts=3, duration=60)
        lockout.clear("nobody")  # should not raise

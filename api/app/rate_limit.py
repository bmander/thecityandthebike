import threading
import time

from slowapi import Limiter
from slowapi.util import get_remote_address


limiter = Limiter(key_func=get_remote_address)


class AccountLockout:
    """Tracks consecutive failed login attempts per username.

    After ``max_attempts`` consecutive failures within ``duration`` seconds,
    the account is locked out until the window expires or the record is
    cleared (e.g. on a successful login).
    """

    def __init__(self, max_attempts: int = 10, duration: int = 900):
        self.max_attempts = max_attempts
        self.duration = duration
        self._lock = threading.Lock()
        # {username: [timestamp, ...]}
        self._attempts: dict[str, list[float]] = {}

    def record_failure(self, username: str) -> None:
        with self._lock:
            now = time.monotonic()
            attempts = self._attempts.setdefault(username, [])
            attempts.append(now)
            # Trim old attempts outside the window
            cutoff = now - self.duration
            self._attempts[username] = [t for t in attempts if t > cutoff]

    def is_locked(self, username: str) -> bool:
        with self._lock:
            attempts = self._attempts.get(username)
            if not attempts:
                return False
            now = time.monotonic()
            cutoff = now - self.duration
            valid = [t for t in attempts if t > cutoff]
            self._attempts[username] = valid
            return len(valid) >= self.max_attempts

    def clear(self, username: str) -> None:
        with self._lock:
            self._attempts.pop(username, None)


_account_lockout: AccountLockout | None = None


def get_account_lockout() -> AccountLockout:
    global _account_lockout
    if _account_lockout is None:
        from .config import settings

        _account_lockout = AccountLockout(
            max_attempts=settings.ACCOUNT_LOCKOUT_ATTEMPTS,
            duration=settings.ACCOUNT_LOCKOUT_DURATION,
        )
    return _account_lockout

import time
from datetime import datetime, timedelta, timezone

from fastapi import Request
from fastapi.responses import JSONResponse
from slowapi import Limiter
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address
from sqlalchemy.orm import Session

from .models.orm import LoginAttempt


limiter = Limiter(key_func=get_remote_address)


class AccountLockout:
    """Tracks consecutive failed login attempts per username using the database.

    After ``max_attempts`` failures within ``duration`` seconds the account is
    considered locked until the window expires or the record is cleared (e.g.
    on a successful login).

    .. warning::

        The lockout is keyed on **username only**.  An attacker can therefore
        lock any account by deliberately sending failed login attempts.
        Possible mitigations include:

        * Requiring a CAPTCHA after N consecutive failures.
        * Keying the lockout on a (username, IP) pair so that only the
          attacker's own IP is penalized.
        * Using progressive delays (exponential back-off) instead of a hard
          lockout.
    """

    def __init__(self, max_attempts: int = 10, duration: int = 900):
        self.max_attempts = max_attempts
        self.duration = duration

    def record_failure(self, username: str, db: Session) -> None:
        db.add(LoginAttempt(username=username))
        db.commit()

    def is_locked(self, username: str, db: Session) -> bool:
        cutoff = datetime.now(timezone.utc) - timedelta(seconds=self.duration)
        count = (
            db.query(LoginAttempt)
            .filter(LoginAttempt.username == username, LoginAttempt.attempted_at >= cutoff)
            .count()
        )
        return count >= self.max_attempts

    def clear(self, username: str, db: Session) -> None:
        db.query(LoginAttempt).filter(LoginAttempt.username == username).delete()
        db.commit()


def get_account_lockout() -> AccountLockout:
    from .config import settings

    return AccountLockout(
        max_attempts=settings.ACCOUNT_LOCKOUT_ATTEMPTS,
        duration=settings.ACCOUNT_LOCKOUT_DURATION,
    )


async def rate_limit_exceeded_handler(request: Request, exc: RateLimitExceeded):
    response = JSONResponse(
        status_code=429,
        content={"msg": "Rate limit exceeded. Try again later."},
    )
    try:
        current_limit = request.state.view_rate_limit
        if current_limit is not None:
            lmtr = request.app.state.limiter
            window_stats = lmtr._limiter.get_window_stats(
                current_limit[0], *current_limit[1]
            )
            reset_in = 1 + window_stats[0]
            response.headers["Retry-After"] = str(int(reset_in - time.time()))
    except Exception:
        pass
    return response

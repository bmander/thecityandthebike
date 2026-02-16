import logging
import time
from datetime import datetime, timedelta, timezone

from fastapi import Request
from fastapi.responses import JSONResponse
from slowapi import Limiter
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address
from sqlalchemy.orm import Session

from .models.orm import LoginAttempt

logger = logging.getLogger(__name__)

limiter = Limiter(key_func=get_remote_address)


class AccountLockout:
    """Tracks consecutive failed login attempts per (username, IP) pair.

    After ``max_attempts`` failures from the same IP within ``duration``
    seconds, further login attempts for that username **from that IP** are
    blocked until the window expires or the record is cleared (e.g. on a
    successful login, which clears attempts from *all* IPs).
    """

    def __init__(self, max_attempts: int = 10, duration: int = 900):
        self.max_attempts = max_attempts
        self.duration = duration

    def record_failure(self, username: str, ip_address: str, db: Session) -> None:
        db.add(LoginAttempt(username=username, ip_address=ip_address))
        db.commit()
        logger.info("Login failure recorded", extra={"username": username, "ip_address": ip_address})

    def is_locked(self, username: str, ip_address: str, db: Session) -> bool:
        cutoff = datetime.now(timezone.utc) - timedelta(seconds=self.duration)
        count = (
            db.query(LoginAttempt)
            .filter(
                LoginAttempt.username == username,
                LoginAttempt.ip_address == ip_address,
                LoginAttempt.attempted_at >= cutoff,
            )
            .count()
        )
        locked = count >= self.max_attempts
        if locked:
            logger.warning(
                "Account lockout triggered",
                extra={
                    "username": username,
                    "ip_address": ip_address,
                    "attempt_count": count,
                    "window_seconds": self.duration,
                },
            )
        return locked

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

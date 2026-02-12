from .auth import router as auth_router
from .users import router as users_router
from .submissions import router as submissions_router
from .bikes import router as bikes_router
from .uploads import router as uploads_router
from .leaderboard import router as leaderboard_router

__all__ = ["auth_router", "users_router", "submissions_router", "bikes_router", "uploads_router", "leaderboard_router"]

"""CLI script to promote a user to admin.

Usage:
    python -m app.promote_admin <username>
"""

import sys

from .database import SessionLocal
from .models import User


def promote_user(username, session_factory=None):
    if session_factory is None:
        session_factory = SessionLocal
    session = session_factory()
    try:
        user = session.query(User).filter(User.username == username).first()
        if not user:
            print(f"User '{username}' not found.")
            return False
        if user.is_admin:
            print(f"User '{username}' is already an admin.")
            return True
        user.is_admin = True
        session.commit()
        print(f"User '{username}' has been promoted to admin.")
        return True
    finally:
        session.close()


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python -m app.promote_admin <username>")
        sys.exit(1)
    username = sys.argv[1]
    success = promote_user(username)
    sys.exit(0 if success else 1)

"""add ban support

Revision ID: p8k2f6g01234
Revises: o7j1e5f90123
Create Date: 2026-02-22 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "p8k2f6g01234"
down_revision: Union[str, None] = "o7j1e5f90123"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.add_column("users", sa.Column("is_banned", sa.Boolean(), server_default="0", nullable=False))
    op.add_column("users", sa.Column("android_id", sa.String(64), nullable=True))
    op.create_index("ix_users_android_id", "users", ["android_id"])

    op.create_table(
        "device_bans",
        sa.Column("id", sa.Integer(), primary_key=True, autoincrement=True),
        sa.Column("android_id", sa.String(64), nullable=False, unique=True),
        sa.Column("banned_by", sa.Uuid(), sa.ForeignKey("users.user_id"), nullable=False),
        sa.Column("reason", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True)),
    )
    op.create_index("ix_device_bans_android_id", "device_bans", ["android_id"])


def downgrade() -> None:
    op.drop_table("device_bans")
    op.drop_index("ix_users_android_id", table_name="users")
    op.drop_column("users", "android_id")
    op.drop_column("users", "is_banned")

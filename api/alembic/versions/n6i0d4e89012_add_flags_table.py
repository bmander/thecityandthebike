"""add flags table

Revision ID: n6i0d4e89012
Revises: m5h9c3d78901
Create Date: 2026-02-18 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "n6i0d4e89012"
down_revision: Union[str, None] = "m5h9c3d78901"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "flags",
        sa.Column("flag_id", sa.Uuid(), nullable=False),
        sa.Column("submission_id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(["submission_id"], ["fender_submissions.submission_id"]),
        sa.ForeignKeyConstraint(["user_id"], ["users.user_id"]),
        sa.PrimaryKeyConstraint("flag_id"),
        sa.UniqueConstraint("submission_id", "user_id", name="uq_flags_submission_user"),
    )
    op.create_index(op.f("ix_flags_submission_id"), "flags", ["submission_id"])


def downgrade() -> None:
    op.drop_index(op.f("ix_flags_submission_id"), table_name="flags")
    op.drop_table("flags")

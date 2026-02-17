"""add scoring events table

Revision ID: m5h9c3d78901
Revises: l4g8b2c67890
Create Date: 2026-02-16 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "m5h9c3d78901"
down_revision: Union[str, None] = "l4g8b2c67890"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "scoring_events",
        sa.Column("event_id", sa.Uuid(), nullable=False),
        sa.Column("user_id", sa.Uuid(), nullable=False),
        sa.Column("event_type", sa.String(50), nullable=False),
        sa.Column("points", sa.Integer(), nullable=False),
        sa.Column("submission_id", sa.Uuid(), nullable=True),
        sa.Column("tag_id", sa.Uuid(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("revoked_at", sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(["user_id"], ["users.user_id"]),
        sa.ForeignKeyConstraint(["submission_id"], ["fender_submissions.submission_id"]),
        sa.ForeignKeyConstraint(["tag_id"], ["tags.tag_id"]),
        sa.PrimaryKeyConstraint("event_id"),
    )
    op.create_index(op.f("ix_scoring_events_user_id"), "scoring_events", ["user_id"])


def downgrade() -> None:
    op.drop_index(op.f("ix_scoring_events_user_id"), table_name="scoring_events")
    op.drop_table("scoring_events")

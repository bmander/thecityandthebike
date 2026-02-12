"""add bike_id uploaded_at index

Revision ID: h0c4d8e23456
Revises: g9b3c7d12345
Create Date: 2026-02-12 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = 'h0c4d8e23456'
down_revision: Union[str, Sequence[str], None] = 'g9b3c7d12345'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Add composite index on bike_id and uploaded_at for captured-by queries."""
    op.create_index(
        "ix_fender_submissions_bike_id_uploaded_at",
        "fender_submissions",
        ["bike_id", "uploaded_at"],
    )


def downgrade() -> None:
    """Remove composite index on bike_id and uploaded_at."""
    op.drop_index("ix_fender_submissions_bike_id_uploaded_at", table_name="fender_submissions")

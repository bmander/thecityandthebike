"""add side to submissions

Revision ID: k3f7a1b56789
Revises: j2e6f0a45678
Create Date: 2026-02-15 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'k3f7a1b56789'
down_revision: Union[str, Sequence[str], None] = 'j2e6f0a45678'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Add side column to fender_submissions table."""
    op.add_column('fender_submissions', sa.Column('side', sa.Text(), nullable=True))


def downgrade() -> None:
    """Remove side column from fender_submissions table."""
    op.drop_column('fender_submissions', 'side')

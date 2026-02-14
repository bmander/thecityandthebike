"""add ring columns to tags

Revision ID: j2e6f0a45678
Revises: i1d5e9f34567
Create Date: 2026-02-13 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'j2e6f0a45678'
down_revision: Union[str, Sequence[str], None] = 'i1d5e9f34567'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Add ring, ring_width, ring_height columns to tags table."""
    op.add_column('tags', sa.Column('ring', sa.JSON(), nullable=True))
    op.add_column('tags', sa.Column('ring_width', sa.Integer(), nullable=True))
    op.add_column('tags', sa.Column('ring_height', sa.Integer(), nullable=True))


def downgrade() -> None:
    """Remove ring columns from tags table."""
    op.drop_column('tags', 'ring_height')
    op.drop_column('tags', 'ring_width')
    op.drop_column('tags', 'ring')

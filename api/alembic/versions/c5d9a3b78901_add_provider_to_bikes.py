"""add provider to bikes

Revision ID: c5d9a3b78901
Revises: b4e8f2a67890
Create Date: 2026-02-08 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'c5d9a3b78901'
down_revision: Union[str, Sequence[str], None] = 'b4e8f2a67890'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Add provider column to bikes table."""
    op.add_column('bikes', sa.Column('provider', sa.String(50), nullable=True))


def downgrade() -> None:
    """Remove provider column from bikes table."""
    op.drop_column('bikes', 'provider')

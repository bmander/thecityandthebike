"""add thumbnail url to submissions

Revision ID: a3f7c9e12345
Revises: 2058b44a05cc
Create Date: 2026-02-06 16:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'a3f7c9e12345'
down_revision: Union[str, Sequence[str], None] = '2058b44a05cc'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    op.add_column('fender_submissions', sa.Column('image_url_thumbnail', sa.Text(), nullable=True))


def downgrade() -> None:
    """Downgrade schema."""
    op.drop_column('fender_submissions', 'image_url_thumbnail')

"""deduplicate image url columns

Revision ID: g9b3c7d12345
Revises: f8a2b6c01234
Create Date: 2026-02-11 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'g9b3c7d12345'
down_revision: Union[str, Sequence[str], None] = 'a1b2c3d45678'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Rename image_url_original to image_url and drop image_url_processed."""
    op.alter_column('fender_submissions', 'image_url_original', new_column_name='image_url')
    op.drop_column('fender_submissions', 'image_url_processed')


def downgrade() -> None:
    """Restore image_url_original and image_url_processed columns."""
    op.alter_column('fender_submissions', 'image_url', new_column_name='image_url_original')
    op.add_column('fender_submissions', sa.Column('image_url_processed', sa.Text(), nullable=True))

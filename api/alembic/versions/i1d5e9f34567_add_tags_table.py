"""add tags table

Revision ID: i1d5e9f34567
Revises: h0c4d8e23456
Create Date: 2026-02-12 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'i1d5e9f34567'
down_revision: Union[str, Sequence[str], None] = 'h0c4d8e23456'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Create tags table."""
    op.create_table(
        'tags',
        sa.Column('tag_id', sa.Uuid(), nullable=False),
        sa.Column('submission_id', sa.Uuid(), sa.ForeignKey('fender_submissions.submission_id'), nullable=False),
        sa.Column('user_id', sa.Uuid(), sa.ForeignKey('users.user_id'), nullable=False),
        sa.Column('image_url', sa.Text(), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), nullable=True),
        sa.PrimaryKeyConstraint('tag_id'),
    )
    op.create_index('ix_tags_submission_id', 'tags', ['submission_id'])


def downgrade() -> None:
    """Drop tags table."""
    op.drop_index('ix_tags_submission_id', table_name='tags')
    op.drop_table('tags')

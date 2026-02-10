"""add indexes on fender_submissions

Revision ID: a1b2c3d45678
Revises: f8a2b6c01234
Create Date: 2026-02-09 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = 'a1b2c3d45678'
down_revision: Union[str, Sequence[str], None] = 'f8a2b6c01234'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_index('ix_fender_submissions_user_id', 'fender_submissions', ['user_id'])
    op.create_index('ix_fender_submissions_bike_id', 'fender_submissions', ['bike_id'])
    op.create_index('ix_fender_submissions_uploaded_at', 'fender_submissions', ['uploaded_at'])
    op.create_index(
        'ix_fender_submissions_user_id_uploaded_at',
        'fender_submissions',
        ['user_id', 'uploaded_at'],
    )


def downgrade() -> None:
    op.drop_index('ix_fender_submissions_user_id_uploaded_at', 'fender_submissions')
    op.drop_index('ix_fender_submissions_uploaded_at', 'fender_submissions')
    op.drop_index('ix_fender_submissions_bike_id', 'fender_submissions')
    op.drop_index('ix_fender_submissions_user_id', 'fender_submissions')

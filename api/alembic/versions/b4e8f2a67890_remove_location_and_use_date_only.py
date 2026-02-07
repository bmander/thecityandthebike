"""remove location and use date only

Revision ID: b4e8f2a67890
Revises: a3f7c9e12345
Create Date: 2026-02-07 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'b4e8f2a67890'
down_revision: Union[str, Sequence[str], None] = 'a3f7c9e12345'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Upgrade schema."""
    # Add captured_date column (nullable initially for data migration)
    op.add_column('fender_submissions', sa.Column('captured_date', sa.Date(), nullable=True))

    # Populate captured_date from captured_at using America/Los_Angeles timezone
    op.execute(
        "UPDATE fender_submissions "
        "SET captured_date = DATE(captured_at AT TIME ZONE 'America/Los_Angeles')"
    )

    # Make captured_date NOT NULL
    op.alter_column('fender_submissions', 'captured_date', nullable=False)

    # Drop old columns
    op.drop_column('fender_submissions', 'latitude')
    op.drop_column('fender_submissions', 'longitude')
    op.drop_column('fender_submissions', 'captured_at')


def downgrade() -> None:
    """Downgrade schema."""
    op.add_column('fender_submissions', sa.Column('captured_at', sa.DateTime(timezone=True), nullable=True))
    op.add_column('fender_submissions', sa.Column('longitude', sa.Float(), nullable=True))
    op.add_column('fender_submissions', sa.Column('latitude', sa.Float(), nullable=True))
    op.drop_column('fender_submissions', 'captured_date')

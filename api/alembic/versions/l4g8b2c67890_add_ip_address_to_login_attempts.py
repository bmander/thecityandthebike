"""add ip_address to login_attempts

Revision ID: l4g8b2c67890
Revises: k3f7a1b56789
Create Date: 2026-02-16 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'l4g8b2c67890'
down_revision: Union[str, Sequence[str], None] = 'k3f7a1b56789'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Add ip_address column and compound index to login_attempts."""
    op.add_column('login_attempts', sa.Column('ip_address', sa.String(45), nullable=False, server_default='unknown'))
    op.drop_index('ix_login_attempts_username', table_name='login_attempts')
    op.create_index('ix_login_attempts_username_ip', 'login_attempts', ['username', 'ip_address'])


def downgrade() -> None:
    """Remove ip_address column and restore original index."""
    op.drop_index('ix_login_attempts_username_ip', table_name='login_attempts')
    op.create_index('ix_login_attempts_username', 'login_attempts', ['username'])
    op.drop_column('login_attempts', 'ip_address')

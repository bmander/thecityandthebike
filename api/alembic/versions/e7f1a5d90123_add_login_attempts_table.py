"""add login_attempts table

Revision ID: e7f1a5d90123
Revises: d6e0f4c89012
Create Date: 2026-02-09 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'e7f1a5d90123'
down_revision: Union[str, Sequence[str], None] = 'd6e0f4c89012'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        'login_attempts',
        sa.Column('id', sa.Integer, primary_key=True, autoincrement=True),
        sa.Column('username', sa.String(255), nullable=False),
        sa.Column('attempted_at', sa.DateTime(timezone=True)),
    )
    op.create_index('ix_login_attempts_username', 'login_attempts', ['username'])


def downgrade() -> None:
    op.drop_index('ix_login_attempts_username', table_name='login_attempts')
    op.drop_table('login_attempts')

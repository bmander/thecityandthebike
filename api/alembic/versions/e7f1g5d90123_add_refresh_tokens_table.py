"""add refresh_tokens table

Revision ID: e7f1g5d90123
Revises: d6e0f4c89012
Create Date: 2026-02-09 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'e7f1g5d90123'
down_revision: Union[str, Sequence[str], None] = 'd6e0f4c89012'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        'refresh_tokens',
        sa.Column('id', sa.Integer, primary_key=True, autoincrement=True),
        sa.Column('token', sa.String(64), unique=True, index=True, nullable=False),
        sa.Column('user_id', sa.String(36), sa.ForeignKey('users.user_id'), nullable=False),
        sa.Column('expires_at', sa.DateTime(timezone=True), nullable=False),
        sa.Column('created_at', sa.DateTime(timezone=True), server_default=sa.func.now()),
    )


def downgrade() -> None:
    op.drop_table('refresh_tokens')

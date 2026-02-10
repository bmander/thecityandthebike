"""use native uuid columns

Revision ID: f8a2b6c01234
Revises: e7f1g5d90123
Create Date: 2026-02-09 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'f8a2b6c01234'
down_revision: Union[str, Sequence[str], None] = 'e7f1g5d90123'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    # Drop foreign keys first so we can alter the referenced columns
    op.drop_constraint(
        'fender_submissions_user_id_fkey', 'fender_submissions', type_='foreignkey'
    )
    op.drop_constraint(
        'refresh_tokens_user_id_fkey', 'refresh_tokens', type_='foreignkey'
    )

    # Convert users.user_id from VARCHAR(36) to native UUID
    op.alter_column(
        'users', 'user_id',
        type_=sa.Uuid(),
        existing_type=sa.String(36),
        postgresql_using='user_id::uuid',
        server_default=sa.text('gen_random_uuid()'),
    )

    # Convert fender_submissions.submission_id from VARCHAR(36) to native UUID
    op.alter_column(
        'fender_submissions', 'submission_id',
        type_=sa.Uuid(),
        existing_type=sa.String(36),
        postgresql_using='submission_id::uuid',
        server_default=sa.text('gen_random_uuid()'),
    )

    # Convert fender_submissions.user_id from VARCHAR(36) to native UUID
    op.alter_column(
        'fender_submissions', 'user_id',
        type_=sa.Uuid(),
        existing_type=sa.String(36),
        postgresql_using='user_id::uuid',
    )

    # Convert refresh_tokens.user_id from VARCHAR(36) to native UUID
    op.alter_column(
        'refresh_tokens', 'user_id',
        type_=sa.Uuid(),
        existing_type=sa.String(36),
        postgresql_using='user_id::uuid',
    )

    # Re-create foreign keys
    op.create_foreign_key(
        'fender_submissions_user_id_fkey',
        'fender_submissions', 'users',
        ['user_id'], ['user_id'],
    )
    op.create_foreign_key(
        'refresh_tokens_user_id_fkey',
        'refresh_tokens', 'users',
        ['user_id'], ['user_id'],
    )


def downgrade() -> None:
    # Drop foreign keys
    op.drop_constraint(
        'fender_submissions_user_id_fkey', 'fender_submissions', type_='foreignkey'
    )
    op.drop_constraint(
        'refresh_tokens_user_id_fkey', 'refresh_tokens', type_='foreignkey'
    )

    # Revert users.user_id to VARCHAR(36)
    op.alter_column(
        'users', 'user_id',
        type_=sa.String(36),
        existing_type=sa.Uuid(),
        postgresql_using='user_id::text',
        server_default=None,
    )

    # Revert fender_submissions.submission_id to VARCHAR(36)
    op.alter_column(
        'fender_submissions', 'submission_id',
        type_=sa.String(36),
        existing_type=sa.Uuid(),
        postgresql_using='submission_id::text',
        server_default=None,
    )

    # Revert fender_submissions.user_id to VARCHAR(36)
    op.alter_column(
        'fender_submissions', 'user_id',
        type_=sa.String(36),
        existing_type=sa.Uuid(),
        postgresql_using='user_id::text',
    )

    # Revert refresh_tokens.user_id to VARCHAR(36)
    op.alter_column(
        'refresh_tokens', 'user_id',
        type_=sa.String(36),
        existing_type=sa.Uuid(),
        postgresql_using='user_id::text',
    )

    # Re-create foreign keys
    op.create_foreign_key(
        'fender_submissions_user_id_fkey',
        'fender_submissions', 'users',
        ['user_id'], ['user_id'],
    )
    op.create_foreign_key(
        'refresh_tokens_user_id_fkey',
        'refresh_tokens', 'users',
        ['user_id'], ['user_id'],
    )

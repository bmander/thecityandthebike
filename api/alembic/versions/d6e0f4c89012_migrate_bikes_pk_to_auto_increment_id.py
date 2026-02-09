"""migrate bikes pk to auto-increment id

Revision ID: d6e0f4c89012
Revises: c5d9a3b78901
Create Date: 2026-02-08 00:00:00.000000

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = 'd6e0f4c89012'
down_revision: Union[str, Sequence[str], None] = 'c5d9a3b78901'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Migrate bikes PK from bike_qr_id to auto-increment integer id."""

    # 1. Drop FK constraint on fender_submissions.bike_qr_id
    op.drop_constraint(
        'fender_submissions_bike_qr_id_fkey',
        'fender_submissions',
        type_='foreignkey',
    )

    # 2. Parse existing URL bike_qr_id values in both tables
    # Lime URLs: https://lime.bike/bc/v1/G5EZAYI= -> strip trailing '=' padding
    op.execute("""
        UPDATE bikes
        SET bike_qr_id = RTRIM(SUBSTRING(bike_qr_id FROM '.*/([^/]+)$'), '='),
            provider = 'lime'
        WHERE bike_qr_id LIKE 'https://lime.bike/%'
    """)
    op.execute("""
        UPDATE fender_submissions
        SET bike_qr_id = RTRIM(SUBSTRING(bike_qr_id FROM '.*/([^/]+)$'), '=')
        WHERE bike_qr_id LIKE 'https://lime.bike/%'
    """)

    # Bird URLs: https://ride.bird.co/bc/v1/abc123 -> last segment
    op.execute("""
        UPDATE bikes
        SET bike_qr_id = SUBSTRING(bike_qr_id FROM '.*/([^/]+)$'),
            provider = 'bird'
        WHERE bike_qr_id LIKE 'https://ride.bird.co/%'
    """)
    op.execute("""
        UPDATE fender_submissions
        SET bike_qr_id = SUBSTRING(bike_qr_id FROM '.*/([^/]+)$')
        WHERE bike_qr_id LIKE 'https://ride.bird.co/%'
    """)

    # 3. Add id SERIAL column to bikes (PG backfills existing rows)
    op.add_column('bikes', sa.Column('id', sa.Integer, autoincrement=True))
    op.execute("CREATE SEQUENCE IF NOT EXISTS bikes_id_seq OWNED BY bikes.id")
    op.execute("ALTER TABLE bikes ALTER COLUMN id SET DEFAULT nextval('bikes_id_seq')")
    op.execute("UPDATE bikes SET id = nextval('bikes_id_seq') WHERE id IS NULL")

    # 4. Add bike_id INTEGER column to fender_submissions
    op.add_column('fender_submissions', sa.Column('bike_id', sa.Integer))

    # 5. Populate bike_id via JOIN on bike_qr_id
    op.execute("""
        UPDATE fender_submissions fs
        SET bike_id = b.id
        FROM bikes b
        WHERE fs.bike_qr_id = b.bike_qr_id
    """)

    # 6. Make bike_id NOT NULL
    op.alter_column('fender_submissions', 'bike_id', nullable=False)

    # 7. Drop old PK, create new PK on bikes.id
    op.drop_constraint('bikes_pkey', 'bikes', type_='primary')
    op.create_primary_key('bikes_pkey', 'bikes', ['id'])

    # 8. Add unique constraint on bikes.bike_qr_id
    op.create_unique_constraint('uq_bikes_bike_qr_id', 'bikes', ['bike_qr_id'])

    # 9. Add FK fender_submissions.bike_id -> bikes.id
    op.create_foreign_key(
        'fender_submissions_bike_id_fkey',
        'fender_submissions',
        'bikes',
        ['bike_id'],
        ['id'],
    )

    # 10. Drop fender_submissions.bike_qr_id column
    op.drop_column('fender_submissions', 'bike_qr_id')


def downgrade() -> None:
    """Revert to bike_qr_id as PK."""

    # Re-add bike_qr_id column to fender_submissions
    op.add_column('fender_submissions', sa.Column('bike_qr_id', sa.String(255)))

    # Populate bike_qr_id from bikes via bike_id
    op.execute("""
        UPDATE fender_submissions fs
        SET bike_qr_id = b.bike_qr_id
        FROM bikes b
        WHERE fs.bike_id = b.id
    """)
    op.alter_column('fender_submissions', 'bike_qr_id', nullable=False)

    # Drop FK on bike_id
    op.drop_constraint('fender_submissions_bike_id_fkey', 'fender_submissions', type_='foreignkey')

    # Drop bike_id column
    op.drop_column('fender_submissions', 'bike_id')

    # Drop unique constraint on bike_qr_id
    op.drop_constraint('uq_bikes_bike_qr_id', 'bikes', type_='unique')

    # Drop new PK, restore old PK on bike_qr_id
    op.drop_constraint('bikes_pkey', 'bikes', type_='primary')
    op.create_primary_key('bikes_pkey', 'bikes', ['bike_qr_id'])

    # Drop id column and sequence
    op.drop_column('bikes', 'id')
    op.execute("DROP SEQUENCE IF EXISTS bikes_id_seq")

    # Re-add FK on fender_submissions.bike_qr_id
    op.create_foreign_key(
        'fender_submissions_bike_qr_id_fkey',
        'fender_submissions',
        'bikes',
        ['bike_qr_id'],
        ['bike_qr_id'],
    )

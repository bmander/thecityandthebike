"""Backfill thumbnails for existing submissions that have an image but no thumbnail.

Reads original images from storage (GCS or local), generates 300px thumbnails,
saves them alongside the originals, and updates the database.

Usage:
    cd api && source venv/bin/activate && python backfill_thumbnails.py
"""

import logging
import os
import sys

from sqlalchemy import create_engine, text
from sqlalchemy.orm import Session

# Add parent to path so we can import app modules
sys.path.insert(0, os.path.dirname(__file__))

from app.config import settings
from app.services.storage import (
    generate_thumbnail,
    resolve_image_url,
    retrieve_image,
    store_image,
)

logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)


def backfill():
    engine = create_engine(settings.DATABASE_URL)

    with Session(engine) as session:
        rows = session.execute(
            text(
                "SELECT submission_id, image_url FROM fender_submissions "
                "WHERE image_url IS NOT NULL AND image_url_thumbnail IS NULL"
            )
        ).fetchall()

        logger.info("Found %d submissions to backfill", len(rows))

        updated = 0
        for submission_id, image_url in rows:
            filename = image_url.rsplit("/", 1)[-1]
            file_id = os.path.splitext(filename)[0]
            thumb_filename = f"thumb_{file_id}.jpg"

            try:
                # Read the original image
                contents = retrieve_image(filename)
                if contents is None:
                    logger.warning("Missing file for %s, skipping", submission_id)
                    continue

                # Generate thumbnail
                thumb_bytes = generate_thumbnail(contents)
                if thumb_bytes is None:
                    logger.warning("Failed to generate thumbnail for %s", submission_id)
                    continue

                # Save thumbnail
                store_image(thumb_bytes, thumb_filename, "image/jpeg")

                # Update database
                thumbnail_url = resolve_image_url(thumb_filename)
                session.execute(
                    text(
                        "UPDATE fender_submissions SET image_url_thumbnail = :url "
                        "WHERE submission_id = :sid"
                    ),
                    {"url": thumbnail_url, "sid": submission_id},
                )
                session.commit()
                updated += 1
                logger.info("Backfilled thumbnail for %s", submission_id)

            except Exception:
                logger.exception("Error processing submission %s", submission_id)
                session.rollback()

        logger.info("Done. Updated %d / %d submissions", updated, len(rows))


if __name__ == "__main__":
    backfill()

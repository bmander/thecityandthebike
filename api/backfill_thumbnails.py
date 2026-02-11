"""Backfill thumbnails for existing submissions that have an image but no thumbnail.

Reads original images from storage (GCS or local), generates 300px thumbnails,
saves them alongside the originals, and updates the database.

Usage:
    cd api && source venv/bin/activate && python backfill_thumbnails.py
"""

import logging
import os
import sys

from sqlalchemy import create_engine, select, text
from sqlalchemy.orm import Session

# Add parent to path so we can import app modules
sys.path.insert(0, os.path.dirname(__file__))

from app.config import settings
from app.routers.uploads import (
    UPLOAD_DIR,
    _generate_thumbnail,
    _get_gcs_bucket,
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

        if settings.STORAGE_BUCKET:
            bucket = _get_gcs_bucket()

        updated = 0
        for submission_id, image_url in rows:
            filename = image_url.rsplit("/", 1)[-1]
            file_id = os.path.splitext(filename)[0]
            thumb_filename = f"thumb_{file_id}.jpg"

            try:
                # Read the original image
                if settings.STORAGE_BUCKET:
                    blob = bucket.blob(f"images/{filename}")
                    if not blob.exists():
                        logger.warning("Missing GCS blob for %s, skipping", submission_id)
                        continue
                    contents = blob.download_as_bytes()
                else:
                    file_path = os.path.join(UPLOAD_DIR, "images", filename)
                    if not os.path.isfile(file_path):
                        logger.warning("Missing local file for %s, skipping", submission_id)
                        continue
                    with open(file_path, "rb") as f:
                        contents = f.read()

                # Generate thumbnail
                thumb_bytes = _generate_thumbnail(contents)
                if thumb_bytes is None:
                    logger.warning("Failed to generate thumbnail for %s", submission_id)
                    continue

                # Save thumbnail
                if settings.STORAGE_BUCKET:
                    thumb_blob = bucket.blob(f"images/{thumb_filename}")
                    thumb_blob.upload_from_string(thumb_bytes, content_type="image/jpeg")
                else:
                    thumb_path = os.path.join(UPLOAD_DIR, "images", thumb_filename)
                    with open(thumb_path, "wb") as f:
                        f.write(thumb_bytes)

                # Update database
                thumbnail_url = f"/uploads/images/{thumb_filename}"
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

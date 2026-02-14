import datetime
import io
import logging
import os
import uuid
from typing import Optional

from PIL import Image

from ..config import settings

logger = logging.getLogger(__name__)

ALLOWED_EXTENSIONS = {".jpg", ".jpeg"}
ALLOWED_SERVE_EXTENSIONS = {".jpg", ".jpeg", ".png"}
ALLOWED_TAG_EXTENSIONS = {".png"}
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10 MB
CHUNK_SIZE = 64 * 1024  # 64 KB
THUMBNAIL_MAX_SIZE = 300

# Local upload dir (used when STORAGE_BUCKET is not set)
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "uploads")
if not settings.STORAGE_BUCKET:
    os.makedirs(os.path.join(UPLOAD_DIR, "images"), exist_ok=True)


def _get_gcs_bucket():
    from google.cloud import storage
    client = storage.Client()
    return client.bucket(settings.STORAGE_BUCKET)


def validate_image(contents: bytes) -> None:
    """Open and verify image bytes with PIL. Raises ValueError on invalid data."""
    try:
        img = Image.open(io.BytesIO(contents))
        img.verify()
    except Exception as exc:
        raise ValueError("File is not a valid image") from exc


def reencode_jpeg(contents: bytes) -> bytes:
    """Re-encode image bytes as JPEG quality 90, stripping EXIF metadata."""
    with Image.open(io.BytesIO(contents)) as img:
        if img.mode in ("RGBA", "P"):
            img = img.convert("RGB")
        buf = io.BytesIO()
        img.save(buf, format="JPEG", quality=90)
        return buf.getvalue()


def reencode_png(contents: bytes) -> bytes:
    """Re-encode image bytes as PNG to sanitize."""
    with Image.open(io.BytesIO(contents)) as img:
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        return buf.getvalue()


def generate_thumbnail(contents: bytes) -> Optional[bytes]:
    """Generate a JPEG thumbnail with max dimension of THUMBNAIL_MAX_SIZE pixels."""
    try:
        with Image.open(io.BytesIO(contents)) as img:
            img.thumbnail((THUMBNAIL_MAX_SIZE, THUMBNAIL_MAX_SIZE))
            if img.mode in ("RGBA", "P"):
                img = img.convert("RGB")
            buf = io.BytesIO()
            img.save(buf, format="JPEG", quality=85)
            return buf.getvalue()
    except Exception:
        logger.warning("Failed to generate thumbnail", exc_info=True)
        return None


def store_image(contents: bytes, filename: str, content_type: str) -> None:
    """Store image bytes to GCS or local filesystem."""
    if settings.STORAGE_BUCKET:
        bucket = _get_gcs_bucket()
        blob = bucket.blob(f"images/{filename}")
        blob.upload_from_string(contents, content_type=content_type)
    else:
        file_path = os.path.join(UPLOAD_DIR, "images", filename)
        with open(file_path, "wb") as f:
            f.write(contents)


def retrieve_image(filename: str) -> Optional[bytes]:
    """Retrieve image bytes from GCS or local filesystem. Returns None if not found."""
    if settings.STORAGE_BUCKET:
        bucket = _get_gcs_bucket()
        blob = bucket.blob(f"images/{filename}")
        if not blob.exists():
            return None
        return blob.download_as_bytes()
    else:
        file_path = os.path.join(UPLOAD_DIR, "images", filename)
        if not os.path.isfile(file_path):
            return None
        with open(file_path, "rb") as f:
            return f.read()


def image_exists(filename: str) -> bool:
    """Check whether an image exists in storage."""
    if settings.STORAGE_BUCKET:
        bucket = _get_gcs_bucket()
        blob = bucket.blob(f"images/{filename}")
        return blob.exists()
    else:
        file_path = os.path.join(UPLOAD_DIR, "images", filename)
        return os.path.isfile(file_path)


def resolve_image_url(filename: str) -> str:
    """Return the URL path for an image filename."""
    return f"/uploads/images/{filename}"


def store_tag_image(contents: bytes, ext: str) -> str:
    """Save a tag image to storage and return its URL path."""
    file_id = str(uuid.uuid4())
    unique_filename = f"{file_id}{ext}"
    store_image(contents, unique_filename, "image/png")
    return resolve_image_url(unique_filename)


def generate_signed_url(filename: str) -> str:
    """Generate a V4 signed URL for a GCS blob with configurable expiration."""
    bucket = _get_gcs_bucket()
    blob = bucket.blob(f"images/{filename}")
    return blob.generate_signed_url(
        version="v4",
        expiration=datetime.timedelta(seconds=settings.SIGNED_URL_EXPIRATION),
        method="GET",
    )


def delete_image(url: Optional[str]) -> None:
    """Delete an image file from storage given its URL path (e.g. /uploads/images/abc.jpg).

    Logs warnings on failure but does not raise — file deletion should not block DB deletion.
    """
    if url is None:
        return

    filename = url.rsplit("/", 1)[-1]

    if settings.STORAGE_BUCKET:
        try:
            bucket = _get_gcs_bucket()
            blob = bucket.blob(f"images/{filename}")
            blob.delete()
        except Exception:
            logger.warning("Failed to delete GCS blob images/%s", filename, exc_info=True)
    else:
        try:
            file_path = os.path.join(UPLOAD_DIR, "images", filename)
            if os.path.isfile(file_path):
                os.remove(file_path)
        except Exception:
            logger.warning("Failed to delete local file images/%s", filename, exc_info=True)

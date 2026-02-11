import io
import logging
import mimetypes
import os
import uuid
from typing import Annotated, Optional

from fastapi import APIRouter, Depends, UploadFile, File, HTTPException, status
from fastapi.responses import FileResponse, Response
from PIL import Image
from pydantic import BaseModel

from ..config import settings
from ..dependencies import get_current_user
from ..models import User

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/uploads", tags=["uploads"])

ALLOWED_EXTENSIONS = {".jpg", ".jpeg"}
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


def _generate_thumbnail(contents: bytes) -> Optional[bytes]:
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


class UploadResponse(BaseModel):
    url: str
    filename: str
    thumbnail_url: Optional[str] = None


@router.post("/images", response_model=UploadResponse, status_code=status.HTTP_201_CREATED)
async def upload_image(
    current_user: Annotated[User, Depends(get_current_user)],
    image: UploadFile = File(...),
):
    # Validate file extension
    ext = os.path.splitext(image.filename or "")[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": f"File type not allowed. Allowed types: {', '.join(ALLOWED_EXTENSIONS)}"},
        )

    # Read in chunks, abort early if file exceeds max size
    chunks = []
    total = 0
    while True:
        chunk = await image.read(CHUNK_SIZE)
        if not chunk:
            break
        total += len(chunk)
        if total > MAX_FILE_SIZE:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"msg": f"File too large. Maximum size: {MAX_FILE_SIZE // (1024 * 1024)} MB"},
            )
        chunks.append(chunk)
    contents = b"".join(chunks)

    # Validate image content
    try:
        img = Image.open(io.BytesIO(contents))
        img.verify()
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": "File is not a valid image"},
        )

    # Re-encode as JPEG (strips EXIF, neutralizes any embedded payloads)
    try:
        with Image.open(io.BytesIO(contents)) as img:
            if img.mode in ("RGBA", "P"):
                img = img.convert("RGB")
            clean = io.BytesIO()
            img.save(clean, format="JPEG", quality=90)
            contents = clean.getvalue()
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": "File is not a valid image"},
        )

    # Generate unique filename
    file_id = str(uuid.uuid4())
    unique_filename = f"{file_id}{ext}"
    thumb_filename = f"thumb_{file_id}.jpg"

    # Generate thumbnail
    thumb_bytes = _generate_thumbnail(contents)

    if settings.STORAGE_BUCKET:
        # Upload to Google Cloud Storage
        bucket = _get_gcs_bucket()
        blob = bucket.blob(f"images/{unique_filename}")
        blob.upload_from_string(contents, content_type="image/jpeg")
        if thumb_bytes:
            thumb_blob = bucket.blob(f"images/{thumb_filename}")
            thumb_blob.upload_from_string(thumb_bytes, content_type="image/jpeg")
    else:
        # Save locally (development)
        file_path = os.path.join(UPLOAD_DIR, "images", unique_filename)
        with open(file_path, "wb") as f:
            f.write(contents)
        if thumb_bytes:
            thumb_path = os.path.join(UPLOAD_DIR, "images", thumb_filename)
            with open(thumb_path, "wb") as f:
                f.write(thumb_bytes)

    url = f"/uploads/images/{unique_filename}"
    thumbnail_url = f"/uploads/images/{thumb_filename}" if thumb_bytes else None
    return UploadResponse(url=url, filename=unique_filename, thumbnail_url=thumbnail_url)


@router.get("/images/{filename}")
async def get_image(filename: str):
    ext = os.path.splitext(filename)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")

    if ".." in filename or "/" in filename or "\\" in filename:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")

    if settings.STORAGE_BUCKET:
        bucket = _get_gcs_bucket()
        blob = bucket.blob(f"images/{filename}")
        if not blob.exists():
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")
        content = blob.download_as_bytes()
        content_type = mimetypes.guess_type(filename)[0] or "application/octet-stream"
        return Response(content=content, media_type=content_type)
    else:
        file_path = os.path.join(UPLOAD_DIR, "images", filename)
        if not os.path.isfile(file_path):
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")
        return FileResponse(file_path)

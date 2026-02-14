import asyncio
import os
import uuid
from typing import Annotated, Optional

from fastapi import APIRouter, Depends, UploadFile, File, HTTPException, status
from fastapi.responses import FileResponse, RedirectResponse
from pydantic import BaseModel

from ..config import settings
from ..dependencies import get_current_user
from ..models import User
from ..services import storage as storage_service
from ..services.storage import (
    ALLOWED_EXTENSIONS,
    ALLOWED_SERVE_EXTENSIONS,
    CHUNK_SIZE,
    MAX_FILE_SIZE,
    generate_signed_url,
    generate_thumbnail,
    image_exists,
    reencode_jpeg,
    resolve_image_url,
    store_image,
    validate_image,
)

router = APIRouter(prefix="/uploads", tags=["uploads"])


class UploadResponse(BaseModel):
    url: str
    filename: str
    thumbnail_url: Optional[str] = None


async def process_and_store_image(image: UploadFile) -> tuple[str, str | None]:
    """Validate, re-encode, store an uploaded image and generate a thumbnail.

    Returns (image_url, thumbnail_url). Raises HTTPException on failure.
    """
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
        await asyncio.to_thread(validate_image, contents)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": "File is not a valid image"},
        )

    # Re-encode as JPEG (strips EXIF, neutralizes any embedded payloads)
    try:
        contents = await asyncio.to_thread(reencode_jpeg, contents)
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
    thumb_bytes = await asyncio.to_thread(generate_thumbnail, contents)

    # Store original image
    await asyncio.to_thread(store_image, contents, unique_filename, "image/jpeg")

    # Store thumbnail
    if thumb_bytes:
        await asyncio.to_thread(store_image, thumb_bytes, thumb_filename, "image/jpeg")

    url = resolve_image_url(unique_filename)
    thumbnail_url = resolve_image_url(thumb_filename) if thumb_bytes else None
    return url, thumbnail_url


@router.post("/images", response_model=UploadResponse, status_code=status.HTTP_201_CREATED)
async def upload_image(
    current_user: Annotated[User, Depends(get_current_user)],
    image: UploadFile = File(...),
):
    url, thumbnail_url = await process_and_store_image(image)
    filename = url.rsplit("/", 1)[-1]
    return UploadResponse(url=url, filename=filename, thumbnail_url=thumbnail_url)


@router.get("/images/{filename}")
async def get_image(filename: str):
    ext = os.path.splitext(filename)[1].lower()
    if ext not in ALLOWED_SERVE_EXTENSIONS:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")

    if ".." in filename or "/" in filename or "\\" in filename:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")

    if settings.STORAGE_BUCKET:
        exists = await asyncio.to_thread(image_exists, filename)
        if not exists:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")
        signed_url = await asyncio.to_thread(generate_signed_url, filename)
        return RedirectResponse(url=signed_url, status_code=307)
    else:
        file_path = os.path.join(storage_service.UPLOAD_DIR, "images", filename)
        if not os.path.isfile(file_path):
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Not found")
        return FileResponse(file_path)

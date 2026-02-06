import mimetypes
import os
import uuid
from typing import Annotated

from fastapi import APIRouter, Depends, UploadFile, File, HTTPException, status
from fastapi.responses import FileResponse, Response
from pydantic import BaseModel

from ..config import settings
from ..dependencies import get_current_user
from ..models import User

router = APIRouter(prefix="/uploads", tags=["uploads"])

ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp"}
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10 MB

# Local upload dir (used when STORAGE_BUCKET is not set)
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "uploads")
os.makedirs(os.path.join(UPLOAD_DIR, "images"), exist_ok=True)


def _get_gcs_bucket():
    from google.cloud import storage
    client = storage.Client()
    return client.bucket(settings.STORAGE_BUCKET)


class UploadResponse(BaseModel):
    url: str
    filename: str


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

    # Read and validate file size
    contents = await image.read()
    if len(contents) > MAX_FILE_SIZE:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": f"File too large. Maximum size: {MAX_FILE_SIZE // (1024 * 1024)} MB"},
        )

    # Generate unique filename
    unique_filename = f"{uuid.uuid4()}{ext}"

    if settings.STORAGE_BUCKET:
        # Upload to Google Cloud Storage
        bucket = _get_gcs_bucket()
        blob = bucket.blob(f"images/{unique_filename}")
        blob.upload_from_string(contents, content_type=image.content_type)
    else:
        # Save locally (development)
        file_path = os.path.join(UPLOAD_DIR, "images", unique_filename)
        with open(file_path, "wb") as f:
            f.write(contents)

    url = f"/uploads/images/{unique_filename}"
    return UploadResponse(url=url, filename=unique_filename)


@router.get("/images/{filename}")
async def get_image(filename: str):
    ext = os.path.splitext(filename)[1].lower()
    if ext not in ALLOWED_EXTENSIONS:
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

import io
import os
import uuid as uuid_mod
from typing import Annotated, List
from uuid import UUID

import json

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from PIL import Image
from sqlalchemy.orm import Session

from ..config import settings
from ..database import get_db
from ..dependencies import get_current_user
from ..models import User, FenderSubmission, Tag
from ..schemas.tag import TagResponse
from ..schemas.mask import ProcessedMaskResponse
from ..schemas.auth import MessageResponse
from ..services.mask_processing import process_mask_to_ring
from .uploads import delete_stored_image, UPLOAD_DIR, MAX_FILE_SIZE, CHUNK_SIZE

router = APIRouter(tags=["tags"])

ALLOWED_TAG_EXTENSIONS = {".png"}


def _save_tag_image(contents: bytes, ext: str) -> str:
    """Save a tag image to storage and return its URL path."""
    file_id = str(uuid_mod.uuid4())
    unique_filename = f"{file_id}{ext}"

    if settings.STORAGE_BUCKET:
        from .uploads import _get_gcs_bucket
        bucket = _get_gcs_bucket()
        blob = bucket.blob(f"images/{unique_filename}")
        blob.upload_from_string(contents, content_type="image/png")
    else:
        os.makedirs(os.path.join(UPLOAD_DIR, "images"), exist_ok=True)
        file_path = os.path.join(UPLOAD_DIR, "images", unique_filename)
        with open(file_path, "wb") as f:
            f.write(contents)

    return f"/uploads/images/{unique_filename}"


@router.get(
    "/submissions/{submission_id}/tags",
    response_model=List[TagResponse],
)
def list_tags(submission_id: UUID, db: Annotated[Session, Depends(get_db)]):
    submission = (
        db.query(FenderSubmission)
        .filter(FenderSubmission.submission_id == submission_id)
        .first()
    )
    if submission is None:
        raise HTTPException(status_code=404, detail="Submission not found")
    tags = (
        db.query(Tag)
        .filter(Tag.submission_id == submission_id)
        .order_by(Tag.created_at.desc())
        .all()
    )
    return tags


@router.post(
    "/submissions/{submission_id}/tags",
    response_model=TagResponse,
    status_code=status.HTTP_201_CREATED,
)
async def create_tag(
    submission_id: UUID,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    image: UploadFile = File(...),
    ring: str | None = Form(None),
    ring_width: int | None = Form(None),
    ring_height: int | None = Form(None),
):
    # Verify submission exists
    submission = (
        db.query(FenderSubmission)
        .filter(FenderSubmission.submission_id == submission_id)
        .first()
    )
    if submission is None:
        raise HTTPException(status_code=404, detail="Submission not found")

    # Validate file extension
    ext = os.path.splitext(image.filename or "")[1].lower()
    if ext not in ALLOWED_TAG_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": f"File type not allowed. Allowed types: {', '.join(ALLOWED_TAG_EXTENSIONS)}"},
        )

    # Read file contents with size limit
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

    # Re-encode as PNG to sanitize
    try:
        with Image.open(io.BytesIO(contents)) as img:
            clean = io.BytesIO()
            img.save(clean, format="PNG")
            contents = clean.getvalue()
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": "File is not a valid image"},
        )

    # Parse ring JSON if provided
    parsed_ring = None
    if ring is not None:
        try:
            parsed_ring = json.loads(ring)
        except (json.JSONDecodeError, TypeError):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"msg": "Invalid ring JSON"},
            )

    # Save the image
    image_url = _save_tag_image(contents, ext)

    # Create tag record
    tag = Tag(
        submission_id=submission_id,
        user_id=current_user.user_id,
        image_url=image_url,
        ring=parsed_ring,
        ring_width=ring_width,
        ring_height=ring_height,
    )
    db.add(tag)
    db.commit()
    db.refresh(tag)
    return tag


@router.post(
    "/submissions/{submission_id}/process-mask",
    response_model=ProcessedMaskResponse,
)
async def process_mask(
    submission_id: UUID,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
    image: UploadFile = File(...),
):
    # Verify submission exists
    submission = (
        db.query(FenderSubmission)
        .filter(FenderSubmission.submission_id == submission_id)
        .first()
    )
    if submission is None:
        raise HTTPException(status_code=404, detail="Submission not found")

    # Validate file extension
    ext = os.path.splitext(image.filename or "")[1].lower()
    if ext not in ALLOWED_TAG_EXTENSIONS:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": f"File type not allowed. Allowed types: {', '.join(ALLOWED_TAG_EXTENSIONS)}"},
        )

    # Read file contents with size limit
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

    # Process mask to ring geometry
    try:
        result = process_mask_to_ring(contents)
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail={"msg": str(e)},
        )

    return ProcessedMaskResponse(**result)


@router.delete(
    "/tags/{tag_id}",
    response_model=MessageResponse,
)
def delete_tag(
    tag_id: UUID,
    current_user: Annotated[User, Depends(get_current_user)],
    db: Annotated[Session, Depends(get_db)],
):
    tag = db.query(Tag).filter(Tag.tag_id == tag_id).first()
    if tag is None:
        raise HTTPException(status_code=404, detail="Tag not found")
    if tag.user_id != current_user.user_id:
        raise HTTPException(status_code=403, detail="Not authorized to delete this tag")

    image_url = tag.image_url
    db.delete(tag)
    db.commit()
    delete_stored_image(image_url)
    return MessageResponse(msg="Tag deleted")

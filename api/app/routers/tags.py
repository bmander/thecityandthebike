import asyncio
import os
from typing import Annotated, List
from uuid import UUID

import json

from fastapi import APIRouter, Depends, File, Form, HTTPException, UploadFile, status
from sqlalchemy.orm import Session

from ..database import get_db
from ..dependencies import get_current_user
from ..models import User, FenderSubmission, Tag
from ..schemas.tag import TagResponse
from ..schemas.auth import MessageResponse
from ..services.storage import (
    ALLOWED_TAG_EXTENSIONS,
    CHUNK_SIZE,
    MAX_FILE_SIZE,
    delete_image,
    reencode_png,
    store_tag_image,
    validate_image,
)

router = APIRouter(tags=["tags"])


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
        await asyncio.to_thread(validate_image, contents)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": "File is not a valid image"},
        )

    # Re-encode as PNG to sanitize
    try:
        contents = await asyncio.to_thread(reencode_png, contents)
    except Exception:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"msg": "File is not a valid image"},
        )

    # Parse and validate ring JSON if provided
    parsed_ring = None
    if ring is not None:
        try:
            parsed_ring = json.loads(ring)
        except (json.JSONDecodeError, TypeError):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"msg": "Invalid ring JSON"},
            )
        # Validate shape: must be a list of [x, y] coordinate pairs
        if not isinstance(parsed_ring, list) or not all(
            isinstance(pt, list) and len(pt) == 2 and all(isinstance(v, (int, float)) for v in pt)
            for pt in parsed_ring
        ):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={"msg": "ring must be a list of [x, y] coordinate pairs"},
            )

    # Save the image
    image_url = await asyncio.to_thread(store_tag_image, contents, ext)

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
    delete_image(image_url)
    return MessageResponse(msg="Tag deleted")

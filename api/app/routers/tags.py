import asyncio
import os
from typing import Annotated, List, Optional
from uuid import UUID

import json

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from sqlalchemy import and_, or_
from sqlalchemy.orm import Session, joinedload

from ..cursor import decode_cursor, encode_cursor
from ..database import get_db
from ..dependencies import get_current_user
from ..models import User, FenderSubmission, Tag
from ..schemas.tag import TagDetailResponse, TagResponse
from ..schemas.submission import CursorPaginatedResponse, SubmissionResponse
from ..schemas.bike import UserSummary
from ..schemas.auth import MessageResponse
from ..services.scoring import award_tag_points, revoke_tag_points
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


@router.get(
    "/tags/{tag_id}",
    response_model=TagDetailResponse,
)
def get_tag_detail(tag_id: UUID, db: Annotated[Session, Depends(get_db)]):
    tag = db.query(Tag).filter(Tag.tag_id == tag_id).first()
    if tag is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"msg": "Tag not found"},
        )

    # Get all submissions that have this tag (currently one, but future-proof)
    submissions = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user))
        .filter(FenderSubmission.submission_id == tag.submission_id)
        .order_by(FenderSubmission.uploaded_at.asc())
        .all()
    )

    submission_count = len(submissions)
    first_submission = submissions[0] if submissions else None
    last_submission = submissions[-1] if submissions else None

    first_captured_by = (
        UserSummary(name=first_submission.user.username, id=first_submission.user.user_id)
        if first_submission else None
    )
    last_captured_by = (
        UserSummary(name=last_submission.user.username, id=last_submission.user.user_id)
        if last_submission else None
    )
    first_captured_at = first_submission.uploaded_at if first_submission else None
    last_captured_at = last_submission.uploaded_at if last_submission else None

    return TagDetailResponse(
        tag_id=tag.tag_id,
        image_url=tag.image_url,
        created_at=tag.created_at,
        submission_count=submission_count,
        first_captured_at=first_captured_at,
        last_captured_at=last_captured_at,
        first_captured_by=first_captured_by,
        last_captured_by=last_captured_by,
    )


@router.get(
    "/tags/{tag_id}/submissions",
    response_model=CursorPaginatedResponse[SubmissionResponse],
)
def get_tag_submissions(
    tag_id: UUID,
    db: Annotated[Session, Depends(get_db)],
    limit: int = Query(default=20, ge=1, le=100),
    cursor: Optional[str] = Query(default=None),
):
    tag = db.query(Tag).filter(Tag.tag_id == tag_id).first()
    if tag is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"msg": "Tag not found"},
        )

    query = (
        db.query(FenderSubmission)
        .options(joinedload(FenderSubmission.user), joinedload(FenderSubmission.bike))
        .filter(FenderSubmission.submission_id == tag.submission_id)
    )
    if cursor is not None:
        try:
            c = decode_cursor(cursor)
        except ValueError:
            raise HTTPException(status_code=422, detail="Invalid cursor")
        query = query.filter(
            or_(
                FenderSubmission.uploaded_at < c.uploaded_at,
                and_(
                    FenderSubmission.uploaded_at == c.uploaded_at,
                    FenderSubmission.submission_id < c.submission_id,
                ),
            )
        )
    submissions = (
        query.order_by(FenderSubmission.uploaded_at.desc(), FenderSubmission.submission_id.desc())
        .limit(limit + 1)
        .all()
    )
    has_more = len(submissions) > limit
    items = submissions[:limit]
    next_cursor = encode_cursor(items[-1].uploaded_at, items[-1].submission_id) if has_more else None
    return CursorPaginatedResponse(items=items, next_cursor=next_cursor, has_more=has_more)


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
    if submission.user_id != current_user.user_id:
        raise HTTPException(status_code=403, detail="Not authorized to create tags on this submission")

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
    points = award_tag_points(db, current_user.user_id, submission_id, tag)
    db.commit()
    response = TagResponse.model_validate(tag)
    response.points_awarded = points
    return response


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

    revoke_tag_points(db, tag.tag_id)
    image_url = tag.image_url
    db.delete(tag)
    db.commit()
    delete_image(image_url)
    return MessageResponse(msg="Tag deleted")

import base64
import json
from dataclasses import dataclass
from datetime import datetime, timezone
from uuid import UUID


@dataclass
class CursorData:
    uploaded_at: datetime
    submission_id: UUID


def encode_cursor(uploaded_at: datetime, submission_id: UUID) -> str:
    payload = {"t": uploaded_at.isoformat(), "id": str(submission_id)}
    return base64.urlsafe_b64encode(json.dumps(payload).encode()).decode()


def decode_cursor(cursor: str) -> CursorData:
    try:
        payload = json.loads(base64.urlsafe_b64decode(cursor))
        uploaded_at = datetime.fromisoformat(payload["t"])
        if uploaded_at.tzinfo is None:
            uploaded_at = uploaded_at.replace(tzinfo=timezone.utc)
        submission_id = UUID(payload["id"])
        return CursorData(uploaded_at=uploaded_at, submission_id=submission_id)
    except (json.JSONDecodeError, KeyError, ValueError, Exception) as exc:
        raise ValueError(f"Invalid cursor: {exc}") from exc

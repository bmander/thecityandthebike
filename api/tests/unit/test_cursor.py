from datetime import datetime, timezone
from uuid import UUID, uuid4

import pytest

from app.cursor import CursorData, decode_cursor, encode_cursor


class TestCursorRoundTrip:
    def test_round_trip(self):
        ts = datetime(2025, 6, 15, 12, 30, 45, tzinfo=timezone.utc)
        sid = uuid4()
        cursor = encode_cursor(ts, sid)
        result = decode_cursor(cursor)
        assert result.uploaded_at == ts
        assert result.submission_id == sid

    def test_round_trip_preserves_microseconds(self):
        ts = datetime(2025, 1, 1, 0, 0, 0, 123456, tzinfo=timezone.utc)
        sid = uuid4()
        cursor = encode_cursor(ts, sid)
        result = decode_cursor(cursor)
        assert result.uploaded_at == ts

    def test_decode_returns_cursor_data(self):
        ts = datetime(2025, 6, 15, 12, 0, 0, tzinfo=timezone.utc)
        sid = uuid4()
        cursor = encode_cursor(ts, sid)
        result = decode_cursor(cursor)
        assert isinstance(result, CursorData)
        assert isinstance(result.uploaded_at, datetime)
        assert isinstance(result.submission_id, UUID)


class TestDecodeCursorInvalid:
    def test_garbage_string(self):
        with pytest.raises(ValueError, match="Invalid cursor"):
            decode_cursor("not-valid-base64!!!")

    def test_valid_base64_but_not_json(self):
        import base64
        cursor = base64.urlsafe_b64encode(b"not json").decode()
        with pytest.raises(ValueError, match="Invalid cursor"):
            decode_cursor(cursor)

    def test_missing_keys(self):
        import base64
        import json
        cursor = base64.urlsafe_b64encode(json.dumps({"foo": "bar"}).encode()).decode()
        with pytest.raises(ValueError, match="Invalid cursor"):
            decode_cursor(cursor)

    def test_bad_timestamp(self):
        import base64
        import json
        cursor = base64.urlsafe_b64encode(
            json.dumps({"t": "not-a-date", "id": str(uuid4())}).encode()
        ).decode()
        with pytest.raises(ValueError, match="Invalid cursor"):
            decode_cursor(cursor)

    def test_bad_uuid(self):
        import base64
        import json
        cursor = base64.urlsafe_b64encode(
            json.dumps({"t": "2025-01-01T00:00:00+00:00", "id": "not-a-uuid"}).encode()
        ).decode()
        with pytest.raises(ValueError, match="Invalid cursor"):
            decode_cursor(cursor)

    def test_naive_datetime_gets_utc(self):
        """A cursor with a naive (no-tz) timestamp should decode with UTC attached."""
        import base64
        import json
        sid = uuid4()
        cursor = base64.urlsafe_b64encode(
            json.dumps({"t": "2025-06-15T12:30:45", "id": str(sid)}).encode()
        ).decode()
        result = decode_cursor(cursor)
        assert result.uploaded_at.tzinfo == timezone.utc
        assert result.uploaded_at == datetime(2025, 6, 15, 12, 30, 45, tzinfo=timezone.utc)
        assert result.submission_id == sid

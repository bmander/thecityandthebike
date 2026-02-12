from app.models.orm import FenderSubmission


class TestFenderSubmissionProperties:
    """Tests for FenderSubmission computed properties when relationships are None."""

    def _make_detached(self, **attrs):
        """Create a FenderSubmission without SQLAlchemy instrumentation."""
        obj = FenderSubmission.__new__(FenderSubmission)
        obj.__dict__.update(attrs)
        return obj

    def test_bike_qr_id_returns_none_when_bike_is_none(self):
        submission = self._make_detached(bike=None)
        assert submission.bike_qr_id is None

    def test_username_returns_none_when_user_is_none(self):
        submission = self._make_detached(user=None)
        assert submission.username is None

    def test_provider_returns_none_when_bike_is_none(self):
        submission = self._make_detached(bike=None)
        assert submission.provider is None

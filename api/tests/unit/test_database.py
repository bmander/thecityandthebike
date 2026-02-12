from unittest.mock import MagicMock, patch


class TestGetDb:
    """Tests for the get_db generator session lifecycle."""

    def test_session_closed_on_exception(self):
        mock_session = MagicMock()
        mock_factory = MagicMock(return_value=mock_session)

        with patch("app.database.SessionLocal", mock_factory):
            from app.database import get_db

            gen = get_db()
            db = next(gen)
            assert db is mock_session

            try:
                gen.throw(RuntimeError("boom"))
            except RuntimeError:
                pass

            mock_session.close.assert_called_once()

    def test_session_closed_on_normal_exit(self):
        mock_session = MagicMock()
        mock_factory = MagicMock(return_value=mock_session)

        with patch("app.database.SessionLocal", mock_factory):
            from app.database import get_db

            gen = get_db()
            next(gen)

            try:
                next(gen)
            except StopIteration:
                pass

            mock_session.close.assert_called_once()

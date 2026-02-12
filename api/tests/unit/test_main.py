import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.config import API_VERSION
from app.main import generic_exception_handler


def _make_error_app():
    """Build a minimal app with the production exception handler."""
    app = FastAPI()
    app.add_exception_handler(Exception, generic_exception_handler)

    @app.get("/explode")
    async def explode():
        raise RuntimeError("secret db connection string")

    @app.get("/admin/explode")
    async def admin_explode():
        raise RuntimeError("admin secret")

    return app


class TestGenericExceptionHandler:
    """Tests for the generic exception handler in main.py."""

    def test_500_returns_safe_message(self):
        """Non-admin endpoints should return a generic 500 with no internal details."""
        app = _make_error_app()
        with TestClient(app, raise_server_exceptions=False) as c:
            response = c.get("/explode")

        assert response.status_code == 500
        body = response.json()
        assert body == {"msg": "Internal server error"}
        assert "secret" not in response.text
        assert "db connection" not in response.text

    def test_admin_path_reraises(self):
        """Admin path exceptions should re-raise (not be caught)."""
        app = _make_error_app()
        with TestClient(app, raise_server_exceptions=True) as c:
            with pytest.raises(RuntimeError, match="admin secret"):
                c.get("/admin/explode")


class TestHealthEndpoint:
    """Tests for the /health endpoint."""

    def test_health_returns_expected_json(self):
        """Health endpoint should return status and current version."""
        from app.main import app

        with TestClient(app) as c:
            response = c.get("/health")
        assert response.status_code == 200
        body = response.json()
        assert body["status"] == "healthy"
        assert body["version"] == API_VERSION

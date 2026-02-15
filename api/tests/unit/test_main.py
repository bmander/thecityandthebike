import logging
import uuid

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.config import API_VERSION
from app.main import generic_exception_handler, RequestIDMiddleware


def _make_error_app():
    """Build a minimal app with the production exception handler and middleware."""
    app = FastAPI()
    app.add_middleware(RequestIDMiddleware)
    app.add_exception_handler(Exception, generic_exception_handler)

    @app.get("/explode")
    async def explode():
        raise RuntimeError("secret db connection string")

    @app.get("/admin/explode")
    async def admin_explode():
        raise RuntimeError("admin secret")

    return app


class TestRequestIDMiddleware:
    """Tests for the RequestIDMiddleware."""

    def test_request_id_header_on_success(self):
        """Successful responses should include X-Request-ID header."""
        from app.main import app

        with TestClient(app) as c:
            response = c.get("/health")
        assert response.status_code == 200
        assert "X-Request-ID" in response.headers
        uuid.UUID(response.headers["X-Request-ID"])

    def test_request_id_header_on_error(self):
        """Error responses should include X-Request-ID header."""
        app = _make_error_app()
        with TestClient(app, raise_server_exceptions=False) as c:
            response = c.get("/explode")
        assert response.status_code == 500
        assert "X-Request-ID" in response.headers
        uuid.UUID(response.headers["X-Request-ID"])

    def test_request_id_in_error_body(self):
        """Error response body should include request_id for client correlation."""
        app = _make_error_app()
        with TestClient(app, raise_server_exceptions=False) as c:
            response = c.get("/explode")
        body = response.json()
        assert "request_id" in body
        assert body["request_id"] == response.headers["X-Request-ID"]

    def test_unique_request_ids(self):
        """Each request should get a unique request ID."""
        from app.main import app

        with TestClient(app) as c:
            r1 = c.get("/health")
            r2 = c.get("/health")
        assert r1.headers["X-Request-ID"] != r2.headers["X-Request-ID"]


class TestGenericExceptionHandler:
    """Tests for the generic exception handler in main.py."""

    def test_500_returns_safe_message(self):
        """Non-admin endpoints should return a generic 500 with no internal details."""
        app = _make_error_app()
        with TestClient(app, raise_server_exceptions=False) as c:
            response = c.get("/explode")

        assert response.status_code == 500
        body = response.json()
        assert body["msg"] == "Internal server error"
        assert "request_id" in body
        assert "secret" not in response.text
        assert "db connection" not in response.text

    def test_admin_path_reraises(self):
        """Admin path exceptions should re-raise (not be caught)."""
        app = _make_error_app()
        with TestClient(app, raise_server_exceptions=True) as c:
            with pytest.raises(RuntimeError, match="admin secret"):
                c.get("/admin/explode")

    def test_exception_generates_structured_log(self, caplog):
        """Unhandled exceptions should produce a structured error log with correlation metadata."""
        app = _make_error_app()
        with caplog.at_level(logging.ERROR, logger="app.main"):
            with TestClient(app, raise_server_exceptions=False) as c:
                response = c.get("/explode")

        error_records = [r for r in caplog.records if r.levelno == logging.ERROR]
        assert len(error_records) == 1
        record = error_records[0]

        assert record.request_id is not None
        uuid.UUID(record.request_id)
        assert record.method == "GET"
        assert record.path == "/explode"
        assert record.exception_class == "RuntimeError"
        assert record.request_id == response.json()["request_id"]

    def test_exception_log_includes_traceback(self, caplog):
        """Structured error log should include exception traceback."""
        app = _make_error_app()
        with caplog.at_level(logging.ERROR, logger="app.main"):
            with TestClient(app, raise_server_exceptions=False) as c:
                c.get("/explode")

        error_records = [r for r in caplog.records if r.levelno == logging.ERROR]
        record = error_records[0]
        assert record.exc_info is not None
        assert record.exc_info[0] is RuntimeError


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

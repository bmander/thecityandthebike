import pytest
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.testclient import TestClient


ALLOWED_ORIGIN = "https://bmander.github.io"
DISALLOWED_ORIGIN = "https://evil.example.com"


@pytest.fixture
def cors_client():
    """Create a minimal app with CORS enabled for testing."""
    app = FastAPI()
    app.add_middleware(
        CORSMiddleware,
        allow_origins=[ALLOWED_ORIGIN],
        allow_methods=["GET"],
        allow_headers=["*"],
    )

    @app.get("/test")
    def test_endpoint():
        return {"ok": True}

    with TestClient(app) as c:
        yield c


class TestCorsDisabledByDefault:
    def test_no_cors_headers_without_config(self, client):
        """Default app (no CORS_ORIGINS) should not return CORS headers."""
        response = client.get(
            "/submissions", headers={"Origin": ALLOWED_ORIGIN}
        )
        assert response.status_code == 200
        assert "access-control-allow-origin" not in response.headers


class TestCorsEnabled:
    def test_allowed_origin_gets_cors_headers(self, cors_client):
        """Allowed origin should receive Access-Control-Allow-Origin."""
        response = cors_client.get(
            "/test", headers={"Origin": ALLOWED_ORIGIN}
        )
        assert response.status_code == 200
        assert response.headers["access-control-allow-origin"] == ALLOWED_ORIGIN

    def test_disallowed_origin_no_cors_headers(self, cors_client):
        """Disallowed origin should not receive CORS headers."""
        response = cors_client.get(
            "/test", headers={"Origin": DISALLOWED_ORIGIN}
        )
        assert response.status_code == 200
        assert "access-control-allow-origin" not in response.headers

    def test_preflight_request(self, cors_client):
        """OPTIONS preflight for allowed origin should succeed."""
        response = cors_client.options(
            "/test",
            headers={
                "Origin": ALLOWED_ORIGIN,
                "Access-Control-Request-Method": "GET",
            },
        )
        assert response.status_code == 200
        assert response.headers["access-control-allow-origin"] == ALLOWED_ORIGIN

    def test_preflight_disallowed_origin(self, cors_client):
        """OPTIONS preflight for disallowed origin should not include CORS headers."""
        response = cors_client.options(
            "/test",
            headers={
                "Origin": DISALLOWED_ORIGIN,
                "Access-Control-Request-Method": "GET",
            },
        )
        assert "access-control-allow-origin" not in response.headers

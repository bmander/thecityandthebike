import pytest


EXPECTED_HEADERS = {
    "X-Content-Type-Options": "nosniff",
    "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
    "X-Frame-Options": "DENY",
    "Content-Security-Policy": "default-src 'self'",
    "Referrer-Policy": "strict-origin-when-cross-origin",
    "Permissions-Policy": "camera=(), microphone=(), geolocation=()",
}


class TestSecurityHeaders:
    @pytest.mark.parametrize("header,expected_value", EXPECTED_HEADERS.items())
    def test_security_header_present(self, client, header, expected_value):
        response = client.get("/health")
        assert response.headers.get(header) == expected_value

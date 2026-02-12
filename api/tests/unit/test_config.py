from app.config import Settings


class TestDatabaseUrl:
    """Test Settings.database_url property branches."""

    def test_explicit_database_url(self, monkeypatch):
        """When DATABASE_URL is set, it should be used directly."""
        monkeypatch.setenv("JWT_SECRET_KEY", "test-secret")
        monkeypatch.setenv("DATABASE_URL", "postgresql://custom:pass@host/db")
        s = Settings()
        assert s.database_url == "postgresql://custom:pass@host/db"

    def test_unix_socket_url(self, monkeypatch):
        """When PGHOST starts with /, build a Unix-socket connection string."""
        monkeypatch.setenv("JWT_SECRET_KEY", "test-secret")
        monkeypatch.setenv("PGHOST", "/cloudsql/project:region:instance")
        monkeypatch.setenv("PGUSER", "myuser")
        monkeypatch.setenv("PGPASSWORD", "mypass")
        monkeypatch.setenv("PGDATABASE", "mydb")
        monkeypatch.delenv("DATABASE_URL", raising=False)
        s = Settings()
        assert s.database_url == (
            "postgresql://myuser:mypass@/mydb"
            "?host=/cloudsql/project:region:instance"
        )

    def test_standard_postgresql_url(self, monkeypatch):
        """Default TCP connection string from individual PG* settings."""
        monkeypatch.setenv("JWT_SECRET_KEY", "test-secret")
        monkeypatch.setenv("PGHOST", "db.example.com")
        monkeypatch.setenv("PGPORT", "5433")
        monkeypatch.setenv("PGUSER", "app")
        monkeypatch.setenv("PGPASSWORD", "secret")
        monkeypatch.setenv("PGDATABASE", "prod")
        monkeypatch.delenv("DATABASE_URL", raising=False)
        s = Settings()
        assert s.database_url == "postgresql://app:secret@db.example.com:5433/prod"


class TestCorsOriginsList:
    """Test Settings.cors_origins_list property."""

    def test_empty_string_returns_empty_list(self, monkeypatch):
        monkeypatch.setenv("JWT_SECRET_KEY", "test-secret")
        monkeypatch.setenv("CORS_ORIGINS", "")
        s = Settings()
        assert s.cors_origins_list == []

    def test_unset_returns_empty_list(self, monkeypatch):
        monkeypatch.setenv("JWT_SECRET_KEY", "test-secret")
        monkeypatch.delenv("CORS_ORIGINS", raising=False)
        s = Settings()
        assert s.cors_origins_list == []

    def test_single_origin(self, monkeypatch):
        monkeypatch.setenv("JWT_SECRET_KEY", "test-secret")
        monkeypatch.setenv("CORS_ORIGINS", "https://example.com")
        s = Settings()
        assert s.cors_origins_list == ["https://example.com"]

    def test_multiple_origins(self, monkeypatch):
        monkeypatch.setenv("JWT_SECRET_KEY", "test-secret")
        monkeypatch.setenv("CORS_ORIGINS", "https://a.com,https://b.com,https://c.com")
        s = Settings()
        assert s.cors_origins_list == ["https://a.com", "https://b.com", "https://c.com"]

    def test_whitespace_is_stripped(self, monkeypatch):
        monkeypatch.setenv("JWT_SECRET_KEY", "test-secret")
        monkeypatch.setenv("CORS_ORIGINS", "  https://a.com , https://b.com  ")
        s = Settings()
        assert s.cors_origins_list == ["https://a.com", "https://b.com"]

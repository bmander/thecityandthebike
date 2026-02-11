from typing import Optional

from pydantic import ConfigDict
from pydantic_settings import BaseSettings


API_VERSION = "0.1.0"


class Settings(BaseSettings):
    model_config = ConfigDict(env_file=".env")

    PGUSER: str = "tcatb"
    PGPASSWORD: str = ""
    PGHOST: str = "localhost"
    PGPORT: str = "5432"
    PGDATABASE: str = "tcatb_dev"
    JWT_SECRET_KEY: str
    DATABASE_URL: Optional[str] = None
    STORAGE_BUCKET: Optional[str] = None
    LOGIN_RATE_LIMIT: str = "5/minute"
    REGISTER_RATE_LIMIT: str = "3/hour"
    ACCOUNT_LOCKOUT_ATTEMPTS: int = 10
    ACCOUNT_LOCKOUT_DURATION: int = 900  # seconds (15 min)
    CORS_ORIGINS: str = ""  # comma-separated list of allowed origins

    @property
    def cors_origins_list(self) -> list[str]:
        if not self.CORS_ORIGINS:
            return []
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",")]

    @property
    def database_url(self) -> str:
        if self.DATABASE_URL:
            return self.DATABASE_URL
        if self.PGHOST.startswith("/"):
            # Unix socket (Cloud SQL): host goes in query param
            return (
                f"postgresql://{self.PGUSER}:{self.PGPASSWORD}"
                f"@/{self.PGDATABASE}?host={self.PGHOST}"
            )
        return (
            f"postgresql://{self.PGUSER}:{self.PGPASSWORD}"
            f"@{self.PGHOST}:{self.PGPORT}/{self.PGDATABASE}"
        )


settings = Settings()

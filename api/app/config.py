from typing import Optional

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    PGUSER: str = "tcatb"
    PGPASSWORD: str = ""
    PGHOST: str = "localhost"
    PGPORT: str = "5432"
    PGDATABASE: str = "tcatb_dev"
    JWT_SECRET_KEY: str
    DATABASE_URL: Optional[str] = None

    @property
    def database_url(self) -> str:
        if self.DATABASE_URL:
            return self.DATABASE_URL
        return (
            f"postgresql://{self.PGUSER}:{self.PGPASSWORD}"
            f"@{self.PGHOST}:{self.PGPORT}/{self.PGDATABASE}"
        )

    class Config:
        env_file = ".env"


settings = Settings()

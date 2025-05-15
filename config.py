import os

class Config:
    SQLALCHEMY_DATABASE_URI = (
        f"postgresql://{os.getenv('PGUSER','tcatb')}:{os.getenv('PGPASSWORD','tcatbpass')}"
        f"@{os.getenv('PGHOST','localhost')}:{os.getenv('PGPORT','5432')}"  
        f"/{os.getenv('PGDATABASE','tcatb_dev')}"
    )
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    JWT_SECRET_KEY = os.getenv('JWT_SECRET_KEY', 'super-secret')
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from fastapi.staticfiles import StaticFiles

from .config import settings
from .database import engine, Base
from .routers import auth_router, users_router, submissions_router, bikes_router, uploads_router


@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(title="The City and the Bike API", lifespan=lifespan)


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content={"msg": "Internal server error"},
    )


@app.get("/health")
async def health():
    return {"status": "healthy"}


app.include_router(auth_router)
app.include_router(users_router)
app.include_router(submissions_router)
app.include_router(bikes_router)
app.include_router(uploads_router)

if not settings.STORAGE_BUCKET:
    upload_dir = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads")
    os.makedirs(upload_dir, exist_ok=True)
    app.mount("/uploads", StaticFiles(directory=upload_dir), name="uploads")

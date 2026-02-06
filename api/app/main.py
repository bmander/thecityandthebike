from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from starlette.middleware.base import BaseHTTPMiddleware

from .database import engine, Base
from .routers import auth_router, users_router, submissions_router, bikes_router, uploads_router


class NoSniffMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        return response


@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    yield


app = FastAPI(title="The City and the Bike API", lifespan=lifespan)
app.add_middleware(NoSniffMiddleware)


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

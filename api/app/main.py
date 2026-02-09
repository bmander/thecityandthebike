from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from starlette.middleware.base import BaseHTTPMiddleware

from .admin import setup_admin
from .config import settings
from .database import engine
from .rate_limit import limiter
from .routers import auth_router, users_router, submissions_router, bikes_router, uploads_router


class NoSniffMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        return response


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(title="The City and the Bike API", lifespan=lifespan)
app.state.limiter = limiter
app.add_middleware(NoSniffMiddleware)


async def _rate_limit_exceeded_handler(request: Request, exc: RateLimitExceeded):
    headers = getattr(exc, "headers", {}) or {}
    response = JSONResponse(
        status_code=429,
        content={"msg": "Rate limit exceeded. Try again later."},
    )
    if "Retry-After" in headers:
        response.headers["Retry-After"] = headers["Retry-After"]
    elif hasattr(exc, "retry_after"):
        response.headers["Retry-After"] = str(exc.retry_after)
    return response


app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    if request.url.path.startswith("/admin"):
        raise exc
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

setup_admin(app, engine, settings.JWT_SECRET_KEY)

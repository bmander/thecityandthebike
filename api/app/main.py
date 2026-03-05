import logging
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from starlette.middleware.base import BaseHTTPMiddleware

from .admin import setup_admin
from .config import API_VERSION, settings
from .database import engine
from .rate_limit import limiter, rate_limit_exceeded_handler
from .routers import auth_router, users_router, submissions_router, bikes_router, uploads_router, leaderboard_router, tags_router, flags_router

logger = logging.getLogger(__name__)


class RequestIDMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        request_id = str(uuid.uuid4())
        request.state.request_id = request_id
        response = await call_next(request)
        response.headers["X-Request-ID"] = request_id
        return response


class SecurityHeadersMiddleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response = await call_next(request)
        response.headers["X-Content-Type-Options"] = "nosniff"
        response.headers["Strict-Transport-Security"] = "max-age=31536000; includeSubDomains"
        response.headers["X-Frame-Options"] = "DENY"
        response.headers["Content-Security-Policy"] = "default-src 'self'"
        response.headers["Referrer-Policy"] = "strict-origin-when-cross-origin"
        response.headers["Permissions-Policy"] = (
            "camera=(), microphone=(), geolocation=()"
        )
        return response


@asynccontextmanager
async def lifespan(app: FastAPI):
    yield


app = FastAPI(title="The City and the Bike API", lifespan=lifespan)
app.state.limiter = limiter
app.add_middleware(SecurityHeadersMiddleware)
app.add_middleware(RequestIDMiddleware)
if settings.cors_origins_list:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins_list,
        allow_methods=["GET"],
        allow_headers=["*"],
    )

app.add_exception_handler(RateLimitExceeded, rate_limit_exceeded_handler)

SENSITIVE_FIELDS = {"password"}


@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    sanitized_errors = []
    for error in exc.errors():
        sanitized = {k: v for k, v in error.items() if k != "input"}
        field_names = {str(loc).lower() for loc in error.get("loc", [])}
        if field_names & SENSITIVE_FIELDS:
            sanitized["input"] = "[REDACTED]"
        else:
            sanitized["input"] = error.get("input")
        sanitized_errors.append(sanitized)

    logger.warning(
        "Validation error on %s %s: %s",
        request.method,
        request.url.path,
        sanitized_errors,
    )

    return JSONResponse(
        status_code=422,
        content={"detail": sanitized_errors},
    )


@app.exception_handler(Exception)
async def generic_exception_handler(request: Request, exc: Exception):
    if request.url.path.startswith("/admin"):
        raise exc

    request_id = getattr(request.state, "request_id", None)

    logger.error(
        "Unhandled exception on %s %s",
        request.method,
        request.url.path,
        extra={
            "request_id": request_id,
            "method": request.method,
            "path": request.url.path,
            "exception_class": type(exc).__name__,
        },
        exc_info=True,
    )

    response = JSONResponse(
        status_code=500,
        content={"msg": "Internal server error", "request_id": request_id},
    )
    if request_id:
        response.headers["X-Request-ID"] = request_id
    return response


@app.get("/health")
async def health():
    return {"status": "healthy", "version": API_VERSION}


app.include_router(auth_router)
app.include_router(users_router)
app.include_router(submissions_router)
app.include_router(bikes_router)
app.include_router(uploads_router)
app.include_router(leaderboard_router)
app.include_router(tags_router)
app.include_router(flags_router)

setup_admin(app, engine, settings.JWT_SECRET_KEY)

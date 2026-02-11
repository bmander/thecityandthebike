from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse
from slowapi.errors import RateLimitExceeded
from starlette.middleware.base import BaseHTTPMiddleware

from .admin import setup_admin
from .config import API_VERSION, settings
from .database import engine
from .rate_limit import limiter, rate_limit_exceeded_handler
from .routers import auth_router, users_router, submissions_router, bikes_router, uploads_router


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

app.add_exception_handler(RateLimitExceeded, rate_limit_exceeded_handler)


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
    return {"status": "healthy", "version": API_VERSION}


app.include_router(auth_router)
app.include_router(users_router)
app.include_router(submissions_router)
app.include_router(bikes_router)
app.include_router(uploads_router)

setup_admin(app, engine, settings.JWT_SECRET_KEY)

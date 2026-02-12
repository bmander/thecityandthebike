import io
import os

# Set test environment variables before importing app modules
os.environ.setdefault("PGPASSWORD", "testpassword")
os.environ.setdefault("JWT_SECRET_KEY", "test-secret-key")
os.environ.setdefault("LOGIN_RATE_LIMIT", "1000/minute")
os.environ.setdefault("REGISTER_RATE_LIMIT", "1000/minute")

from contextlib import asynccontextmanager
from datetime import date, datetime, timezone

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient
from PIL import Image
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from slowapi.errors import RateLimitExceeded

from app.admin import setup_admin
from app.database import Base, get_db
from app.dependencies import get_password_hash, create_access_token
from app.models import User, Bike, FenderSubmission, Tag
from app.main import SecurityHeadersMiddleware
from app.rate_limit import AccountLockout, get_account_lockout, limiter, rate_limit_exceeded_handler
from app.routers import auth_router, users_router, submissions_router, bikes_router, uploads_router, leaderboard_router, tags_router


def create_test_image(width=800, height=600, format="JPEG", mode="RGB", color="red"):
    """Create a real image in memory for testing."""
    img = Image.new(mode, (width, height), color=color)
    buf = io.BytesIO()
    if format == "JPEG" and mode in ("RGBA", "P"):
        img = img.convert("RGB")
    img.save(buf, format=format)
    buf.seek(0)
    return buf


# Create test engine with StaticPool for in-memory SQLite
# This ensures all connections share the same in-memory database
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"
test_engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=test_engine)


@asynccontextmanager
async def test_lifespan(app: FastAPI):
    """Test lifespan that uses test_engine instead of production engine."""
    Base.metadata.create_all(bind=test_engine)
    yield


# Create a test app instance that uses test_engine
test_app = FastAPI(title="The City and the Bike API - Test", lifespan=test_lifespan)
test_app.state.limiter = limiter
test_app.add_middleware(SecurityHeadersMiddleware)
test_app.include_router(auth_router)
test_app.include_router(users_router)
test_app.include_router(submissions_router)
test_app.include_router(bikes_router)
test_app.include_router(uploads_router)
test_app.include_router(leaderboard_router)
test_app.include_router(tags_router)
setup_admin(test_app, test_engine, "test-secret-key")

test_app.add_exception_handler(RateLimitExceeded, rate_limit_exceeded_handler)


@pytest.fixture(scope="function", autouse=True)
def setup_database():
    """Create tables before each test and drop them after."""
    Base.metadata.create_all(bind=test_engine)
    yield
    Base.metadata.drop_all(bind=test_engine)


def get_test_db():
    """Dependency override for get_db that uses the test database."""
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture(scope="function")
def db_session():
    """Create a database session for use in test fixtures."""
    session = TestingSessionLocal()
    try:
        yield session
    finally:
        session.close()


@pytest.fixture(scope="function")
def client():
    """Create a test client with the database dependency overridden."""
    test_app.dependency_overrides[get_db] = get_test_db
    test_app.dependency_overrides[get_account_lockout] = lambda: AccountLockout()
    with TestClient(test_app) as test_client:
        yield test_client
    test_app.dependency_overrides.clear()


@pytest.fixture
def test_user_data():
    """Return test user data for registration."""
    return {
        "username": "testuser",
        "email": "test@example.com",
        "password": "testpassword123",
    }


@pytest.fixture
def test_user(db_session, test_user_data):
    """Create and return a test user in the database.

    Returns a detached object with data attributes accessible.
    """
    user = User(
        username=test_user_data["username"],
        email=test_user_data["email"],
        password_hash=get_password_hash(test_user_data["password"]),
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    # Capture values before expunging
    user_data = type("UserData", (), {
        "user_id": user.user_id,
        "username": user.username,
        "email": user.email,
        "password_hash": user.password_hash,
    })()
    db_session.expunge(user)
    return user_data


@pytest.fixture
def auth_token(test_user):
    """Create a valid JWT token for the test user."""
    return create_access_token(subject=str(test_user.user_id))


@pytest.fixture
def auth_headers(auth_token):
    """Return authorization headers with a valid JWT token."""
    return {"Authorization": f"Bearer {auth_token}"}


@pytest.fixture
def test_bike(db_session):
    """Create and return a test bike in the database."""
    bike = Bike(
        bike_qr_id="BIKE-001",
        bike_brand="Test Brand",
        first_seen_at=datetime.now(timezone.utc),
        last_seen_at=datetime.now(timezone.utc),
        notes="Test bike notes",
    )
    db_session.add(bike)
    db_session.commit()
    db_session.refresh(bike)
    # Capture values before expunging
    bike_data = type("BikeData", (), {
        "id": bike.id,
        "bike_qr_id": bike.bike_qr_id,
        "bike_brand": bike.bike_brand,
        "first_seen_at": bike.first_seen_at,
        "last_seen_at": bike.last_seen_at,
        "notes": bike.notes,
    })()
    db_session.expunge(bike)
    return bike_data


@pytest.fixture
def test_submission(db_session, test_user, test_bike):
    """Create and return a test submission in the database."""
    submission = FenderSubmission(
        user_id=test_user.user_id,
        bike_id=test_bike.id,
        image_url="https://example.com/original.jpg",
        image_url_thumbnail="https://example.com/thumbnail.jpg",
        captured_date=date.today(),
        user_caption="Test caption",
    )
    db_session.add(submission)
    db_session.commit()
    db_session.refresh(submission)
    # Capture values before expunging
    submission_data = type("SubmissionData", (), {
        "submission_id": submission.submission_id,
        "user_id": submission.user_id,
        "bike_qr_id": submission.bike_qr_id,
        "image_url": submission.image_url,
        "image_url_thumbnail": submission.image_url_thumbnail,
        "captured_date": submission.captured_date,
        "user_caption": submission.user_caption,
    })()
    db_session.expunge(submission)
    return submission_data


@pytest.fixture
def test_admin_user(db_session):
    """Create and return an admin user in the database."""
    user = User(
        username="adminuser",
        email="admin@example.com",
        password_hash=get_password_hash("adminpassword123"),
        is_admin=True,
    )
    db_session.add(user)
    db_session.commit()
    db_session.refresh(user)
    user_data = type("UserData", (), {
        "user_id": user.user_id,
        "username": user.username,
        "email": user.email,
        "password_hash": user.password_hash,
        "is_admin": user.is_admin,
    })()
    db_session.expunge(user)
    return user_data


@pytest.fixture
def test_tag(db_session, test_submission, test_user):
    """Create and return a test tag in the database."""
    tag = Tag(
        submission_id=test_submission.submission_id,
        user_id=test_user.user_id,
        image_url="/uploads/images/test-tag.png",
    )
    db_session.add(tag)
    db_session.commit()
    db_session.refresh(tag)
    tag_data = type("TagData", (), {
        "tag_id": tag.tag_id,
        "submission_id": tag.submission_id,
        "user_id": tag.user_id,
        "image_url": tag.image_url,
        "created_at": tag.created_at,
    })()
    db_session.expunge(tag)
    return tag_data

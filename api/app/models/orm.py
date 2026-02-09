from datetime import date, datetime, timezone
import uuid

from sqlalchemy import Boolean, Column, Integer, String, Date, DateTime, Text, ForeignKey
from sqlalchemy.orm import relationship

from ..database import Base


class LoginAttempt(Base):
    __tablename__ = "login_attempts"

    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(255), nullable=False, index=True)
    attempted_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))


class User(Base):
    __tablename__ = "users"

    user_id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    username = Column(String(255), unique=True, nullable=False)
    email = Column(String(255), unique=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    is_admin = Column(Boolean, default=False, nullable=False, server_default="0")
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at = Column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), onupdate=lambda: datetime.now(timezone.utc)
    )

    submissions = relationship("FenderSubmission", back_populates="user")


class Bike(Base):
    __tablename__ = "bikes"

    id = Column(Integer, primary_key=True, autoincrement=True)
    bike_qr_id = Column(String(255), unique=True, nullable=False)
    provider = Column(String(50))
    bike_brand = Column(String(255))
    first_seen_at = Column(DateTime(timezone=True))
    last_seen_at = Column(DateTime(timezone=True))
    notes = Column(Text)

    submissions = relationship("FenderSubmission", back_populates="bike")


class FenderSubmission(Base):
    __tablename__ = "fender_submissions"

    submission_id = Column(
        String(36), primary_key=True, default=lambda: str(uuid.uuid4())
    )
    user_id = Column(String(36), ForeignKey("users.user_id"), nullable=False)
    bike_id = Column(Integer, ForeignKey("bikes.id"), nullable=False)
    image_url_original = Column(Text)
    image_url_thumbnail = Column(Text)
    image_url_processed = Column(Text)
    captured_date = Column(Date, nullable=False)
    uploaded_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    user_caption = Column(Text)

    user = relationship("User", back_populates="submissions")
    bike = relationship("Bike", back_populates="submissions")

    @property
    def bike_qr_id(self):
        return self.bike.bike_qr_id if self.bike else None

    @property
    def username(self):
        return self.user.username if self.user else None

    @property
    def provider(self):
        return self.bike.provider if self.bike else None

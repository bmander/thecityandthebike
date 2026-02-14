from datetime import datetime, timezone
import uuid

from sqlalchemy import Boolean, Column, Index, Integer, JSON, String, Date, DateTime, Text, ForeignKey, Uuid
from sqlalchemy.orm import relationship

from ..database import Base


class LoginAttempt(Base):
    __tablename__ = "login_attempts"

    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(255), nullable=False, index=True)
    attempted_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))


class User(Base):
    __tablename__ = "users"

    user_id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    username = Column(String(255), unique=True, nullable=False)
    email = Column(String(255), unique=True, nullable=False)
    password_hash = Column(String(255), nullable=False)
    is_admin = Column(Boolean, default=False, nullable=False, server_default="0")
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    updated_at = Column(
        DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), onupdate=lambda: datetime.now(timezone.utc)
    )

    submissions = relationship("FenderSubmission", back_populates="user")


class RefreshToken(Base):
    __tablename__ = "refresh_tokens"

    id = Column(Integer, primary_key=True, autoincrement=True)
    token = Column(String(64), unique=True, index=True, nullable=False)
    user_id = Column(Uuid, ForeignKey("users.user_id"), nullable=False)
    expires_at = Column(DateTime(timezone=True), nullable=False)
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))

    user = relationship("User")


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
    __table_args__ = (
        Index("ix_fender_submissions_user_id_uploaded_at", "user_id", "uploaded_at"),
        Index("ix_fender_submissions_bike_id_uploaded_at", "bike_id", "uploaded_at"),
    )

    submission_id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    user_id = Column(Uuid, ForeignKey("users.user_id"), nullable=False, index=True)
    bike_id = Column(Integer, ForeignKey("bikes.id"), nullable=False, index=True)
    image_url = Column(Text)
    image_url_thumbnail = Column(Text)
    captured_date = Column(Date, nullable=False)
    uploaded_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc), index=True)
    user_caption = Column(Text)

    user = relationship("User", back_populates="submissions")
    bike = relationship("Bike", back_populates="submissions")

    tags = relationship("Tag", back_populates="submission", cascade="all, delete-orphan")

    @property
    def bike_qr_id(self):
        return self.bike.bike_qr_id if self.bike else None

    @property
    def username(self):
        return self.user.username if self.user else None

    @property
    def provider(self):
        return self.bike.provider if self.bike else None


class Tag(Base):
    __tablename__ = "tags"

    tag_id = Column(Uuid, primary_key=True, default=uuid.uuid4)
    submission_id = Column(Uuid, ForeignKey("fender_submissions.submission_id"), nullable=False, index=True)
    user_id = Column(Uuid, ForeignKey("users.user_id"), nullable=False)
    image_url = Column(Text, nullable=False)
    ring = Column(JSON, nullable=True)
    ring_width = Column(Integer, nullable=True)
    ring_height = Column(Integer, nullable=True)
    created_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))

    submission = relationship("FenderSubmission", back_populates="tags")
    user = relationship("User")

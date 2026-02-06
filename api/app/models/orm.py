from datetime import datetime, timezone
import uuid

from sqlalchemy import Boolean, Column, String, DateTime, Float, Text, ForeignKey
from sqlalchemy.orm import relationship

from ..database import Base


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

    bike_qr_id = Column(String(255), primary_key=True)
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
    bike_qr_id = Column(String(255), ForeignKey("bikes.bike_qr_id"), nullable=False)
    image_url_original = Column(Text)
    image_url_processed = Column(Text)
    latitude = Column(Float)
    longitude = Column(Float)
    captured_at = Column(DateTime(timezone=True), nullable=False)
    uploaded_at = Column(DateTime(timezone=True), default=lambda: datetime.now(timezone.utc))
    user_caption = Column(Text)

    user = relationship("User", back_populates="submissions")
    bike = relationship("Bike", back_populates="submissions")

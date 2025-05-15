from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
import uuid

# Initialize the SQLAlchemy db instance
db = SQLAlchemy()

class User(db.Model):
    __tablename__ = 'users'
    user_id = db.Column(db.String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    username = db.Column(db.String(255), unique=True, nullable=False)
    email = db.Column(db.String(255), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    created_at = db.Column(db.DateTime(timezone=True), default=datetime.utcnow)
    updated_at = db.Column(db.DateTime(timezone=True), default=datetime.utcnow, onupdate=datetime.utcnow)
    submissions = db.relationship('FenderSubmission', back_populates='user')

    def to_dict(self):
        return {
            'user_id': self.user_id,
            'username': self.username,
            'email': self.email,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'updated_at': self.updated_at.isoformat() if self.updated_at else None,
        }

class Bike(db.Model):
    __tablename__ = 'bikes'
    bike_qr_id = db.Column(db.String(255), primary_key=True)
    bike_brand = db.Column(db.String(255))
    first_seen_at = db.Column(db.DateTime(timezone=True))
    last_seen_at = db.Column(db.DateTime(timezone=True))
    notes = db.Column(db.Text)
    submissions = db.relationship('FenderSubmission', back_populates='bike')

    def to_dict(self):
        return {
            'bike_qr_id': self.bike_qr_id,
            'bike_brand': self.bike_brand,
            'first_seen_at': self.first_seen_at.isoformat() if self.first_seen_at else None,
            'last_seen_at': self.last_seen_at.isoformat() if self.last_seen_at else None,
            'notes': self.notes,
        }

class FenderSubmission(db.Model):
    __tablename__ = 'fender_submissions'
    submission_id = db.Column(db.String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = db.Column(db.String(36), db.ForeignKey('users.user_id'), nullable=False)
    bike_qr_id = db.Column(db.String(255), db.ForeignKey('bikes.bike_qr_id'), nullable=False)
    image_url_original = db.Column(db.Text)
    image_url_processed = db.Column(db.Text)
    latitude = db.Column(db.Float)
    longitude = db.Column(db.Float)
    captured_at = db.Column(db.DateTime(timezone=True), nullable=False)
    uploaded_at = db.Column(db.DateTime(timezone=True), default=datetime.utcnow)
    user_caption = db.Column(db.Text)
    user = db.relationship('User', back_populates='submissions')
    bike = db.relationship('Bike', back_populates='submissions')

    def to_dict(self):
        return {
            'submission_id': self.submission_id,
            'user_id': self.user_id,
            'bike_qr_id': self.bike_qr_id,
            'image_url_original': self.image_url_original,
            'image_url_processed': self.image_url_processed,
            'latitude': self.latitude,
            'longitude': self.longitude,
            'captured_at': self.captured_at.isoformat() if self.captured_at else None,
            'uploaded_at': self.uploaded_at.isoformat() if self.uploaded_at else None,
            'user_caption': self.user_caption,
        }
-- Initialize PostgreSQL schema for The City And The Bike Phase 1
-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users table
CREATE TABLE IF NOT EXISTS users (
  user_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  username VARCHAR(255) UNIQUE NOT NULL,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Bikes table
CREATE TABLE IF NOT EXISTS bikes (
  bike_qr_id VARCHAR(255) PRIMARY KEY,
  bike_brand VARCHAR(255),
  first_seen_at TIMESTAMP WITH TIME ZONE,
  last_seen_at TIMESTAMP WITH TIME ZONE,
  notes TEXT
);

-- Fender submissions table
CREATE TABLE IF NOT EXISTS fender_submissions (
  submission_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
  user_id UUID NOT NULL REFERENCES users(user_id),
  bike_qr_id VARCHAR(255) NOT NULL REFERENCES bikes(bike_qr_id),
  image_url_original TEXT,
  image_url_processed TEXT,
  latitude DECIMAL,
  longitude DECIMAL,
  captured_at TIMESTAMP WITH TIME ZONE NOT NULL,
  uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
  user_caption TEXT
);
-- WorshipHub Database Initialization Script
-- This script initializes the PostgreSQL database with PostGIS extension

-- Create the database if it doesn't exist (handled by POSTGRES_DB env var)
-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS postgis_topology;

-- Verify PostGIS installation
SELECT PostGIS_Version();

-- Create application user (optional, for production use)
-- CREATE USER worshiphub_user WITH PASSWORD 'secure_password';
-- GRANT ALL PRIVILEGES ON DATABASE worshiphub TO worshiphub_user;

-- Log successful initialization
SELECT 'WorshipHub database initialized successfully with PostGIS' AS status;
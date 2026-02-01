-- ArchPilot Database Setup Script
-- Run this script as the PostgreSQL superuser (postgres)

-- Create database
CREATE DATABASE archpilot;

-- Create user
CREATE USER archpilot_user WITH PASSWORD 'archpilot_password';

-- Grant privileges on database
GRANT ALL PRIVILEGES ON DATABASE archpilot TO archpilot_user;

-- Connect to the archpilot database
\c archpilot

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO archpilot_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO archpilot_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO archpilot_user;

-- Grant future privileges (for tables created later)
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO archpilot_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO archpilot_user;

-- Verify setup
SELECT current_database(), current_user;

-- Show granted privileges
\dp

-- Exit
\q
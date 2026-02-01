-- Initialize ArchPilot Database
-- This script runs automatically when the Docker container starts

-- Grant additional privileges to ensure everything works
GRANT ALL PRIVILEGES ON DATABASE archpilot TO archpilot_user;
GRANT ALL ON SCHEMA public TO archpilot_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO archpilot_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO archpilot_user;

-- Grant future privileges
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO archpilot_user;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO archpilot_user;

-- Create a simple test to verify setup
SELECT 'Database initialized successfully' as status;
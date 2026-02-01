@echo off
echo Setting up ArchPilot PostgreSQL Database...
echo.

REM Check if PostgreSQL is installed
where psql >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: PostgreSQL is not installed or not in PATH
    echo Please install PostgreSQL first: https://www.postgresql.org/download/windows/
    pause
    exit /b 1
)

echo Creating database and user...
echo.

REM Create a temporary SQL file
echo CREATE DATABASE archpilot; > temp_setup.sql
echo CREATE USER archpilot_user WITH PASSWORD 'archpilot_password'; >> temp_setup.sql
echo GRANT ALL PRIVILEGES ON DATABASE archpilot TO archpilot_user; >> temp_setup.sql
echo \c archpilot >> temp_setup.sql
echo GRANT ALL ON SCHEMA public TO archpilot_user; >> temp_setup.sql
echo GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO archpilot_user; >> temp_setup.sql
echo GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO archpilot_user; >> temp_setup.sql

REM Execute the SQL file
echo Please enter the PostgreSQL postgres user password when prompted:
psql -U postgres -h localhost -f temp_setup.sql

REM Clean up
del temp_setup.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS: Database setup completed!
    echo Database: archpilot
    echo User: archpilot_user
    echo Password: archpilot_password
    echo.
    echo You can now run the application with: run-app.bat
) else (
    echo.
    echo ERROR: Database setup failed!
    echo Please check the error messages above.
)

echo.
pause
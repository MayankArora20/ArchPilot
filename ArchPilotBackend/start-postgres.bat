@echo off
echo Starting PostgreSQL with Docker...
echo.

REM Check if Docker is installed
where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Docker is not installed or not in PATH
    echo Please install Docker Desktop: https://www.docker.com/products/docker-desktop/
    echo.
    echo Alternative: Install PostgreSQL directly from https://www.postgresql.org/download/windows/
    pause
    exit /b 1
)

REM Check if Docker is running
docker info >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Docker is not running
    echo Please start Docker Desktop and try again
    pause
    exit /b 1
)

echo Starting PostgreSQL container...
docker-compose up -d

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS: PostgreSQL is starting up!
    echo.
    echo Database Details:
    echo - Host: localhost
    echo - Port: 5432
    echo - Database: archpilot
    echo - Username: archpilot_user
    echo - Password: archpilot_password
    echo.
    echo Waiting for database to be ready...
    timeout /t 10 /nobreak >nul
    
    echo Testing connection...
    docker exec archpilot-postgres pg_isready -U archpilot_user -d archpilot
    
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ✓ Database is ready!
        echo You can now run: run-app.bat
    ) else (
        echo.
        echo Database is still starting up. Please wait a moment and try running the app.
    )
) else (
    echo.
    echo ERROR: Failed to start PostgreSQL container
    echo Please check Docker Desktop and try again
)

echo.
echo To stop PostgreSQL later, run: docker-compose down
echo To view logs: docker-compose logs postgres
echo.
pause
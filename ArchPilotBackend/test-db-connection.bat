@echo off
echo Testing PostgreSQL Database Connection...
echo.

REM Check if Docker container is running
docker ps --filter "name=archpilot-postgres" --format "table {{.Names}}\t{{.Status}}" | findstr archpilot-postgres >nul
if %ERRORLEVEL% EQU 0 (
    echo ✓ PostgreSQL Docker container is running
    
    REM Test database connection
    echo Testing database connection...
    docker exec archpilot-postgres psql -U archpilot_user -d archpilot -c "SELECT 'Connection successful!' as status, current_database(), current_user;"
    
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ✓ Database connection test PASSED!
        echo ✓ Ready to run the application
        echo.
        echo Run: run-app.bat
    ) else (
        echo.
        echo ✗ Database connection test FAILED!
        echo Please check the database setup
    )
) else (
    echo ✗ PostgreSQL container is not running
    echo.
    echo Please start PostgreSQL first:
    echo   start-postgres.bat
)

echo.
pause
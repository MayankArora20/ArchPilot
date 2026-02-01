@echo off
echo Setting up ArchPilot Database on existing PostgreSQL...
echo.

REM Try to find psql in common locations
set PSQL_PATH=
where psql >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set PSQL_PATH=psql
) else (
    REM Check common PostgreSQL installation paths
    if exist "C:\Program Files\PostgreSQL\15\bin\psql.exe" set PSQL_PATH="C:\Program Files\PostgreSQL\15\bin\psql.exe"
    if exist "C:\Program Files\PostgreSQL\14\bin\psql.exe" set PSQL_PATH="C:\Program Files\PostgreSQL\14\bin\psql.exe"
    if exist "C:\Program Files\PostgreSQL\13\bin\psql.exe" set PSQL_PATH="C:\Program Files\PostgreSQL\13\bin\psql.exe"
    if exist "C:\Program Files\PostgreSQL\16\bin\psql.exe" set PSQL_PATH="C:\Program Files\PostgreSQL\16\bin\psql.exe"
)

if "%PSQL_PATH%"=="" (
    echo ERROR: Could not find psql command
    echo Please add PostgreSQL bin directory to your PATH or run this manually in pgAdmin:
    echo.
    type database-setup.sql
    echo.
    pause
    exit /b 1
)

echo Found psql at: %PSQL_PATH%
echo.
echo Please enter the password for PostgreSQL 'postgres' user when prompted:
echo.

%PSQL_PATH% -U postgres -h localhost -f database-setup.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ SUCCESS: Database setup completed!
    echo.
    echo Database Details:
    echo - Database: archpilot
    echo - User: archpilot_user  
    echo - Password: archpilot_password
    echo - Host: localhost
    echo - Port: 5432
    echo.
    echo Testing connection...
    %PSQL_PATH% -U archpilot_user -h localhost -d archpilot -c "SELECT 'Connection test successful!' as status;"
    
    if %ERRORLEVEL% EQU 0 (
        echo.
        echo ✓ Connection test PASSED!
        echo You can now run: run-app.bat
    ) else (
        echo.
        echo ⚠ Connection test failed, but database was created.
        echo Try running the app anyway: run-app.bat
    )
) else (
    echo.
    echo ✗ ERROR: Database setup failed!
    echo.
    echo Manual setup option:
    echo 1. Open pgAdmin
    echo 2. Connect to your PostgreSQL server
    echo 3. Right-click on 'Databases' and create new database 'archpilot'
    echo 4. Right-click on 'Login/Group Roles' and create user 'archpilot_user' with password 'archpilot_password'
    echo 5. Grant all privileges on 'archpilot' database to 'archpilot_user'
)

echo.
pause
@echo off
echo Testing Repository Verification API...
echo.

REM Check if curl is available
where curl >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: curl is not available
    echo Please install curl or test manually using a browser/Postman
    echo.
    echo Manual test URLs:
    echo GET: http://localhost:8080/api/repository/verify?url=https://github.com/spring-projects/spring-boot
    echo POST: http://localhost:8080/api/repository/verify
    echo.
    pause
    exit /b 1
)

echo Testing Repository API endpoints...
echo.

REM Test 1: Health check
echo 1. Testing health check...
curl -s -w "Status: %%{http_code}\n" http://localhost:8080/api/repository/health
echo.

REM Test 2: Valid GitHub repository (GET)
echo 2. Testing valid GitHub repository (GET)...
curl -s -w "Status: %%{http_code}\n" "http://localhost:8080/api/repository/verify?url=https://github.com/spring-projects/spring-boot"
echo.

REM Test 3: Invalid repository (GET)
echo 3. Testing invalid repository (GET)...
curl -s -w "Status: %%{http_code}\n" "http://localhost:8080/api/repository/verify?url=https://github.com/nonexistent/nonexistent"
echo.

REM Test 4: Valid repository (POST)
echo 4. Testing valid repository (POST)...
curl -s -w "Status: %%{http_code}\n" ^
  -H "Content-Type: application/json" ^
  -d "{\"repositoryUrl\":\"https://github.com/microsoft/vscode\"}" ^
  http://localhost:8080/api/repository/verify
echo.

REM Test 5: Invalid URL format (POST)
echo 5. Testing invalid URL format (POST)...
curl -s -w "Status: %%{http_code}\n" ^
  -H "Content-Type: application/json" ^
  -d "{\"repositoryUrl\":\"https://invalid-url.com/repo\"}" ^
  http://localhost:8080/api/repository/verify
echo.

REM Test 6: GitLab repository (GET)
echo 6. Testing GitLab repository (GET)...
curl -s -w "Status: %%{http_code}\n" "http://localhost:8080/api/repository/verify?url=https://gitlab.com/gitlab-org/gitlab"
echo.

REM Test 7: Get repository branches (GET)
echo 7. Testing repository branches (GET)...
curl -s -w "Status: %%{http_code}\n" "http://localhost:8080/api/repository/branches?url=https://github.com/microsoft/vscode&limit=5"
echo.

REM Test 8: Get repository branches (POST)
echo 8. Testing repository branches (POST)...
curl -s -w "Status: %%{http_code}\n" ^
  -H "Content-Type: application/json" ^
  -d "{\"repositoryUrl\":\"https://github.com/spring-projects/spring-boot\",\"limit\":3}" ^
  http://localhost:8080/api/repository/branches
echo.

echo.
echo Testing completed!
echo.
echo For detailed API documentation, visit:
echo http://localhost:8080/swagger-ui.html
echo.
pause
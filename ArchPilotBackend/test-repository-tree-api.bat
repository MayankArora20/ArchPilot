@echo off
echo Testing Repository Tree API...
echo.

echo Testing GET request for repository tree structure:
echo.

REM Test 1: Get tree structure for a public repository (root directory)
echo 1. Testing root directory tree structure:
curl -X GET "http://localhost:8080/api/repository/tree?url=https://github.com/spring-projects/spring-boot" -H "Content-Type: application/json"
echo.
echo.

REM Test 2: Get tree structure for a specific path
echo 2. Testing specific path tree structure:
curl -X GET "http://localhost:8080/api/repository/tree?url=https://github.com/spring-projects/spring-boot&path=spring-boot-project" -H "Content-Type: application/json"
echo.
echo.

REM Test 3: Get recursive tree structure (be careful with large repos)
echo 3. Testing recursive tree structure (limited depth):
curl -X GET "http://localhost:8080/api/repository/tree?url=https://github.com/spring-projects/spring-boot&path=spring-boot-project/spring-boot-starters&recursive=true" -H "Content-Type: application/json"
echo.
echo.

REM Test 4: POST request with JSON body
echo 4. Testing POST request with JSON body:
curl -X POST "http://localhost:8080/api/repository/tree" ^
  -H "Content-Type: application/json" ^
  -d "{\"repositoryUrl\":\"https://github.com/spring-projects/spring-boot\",\"path\":\"spring-boot-project\",\"recursive\":false}"
echo.
echo.

REM Test 5: Test with specific branch
echo 5. Testing with specific branch:
curl -X GET "http://localhost:8080/api/repository/tree?url=https://github.com/spring-projects/spring-boot&branch=main&path=spring-boot-project" -H "Content-Type: application/json"
echo.
echo.

echo Repository Tree API tests completed!
pause
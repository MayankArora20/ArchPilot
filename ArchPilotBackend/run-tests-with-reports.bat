@echo off
echo Running tests and generating reports...
echo.

echo [1/3] Running tests...
call gradlew clean test

echo.
echo [2/3] Generating coverage report...
call gradlew jacocoTestReport

echo.
echo [3/3] Opening reports...
echo Test Report: build\reports\tests\test\index.html
echo Coverage Report: build\reports\jacoco\test\html\index.html

echo.
echo Opening test report in browser...
start "" "build\reports\tests\test\index.html"

timeout /t 2 /nobreak >nul

echo Opening coverage report in browser...
start "" "build\reports\jacoco\test\html\index.html"

echo.
echo Done! Reports are now open in your browser.
pause
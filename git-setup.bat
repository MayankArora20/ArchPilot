@echo off
echo Setting up Git configurations for ArchPilot components...

echo.
echo To use UI-specific git configuration:
echo cd ArchPilotUI
echo git config --local include.path ../.gitconfig

echo.
echo To use Backend-specific git configuration:
echo cd ArchPilotBackend  
echo git config --local include.path ../.gitconfig

echo.
echo Available UI aliases after setup:
echo   git ui-build    - Build the UI
echo   git ui-test     - Test the UI
echo   git ui-start    - Start the UI dev server
echo   git ui-status   - Git status
echo   git ui-log      - Pretty git log

echo.
echo Available Backend aliases after setup:
echo   git be-build    - Build the backend
echo   git be-test     - Test the backend  
echo   git be-run      - Run the backend
echo   git be-clean    - Clean build
echo   git be-status   - Git status
echo   git be-log      - Pretty git log

echo.
echo Setup complete! Navigate to each directory and run the git config command shown above.
pause
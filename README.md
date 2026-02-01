# ArchPilot

A comprehensive architectural analysis and design tool that combines AI-powered insights with interactive visualization capabilities.

## Project Structure

This repository contains two main components:

### ArchPilotUI
Angular-based frontend application providing:
- Interactive chat interface for architectural discussions
- PlantUML diagram visualization
- Project management capabilities
- Modern, responsive UI design

### ArchPilotBackend
Spring Boot backend service providing:
- RESTful API endpoints
- Gemini AI integration
- Architectural analysis services
- Robust backend infrastructure

## Quick Start

### Frontend (ArchPilotUI)
```bash
cd ArchPilotUI
npm install
npm start
```
Access the UI at `http://localhost:4200`

### Backend (ArchPilotBackend)
```bash
cd ArchPilotBackend
./gradlew bootRun
```
API available at `http://localhost:8080`

## Features

- **AI-Powered Analysis**: Leverage Gemini AI for architectural insights
- **Interactive Chat**: Natural language interface for architectural discussions
- **Diagram Visualization**: PlantUML integration for visual representations
- **Project Management**: Organize and manage multiple architectural projects
- **Modern Tech Stack**: Angular + Spring Boot for robust full-stack development

## Documentation

- [UI Documentation](./ArchPilotUI/README.md)
- [Backend Documentation](./ArchPilotBackend/README.md)
- [Architecture Guide](./ArchPilotUI/ARCHITECTURE.md)
- [Development Guide](./ArchPilotUI/DEVELOPMENT_GUIDE.md)

## Contributing

Each component (UI and Backend) has its own git configuration for independent development while maintaining the unified project structure.

## License

Private Repository - All Rights Reserved
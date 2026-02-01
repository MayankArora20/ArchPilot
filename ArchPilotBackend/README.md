# ArchPilot Backend

Spring Boot backend service for the ArchPilot application.

## Features

- RESTful API endpoints
- Gemini AI integration for architectural analysis
- Spring Boot framework
- Gradle build system

## Getting Started

### Prerequisites

- Java 17 or higher
- Gradle 7.0 or higher

### Running the Application

1. Navigate to the backend directory:
   ```bash
   cd ArchPilotBackend
   ```

2. Run the application:
   ```bash
   ./gradlew bootRun
   ```
   
   Or on Windows:
   ```bash
   gradlew.bat bootRun
   ```

3. The API will be available at `http://localhost:8080`

## API Endpoints

- `POST /api/agent/analyze` - Analyze architectural patterns
- `GET /api/agent/health` - Health check endpoint

## Configuration

Configure the application using `application.properties` or `application-local.properties` for local development.

## Build

To build the application:
```bash
./gradlew build
```

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/archpilot/
│   │       ├── ArchpilotApplication.java
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dto/
│   │       └── service/
│   └── resources/
│       ├── application.properties
│       └── application-local.properties
```
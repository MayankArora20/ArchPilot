# ArchPilot QuickStart Guide

## Prerequisites

- Java 21 or higher
- PostgreSQL 12 or higher
- Git

## Database Setup

Choose one of the following options:

### Option 1: Docker (Recommended - Easiest)

**Prerequisites:**
- Docker Desktop installed and running

**Steps:**
1. Run the setup script:
   ```bash
   start-postgres.bat
   ```

2. Wait for the database to start (about 30 seconds)

That's it! The Docker container will automatically:
- Create the `archpilot` database
- Create the `archpilot_user` with proper permissions
- Start PostgreSQL on port 5432

**Docker Commands:**
```bash
# Start PostgreSQL
docker-compose up -d

# Stop PostgreSQL
docker-compose down

# View logs
docker-compose logs postgres

# Connect to database
docker exec -it archpilot-postgres psql -U archpilot_user -d archpilot
```

### Option 2: Manual PostgreSQL Installation

**Windows:**
1. Download PostgreSQL from https://www.postgresql.org/download/windows/
2. Run the installer and follow the setup wizard
3. Remember the password you set for the `postgres` user
4. Add PostgreSQL bin directory to your PATH (usually `C:\Program Files\PostgreSQL\15\bin`)

**Create Database and User:**
1. Open Command Prompt as Administrator
2. Connect to PostgreSQL:
   ```bash
   psql -U postgres -h localhost
   ```
3. Run the setup script:
   ```bash
   psql -U postgres -h localhost -f database-setup.sql
   ```

   Or manually execute:
   ```sql
   CREATE DATABASE archpilot;
   CREATE USER archpilot_user WITH PASSWORD 'archpilot_password';
   GRANT ALL PRIVILEGES ON DATABASE archpilot TO archpilot_user;
   \c archpilot
   GRANT ALL ON SCHEMA public TO archpilot_user;
   GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO archpilot_user;
   GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO archpilot_user;
   \q
   ```

## Application Setup

### 1. Clone and Build

```bash
git clone <your-repo-url>
cd archpilot
./gradlew build
```

### 2. Configure Environment

The application uses `application-local.properties` for local development. Update the database credentials if needed:

```properties
# Database Configuration (if different from defaults)
spring.datasource.url=jdbc:postgresql://localhost:5432/archpilot
spring.datasource.username=archpilot_user
spring.datasource.password=archpilot_password
```

### 3. Run the Application

**Using Gradle:**
```bash
./gradlew bootRun
```

**Using the batch file:**
```bash
run-app.bat
```

**Using Java directly:**
```bash
./gradlew build
java -jar build/libs/archpilot-0.0.1-SNAPSHOT.jar
```

## Verify Setup

### 1. Check Application Health
- Open http://localhost:8080/actuator/health (if actuator is enabled)
- Or check the console logs for successful startup

### 2. Test Database Connection
- Visit http://localhost:8080/api/test/db-connection
- Should return connection status and test data

### 3. API Documentation
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Troubleshooting

### Database Connection Issues

**Error: "role 'archpilot_user' does not exist"**
- The database user wasn't created properly
- **Docker Solution:** Run `start-postgres.bat` to recreate the container
- **Manual Solution:** Run the database setup commands again

**Error: "database 'archpilot' does not exist"**
- **Docker Solution:** Run `docker-compose down` then `start-postgres.bat`
- **Manual Solution:** Create the database: `CREATE DATABASE archpilot;`

**Error: "Connection refused"**
- **Docker:** Check if Docker is running: `docker ps`
- **Manual:** Check if PostgreSQL service is running
- Verify PostgreSQL is listening on port 5432

**General Database Troubleshooting:**
1. **Docker users:**
   ```bash
   # Check container status
   docker ps
   
   # View PostgreSQL logs
   docker-compose logs postgres
   
   # Restart container
   docker-compose restart postgres
   
   # Connect to database directly
   docker exec -it archpilot-postgres psql -U archpilot_user -d archpilot
   ```

2. **Manual installation users:**
   ```bash
   # Test PostgreSQL connection
   psql -U postgres -h localhost -c "SELECT version();"
   
   # Check if database exists
   psql -U postgres -l
   
   # Check if user exists
   psql -U postgres -c "\du"
   ```

### Application Issues
1. Check Java version: `java -version`
2. Verify Gradle build: `./gradlew clean build`
3. Check application logs for detailed error messages

### Port Conflicts
If port 8080 is in use, change it in `application-local.properties`:
```properties
server.port=8081
```

## Next Steps

1. Review the [Database Documentation](requirements/DB.md) for schema details
2. Explore the API endpoints using Swagger UI
3. Check the requirements folder for feature specifications
4. Start developing your AI agent features!
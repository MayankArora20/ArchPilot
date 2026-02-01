# Database Documentation

## Overview

ArchPilot uses PostgreSQL as its primary database for storing agent configurations, conversation history, and application data.

## Database Configuration

### Connection Details
- **Database Name:** archpilot
- **Default Port:** 5432
- **Username:** archpilot_user
- **Password:** archpilot_password (configurable)

### Spring Boot Configuration

The application uses Spring Data JPA with Hibernate for database operations.

**Key Configuration Properties:**
```properties
# Database Connection
spring.datasource.url=jdbc:postgresql://localhost:5432/archpilot
spring.datasource.username=archpilot_user
spring.datasource.password=archpilot_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Settings
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
```

## Database Schema

### Current Tables

#### test_data (Development/Testing)
Simple table for testing database connectivity and basic operations.

```sql
CREATE TABLE test_data (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Planned Schema (Future Development)

#### agents
Stores AI agent configurations and metadata.
```sql
CREATE TABLE agents (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    model_name VARCHAR(100),
    temperature DECIMAL(3,2),
    max_tokens INTEGER,
    system_prompt TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### conversations
Tracks conversation sessions with agents.
```sql
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT REFERENCES agents(id),
    session_id VARCHAR(255) NOT NULL,
    title VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### messages
Stores individual messages within conversations.
```sql
CREATE TABLE messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT REFERENCES conversations(id),
    role VARCHAR(20) NOT NULL, -- 'user', 'assistant', 'system'
    content TEXT NOT NULL,
    token_count INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### agent_configurations
Stores dynamic configuration parameters for agents.
```sql
CREATE TABLE agent_configurations (
    id BIGSERIAL PRIMARY KEY,
    agent_id BIGINT REFERENCES agents(id),
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(50), -- 'string', 'number', 'boolean', 'json'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(agent_id, config_key)
);
```

## Database Operations

### Connection Testing
The application provides endpoints to test database connectivity:
- `GET /api/test/db-connection` - Tests basic connection
- `GET /api/test/db-data` - Retrieves test data
- `POST /api/test/db-data` - Inserts test data

### Migration Strategy
- **Development:** Uses `hibernate.ddl-auto=update` for automatic schema updates
- **Production:** Should use `hibernate.ddl-auto=validate` with proper migration scripts

### Backup and Recovery
```bash
# Backup
pg_dump -U archpilot_user -h localhost archpilot > archpilot_backup.sql

# Restore
psql -U archpilot_user -h localhost archpilot < archpilot_backup.sql
```

## Performance Considerations

### Indexing Strategy
```sql
-- Conversation lookups
CREATE INDEX idx_conversations_agent_id ON conversations(agent_id);
CREATE INDEX idx_conversations_session_id ON conversations(session_id);

-- Message queries
CREATE INDEX idx_messages_conversation_id ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);

-- Configuration lookups
CREATE INDEX idx_agent_configurations_agent_id ON agent_configurations(agent_id);
```

### Connection Pooling
Spring Boot automatically configures HikariCP connection pool with sensible defaults.

## Security

### Database Security
- Use strong passwords for database users
- Limit database user privileges to necessary operations only
- Enable SSL connections in production
- Regular security updates for PostgreSQL

### Application Security
- Database credentials stored in environment-specific property files
- No hardcoded passwords in source code
- Use of prepared statements (automatic with JPA)

## Monitoring and Maintenance

### Health Checks
- Database connectivity monitoring via Spring Actuator
- Connection pool metrics
- Query performance monitoring

### Maintenance Tasks
- Regular VACUUM and ANALYZE operations
- Index maintenance
- Log rotation
- Backup verification

## Development Guidelines

### Entity Design
- Use JPA annotations for entity mapping
- Follow naming conventions (snake_case for database, camelCase for Java)
- Include audit fields (created_at, updated_at) where appropriate
- Use appropriate data types and constraints

### Repository Pattern
- Extend JpaRepository for basic CRUD operations
- Use @Query annotations for complex queries
- Implement custom repository methods when needed

### Testing
- Use @DataJpaTest for repository layer testing
- Use TestContainers for integration testing with real PostgreSQL
- Mock database interactions in unit tests

## Environment-Specific Configuration

### Development
```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
logging.level.org.hibernate.SQL=DEBUG
```

### Production
```properties
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.datasource.hikari.maximum-pool-size=20
```

## Troubleshooting

### Common Issues
1. **Connection refused:** Check if PostgreSQL is running
2. **Authentication failed:** Verify username/password
3. **Database does not exist:** Create the database first
4. **Permission denied:** Grant proper privileges to user
5. **Port conflicts:** Check if port 5432 is available

### Diagnostic Queries
```sql
-- Check active connections
SELECT * FROM pg_stat_activity WHERE datname = 'archpilot';

-- Check table sizes
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables WHERE schemaname = 'public';

-- Check index usage
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes;
```
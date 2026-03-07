# WorshipHub Database Setup

## Quick Start

### 1. Start Database with PostGIS
```bash
# Start PostgreSQL with PostGIS
.\start-database.bat

# Or use the database manager
.\db-manager.bat start
```

### 2. Run Application
```bash
# Start with local profile (uses PostgreSQL)
.\gradlew :api:bootRun --args='--spring.profiles.active=local'
```

## Database Configuration

### PostGIS Setup
- **Image**: `postgis/postgis:16-3.5`
- **Extensions**: PostGIS, PostGIS Topology
- **Port**: 5442 (mapped from container's 5432)
- **Database**: worshiphub
- **User**: postgres / postgres

### Connection Details
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5442/worshiphub
    username: postgres
    password: postgres
    driver-class-name: org.postgresql.Driver
```

## Database Management Commands

```bash
# Database operations
.\db-manager.bat start     # Start database
.\db-manager.bat stop      # Stop database
.\db-manager.bat restart   # Restart database
.\db-manager.bat reset     # Reset database (deletes all data)
.\db-manager.bat logs      # View logs
.\db-manager.bat connect   # Connect via psql
.\db-manager.bat status    # Check status
```

## Application Profiles

### Local Development (`local`)
- **Database**: PostgreSQL with PostGIS
- **Port**: 9090
- **Flyway**: Disabled (uses JPA ddl-auto=update)
- **Logging**: DEBUG enabled

### Default (`dev`)
- **Database**: H2 in-memory
- **Flyway**: Enabled
- **Logging**: INFO level

## PostGIS Features Available

- Spatial data types (POINT, POLYGON, etc.)
- Spatial functions and operators
- Spatial indexing
- Coordinate system transformations

## Troubleshooting

### Database Connection Issues
```bash
# Check if container is running
docker ps

# Check database logs
.\db-manager.bat logs

# Test connection
.\db-manager.bat connect
```

### PostGIS Verification
```sql
-- Check PostGIS version
SELECT PostGIS_Version();

-- List installed extensions
SELECT * FROM pg_extension;
```
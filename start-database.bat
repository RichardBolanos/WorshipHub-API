@echo off
echo Starting WorshipHub PostgreSQL with PostGIS...

REM Stop existing container if running
docker-compose down

REM Remove old data if needed (uncomment next line to reset database)
REM rmdir /s /q data\db

REM Start PostgreSQL with PostGIS
docker-compose up -d

echo Waiting for database to be ready...
timeout /t 10 /nobreak > nul

REM Check if database is ready
docker exec WorshipHubPostgres pg_isready -U postgres -d worshiphub

if %errorlevel% equ 0 (
    echo Database is ready!
    echo Connection details:
    echo   Host: localhost
    echo   Port: 5442
    echo   Database: worshiphub
    echo   Username: postgres
    echo   Password: postgres
    echo.
    echo PostGIS extensions installed:
    docker exec WorshipHubPostgres psql -U postgres -d worshiphub -c "SELECT PostGIS_Version();"
) else (
    echo Database failed to start. Check logs with: docker-compose logs db
)

pause
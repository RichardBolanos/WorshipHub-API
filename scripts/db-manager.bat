@echo off
setlocal

if "%1"=="" goto usage
if "%1"=="start" goto start
if "%1"=="stop" goto stop
if "%1"=="restart" goto restart
if "%1"=="reset" goto reset
if "%1"=="logs" goto logs
if "%1"=="connect" goto connect
if "%1"=="status" goto status
goto usage

:start
echo Starting WorshipHub PostgreSQL with PostGIS...
docker-compose up -d
echo Waiting for database...
timeout /t 10 /nobreak > nul
docker exec WorshipHubPostgres pg_isready -U postgres -d worshiphub
if %errorlevel% equ 0 (
    echo Database ready at localhost:5442
) else (
    echo Database failed to start
)
goto end

:stop
echo Stopping WorshipHub PostgreSQL...
docker-compose down
goto end

:restart
echo Restarting WorshipHub PostgreSQL...
docker-compose down
docker-compose up -d
echo Waiting for database...
timeout /t 10 /nobreak > nul
docker exec WorshipHubPostgres pg_isready -U postgres -d worshiphub
goto end

:reset
echo WARNING: This will delete all data!
set /p confirm="Are you sure? (y/N): "
if /i "%confirm%"=="y" (
    docker-compose down
    rmdir /s /q data\db 2>nul
    docker-compose up -d
    echo Database reset complete
) else (
    echo Reset cancelled
)
goto end

:logs
docker-compose logs -f db
goto end

:connect
echo Connecting to WorshipHub database...
docker exec -it WorshipHubPostgres psql -U postgres -d worshiphub
goto end

:status
docker-compose ps
docker exec WorshipHubPostgres pg_isready -U postgres -d worshiphub
if %errorlevel% equ 0 (
    echo PostGIS Version:
    docker exec WorshipHubPostgres psql -U postgres -d worshiphub -c "SELECT PostGIS_Version();"
)
goto end

:usage
echo Usage: %0 [start^|stop^|restart^|reset^|logs^|connect^|status]
echo.
echo Commands:
echo   start    - Start the database
echo   stop     - Stop the database
echo   restart  - Restart the database
echo   reset    - Reset database (deletes all data)
echo   logs     - Show database logs
echo   connect  - Connect to database via psql
echo   status   - Show database status
goto end

:end
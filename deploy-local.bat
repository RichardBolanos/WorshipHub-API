@echo off
setlocal enabledelayedexpansion

:: ============================================================
::  WorshipHub - Local Native Deploy Script
::  Builds and manages the Docker Compose stack:
::    PostgreSQL + Mailpit + Backend (GraalVM Native)
:: ============================================================

echo.
echo   =============================================
echo    __        __              _     _       _   _       _
echo    \ \      / /__  _ __ ___^| ^|__ ^(_)_ __ ^| ^| ^| ^|_   _^| ^|__
echo     \ \ /\ / / _ \^| '__/ __^| '_ \^| ^| '_ \^| ^|_^| ^| ^| ^| ^| '_ \
echo      \ V  V / (_) ^| ^|  \__ \ ^| ^| ^| ^| ^|_) ^|  _  ^| ^|_^| ^| ^|_) ^|
echo       \_/\_/ \___/^|_^|  ^|___/_^| ^|_^|_^| .__/^|_^| ^|_^|\__,_^|_.__/
echo                                     ^|_^|
echo   =============================================
echo    Local Native Deploy - Docker Compose Stack
echo   =============================================
echo.

:: -----------------------------------------------------------
::  Verify Docker Desktop is running
:: -----------------------------------------------------------
docker info >NUL 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Docker is not available.
    echo.
    echo   Please make sure Docker Desktop is installed and running.
    echo   Download: https://www.docker.com/products/docker-desktop
    echo.
    echo   After starting Docker Desktop, wait a few seconds and try again.
    exit /b 1
)

:: -----------------------------------------------------------
::  Route to the requested command
:: -----------------------------------------------------------
if "%~1"=="" goto :cmd_default
if /i "%~1"=="build" goto :cmd_build
if /i "%~1"=="up" goto :cmd_up
if /i "%~1"=="down" goto :cmd_down
if /i "%~1"=="logs" goto :cmd_logs
if /i "%~1"=="rebuild" goto :cmd_rebuild
if /i "%~1"=="clean" goto :cmd_clean
goto :cmd_help

:: -----------------------------------------------------------
::  DEFAULT: build + up
:: -----------------------------------------------------------
:cmd_default
echo [INFO] Building backend image and starting all services...
echo.
docker compose build backend
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build failed. Check the output above for details.
    exit /b 1
)
docker compose up -d
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Failed to start services. Check the output above for details.
    exit /b 1
)
goto :show_urls

:: -----------------------------------------------------------
::  BUILD: only build the backend image
:: -----------------------------------------------------------
:cmd_build
echo [INFO] Building backend image...
echo.
docker compose build backend
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Build failed. Check the output above for details.
    exit /b 1
)
goto :show_urls

:: -----------------------------------------------------------
::  UP: only start services
:: -----------------------------------------------------------
:cmd_up
echo [INFO] Starting all services...
echo.
docker compose up -d
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Failed to start services. Check the output above for details.
    exit /b 1
)
goto :show_urls

:: -----------------------------------------------------------
::  DOWN: stop services
:: -----------------------------------------------------------
:cmd_down
echo [INFO] Stopping all services...
echo.
docker compose down
echo.
echo [OK] All services stopped.
goto :eof

:: -----------------------------------------------------------
::  LOGS: follow backend logs
:: -----------------------------------------------------------
:cmd_logs
echo [INFO] Following backend logs (Ctrl+C to stop)...
echo.
docker compose logs -f backend
goto :eof

:: -----------------------------------------------------------
::  REBUILD: no-cache build + up
:: -----------------------------------------------------------
:cmd_rebuild
echo [INFO] Rebuilding backend image (no cache) and starting...
echo.
docker compose build --no-cache backend
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Rebuild failed. Check the output above for details.
    exit /b 1
)
docker compose up -d backend
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Failed to start backend. Check the output above for details.
    exit /b 1
)
goto :show_urls

:: -----------------------------------------------------------
::  CLEAN: remove volumes, delete data, recreate
:: -----------------------------------------------------------
:cmd_clean
echo [INFO] Cleaning up: stopping services, removing volumes, deleting data...
echo.
docker compose down -v
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Failed to stop services. Check the output above for details.
    exit /b 1
)
if exist ".\data\db" (
    echo [INFO] Removing database data directory...
    rmdir /s /q ".\data\db"
)
echo [INFO] Starting fresh stack...
echo.
docker compose up -d
if %ERRORLEVEL% neq 0 (
    echo.
    echo [ERROR] Failed to start services. Check the output above for details.
    exit /b 1
)
goto :show_urls

:: -----------------------------------------------------------
::  HELP: unknown command
:: -----------------------------------------------------------
:cmd_help
echo [ERROR] Unknown command: %~1
echo.
echo Usage: deploy-local.bat [command]
echo.
echo Commands:
echo   (none)    Build backend image and start all services
echo   build     Build the backend Docker image only
echo   up        Start all services (without building)
echo   down      Stop all services
echo   logs      Follow backend container logs
echo   rebuild   Rebuild backend image (no cache) and restart
echo   clean     Remove volumes, delete DB data, and start fresh
echo.
exit /b 1

:: -----------------------------------------------------------
::  Show access URLs after successful start
:: -----------------------------------------------------------
:show_urls
echo.
echo   =============================================
echo    Services are starting up!
echo   =============================================
echo.
echo    API:      http://localhost:9090
echo    Swagger:  http://localhost:9090/swagger-ui/index.html
echo    Mailpit:  http://localhost:8025
echo    Health:   http://localhost:9090/api/v1/health
echo.
echo   =============================================
echo.

:eof
endlocal

@echo off
REM Script para resetear la base de datos y iniciar la aplicación
REM Uso: reset-and-start.bat

echo ========================================
echo WorshipHub - Reset Database and Start
echo ========================================
echo.

echo [1/4] Deteniendo contenedores existentes...
docker-compose down -v
if %ERRORLEVEL% NEQ 0 (
    echo Error al detener contenedores
    pause
    exit /b 1
)

echo.
echo [2/4] Eliminando datos de la base de datos...
if exist "data\db" (
    rmdir /s /q "data\db"
    echo Datos eliminados correctamente
) else (
    echo No hay datos previos para eliminar
)

echo.
echo [3/4] Iniciando contenedores (PostgreSQL + Mailpit)...
docker-compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo Error al iniciar contenedores
    pause
    exit /b 1
)

echo.
echo Esperando a que PostgreSQL esté listo...
timeout /t 10 /nobreak > nul

echo.
echo [4/4] Iniciando aplicación Spring Boot...
echo La aplicación se iniciará en el puerto 9090
echo Flyway ejecutará las migraciones automáticamente
echo.
echo Presiona Ctrl+C para detener la aplicación
echo.

cd api
..\gradlew.bat bootRun --args='--spring.profiles.active=local'

pause

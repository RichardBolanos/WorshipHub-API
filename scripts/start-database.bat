@echo off
echo ========================================
echo  WorshipHub - PostgreSQL con PostGIS
echo ========================================

echo.
echo Verificando Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker no esta instalado o no esta en el PATH
    echo Por favor, instala Docker Desktop desde: https://www.docker.com/products/docker-desktop
    pause
    exit /b 1
)
echo Docker detectado ✓

echo.
echo Verificando Docker Compose...
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Docker Compose no esta disponible
    pause
    exit /b 1
)
echo Docker Compose detectado ✓

echo.
echo Deteniendo contenedores existentes...
docker-compose down

REM Uncomment to reset database (delete all data)
REM echo Eliminando datos antiguos...
REM rmdir /s /q data\db

echo.
echo Verificando si el puerto 5442 esta en uso...
netstat -an | findstr ":5442 " >nul 2>&1
if not errorlevel 1 (
    echo [ADVERTENCIA] Puerto 5442 ya esta en uso
    echo Intentando detener contenedor existente...
    docker stop WorshipHubPostgres 2>nul
    docker rm WorshipHubPostgres 2>nul
    timeout /t 2 /nobreak >nul
)

echo.
echo Iniciando PostgreSQL con PostGIS...
docker-compose up -d

echo.
echo Esperando a que la base de datos este lista...
set /a counter=0
:wait_loop
timeout /t 2 /nobreak > nul
docker exec WorshipHubPostgres pg_isready -U postgres -d worshiphub >nul 2>&1
if %errorlevel% equ 0 goto db_ready
set /a counter+=1
if %counter% lss 15 (
    echo Intentando conectar... (%counter%/15^)
    goto wait_loop
)

echo.
echo [ERROR] La base de datos no respondio despues de 30 segundos
echo Verifica los logs con: docker-compose logs db
pause
exit /b 1

:db_ready
echo.
echo ========================================
echo  Servicios Docker Listos!
echo ========================================
echo.
echo PostgreSQL:
echo   Host: localhost
echo   Port: 5442
echo   Database: worshiphub
echo   Username: postgres
echo   Password: postgres
echo   JDBC URL: jdbc:postgresql://localhost:5442/worshiphub
echo.
echo Mailpit (Servidor de Email de Desarrollo):
echo   Web UI: http://localhost:8025
echo   SMTP Server: localhost:1025
echo   Uso: Ver emails enviados por la aplicacion
echo.
echo ========================================

echo.
echo Verificando extensiones PostGIS...
docker exec WorshipHubPostgres psql -U postgres -d worshiphub -c "SELECT PostGIS_Version();" 2>nul
if %errorlevel% neq 0 (
    echo [ADVERTENCIA] PostGIS puede no estar instalado correctamente
)

echo.
echo ========================================
echo  Comandos Utiles
echo ========================================
echo.
echo Detener servicios:
echo   docker-compose down
echo.
echo Ver logs:
echo   docker-compose logs -f
echo   docker-compose logs -f db
echo   docker-compose logs -f mailpit
echo.
echo Abrir Mailpit:
echo   start http://localhost:8025
echo.
echo Conectar a PostgreSQL:
echo   docker exec -it WorshipHubPostgres psql -U postgres -d worshiphub
echo.
echo ========================================
echo.
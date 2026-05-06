@echo off
setlocal enabledelayedexpansion

echo.
echo ========================================
echo     WorshipHub API - Inicio Rapido
echo ========================================
echo.

:menu
echo Selecciona el modo de inicio:
echo.
echo   1. Desarrollo Local (PostgreSQL + Mailpit)
echo   2. Desarrollo Rapido (H2 en memoria)
echo   3. Solo iniciar servicios Docker
echo   4. Detener todos los servicios
echo   5. Ver logs (PostgreSQL)
echo   6. Ver logs (Mailpit)
echo   7. Ver logs (Todos los servicios)
echo   8. Limpiar y reiniciar base de datos
echo   9. Abrir Mailpit en navegador
echo   A. Reparar puertos ocupados
echo   B. Build y Push imagen Docker a GitHub (GHCR)
echo   0. Salir
echo.

choice /C 1234567890AB /N /M "Opcion: "
set option=%errorlevel%

if %option%==1 goto start_local
if %option%==2 goto start_h2
if %option%==3 goto start_db_only
if %option%==4 goto stop_services
if %option%==5 goto view_logs_db
if %option%==6 goto view_logs_mailpit
if %option%==7 goto view_logs_all
if %option%==8 goto clean_restart
if %option%==9 goto open_mailpit
if %option%==10 goto end
if %option%==11 goto fix_ports
if %option%==12 goto push_ghcr

:start_local
echo.
echo ========================================
echo  Iniciando en modo Desarrollo Local
echo ========================================
echo.
echo Este modo incluye:
echo   - PostgreSQL 16 con PostGIS (puerto 5442)
echo   - Mailpit para emails de desarrollo
echo     * Web UI: http://localhost:8025
echo     * SMTP: localhost:1025
echo.

REM Check if database is running
netstat -an | findstr ":5442 " >nul 2>&1
if errorlevel 1 (
    echo PostgreSQL no detectado. Iniciando servicios Docker...
    echo.
    call "%~dp0start-database.bat"
    if errorlevel 1 (
        echo.
        echo [ERROR] No se pudo iniciar la base de datos
        pause
        goto menu
    )
    echo.
    echo [OK] Servicios Docker iniciados correctamente
) else (
    echo PostgreSQL ya esta corriendo en puerto 5442
    
    REM Check if it's our container
    docker ps | findstr "WorshipHubPostgres" >nul 2>&1
    if errorlevel 1 (
        echo.
        echo [ADVERTENCIA] Hay algo corriendo en puerto 5442 pero no es nuestro contenedor
        echo.
        choice /C 123 /N /M "Que deseas hacer? (1=Detener y reiniciar, 2=Continuar, 3=Cancelar): "
        
        if errorlevel 3 goto menu
        if errorlevel 2 goto check_mailpit
        if errorlevel 1 (
            echo.
            echo Deteniendo servicios y reiniciando...
            cd /d "%~dp0.."
            docker-compose down
            timeout /t 2 /nobreak >nul
            call "%~dp0start-database.bat"
            if errorlevel 1 (
                echo.
                echo [ERROR] No se pudo iniciar la base de datos
                pause
                goto menu
            )
        )
    )
)

:check_mailpit
REM Check if Mailpit is running
netstat -an | findstr ":8025 " >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ADVERTENCIA] Mailpit no esta corriendo
    echo Los servicios Docker deberan estar iniciados...
) else (
    echo Mailpit esta corriendo en puerto 8025
)

echo.
echo ========================================
echo  Iniciando Aplicacion
echo ========================================
echo.
cd /d "%~dp0..\api\scripts"
call start-local.bat
goto end

:start_h2
echo.
echo ========================================
echo  Iniciando en modo Desarrollo Rapido
echo ========================================
echo.
echo Este modo usa H2 en memoria (no requiere Docker)
echo Los datos se perderan al cerrar la aplicacion
echo.
echo NOTA: Los emails NO se enviaran en este modo
echo.
cd /d "%~dp0..\api\scripts"
call start-h2.bat
goto end

:start_db_only
echo.
echo ========================================
echo  Iniciando Servicios Docker
echo ========================================
echo.
call "%~dp0start-database.bat"
echo.
echo ========================================
echo  Servicios Iniciados
echo ========================================
echo.
echo PostgreSQL:
echo   Host: localhost:5442
echo   Database: worshiphub
echo   User: postgres / postgres
echo.
echo Mailpit:
echo   Web UI: http://localhost:8025
echo   SMTP: localhost:1025
echo.
echo Para iniciar la aplicacion:
echo   cd api\scripts
echo   start-local.bat
echo.
pause
goto menu

:stop_services
echo.
echo ========================================
echo  Deteniendo Servicios Docker
echo ========================================
echo.
cd /d "%~dp0.."
docker-compose down
if errorlevel 1 (
    echo.
    echo [ERROR] No se pudieron detener los servicios
    echo Verifica que Docker este corriendo
) else (
    echo.
    echo [OK] Servicios detenidos correctamente
    echo   - PostgreSQL detenido
    echo   - Mailpit detenido
)
echo.
pause
goto menu

:view_logs_db
echo.
echo ========================================
echo  Logs de PostgreSQL
echo ========================================
echo.
echo Presiona Ctrl+C para salir de los logs
echo.
timeout /t 2 /nobreak >nul
cd /d "%~dp0.."
docker-compose logs -f db
goto menu

:view_logs_mailpit
echo.
echo ========================================
echo  Logs de Mailpit
echo ========================================
echo.
echo Presiona Ctrl+C para salir de los logs
echo.
timeout /t 2 /nobreak >nul
cd /d "%~dp0.."
docker-compose logs -f mailpit
goto menu

:view_logs_all
echo.
echo ========================================
echo  Logs de Todos los Servicios
echo ========================================
echo.
echo Presiona Ctrl+C para salir de los logs
echo.
timeout /t 2 /nobreak >nul
cd /d "%~dp0.."
docker-compose logs -f
goto menu

:clean_restart
echo.
echo ========================================
echo  Limpiar y Reiniciar Base de Datos
echo ========================================
echo.
echo [ADVERTENCIA] Esto eliminara TODOS los datos de la base de datos
echo Los emails en Mailpit tambien se perderan
echo.
choice /C SN /M "Estas seguro? (S/N): "
if errorlevel 2 goto menu

echo.
echo Deteniendo servicios...
cd /d "%~dp0.."
docker-compose down

echo Eliminando datos de PostgreSQL...
if exist data\db (
    rmdir /s /q data\db
    echo   [OK] Datos eliminados
) else (
    echo   [INFO] No hay datos para eliminar
)

echo.
echo Iniciando servicios limpios...
call "%~dp0start-database.bat"

echo.
echo ========================================
echo [OK] Base de datos reiniciada con exito
echo ========================================
echo.
pause
goto menu

:open_mailpit
echo.
echo Abriendo Mailpit en el navegador...
echo.

REM Check if Mailpit is running
netstat -an | findstr ":8025 " >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Mailpit no esta corriendo
    echo.
    echo Inicia los servicios Docker primero (Opcion 3)
    echo.
    pause
    goto menu
)

start http://localhost:8025
echo.
echo Mailpit abierto en el navegador
echo URL: http://localhost:8025
echo.
pause
goto menu

:fix_ports
echo.
call "%~dp0fix-ports.bat"
goto menu

:push_ghcr
echo.
echo ========================================
echo  Build y Push imagen Docker a GHCR
echo ========================================
echo.
echo Este proceso construira la imagen nativa de GraalVM
echo y la subira a GitHub Container Registry (ghcr.io).
echo.
echo Requisitos:
echo   - Docker Desktop corriendo
echo   - Personal Access Token de GitHub con scope: write:packages
echo     https://github.com/settings/tokens
echo.

REM Verify Docker is running
docker info >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker no esta corriendo. Inicia Docker Desktop.
    echo.
    pause
    goto menu
)

REM GitHub user/owner (lowercase required by GHCR)
set "GHCR_OWNER=richardbolanos"
set "IMAGE_NAME=worshiphub-api"
set "GHCR_IMAGE=ghcr.io/%GHCR_OWNER%/%IMAGE_NAME%"

REM Get short commit SHA for versioned tag
for /f "delims=" %%i in ('git -C "%~dp0.." rev-parse --short HEAD 2^>nul') do set "GIT_SHA=%%i"
if "%GIT_SHA%"=="" set "GIT_SHA=manual"

echo Imagen destino : %GHCR_IMAGE%
echo Tags          : latest, %GIT_SHA%
echo.

REM Resolve token: prefer GHCR_TOKEN, fallback GITHUB_TOKEN, else prompt
set "TOKEN=%GHCR_TOKEN%"
if "%TOKEN%"=="" set "TOKEN=%GITHUB_TOKEN%"

if "%TOKEN%"=="" (
    echo.
    echo ========================================
    echo  Token de GitHub no encontrado
    echo ========================================
    echo.
    echo No se encontro la variable de entorno GHCR_TOKEN ni GITHUB_TOKEN.
    echo.
    echo Como crear un Personal Access Token (PAT^):
    echo.
    echo   1. Abre: https://github.com/settings/tokens/new
    echo   2. Note: WorshipHub GHCR
    echo   3. Expiration: 90 days (o la que prefieras^)
    echo   4. Marca los scopes:
    echo        [x] write:packages   (sube imagenes^)
    echo        [x] read:packages    (lee imagenes^)
    echo        [x] delete:packages  (opcional, para borrar^)
    echo   5. Click "Generate token" y COPIA el token (empieza con ghp_^)
    echo.
    echo Como guardarlo permanentemente (recomendado^):
    echo.
    echo   PowerShell:
    echo     [Environment]::SetEnvironmentVariable("GHCR_TOKEN","ghp_xxxx","User"^)
    echo.
    echo   CMD (solo sesion actual^):
    echo     set GHCR_TOKEN=ghp_xxxx
    echo.
    echo   CMD (permanente^):
    echo     setx GHCR_TOKEN "ghp_xxxx"
    echo.
    echo Despues de guardarlo, ABRE UNA NUEVA TERMINAL para que tome efecto.
    echo.
    echo ----------------------------------------
    echo.
    choice /C SN /M "Quieres pegar el token ahora para usarlo solo esta vez? (S/N): "
    if errorlevel 2 (
        echo.
        echo Cancelado. Configura GHCR_TOKEN y vuelve a intentar.
        echo.
        pause
        goto menu
    )
    echo.
    set /p "TOKEN=Pega tu Personal Access Token: "
    if "!TOKEN!"=="" (
        echo.
        echo [ERROR] No se proporciono ningun token. Cancelando.
        echo.
        pause
        goto menu
    )
    REM Basic format validation
    echo !TOKEN! | findstr /R "^gh[ps]_" >nul
    if errorlevel 1 (
        echo.
        echo [ADVERTENCIA] El token no parece tener el formato esperado (ghp_... o ghs_...^).
        choice /C SN /M "Continuar de todos modos? (S/N): "
        if errorlevel 2 goto menu
    )
)

echo.
echo [1/3] Login en ghcr.io...
echo !TOKEN! | docker login ghcr.io -u %GHCR_OWNER% --password-stdin
if errorlevel 1 (
    echo.
    echo ========================================
    echo [ERROR] Fallo el login en ghcr.io
    echo ========================================
    echo.
    echo Posibles causas:
    echo   - Token invalido o expirado
    echo   - Token sin scope 'write:packages'
    echo   - Usuario incorrecto (actual: %GHCR_OWNER%^)
    echo.
    echo Verifica tu token en: https://github.com/settings/tokens
    echo.
    pause
    goto menu
)

echo.
echo [2/3] Construyendo imagen nativa (esto puede tardar varios minutos)...
cd /d "%~dp0.."
docker build -f Dockerfile.native -t %GHCR_IMAGE%:latest -t %GHCR_IMAGE%:%GIT_SHA% .
if errorlevel 1 (
    echo [ERROR] Fallo el build de la imagen.
    pause
    goto menu
)

echo.
echo [3/3] Haciendo push a GHCR...
docker push %GHCR_IMAGE%:latest
if errorlevel 1 (
    echo [ERROR] Fallo el push de :latest
    pause
    goto menu
)
docker push %GHCR_IMAGE%:%GIT_SHA%
if errorlevel 1 (
    echo [ERROR] Fallo el push de :%GIT_SHA%
    pause
    goto menu
)

echo.
echo ========================================
echo [OK] Imagen publicada correctamente
echo ========================================
echo.
echo URL : https://github.com/%GHCR_OWNER%?tab=packages
echo Imagen para Render.com:
echo   %GHCR_IMAGE%:latest
echo   %GHCR_IMAGE%:%GIT_SHA%
echo.
echo NOTA: Si la imagen es privada, en Render.com agrega
echo Registry Credentials (Account Settings) usando tu PAT.
echo.
pause
goto menu

:end
echo.
echo ========================================
echo   Gracias por usar WorshipHub!
echo ========================================
echo.
echo Servicios Docker siguen corriendo.
echo Para detenerlos, ejecuta: docker-compose down
echo.
endlocal
exit /b 0

@echo off
setlocal enabledelayedexpansion

echo.
echo ========================================
echo  WorshipHub - Reparar Puertos
echo ========================================
echo.

echo Este script te ayudara a liberar puertos ocupados
echo.

:menu
echo Que puerto deseas liberar?
echo.
echo   1. Puerto 9090 (API)
echo   2. Puerto 5442 (PostgreSQL)
echo   3. Puerto 8025 (Mailpit Web UI)
echo   4. Puerto 1025 (Mailpit SMTP)
echo   5. Todos los puertos
echo   6. Ver puertos en uso
echo   7. Detener contenedores Docker
echo   8. Volver
echo.

choice /C 12345678 /N /M "Opcion: "
set option=%errorlevel%

if %option%==1 goto fix_9090
if %option%==2 goto fix_5442
if %option%==3 goto fix_8025
if %option%==4 goto fix_1025
if %option%==5 goto fix_all
if %option%==6 goto show_ports
if %option%==7 goto stop_docker
if %option%==8 goto end

:fix_9090
echo.
echo Buscando proceso en puerto 9090...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":9090 "') do (
    set PID=%%a
    if not "!PID!"=="0" (
        echo Encontrado proceso con PID: !PID!
        tasklist /FI "PID eq !PID!" 2>nul | findstr /I "!PID!" >nul
        if not errorlevel 1 (
            echo Deteniendo proceso...
            taskkill /PID !PID! /F
            echo [OK] Proceso detenido
        )
    )
)
echo.
pause
goto menu

:fix_5442
echo.
echo Buscando proceso en puerto 5442...
echo.
echo Primero intentando detener contenedor Docker...
docker stop WorshipHubPostgres 2>nul
docker rm WorshipHubPostgres 2>nul
if errorlevel 1 (
    echo No hay contenedor Docker en ese puerto
    echo.
    echo Buscando otros procesos...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5442 "') do (
        set PID=%%a
        if not "!PID!"=="0" (
            echo Encontrado proceso con PID: !PID!
            tasklist /FI "PID eq !PID!" 2>nul | findstr /I "!PID!" >nul
            if not errorlevel 1 (
                echo Deteniendo proceso...
                taskkill /PID !PID! /F
                echo [OK] Proceso detenido
            )
        )
    )
) else (
    echo [OK] Contenedor Docker detenido
)
echo.
pause
goto menu

:fix_8025
echo.
echo Buscando proceso en puerto 8025...
echo.
echo Primero intentando detener contenedor Docker...
docker stop WorshipHubMailpit 2>nul
docker rm WorshipHubMailpit 2>nul
if errorlevel 1 (
    echo No hay contenedor Docker en ese puerto
    echo.
    echo Buscando otros procesos...
    for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8025 "') do (
        set PID=%%a
        if not "!PID!"=="0" (
            echo Encontrado proceso con PID: !PID!
            tasklist /FI "PID eq !PID!" 2>nul | findstr /I "!PID!" >nul
            if not errorlevel 1 (
                echo Deteniendo proceso...
                taskkill /PID !PID! /F
                echo [OK] Proceso detenido
            )
        )
    )
) else (
    echo [OK] Contenedor Docker detenido
)
echo.
pause
goto menu

:fix_1025
echo.
echo Buscando proceso en puerto 1025...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":1025 "') do (
    set PID=%%a
    if not "!PID!"=="0" (
        echo Encontrado proceso con PID: !PID!
        tasklist /FI "PID eq !PID!" 2>nul | findstr /I "!PID!" >nul
        if not errorlevel 1 (
            echo Deteniendo proceso...
            taskkill /PID !PID! /F
            echo [OK] Proceso detenido
        )
    )
)
echo.
pause
goto menu

:fix_all
echo.
echo Liberando todos los puertos...
echo.

echo Deteniendo contenedores Docker...
cd /d "%~dp0.."
docker-compose down
timeout /t 2 /nobreak >nul

echo.
echo Verificando puerto 9090...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":9090 "') do (
    set PID=%%a
    if not "!PID!"=="0" (
        taskkill /PID !PID! /F 2>nul
    )
)

echo Verificando puerto 5442...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":5442 "') do (
    set PID=%%a
    if not "!PID!"=="0" (
        taskkill /PID !PID! /F 2>nul
    )
)

echo Verificando puerto 8025...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":8025 "') do (
    set PID=%%a
    if not "!PID!"=="0" (
        taskkill /PID !PID! /F 2>nul
    )
)

echo Verificando puerto 1025...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr ":1025 "') do (
    set PID=%%a
    if not "!PID!"=="0" (
        taskkill /PID !PID! /F 2>nul
    )
)

echo.
echo [OK] Todos los puertos liberados
echo.
pause
goto menu

:show_ports
echo.
echo ========================================
echo  Puertos en Uso
echo ========================================
echo.

echo Puerto 9090 (API):
netstat -ano | findstr ":9090 " | findstr "LISTENING"
if errorlevel 1 echo   [OK] Disponible

echo.
echo Puerto 5442 (PostgreSQL):
netstat -ano | findstr ":5442 " | findstr "LISTENING"
if errorlevel 1 echo   [OK] Disponible

echo.
echo Puerto 8025 (Mailpit Web):
netstat -ano | findstr ":8025 " | findstr "LISTENING"
if errorlevel 1 echo   [OK] Disponible

echo.
echo Puerto 1025 (Mailpit SMTP):
netstat -ano | findstr ":1025 " | findstr "LISTENING"
if errorlevel 1 echo   [OK] Disponible

echo.
echo ========================================
echo.
pause
goto menu

:stop_docker
echo.
echo Deteniendo todos los contenedores Docker...
cd /d "%~dp0.."
docker-compose down
if errorlevel 1 (
    echo [ERROR] No se pudieron detener los contenedores
) else (
    echo [OK] Contenedores detenidos
)
echo.
pause
goto menu

:end
echo.
endlocal
exit /b 0

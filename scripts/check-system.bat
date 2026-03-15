@echo off
setlocal enabledelayedexpansion

echo.
echo ========================================
echo  WorshipHub - Verificacion del Sistema
echo ========================================
echo.

set "all_ok=1"

REM Check Java
echo [1/5] Verificando Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo   [ERROR] Java no encontrado
    echo   Instala Java 21 o superior desde: https://adoptium.net/
    set "all_ok=0"
) else (
    echo   [OK] Java instalado
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VERSION=%%g
    )
    set JAVA_VERSION=%JAVA_VERSION:"=%
    echo   Version: %JAVA_VERSION%
    
    REM Check if Java version is 25 or higher (not compatible)
    for /f "tokens=1 delims=." %%a in ("%JAVA_VERSION%") do set JAVA_MAJOR=%%a
    if %JAVA_MAJOR% GEQ 25 (
        echo.
        echo   [ERROR] Java %JAVA_VERSION% no es compatible con Kotlin 2.1.0
        echo   Por favor, instala Java 21 LTS desde: https://adoptium.net/temurin/releases/?version=21
        echo   Java 21 es la version LTS recomendada para este proyecto
        set "all_ok=0"
    )
)

echo.

REM Check Docker
echo [2/5] Verificando Docker...
docker --version >nul 2>&1
if errorlevel 1 (
    echo   [ERROR] Docker no encontrado
    echo   Instala Docker Desktop desde: https://www.docker.com/products/docker-desktop
    set "all_ok=0"
) else (
    echo   [OK] Docker instalado
    docker --version
    
    REM Check if Docker is running
    docker ps >nul 2>&1
    if errorlevel 1 (
        echo   [ADVERTENCIA] Docker no esta corriendo
        echo   Inicia Docker Desktop
        set "all_ok=0"
    ) else (
        echo   [OK] Docker esta corriendo
    )
)

echo.

REM Check Docker Compose
echo [3/5] Verificando Docker Compose...
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo   [ERROR] Docker Compose no encontrado
    set "all_ok=0"
) else (
    echo   [OK] Docker Compose instalado
    docker-compose --version
)

echo.

REM Check Gradle
echo [4/5] Verificando Gradle...
if exist gradlew.bat (
    echo   [OK] Gradle Wrapper encontrado
    call gradlew.bat --version 2>nul | findstr /C:"Gradle"
) else (
    echo   [ERROR] Gradle Wrapper no encontrado
    set "all_ok=0"
)

echo.

REM Check ports
echo [5/5] Verificando puertos disponibles...

netstat -an | findstr ":9090 " >nul 2>&1
if errorlevel 1 (
    echo   [OK] Puerto 9090 - API disponible
) else (
    echo   [ADVERTENCIA] Puerto 9090 - API ya esta en uso
    set "all_ok=0"
)

netstat -an | findstr ":5442 " >nul 2>&1
if errorlevel 1 (
    echo   [OK] Puerto 5442 - PostgreSQL disponible
) else (
    echo   [INFO] Puerto 5442 - PostgreSQL en uso
)

netstat -an | findstr ":8025 " >nul 2>&1
if errorlevel 1 (
    echo   [OK] Puerto 8025 - Mailpit disponible
) else (
    echo   [INFO] Puerto 8025 - Mailpit en uso
)

echo.
echo ========================================

if "%all_ok%"=="1" (
    echo.
    echo [EXITO] Sistema listo para ejecutar WorshipHub!
    echo.
    echo Siguiente paso:
    echo   Ejecuta: start-worshiphub.bat
    echo.
) else (
    echo.
    echo [ATENCION] Hay problemas que deben resolverse
    echo Por favor, corrige los errores indicados arriba
    echo.
)

echo ========================================
echo.

pause
endlocal
exit /b 0

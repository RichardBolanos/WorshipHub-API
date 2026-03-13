@echo off
echo ========================================
echo  WorshipHub - Inicio Desarrollo Local
echo ========================================

echo.
echo Verificando Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java no encontrado
    echo Por favor, instala Java 21 LTS desde: https://adoptium.net/temurin/releases/?version=21
    echo.
    pause
    exit /b 1
)

REM Get Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
set JAVA_VERSION=%JAVA_VERSION:"=%

REM Check if Java version is 25 or higher (not compatible)
for /f "tokens=1 delims=." %%a in ("%JAVA_VERSION%") do set JAVA_MAJOR=%%a
if %JAVA_MAJOR% GEQ 25 (
    echo.
    echo ========================================
    echo  [ERROR] Java Version Incompatible
    echo ========================================
    echo.
    echo Java detectado: %JAVA_VERSION%
    echo.
    echo Este proyecto requiere Java 21 LTS.
    echo Java 25+ no es compatible con Kotlin 2.1.0
    echo.
    echo Por favor, instala Java 21 LTS desde:
    echo https://adoptium.net/temurin/releases/?version=21
    echo.
    echo Despues de instalar Java 21:
    echo 1. Verifica con: java -version
    echo 2. Ejecuta: scripts\check-system.bat
    echo 3. Inicia la app: worshiphub.bat
    echo.
    echo ========================================
    pause
    exit /b 1
)

if %JAVA_MAJOR% LSS 17 (
    echo.
    echo [ADVERTENCIA] Java %JAVA_VERSION% detectado
    echo Se recomienda Java 21 LTS para mejor compatibilidad
    echo.
)

echo Java %JAVA_VERSION% detectado ✓

echo.
echo Verificando PostgreSQL en puerto 5442...
netstat -an | findstr ":5442 " >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERROR] PostgreSQL no esta ejecutandose en puerto 5442
    echo.
    echo Por favor, inicia PostgreSQL primero:
    echo   1. Ejecuta: scripts\start-database.bat
    echo   2. O usa el menu principal: worshiphub.bat
    echo.
    pause
    exit /b 1
)

echo PostgreSQL detectado en puerto 5442 ✓

echo.
echo Configurando variables de entorno...
set SPRING_PROFILES_ACTIVE=local
set DATABASE_URL=jdbc:postgresql://localhost:5442/worshiphub
set DATABASE_USERNAME=postgres
set DATABASE_PASSWORD=postgres
set JWT_SECRET=local-dev-secret-key-for-development-only-not-for-production-use-32chars

echo.
echo ========================================
echo  Configuracion de Inicio
echo ========================================
echo Perfil activo: %SPRING_PROFILES_ACTIVE%
echo Base de datos: %DATABASE_URL%
echo Usuario DB: %DATABASE_USERNAME%
echo Puerto API: 9090
echo.
echo URLs Importantes:
echo   API: http://localhost:9090/api/v1
echo   Swagger: http://localhost:9090/swagger-ui.html
echo   Health: http://localhost:9090/api/v1/health
echo   Mailpit: http://localhost:8025
echo.
echo Mailpit - Servidor de Email de Desarrollo:
echo   Todos los emails enviados por la app se pueden ver en:
echo   http://localhost:8025
echo.
echo ========================================

echo.
echo Iniciando aplicacion WorshipHub...
echo Flyway ejecutara las migraciones automaticamente...
echo.

cd /d "%~dp0..\.."
call gradlew.bat :api:bootRun --args="--spring.profiles.active=local"

pause
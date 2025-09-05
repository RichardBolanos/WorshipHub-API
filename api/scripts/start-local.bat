@echo off
echo ========================================
echo  WorshipHub - Inicio Desarrollo Local
echo ========================================

echo.
echo Verificando PostgreSQL en puerto 5432...
netstat -an | findstr :5432 >nul
if %errorlevel% neq 0 (
    echo ERROR: PostgreSQL no está ejecutándose en puerto 5432
    echo Por favor, inicia PostgreSQL antes de continuar
    pause
    exit /b 1
)

echo PostgreSQL detectado en puerto 5432 ✓

echo.
echo Configurando variables de entorno...
set SPRING_PROFILES_ACTIVE=local
set DATABASE_URL=jdbc:postgresql://localhost:5432/worshiphub
set DATABASE_USERNAME=postgres
set DATABASE_PASSWORD=admin

echo.
echo Iniciando aplicación WorshipHub...
echo Perfil activo: %SPRING_PROFILES_ACTIVE%
echo Base de datos: %DATABASE_URL%

cd /d "%~dp0.."
gradlew bootRun --args="--spring.profiles.active=local"

pause
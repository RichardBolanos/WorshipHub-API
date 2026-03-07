@echo off
echo ========================================
echo  WorshipHub - Inicio con H2 Database
echo ========================================

echo.
echo Iniciando aplicación con H2 en memoria...
echo Base de datos: H2 (en memoria)
echo Consola H2: http://localhost:9090/h2-console

cd /d "%~dp0.."
gradlew bootRun --args="--spring.profiles.active=h2"

pause
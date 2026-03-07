@echo off
echo ========================================
echo  WorshipHub API - Native Local Build
echo ========================================

echo.
echo [1/4] Compilando ejecutable nativo...
gradlew :api:nativeCompile -x test

if %ERRORLEVEL% NEQ 0 (
    echo ✗ Error en compilación nativa
    pause
    exit /b 1
)

echo.
echo [2/4] Creando imagen Docker...
docker build -f Dockerfile.native -t worshiphub-api-native:local .

if %ERRORLEVEL% NEQ 0 (
    echo ✗ Error creando imagen Docker
    pause
    exit /b 1
)

echo.
echo [3/4] Taggeando para Google Cloud...
docker tag worshiphub-api-native:local us-central1-docker.pkg.dev/worshiphub-478318/worshiphub-repo/worshiphub-api-native:latest

echo.
echo [4/4] Subiendo a Google Cloud...
docker push us-central1-docker.pkg.dev/worshiphub-478318/worshiphub-repo/worshiphub-api-native:latest

echo.
echo ========================================
echo  BUILD COMPLETADO
echo ========================================
echo.
echo Para ejecutar localmente:
echo   docker run -p 8080:8080 worshiphub-api-native:local
echo.
echo Para desplegar en Cloud Run:
echo   gcloud run deploy worshiphub-api-native --image us-central1-docker.pkg.dev/worshiphub-478318/worshiphub-repo/worshiphub-api-native:latest --region us-central1

pause
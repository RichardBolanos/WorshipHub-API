@echo off
setlocal enabledelayedexpansion

echo.
echo ========================================
echo     Mailpit - Gestor de Emails
echo ========================================
echo.

:menu
echo Opciones de Mailpit:
echo.
echo   1. Abrir Mailpit en navegador
echo   2. Ver estado de Mailpit
echo   3. Ver logs de Mailpit
echo   4. Reiniciar Mailpit
echo   5. Limpiar todos los emails
echo   6. Informacion de Mailpit
echo   7. Volver
echo.

choice /C 1234567 /N /M "Opcion: "
set option=%errorlevel%

if %option%==1 goto open_mailpit
if %option%==2 goto status_mailpit
if %option%==3 goto logs_mailpit
if %option%==4 goto restart_mailpit
if %option%==5 goto clear_emails
if %option%==6 goto info_mailpit
if %option%==7 goto end

:open_mailpit
echo.
echo Verificando si Mailpit esta corriendo...
netstat -an | findstr ":8025 " >nul 2>&1
if errorlevel 1 (
    echo.
    echo [ERROR] Mailpit no esta corriendo
    echo.
    echo Para iniciar Mailpit, ejecuta:
    echo   docker-compose up -d mailpit
    echo.
    echo O inicia todos los servicios:
    echo   start-database.bat
    echo.
    pause
    goto menu
)

echo [OK] Mailpit esta corriendo
echo.
echo Abriendo en el navegador...
start http://localhost:8025
echo.
echo Mailpit Web UI: http://localhost:8025
echo.
pause
goto menu

:status_mailpit
echo.
echo ========================================
echo  Estado de Mailpit
echo ========================================
echo.

docker ps --filter "name=WorshipHubMailpit" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
if errorlevel 1 (
    echo [ERROR] No se pudo obtener el estado
    echo Verifica que Docker este corriendo
) else (
    echo.
    netstat -an | findstr ":8025 " >nul 2>&1
    if errorlevel 1 (
        echo [INFO] Puerto 8025 (Web UI): No disponible
    ) else (
        echo [OK] Puerto 8025 (Web UI): Disponible
    )
    
    netstat -an | findstr ":1025 " >nul 2>&1
    if errorlevel 1 (
        echo [INFO] Puerto 1025 (SMTP): No disponible
    ) else (
        echo [OK] Puerto 1025 (SMTP): Disponible
    )
)
echo.
pause
goto menu

:logs_mailpit
echo.
echo ========================================
echo  Logs de Mailpit
echo ========================================
echo.
echo Presiona Ctrl+C para salir
echo.
timeout /t 2 /nobreak >nul
docker-compose logs -f mailpit
goto menu

:restart_mailpit
echo.
echo ========================================
echo  Reiniciando Mailpit
echo ========================================
echo.
docker-compose restart mailpit
if errorlevel 1 (
    echo [ERROR] No se pudo reiniciar Mailpit
) else (
    echo [OK] Mailpit reiniciado correctamente
    echo.
    echo Esperando a que este listo...
    timeout /t 3 /nobreak >nul
    echo.
    echo Mailpit Web UI: http://localhost:8025
)
echo.
pause
goto menu

:clear_emails
echo.
echo ========================================
echo  Limpiar Emails de Mailpit
echo ========================================
echo.
echo [ADVERTENCIA] Esto eliminara todos los emails almacenados
echo.
choice /C SN /M "Estas seguro? (S/N): "
if errorlevel 2 goto menu

echo.
echo Reiniciando Mailpit para limpiar emails...
docker-compose restart mailpit
if errorlevel 1 (
    echo [ERROR] No se pudo reiniciar Mailpit
) else (
    echo [OK] Emails eliminados correctamente
    echo.
    echo Mailpit reiniciado y listo para usar
)
echo.
pause
goto menu

:info_mailpit
echo.
echo ========================================
echo  Informacion de Mailpit
echo ========================================
echo.
echo Mailpit es un servidor SMTP de desarrollo que captura
echo todos los emails enviados por la aplicacion.
echo.
echo Caracteristicas:
echo   - Captura todos los emails sin enviarlos realmente
echo   - Interfaz web para ver emails
echo   - Soporte para HTML y texto plano
echo   - Ver adjuntos
echo   - Buscar emails
echo.
echo Configuracion:
echo   Web UI: http://localhost:8025
echo   SMTP Host: localhost
echo   SMTP Port: 1025
echo   No requiere autenticacion
echo.
echo Uso en la aplicacion:
echo   La aplicacion esta configurada para enviar emails
echo   a Mailpit automaticamente en el perfil 'local'
echo.
echo Emails que veras en Mailpit:
echo   - Verificacion de email
echo   - Recuperacion de contrasena
echo   - Invitaciones a equipos
echo   - Notificaciones
echo.
echo Para ver los emails:
echo   1. Inicia la aplicacion con perfil 'local'
echo   2. Registra un usuario o solicita recuperacion
echo   3. Abre http://localhost:8025
echo   4. Los emails apareceran instantaneamente
echo.
echo ========================================
echo.
pause
goto menu

:end
echo.
endlocal
exit /b 0

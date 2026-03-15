# WorshipHub API - Scripts de Inicio

Esta carpeta contiene scripts multiplataforma para facilitar el inicio y gestión del proyecto WorshipHub API.

## 🚀 Scripts Disponibles

### Scripts Principales

| Script | Windows | Linux/Mac | Descripción |
|--------|---------|-----------|-------------|
| Inicio Rápido | `start-worshiphub.bat` | `start-worshiphub.sh` | Menú interactivo principal |
| Verificación | `check-system.bat` | `check-system.sh` | Verifica requisitos del sistema |
| Base de Datos | `start-database.bat` | `start-database.sh` | Inicia PostgreSQL + Mailpit |
| Mailpit | `mailpit-manager.bat` | _(usar menú principal)_ | Gestor de Mailpit |

### Scripts de la API

Ubicados en `api/scripts/`:

| Script | Windows | Linux/Mac | Descripción |
|--------|---------|-----------|-------------|
| Local | `start-local.bat` | `start-local.sh` | Inicia API con PostgreSQL |
| H2 | `start-h2.bat` | `start-h2.sh` | Inicia API con H2 en memoria |

## 📋 Uso Rápido

### Windows

```cmd
REM 1. Verificar sistema
scripts\check-system.bat

REM 2. Iniciar con menú interactivo
scripts\start-worshiphub.bat
```

### Linux/Mac

```bash
# 1. Verificar sistema
./scripts/check-system.sh

# 2. Iniciar con menú interactivo
./scripts/start-worshiphub.sh
```

## 🎯 Opciones del Menú Principal

1. **Desarrollo Local** - PostgreSQL + Mailpit (datos persistentes)
2. **Desarrollo Rápido** - H2 en memoria (sin Docker)
3. **Solo Servicios Docker** - Inicia PostgreSQL y Mailpit
4. **Detener Servicios** - Detiene todos los contenedores
5. **Ver Logs (PostgreSQL)** - Muestra logs de la base de datos
6. **Ver Logs (Mailpit)** - Muestra logs del servidor de email
7. **Ver Logs (Todos)** - Muestra todos los logs
8. **Limpiar y Reiniciar** - Reinicia con base de datos limpia
9. **Abrir Mailpit** - Abre Mailpit en el navegador
0. **Salir** - Sale del menú

## 🔧 Requisitos

### Todos los Sistemas

- **Java 21+** - [Descargar](https://adoptium.net/)
- **Docker Desktop** - [Descargar](https://www.docker.com/products/docker-desktop)
- **Git** - [Descargar](https://git-scm.com/)

### Linux/Mac Adicional

- `bash` (generalmente preinstalado)
- `netcat` o `lsof` para verificación de puertos

## 📝 Permisos (Linux/Mac)

Los scripts `.sh` necesitan permisos de ejecución:

```bash
chmod +x scripts/*.sh
chmod +x api/scripts/*.sh
```

## 🌐 URLs Importantes

Una vez iniciados los servicios:

- **API**: http://localhost:9090/api/v1
- **Swagger UI**: http://localhost:9090/swagger-ui.html
- **Health Check**: http://localhost:9090/api/v1/health
- **Mailpit Web UI**: http://localhost:8025
- **H2 Console**: http://localhost:9090/h2-console (solo con perfil H2)

## 📧 Mailpit

Mailpit captura todos los emails enviados por la aplicación:

- **Web UI**: http://localhost:8025
- **SMTP**: localhost:1025
- **Uso**: Ver emails de verificación, recuperación de contraseña, invitaciones

## 🐛 Solución de Problemas

### Windows

```cmd
REM Puerto en uso
netstat -ano | findstr :9090
taskkill /PID <PID> /F

REM Docker no responde
docker-compose down
docker-compose up -d

REM Ver logs
docker-compose logs -f
```

### Linux/Mac

```bash
# Puerto en uso
lsof -ti:9090 | xargs kill -9

# Docker no responde
docker-compose down
docker-compose up -d

# Ver logs
docker-compose logs -f
```

## 📚 Documentación Adicional

- **Guía Rápida**: `../QUICK_START.md`
- **Guía de Scripts**: `../SCRIPTS_GUIDE.md`
- **README Principal**: `../README.md`
- **Documentación Técnica**: `../WORSHIPHUB_DOCUMENTATION.md`

## 🔄 Flujo de Trabajo Típico

### Primera Vez

```bash
# 1. Verificar sistema
./scripts/check-system.sh  # o .bat en Windows

# 2. Iniciar todo
./scripts/start-worshiphub.sh  # o .bat en Windows
# Seleccionar opción 1 (Desarrollo Local)

# 3. Verificar que funciona
curl http://localhost:9090/api/v1/health

# 4. Ver Swagger
# Abrir http://localhost:9090/swagger-ui.html

# 5. Ver emails en Mailpit
# Abrir http://localhost:8025
```

### Desarrollo Diario

```bash
# Opción A: Usar menú interactivo
./scripts/start-worshiphub.sh

# Opción B: Comandos directos
docker-compose up -d  # Iniciar servicios
cd api/scripts
./start-local.sh      # Iniciar API
```

### Limpiar y Reiniciar

```bash
# Desde el menú
./scripts/start-worshiphub.sh
# Opción 8: Limpiar y reiniciar

# O manualmente
docker-compose down
rm -rf data/db
./scripts/start-database.sh
```

## 💡 Tips

- Usa **Desarrollo Local** para trabajo completo con datos persistentes
- Usa **Desarrollo Rápido (H2)** para pruebas rápidas sin Docker
- Mailpit es perfecto para ver emails sin configurar SMTP real
- Los logs de la app están en `api/logs/worshiphub.log`
- Usa Swagger UI para probar endpoints interactivamente

## 🆘 Ayuda

Si tienes problemas:

1. Ejecuta `check-system` para verificar tu configuración
2. Revisa los logs: `docker-compose logs`
3. Consulta `SCRIPTS_GUIDE.md` para más detalles
4. Revisa `QUICK_START.md` para guía paso a paso

---

**¡Listo para desarrollar! 🚀**

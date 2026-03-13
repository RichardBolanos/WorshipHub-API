# API Scripts

Scripts para iniciar la aplicación WorshipHub API directamente.

## 📋 Scripts Disponibles

| Script | Windows | Linux/Mac | Descripción |
|--------|---------|-----------|-------------|
| Local | `start-local.bat` | `start-local.sh` | Inicia con PostgreSQL (perfil local) |
| H2 | `start-h2.bat` | `start-h2.sh` | Inicia con H2 en memoria |

## 🚀 Uso

### Perfil Local (PostgreSQL)

**Windows:**
```cmd
cd api\scripts
start-local.bat
```

**Linux/Mac:**
```bash
cd api/scripts
./start-local.sh
```

**Requisitos:**
- PostgreSQL corriendo en puerto 5442
- Mailpit corriendo en puerto 8025

**URLs:**
- API: http://localhost:9090/api/v1
- Swagger: http://localhost:9090/swagger-ui.html
- Mailpit: http://localhost:8025

### Perfil H2 (Memoria)

**Windows:**
```cmd
cd api\scripts
start-h2.bat
```

**Linux/Mac:**
```bash
cd api/scripts
./start-h2.sh
```

**Características:**
- No requiere Docker
- Base de datos en memoria
- Datos se pierden al cerrar
- H2 Console: http://localhost:9090/h2-console

## 💡 Recomendación

En lugar de usar estos scripts directamente, usa el launcher principal:

**Windows:**
```cmd
worshiphub.bat
```

**Linux/Mac:**
```bash
./worshiphub.sh
```

El launcher principal ofrece un menú interactivo con más opciones y mejor manejo de errores.

## 📚 Más Información

- [Guía de Inicio](../../START_HERE.md)
- [Scripts Principales](../../scripts/README.md)
- [Guía Rápida](../../QUICK_START.md)

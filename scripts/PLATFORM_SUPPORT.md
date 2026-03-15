# Soporte Multiplataforma - WorshipHub API

## ✅ Plataformas Soportadas

| Plataforma | Soportado | Scripts | Notas |
|------------|-----------|---------|-------|
| **Windows 10/11** | ✅ | `.bat` | Completamente probado |
| **macOS** | ✅ | `.sh` | Requiere Homebrew para Docker |
| **Linux (Ubuntu/Debian)** | ✅ | `.sh` | Requiere Docker instalado |
| **Linux (Fedora/RHEL)** | ✅ | `.sh` | Requiere Docker instalado |
| **WSL2 (Windows)** | ✅ | `.sh` | Usar scripts de Linux |

## 📁 Estructura de Scripts

```
worship_hub_api/
├── worshiphub.bat              # 🎯 Launcher principal (Windows)
├── worshiphub.sh               # 🎯 Launcher principal (Linux/Mac)
├── START_HERE.md               # 📖 Guía de inicio
│
└── scripts/
    ├── README.md               # Documentación de scripts
    ├── PLATFORM_SUPPORT.md     # Este archivo
    │
    ├── start-worshiphub.bat    # Menú principal (Windows)
    ├── start-worshiphub.sh     # Menú principal (Linux/Mac)
    │
    ├── start-database.bat      # Iniciar Docker (Windows)
    ├── start-database.sh       # Iniciar Docker (Linux/Mac)
    │
    ├── check-system.bat        # Verificar requisitos (Windows)
    ├── check-system.sh         # Verificar requisitos (Linux/Mac)
    │
    └── mailpit-manager.bat     # Gestor Mailpit (Windows)
```

## 🚀 Uso por Plataforma

### Windows

```cmd
REM Launcher principal
worshiphub.bat

REM O scripts individuales
scripts\check-system.bat
scripts\start-worshiphub.bat
scripts\start-database.bat
scripts\mailpit-manager.bat
```

### macOS

```bash
# Launcher principal
./worshiphub.sh

# O scripts individuales
./scripts/check-system.sh
./scripts/start-worshiphub.sh
./scripts/start-database.sh
```

### Linux

```bash
# Launcher principal
./worshiphub.sh

# O scripts individuales
./scripts/check-system.sh
./scripts/start-worshiphub.sh
./scripts/start-database.sh
```

### WSL2 (Windows Subsystem for Linux)

```bash
# Usar scripts de Linux
./worshiphub.sh

# Asegúrate de que Docker Desktop esté configurado para WSL2
```

## 🔧 Requisitos por Plataforma

### Windows

- ✅ Windows 10/11
- ✅ PowerShell 5.1+ (incluido en Windows)
- ✅ Java 21+ ([Adoptium](https://adoptium.net/))
- ✅ Docker Desktop ([Docker](https://www.docker.com/products/docker-desktop))
- ✅ Git ([Git for Windows](https://git-scm.com/))

### macOS

- ✅ macOS 10.15+ (Catalina o superior)
- ✅ Bash 3.2+ (incluido en macOS)
- ✅ Java 21+ (instalar con Homebrew: `brew install openjdk@21`)
- ✅ Docker Desktop ([Docker](https://www.docker.com/products/docker-desktop))
- ✅ Git (incluido en Xcode Command Line Tools)

```bash
# Instalar Homebrew (si no está instalado)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Instalar Java
brew install openjdk@21

# Instalar Docker Desktop
brew install --cask docker
```

### Linux (Ubuntu/Debian)

- ✅ Ubuntu 20.04+ o Debian 11+
- ✅ Bash 4.0+
- ✅ Java 21+
- ✅ Docker + Docker Compose
- ✅ Git

```bash
# Instalar Java
sudo apt update
sudo apt install openjdk-21-jdk

# Instalar Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Instalar Docker Compose
sudo apt install docker-compose

# Instalar Git
sudo apt install git
```

### Linux (Fedora/RHEL)

- ✅ Fedora 35+ o RHEL 8+
- ✅ Bash 4.0+
- ✅ Java 21+
- ✅ Docker + Docker Compose
- ✅ Git

```bash
# Instalar Java
sudo dnf install java-21-openjdk-devel

# Instalar Docker
sudo dnf install docker docker-compose
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker $USER

# Instalar Git
sudo dnf install git
```

## 🎯 Características por Plataforma

| Característica | Windows | macOS | Linux |
|----------------|---------|-------|-------|
| Menú interactivo | ✅ | ✅ | ✅ |
| Detección automática de servicios | ✅ | ✅ | ✅ |
| Inicio automático de Docker | ✅ | ✅ | ✅ |
| Verificación de puertos | ✅ | ✅ | ✅ |
| Logs en tiempo real | ✅ | ✅ | ✅ |
| Abrir navegador automático | ✅ | ✅ | ✅* |
| Colores en terminal | ❌ | ✅ | ✅ |

*En Linux, requiere `xdg-open` (generalmente preinstalado)

## 🔍 Diferencias entre Plataformas

### Comandos de Red

**Windows:**
```cmd
netstat -an | findstr :9090
```

**Linux/Mac:**
```bash
lsof -ti:9090
# o
netstat -an | grep :9090
```

### Abrir Navegador

**Windows:**
```cmd
start http://localhost:8025
```

**macOS:**
```bash
open http://localhost:8025
```

**Linux:**
```bash
xdg-open http://localhost:8025
```

### Permisos de Ejecución

**Windows:**
- No requiere permisos especiales
- Los archivos `.bat` son ejecutables por defecto

**Linux/Mac:**
```bash
# Dar permisos de ejecución
chmod +x worshiphub.sh
chmod +x scripts/*.sh
```

## 🐛 Problemas Comunes por Plataforma

### Windows

**Problema:** "El término 'docker' no se reconoce"
```cmd
REM Solución: Reiniciar terminal después de instalar Docker
REM O agregar Docker al PATH manualmente
```

**Problema:** Scripts no se ejecutan
```cmd
REM Solución: Ejecutar como Administrador
REM O cambiar política de ejecución de PowerShell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### macOS

**Problema:** "Permission denied" al ejecutar scripts
```bash
# Solución: Dar permisos de ejecución
chmod +x worshiphub.sh
chmod +x scripts/*.sh
```

**Problema:** Docker no inicia
```bash
# Solución: Abrir Docker Desktop manualmente
open -a Docker
```

### Linux

**Problema:** "docker: permission denied"
```bash
# Solución: Agregar usuario al grupo docker
sudo usermod -aG docker $USER
# Cerrar sesión y volver a iniciar
```

**Problema:** "nc: command not found"
```bash
# Solución: Instalar netcat
sudo apt install netcat  # Ubuntu/Debian
sudo dnf install nmap-ncat  # Fedora/RHEL
```

## 📝 Notas de Desarrollo

### Scripts de Windows (.bat)

- Usan `cmd.exe` como intérprete
- Sintaxis específica de Windows
- No soportan colores ANSI nativamente
- Usan `choice` para menús interactivos

### Scripts de Linux/Mac (.sh)

- Usan `bash` como intérprete
- Sintaxis POSIX compatible
- Soportan colores ANSI
- Usan `read` para entrada de usuario
- Requieren shebang: `#!/bin/bash`

### Compatibilidad

Los scripts están diseñados para ser:
- ✅ Independientes de la plataforma
- ✅ Fáciles de mantener
- ✅ Consistentes en funcionalidad
- ✅ Bien documentados

## 🔄 Migración entre Plataformas

### De Windows a Linux/Mac

```bash
# 1. Clonar el repositorio
git clone <repo-url>
cd worship_hub_api

# 2. Dar permisos de ejecución
chmod +x worshiphub.sh
chmod +x scripts/*.sh

# 3. Ejecutar
./worshiphub.sh
```

### De Linux/Mac a Windows

```cmd
REM 1. Clonar el repositorio
git clone <repo-url>
cd worship_hub_api

REM 2. Ejecutar (no requiere permisos especiales)
worshiphub.bat
```

## 🎓 Mejores Prácticas

### Para Desarrolladores

1. **Usa el launcher principal** (`worshiphub.bat` o `worshiphub.sh`)
2. **Verifica requisitos** antes de empezar (`check-system`)
3. **Usa perfil local** para desarrollo completo
4. **Usa perfil H2** para pruebas rápidas

### Para Equipos Multiplataforma

1. **Documenta** qué plataforma usas en issues/PRs
2. **Prueba** scripts en tu plataforma antes de commit
3. **Reporta** problemas específicos de plataforma
4. **Mantén** ambas versiones de scripts sincronizadas

## 📚 Recursos Adicionales

- [START_HERE.md](../START_HERE.md) - Guía de inicio
- [README.md](README.md) - Documentación de scripts
- [QUICK_START.md](../QUICK_START.md) - Guía rápida
- [SCRIPTS_GUIDE.md](../SCRIPTS_GUIDE.md) - Guía detallada

---

**¿Problemas con tu plataforma?** Abre un issue en GitHub con:
- Sistema operativo y versión
- Versión de Docker
- Versión de Java
- Mensaje de error completo

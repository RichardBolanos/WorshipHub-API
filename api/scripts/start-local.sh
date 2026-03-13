#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo ""
echo "========================================"
echo "  WorshipHub - Inicio Desarrollo Local"
echo "========================================"

echo ""
echo "Verificando Java..."
if ! command -v java &> /dev/null; then
    echo -e "${RED}[ERROR] Java no encontrado${NC}"
    echo "Por favor, instala Java 21 LTS desde: https://adoptium.net/temurin/releases/?version=21"
    echo ""
    read -p "Presiona Enter para salir..."
    exit 1
fi

# Get Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)

# Check if Java version is 25 or higher (not compatible)
if [ "$JAVA_MAJOR" -ge 25 ]; then
    echo ""
    echo "========================================"
    echo "  [ERROR] Java Version Incompatible"
    echo "========================================"
    echo ""
    echo "Java detectado: $JAVA_VERSION"
    echo ""
    echo "Este proyecto requiere Java 21 LTS."
    echo "Java 25+ no es compatible con Kotlin 2.1.0"
    echo ""
    echo "Por favor, instala Java 21 LTS desde:"
    echo "https://adoptium.net/temurin/releases/?version=21"
    echo ""
    echo "Despues de instalar Java 21:"
    echo "1. Verifica con: java -version"
    echo "2. Ejecuta: ./scripts/check-system.sh"
    echo "3. Inicia la app: ./worshiphub.sh"
    echo ""
    echo "========================================"
    read -p "Presiona Enter para salir..."
    exit 1
fi

if [ "$JAVA_MAJOR" -lt 17 ]; then
    echo ""
    echo -e "${RED}[ADVERTENCIA] Java $JAVA_VERSION detectado${NC}"
    echo "Se recomienda Java 21 LTS para mejor compatibilidad"
    echo ""
fi

echo -e "${GREEN}Java $JAVA_VERSION detectado ✓${NC}"

echo ""
echo "Verificando PostgreSQL en puerto 5442..."
if ! nc -z localhost 5442 2>/dev/null; then
    echo ""
    echo -e "${RED}[ERROR] PostgreSQL no esta ejecutandose en puerto 5442${NC}"
    echo ""
    echo "Por favor, inicia PostgreSQL primero:"
    echo "  1. Ejecuta: ./scripts/start-database.sh"
    echo "  2. O usa el menu principal: ./worshiphub.sh"
    echo ""
    read -p "Presiona Enter para salir..."
    exit 1
fi

echo -e "${GREEN}PostgreSQL detectado en puerto 5442 ✓${NC}"

echo ""
echo "Configurando variables de entorno..."
export SPRING_PROFILES_ACTIVE=local
export DATABASE_URL=jdbc:postgresql://localhost:5442/worshiphub
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=postgres
export JWT_SECRET=local-dev-secret-key-for-development-only-not-for-production-use-32chars

echo ""
echo "========================================"
echo "  Configuracion de Inicio"
echo "========================================"
echo "Perfil activo: $SPRING_PROFILES_ACTIVE"
echo "Base de datos: $DATABASE_URL"
echo "Usuario DB: $DATABASE_USERNAME"
echo "Puerto API: 9090"
echo ""
echo "URLs Importantes:"
echo "  API: http://localhost:9090/api/v1"
echo "  Swagger: http://localhost:9090/swagger-ui.html"
echo "  Health: http://localhost:9090/api/v1/health"
echo "  Mailpit: http://localhost:8025"
echo ""
echo "Mailpit - Servidor de Email de Desarrollo:"
echo "  Todos los emails enviados por la app se pueden ver en:"
echo "  http://localhost:8025"
echo ""
echo "========================================"

echo ""
echo "Iniciando aplicacion WorshipHub..."
echo ""

cd "$PROJECT_DIR"
./gradlew :api:bootRun --args="--spring.profiles.active=local"

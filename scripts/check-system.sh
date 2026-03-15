#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo ""
echo "========================================"
echo "  WorshipHub - Verificacion del Sistema"
echo "========================================"
echo ""

all_ok=1

# Check Java
echo "[1/5] Verificando Java..."
if command -v java &> /dev/null; then
    echo -e "  ${GREEN}[OK] Java instalado${NC}"
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo "  Version: $JAVA_VERSION"
    
    # Check if Java version is 25 or higher (not compatible)
    JAVA_MAJOR=$(echo "$JAVA_VERSION" | cut -d'.' -f1)
    if [ "$JAVA_MAJOR" -ge 25 ]; then
        echo ""
        echo -e "  ${RED}[ERROR] Java $JAVA_VERSION no es compatible con Kotlin 2.1.0${NC}"
        echo "  Por favor, instala Java 21 LTS desde: https://adoptium.net/temurin/releases/?version=21"
        echo "  Java 21 es la version LTS recomendada para este proyecto"
        all_ok=0
    fi
else
    echo -e "  ${RED}[ERROR] Java no encontrado${NC}"
    echo "  Instala Java 21 LTS desde: https://adoptium.net/temurin/releases/?version=21"
    all_ok=0
fi

echo ""

# Check Docker
echo "[2/5] Verificando Docker..."
if command -v docker &> /dev/null; then
    echo -e "  ${GREEN}[OK] Docker instalado${NC}"
    docker --version
    
    # Check if Docker is running
    if docker ps &> /dev/null; then
        echo -e "  ${GREEN}[OK] Docker esta corriendo${NC}"
    else
        echo -e "  ${YELLOW}[ADVERTENCIA] Docker no esta corriendo${NC}"
        echo "  Inicia Docker Desktop"
        all_ok=0
    fi
else
    echo -e "  ${RED}[ERROR] Docker no encontrado${NC}"
    echo "  Instala Docker Desktop desde: https://www.docker.com/products/docker-desktop"
    all_ok=0
fi

echo ""

# Check Docker Compose
echo "[3/5] Verificando Docker Compose..."
if command -v docker-compose &> /dev/null; then
    echo -e "  ${GREEN}[OK] Docker Compose instalado${NC}"
    docker-compose --version
else
    echo -e "  ${RED}[ERROR] Docker Compose no encontrado${NC}"
    all_ok=0
fi

echo ""

# Check Gradle
echo "[4/5] Verificando Gradle..."
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

if [ -f "$PROJECT_DIR/gradlew" ]; then
    echo -e "  ${GREEN}[OK] Gradle Wrapper encontrado${NC}"
    cd "$PROJECT_DIR"
    ./gradlew --version 2>/dev/null | grep "Gradle"
else
    echo -e "  ${RED}[ERROR] Gradle Wrapper no encontrado${NC}"
    all_ok=0
fi

echo ""

# Check ports
echo "[5/5] Verificando puertos disponibles..."

# Function to check if port is in use
check_port() {
    local port=$1
    local name=$2
    
    if command -v lsof &> /dev/null; then
        if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
            if [ "$port" == "9090" ]; then
                echo -e "  ${YELLOW}[ADVERTENCIA] Puerto $port - $name ya esta en uso${NC}"
                all_ok=0
            else
                echo -e "  ${YELLOW}[INFO] Puerto $port - $name en uso${NC}"
            fi
        else
            echo -e "  ${GREEN}[OK] Puerto $port - $name disponible${NC}"
        fi
    elif command -v netstat &> /dev/null; then
        if netstat -an | grep ":$port " | grep LISTEN >/dev/null 2>&1; then
            if [ "$port" == "9090" ]; then
                echo -e "  ${YELLOW}[ADVERTENCIA] Puerto $port - $name ya esta en uso${NC}"
                all_ok=0
            else
                echo -e "  ${YELLOW}[INFO] Puerto $port - $name en uso${NC}"
            fi
        else
            echo -e "  ${GREEN}[OK] Puerto $port - $name disponible${NC}"
        fi
    else
        echo -e "  ${YELLOW}[INFO] No se puede verificar puerto $port${NC}"
    fi
}

check_port 9090 "API"
check_port 5442 "PostgreSQL"
check_port 8025 "Mailpit"

echo ""
echo "========================================"

if [ $all_ok -eq 1 ]; then
    echo ""
    echo -e "${GREEN}[EXITO] Sistema listo para ejecutar WorshipHub!${NC}"
    echo ""
    echo "Siguiente paso:"
    echo "  ./scripts/start-worshiphub.sh"
    echo ""
else
    echo ""
    echo -e "${YELLOW}[ATENCION] Hay problemas que deben resolverse${NC}"
    echo "Por favor, corrige los errores indicados arriba"
    echo ""
fi

echo "========================================"
echo ""

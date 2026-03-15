#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

echo ""
echo "========================================"
echo "  WorshipHub - PostgreSQL con PostGIS"
echo "========================================"

echo ""
echo "Verificando Docker..."
if ! command -v docker &> /dev/null; then
    echo -e "${RED}[ERROR] Docker no esta instalado${NC}"
    echo "Por favor, instala Docker desde: https://www.docker.com/products/docker-desktop"
    exit 1
fi
echo -e "${GREEN}Docker detectado ✓${NC}"

echo ""
echo "Verificando Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}[ERROR] Docker Compose no esta disponible${NC}"
    exit 1
fi
echo -e "${GREEN}Docker Compose detectado ✓${NC}"

echo ""
echo "Deteniendo contenedores existentes..."
cd "$PROJECT_DIR"
docker-compose down

# Uncomment to reset database (delete all data)
# echo "Eliminando datos antiguos..."
# rm -rf "$PROJECT_DIR/data/db"

echo ""
echo "Iniciando PostgreSQL con PostGIS..."
docker-compose up -d

echo ""
echo "Esperando a que la base de datos este lista..."
counter=0
max_attempts=15

while [ $counter -lt $max_attempts ]; do
    sleep 2
    if docker exec WorshipHubPostgres pg_isready -U postgres -d worshiphub &>/dev/null; then
        break
    fi
    counter=$((counter + 1))
    echo "Intentando conectar... ($counter/$max_attempts)"
done

if [ $counter -eq $max_attempts ]; then
    echo ""
    echo -e "${RED}[ERROR] La base de datos no respondio despues de 30 segundos${NC}"
    echo "Verifica los logs con: docker-compose logs db"
    exit 1
fi

echo ""
echo "========================================"
echo "  Servicios Docker Listos!"
echo "========================================"
echo ""
echo "PostgreSQL:"
echo "  Host: localhost"
echo "  Port: 5442"
echo "  Database: worshiphub"
echo "  Username: postgres"
echo "  Password: postgres"
echo "  JDBC URL: jdbc:postgresql://localhost:5442/worshiphub"
echo ""
echo "Mailpit (Servidor de Email de Desarrollo):"
echo "  Web UI: http://localhost:8025"
echo "  SMTP Server: localhost:1025"
echo "  Uso: Ver emails enviados por la aplicacion"
echo ""
echo "========================================"

echo ""
echo "Verificando extensiones PostGIS..."
if docker exec WorshipHubPostgres psql -U postgres -d worshiphub -c "SELECT PostGIS_Version();" &>/dev/null; then
    docker exec WorshipHubPostgres psql -U postgres -d worshiphub -c "SELECT PostGIS_Version();"
else
    echo -e "${YELLOW}[ADVERTENCIA] PostGIS puede no estar instalado correctamente${NC}"
fi

echo ""
echo "========================================"
echo "  Comandos Utiles"
echo "========================================"
echo ""
echo "Detener servicios:"
echo "  docker-compose down"
echo ""
echo "Ver logs:"
echo "  docker-compose logs -f"
echo "  docker-compose logs -f db"
echo "  docker-compose logs -f mailpit"
echo ""
echo "Abrir Mailpit:"
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "  open http://localhost:8025"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    echo "  xdg-open http://localhost:8025"
fi
echo ""
echo "Conectar a PostgreSQL:"
echo "  docker exec -it WorshipHubPostgres psql -U postgres -d worshiphub"
echo ""
echo "========================================"
echo ""

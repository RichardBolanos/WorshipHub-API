#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

show_menu() {
    clear
    echo ""
    echo "========================================"
    echo "    WorshipHub API - Inicio Rapido"
    echo "========================================"
    echo ""
    echo "Selecciona el modo de inicio:"
    echo ""
    echo "  1. Desarrollo Local (PostgreSQL + Mailpit)"
    echo "  2. Desarrollo Rapido (H2 en memoria)"
    echo "  3. Solo iniciar servicios Docker"
    echo "  4. Detener todos los servicios"
    echo "  5. Ver logs (PostgreSQL)"
    echo "  6. Ver logs (Mailpit)"
    echo "  7. Ver logs (Todos los servicios)"
    echo "  8. Limpiar y reiniciar base de datos"
    echo "  9. Abrir Mailpit en navegador"
    echo " 10. Build y Push imagen Docker a GitHub (GHCR)"
    echo "  0. Salir"
    echo ""
    read -p "Opcion: " option
}

start_local() {
    echo ""
    echo "========================================"
    echo "  Iniciando en modo Desarrollo Local"
    echo "========================================"
    echo ""
    echo "Este modo incluye:"
    echo "  - PostgreSQL 16 con PostGIS (puerto 5442)"
    echo "  - Mailpit para emails de desarrollo"
    echo "    * Web UI: http://localhost:8025"
    echo "    * SMTP: localhost:1025"
    echo ""

    # Check if database is running
    if ! nc -z localhost 5442 2>/dev/null; then
        echo -e "${YELLOW}PostgreSQL no detectado. Iniciando servicios Docker...${NC}"
        echo ""
        bash "$SCRIPT_DIR/start-database.sh"
        echo ""
        read -p "Presiona Enter para continuar con la aplicacion..."
    else
        echo -e "${GREEN}PostgreSQL ya esta corriendo en puerto 5442${NC}"
    fi

    # Check if Mailpit is running
    if ! nc -z localhost 8025 2>/dev/null; then
        echo ""
        echo -e "${YELLOW}[ADVERTENCIA] Mailpit no esta corriendo${NC}"
        echo "Iniciando servicios Docker..."
        bash "$SCRIPT_DIR/start-database.sh"
        echo ""
        read -p "Presiona Enter para continuar..."
    else
        echo -e "${GREEN}Mailpit ya esta corriendo en puerto 8025${NC}"
    fi

    echo ""
    echo "Iniciando aplicacion..."
    cd "$PROJECT_DIR/api/scripts"
    bash start-local.sh
}

start_h2() {
    echo ""
    echo "========================================"
    echo "  Iniciando en modo Desarrollo Rapido"
    echo "========================================"
    echo ""
    echo "Este modo usa H2 en memoria (no requiere Docker)"
    echo "Los datos se perderan al cerrar la aplicacion"
    echo ""
    echo -e "${YELLOW}NOTA: Los emails NO se enviaran en este modo${NC}"
    echo ""
    cd "$PROJECT_DIR/api/scripts"
    bash start-h2.sh
}

start_db_only() {
    echo ""
    echo "========================================"
    echo "  Iniciando Servicios Docker"
    echo "========================================"
    echo ""
    bash "$SCRIPT_DIR/start-database.sh"
    echo ""
    echo "========================================"
    echo "  Servicios Iniciados"
    echo "========================================"
    echo ""
    echo "PostgreSQL:"
    echo "  Host: localhost:5442"
    echo "  Database: worshiphub"
    echo "  User: postgres / postgres"
    echo ""
    echo "Mailpit:"
    echo "  Web UI: http://localhost:8025"
    echo "  SMTP: localhost:1025"
    echo ""
    echo "Para iniciar la aplicacion:"
    echo "  cd api/scripts"
    echo "  ./start-local.sh"
    echo ""
    read -p "Presiona Enter para continuar..."
}

stop_services() {
    echo ""
    echo "========================================"
    echo "  Deteniendo Servicios Docker"
    echo "========================================"
    echo ""
    cd "$PROJECT_DIR"
    docker-compose down
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}[OK] Servicios detenidos correctamente${NC}"
        echo "  - PostgreSQL detenido"
        echo "  - Mailpit detenido"
    else
        echo ""
        echo -e "${RED}[ERROR] No se pudieron detener los servicios${NC}"
        echo "Verifica que Docker este corriendo"
    fi
    echo ""
    read -p "Presiona Enter para continuar..."
}

view_logs_db() {
    echo ""
    echo "========================================"
    echo "  Logs de PostgreSQL"
    echo "========================================"
    echo ""
    echo "Presiona Ctrl+C para salir de los logs"
    echo ""
    sleep 2
    cd "$PROJECT_DIR"
    docker-compose logs -f db
}

view_logs_mailpit() {
    echo ""
    echo "========================================"
    echo "  Logs de Mailpit"
    echo "========================================"
    echo ""
    echo "Presiona Ctrl+C para salir de los logs"
    echo ""
    sleep 2
    cd "$PROJECT_DIR"
    docker-compose logs -f mailpit
}

view_logs_all() {
    echo ""
    echo "========================================"
    echo "  Logs de Todos los Servicios"
    echo "========================================"
    echo ""
    echo "Presiona Ctrl+C para salir de los logs"
    echo ""
    sleep 2
    cd "$PROJECT_DIR"
    docker-compose logs -f
}

clean_restart() {
    echo ""
    echo "========================================"
    echo "  Limpiar y Reiniciar Base de Datos"
    echo "========================================"
    echo ""
    echo -e "${RED}[ADVERTENCIA] Esto eliminara TODOS los datos de la base de datos${NC}"
    echo "Los emails en Mailpit tambien se perderan"
    echo ""
    read -p "Estas seguro? (s/N): " confirm
    
    if [[ ! $confirm =~ ^[Ss]$ ]]; then
        return
    fi

    echo ""
    echo "Deteniendo servicios..."
    cd "$PROJECT_DIR"
    docker-compose down

    echo "Eliminando datos de PostgreSQL..."
    if [ -d "$PROJECT_DIR/data/db" ]; then
        rm -rf "$PROJECT_DIR/data/db"
        echo -e "${GREEN}  [OK] Datos eliminados${NC}"
    else
        echo "  [INFO] No hay datos para eliminar"
    fi

    echo ""
    echo "Iniciando servicios limpios..."
    bash "$SCRIPT_DIR/start-database.sh"

    echo ""
    echo "========================================"
    echo -e "${GREEN}[OK] Base de datos reiniciada con exito${NC}"
    echo "========================================"
    echo ""
    read -p "Presiona Enter para continuar..."
}

open_mailpit() {
    echo ""
    echo "Abriendo Mailpit en el navegador..."
    echo ""

    # Check if Mailpit is running
    if ! nc -z localhost 8025 2>/dev/null; then
        echo -e "${RED}[ERROR] Mailpit no esta corriendo${NC}"
        echo ""
        echo "Inicia los servicios Docker primero (Opcion 3)"
        echo ""
        read -p "Presiona Enter para continuar..."
        return
    fi

    # Open browser based on OS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        open http://localhost:8025
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        xdg-open http://localhost:8025 2>/dev/null || echo "Abre manualmente: http://localhost:8025"
    fi

    echo ""
    echo "Mailpit abierto en el navegador"
    echo "URL: http://localhost:8025"
    echo ""
    read -p "Presiona Enter para continuar..."
}

push_ghcr() {
    echo ""
    echo "========================================"
    echo "  Build y Push imagen Docker a GHCR"
    echo "========================================"
    echo ""
    echo "Este proceso construira la imagen nativa de GraalVM"
    echo "y la subira a GitHub Container Registry (ghcr.io)."
    echo ""
    echo "Requisitos:"
    echo "  - Docker corriendo"
    echo "  - Personal Access Token de GitHub con scope: write:packages"
    echo "    https://github.com/settings/tokens"
    echo ""

    # Verify Docker is running
    if ! docker info >/dev/null 2>&1; then
        echo -e "${RED}[ERROR] Docker no esta corriendo${NC}"
        echo ""
        read -p "Presiona Enter para continuar..."
        return
    fi

    # GitHub user/owner (lowercase required by GHCR)
    local GHCR_OWNER="richardbolanos"
    local IMAGE_NAME="worshiphub-api"
    local GHCR_IMAGE="ghcr.io/${GHCR_OWNER}/${IMAGE_NAME}"

    # Get short commit SHA for versioned tag
    local GIT_SHA
    GIT_SHA=$(git -C "$PROJECT_DIR" rev-parse --short HEAD 2>/dev/null || echo "manual")

    echo "Imagen destino : ${GHCR_IMAGE}"
    echo "Tags          : latest, ${GIT_SHA}"
    echo ""

    # Resolve token: prefer GHCR_TOKEN, fallback GITHUB_TOKEN, else prompt
    local TOKEN="${GHCR_TOKEN:-${GITHUB_TOKEN:-}}"

    if [ -z "$TOKEN" ]; then
        echo ""
        echo "========================================"
        echo "  Token de GitHub no encontrado"
        echo "========================================"
        echo ""
        echo "No se encontro la variable de entorno GHCR_TOKEN ni GITHUB_TOKEN."
        echo ""
        echo -e "${BLUE}Como crear un Personal Access Token (PAT):${NC}"
        echo ""
        echo "  1. Abre: https://github.com/settings/tokens/new"
        echo "  2. Note: WorshipHub GHCR"
        echo "  3. Expiration: 90 days (o la que prefieras)"
        echo "  4. Marca los scopes:"
        echo "       [x] write:packages   (sube imagenes)"
        echo "       [x] read:packages    (lee imagenes)"
        echo "       [x] delete:packages  (opcional, para borrar)"
        echo "  5. Click 'Generate token' y COPIA el token (empieza con ghp_)"
        echo ""
        echo -e "${BLUE}Como guardarlo permanentemente (recomendado):${NC}"
        echo ""
        echo "  Linux/Mac (bash):"
        echo "    echo 'export GHCR_TOKEN=ghp_xxxx' >> ~/.bashrc"
        echo "    source ~/.bashrc"
        echo ""
        echo "  Linux/Mac (zsh):"
        echo "    echo 'export GHCR_TOKEN=ghp_xxxx' >> ~/.zshrc"
        echo "    source ~/.zshrc"
        echo ""
        echo "  Solo sesion actual:"
        echo "    export GHCR_TOKEN=ghp_xxxx"
        echo ""
        echo "Despues de guardarlo, ABRE UNA NUEVA TERMINAL para que tome efecto."
        echo ""
        echo "----------------------------------------"
        echo ""
        read -p "Quieres pegar el token ahora para usarlo solo esta vez? (s/N): " confirm
        if [[ ! $confirm =~ ^[Ss]$ ]]; then
            echo ""
            echo "Cancelado. Configura GHCR_TOKEN y vuelve a intentar."
            echo ""
            read -p "Presiona Enter para continuar..."
            return
        fi
        echo ""
        read -rsp "Pega tu Personal Access Token (oculto): " TOKEN
        echo ""
        if [ -z "$TOKEN" ]; then
            echo ""
            echo -e "${RED}[ERROR] No se proporciono ningun token. Cancelando.${NC}"
            echo ""
            read -p "Presiona Enter para continuar..."
            return
        fi
        # Basic format validation
        if [[ ! "$TOKEN" =~ ^gh[ps]_ ]]; then
            echo ""
            echo -e "${YELLOW}[ADVERTENCIA] El token no parece tener el formato esperado (ghp_... o ghs_...).${NC}"
            read -p "Continuar de todos modos? (s/N): " cont
            if [[ ! $cont =~ ^[Ss]$ ]]; then
                return
            fi
        fi
    fi

    echo ""
    echo "[1/3] Login en ghcr.io..."
    if ! echo "$TOKEN" | docker login ghcr.io -u "$GHCR_OWNER" --password-stdin; then
        echo ""
        echo "========================================"
        echo -e "${RED}[ERROR] Fallo el login en ghcr.io${NC}"
        echo "========================================"
        echo ""
        echo "Posibles causas:"
        echo "  - Token invalido o expirado"
        echo "  - Token sin scope 'write:packages'"
        echo "  - Usuario incorrecto (actual: ${GHCR_OWNER})"
        echo ""
        echo "Verifica tu token en: https://github.com/settings/tokens"
        echo ""
        read -p "Presiona Enter para continuar..."
        return
    fi

    echo ""
    echo "[2/3] Construyendo imagen nativa (esto puede tardar varios minutos)..."
    cd "$PROJECT_DIR"
    if ! docker build -f Dockerfile.native \
        -t "${GHCR_IMAGE}:latest" \
        -t "${GHCR_IMAGE}:${GIT_SHA}" .; then
        echo -e "${RED}[ERROR] Fallo el build de la imagen.${NC}"
        read -p "Presiona Enter para continuar..."
        return
    fi

    echo ""
    echo "[3/3] Haciendo push a GHCR..."
    if ! docker push "${GHCR_IMAGE}:latest"; then
        echo -e "${RED}[ERROR] Fallo el push de :latest${NC}"
        read -p "Presiona Enter para continuar..."
        return
    fi
    if ! docker push "${GHCR_IMAGE}:${GIT_SHA}"; then
        echo -e "${RED}[ERROR] Fallo el push de :${GIT_SHA}${NC}"
        read -p "Presiona Enter para continuar..."
        return
    fi

    echo ""
    echo "========================================"
    echo -e "${GREEN}[OK] Imagen publicada correctamente${NC}"
    echo "========================================"
    echo ""
    echo "URL : https://github.com/${GHCR_OWNER}?tab=packages"
    echo "Imagen para Render.com:"
    echo "  ${GHCR_IMAGE}:latest"
    echo "  ${GHCR_IMAGE}:${GIT_SHA}"
    echo ""
    echo -e "${YELLOW}NOTA: Si la imagen es privada, en Render.com agrega${NC}"
    echo -e "${YELLOW}Registry Credentials (Account Settings) usando tu PAT.${NC}"
    echo ""
    read -p "Presiona Enter para continuar..."
}

# Main loop
while true; do
    show_menu
    
    case $option in
        1) start_local ;;
        2) start_h2 ;;
        3) start_db_only ;;
        4) stop_services ;;
        5) view_logs_db ;;
        6) view_logs_mailpit ;;
        7) view_logs_all ;;
        8) clean_restart ;;
        9) open_mailpit ;;
        10) push_ghcr ;;
        0) 
            echo ""
            echo "========================================"
            echo "   Gracias por usar WorshipHub!"
            echo "========================================"
            echo ""
            echo "Servicios Docker siguen corriendo."
            echo "Para detenerlos, ejecuta: docker-compose down"
            echo ""
            exit 0
            ;;
        *)
            echo -e "${RED}Opcion invalida${NC}"
            sleep 1
            ;;
    esac
done

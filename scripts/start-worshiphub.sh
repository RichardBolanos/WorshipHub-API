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

#!/bin/bash

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo ""
echo "========================================"
echo "  WorshipHub - Inicio con H2 Database"
echo "========================================"

echo ""
echo "Iniciando aplicación con H2 en memoria..."
echo ""
echo "Configuracion:"
echo "  Base de datos: H2 (en memoria)"
echo "  Puerto API: 9090"
echo "  Consola H2: http://localhost:9090/h2-console"
echo ""
echo "Credenciales H2 Console:"
echo "  JDBC URL: jdbc:h2:mem:worshiphub"
echo "  Username: sa"
echo "  Password: (dejar vacio)"
echo ""
echo "========================================"

echo ""
echo "Iniciando aplicacion..."
echo ""

cd "$PROJECT_DIR"
./gradlew :api:bootRun --args="--spring.profiles.active=h2"

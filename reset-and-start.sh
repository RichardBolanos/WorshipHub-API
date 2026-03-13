#!/bin/bash
# Script para resetear la base de datos y iniciar la aplicación
# Uso: ./reset-and-start.sh

set -e

echo "========================================"
echo "WorshipHub - Reset Database and Start"
echo "========================================"
echo ""

echo "[1/4] Deteniendo contenedores existentes..."
docker-compose down -v

echo ""
echo "[2/4] Eliminando datos de la base de datos..."
if [ -d "data/db" ]; then
    rm -rf data/db
    echo "Datos eliminados correctamente"
else
    echo "No hay datos previos para eliminar"
fi

echo ""
echo "[3/4] Iniciando contenedores (PostgreSQL + Mailpit)..."
docker-compose up -d

echo ""
echo "Esperando a que PostgreSQL esté listo..."
sleep 10

echo ""
echo "[4/4] Iniciando aplicación Spring Boot..."
echo "La aplicación se iniciará en el puerto 9090"
echo "Flyway ejecutará las migraciones automáticamente"
echo ""
echo "Presiona Ctrl+C para detener la aplicación"
echo ""

cd api
../gradlew bootRun --args='--spring.profiles.active=local'

#!/bin/bash

# Wallet Manager - Docker Start Script

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║     Wallet Manager - Starting with Docker Compose          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Error: Docker is not running. Please start Docker Desktop."
    exit 1
fi

echo "→ Building and starting containers..."
echo ""

# Build and start containers (use docker compose for newer versions)
if command -v docker &> /dev/null && docker compose version &> /dev/null; then
    docker compose up --build -d
else
    docker-compose up --build -d
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Waiting for services to be ready..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Wait for application to be ready
echo -n "Waiting for application"
for i in {1..60}; do
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        echo ""
        echo ""
        echo "✅ Application is ready!"
        break
    fi
    echo -n "."
    sleep 2
done

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 Wallet Manager is running!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "  🌐 Web UI:       http://localhost:8080/"
echo "  📡 API:          http://localhost:8080/api/"
echo "  💚 Health:       http://localhost:8080/actuator/health"
echo "  📊 Prometheus:   http://localhost:9090"
echo "  📈 Grafana:      http://localhost:3000"
echo "                    (Username: admin, Password: admin)"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "To view logs:"
echo "  docker compose logs -f wallet-manager"
echo ""
echo "To stop:"
echo "  docker compose down"
echo ""
echo "To stop and remove volumes (clean slate):"
echo "  docker compose down -v"
echo ""


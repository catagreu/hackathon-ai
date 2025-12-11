#!/bin/bash

# Wallet Manager - Start Script

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          Wallet Manager - Starting Application              ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Check if PostgreSQL container exists
if docker ps -a --format '{{.Names}}' | grep -q "^wallet-postgres$"; then
    echo "✓ PostgreSQL container found"
    if docker ps --format '{{.Names}}' | grep -q "^wallet-postgres$"; then
        echo "✓ PostgreSQL is already running"
    else
        echo "→ Starting PostgreSQL container..."
        docker start wallet-postgres
        sleep 2
        echo "✓ PostgreSQL started"
    fi
else
    echo "→ Creating PostgreSQL container..."
    docker run -d --name wallet-postgres \
        -e POSTGRES_DB=walletdb \
        -e POSTGRES_USER=walletuser \
        -e POSTGRES_PASSWORD=walletpass \
        -p 5432:5432 \
        postgres:15-alpine > /dev/null 2>&1
    echo "✓ PostgreSQL container created and started"
    echo "  Waiting for PostgreSQL to be ready..."
    sleep 5
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Starting Spring Boot application..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Once started, access the application at:"
echo "  🌐 Web UI:    http://localhost:8080/"
echo "  📡 API:       http://localhost:8080/api/"
echo ""
echo "Press Ctrl+C to stop the application"
echo ""

# Start the Spring Boot application
mvn spring-boot:run -s temp-settings.xml


# Docker Setup Guide

## Quick Start

### Option 1: Using the Start Script (Easiest)

```bash
./docker-start.sh
```

This will:
- Build the Docker image
- Start PostgreSQL and the application
- Wait for services to be ready
- Show you the access URLs

### Option 2: Using Docker Compose Directly

```bash
# Build and start everything
docker-compose up --build -d

# View logs
docker-compose logs -f wallet-manager

# Stop everything
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v
```

## Access the Application

Once started, access:

- **Web UI:** http://localhost:8080/
- **API:** http://localhost:8080/api/
- **Health Check:** http://localhost:8080/actuator/health

## Docker Services

### PostgreSQL Database
- **Container:** `wallet-postgres`
- **Port:** 5432
- **Database:** walletdb
- **User:** walletuser
- **Password:** walletpass
- **Volume:** `postgres_data` (persists data)

### Wallet Manager Application
- **Container:** `wallet-manager-app`
- **Port:** 8080
- **Image:** Built from Dockerfile
- **Depends on:** PostgreSQL

## Useful Commands

### View Logs
```bash
# All services
docker-compose logs -f

# Just the application
docker-compose logs -f wallet-manager

# Just PostgreSQL
docker-compose logs -f postgres
```

### Stop Services
```bash
# Stop containers (keeps data)
docker-compose stop

# Stop and remove containers (keeps data)
docker-compose down

# Stop and remove everything including volumes (clean slate)
docker-compose down -v
```

### Rebuild After Code Changes
```bash
# Rebuild and restart
docker-compose up --build -d

# Or just rebuild the app (faster)
docker-compose build wallet-manager
docker-compose up -d wallet-manager
```

### Access Database
```bash
# Connect to PostgreSQL
docker exec -it wallet-postgres psql -U walletuser -d walletdb

# Or from host machine
psql -h localhost -U walletuser -d walletdb
```

### Check Container Status
```bash
docker-compose ps
```

## Dockerfile Details

The Dockerfile uses a multi-stage build:

1. **Build Stage:** Uses Maven to compile and package the application
2. **Runtime Stage:** Uses a lightweight JRE image to run the application

This results in a smaller final image (~200MB vs ~800MB).

## Troubleshooting

### Port Already in Use
If port 8080 or 5432 is already in use, modify `docker-compose.yml`:
```yaml
ports:
  - "8081:8080"  # Change host port
```

### Application Won't Start
Check logs:
```bash
docker-compose logs wallet-manager
```

Common issues:
- Database not ready: Wait a bit longer or check PostgreSQL logs
- Connection refused: Verify PostgreSQL is healthy

### Rebuild Everything
```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

### View Application Logs
```bash
docker-compose logs -f wallet-manager | tail -100
```

## Environment Variables

You can override environment variables in `docker-compose.yml`:

```yaml
wallet-manager:
  environment:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/walletdb
    SPRING_DATASOURCE_USERNAME: walletuser
    SPRING_DATASOURCE_PASSWORD: walletpass
    # Add more as needed
```

## Production Considerations

For production, consider:

1. **Use environment variables for secrets** (don't hardcode passwords)
2. **Use Docker secrets** or external secret management
3. **Add resource limits** to containers
4. **Use a reverse proxy** (nginx) in front
5. **Set up proper logging** (ELK stack, etc.)
6. **Add monitoring** (Prometheus, Grafana)
7. **Use health checks** (already included)
8. **Set up backup strategy** for PostgreSQL volumes


# Quick Start Guide

## Prerequisites

1. **Java 21** - Check with: `java -version`
2. **Maven 3.8+** - Check with: `mvn -version`
3. **PostgreSQL** (or Docker for PostgreSQL)

## Step 1: Set Up Database

### Option A: Using Docker (Easiest)

```bash
# Start PostgreSQL container
docker run -d \
  --name wallet-postgres \
  -e POSTGRES_DB=walletdb \
  -e POSTGRES_USER=walletuser \
  -e POSTGRES_PASSWORD=walletpass \
  -p 5432:5432 \
  postgres:15-alpine

# Verify it's running
docker ps
```

### Option B: Local PostgreSQL

1. Install PostgreSQL 15+
2. Create database:
   ```sql
   CREATE DATABASE walletdb;
   CREATE USER walletuser WITH PASSWORD 'walletpass';
   GRANT ALL PRIVILEGES ON DATABASE walletdb TO walletuser;
   ```

## Step 2: Start the Application

```bash
# Navigate to project directory
cd /Users/vmoroz/IdeaProjects/hackathon-ai

# Start Spring Boot application
mvn spring-boot:run -s temp-settings.xml
```

**Wait for:** You should see:
```
Started WalletManagerApplication in X.XXX seconds
```

## Step 3: Access the Application

### Web UI (Recommended)
Open your browser and go to:
```
http://localhost:8080/
```

### API Endpoints
The REST API is available at:
```
http://localhost:8080/api/
```

## Troubleshooting

### Port 8080 already in use?
Change the port in `src/main/resources/application.yml`:
```yaml
server:
  port: 8081  # Change to any available port
```

### Database connection error?
- Make sure PostgreSQL is running
- Check connection details in `src/main/resources/application.yml`
- Verify database exists: `psql -U walletuser -d walletdb`

### Maven repository issues?
Always use: `-s temp-settings.xml` with all Maven commands

## Example Usage

Once the application is running:

1. **Open UI:** http://localhost:8080/
2. **Try a deposit:**
   - Player ID: 1001
   - Amount: 500.00
   - Currency: USD
   - Click "Deposit"

3. **Check balance:**
   - Click "Get Balance" button
   - See the balance displayed at the top

4. **View transactions:**
   - Click "Get History" to see transaction history

## Stop the Application

Press `Ctrl+C` in the terminal where the application is running.

## Stop PostgreSQL (if using Docker)

```bash
docker stop wallet-postgres
docker rm wallet-postgres
```


# Wallet Manager Service - Setup Instructions

## Overview

This is a Spring Boot application that refactors the legacy monolithic `WalletManager` into a modern, clean architecture following SOLID principles and TDD practices.

## Prerequisites

- Java 21 or higher
- Maven 3.8+ 
- Docker (for running PostgreSQL or TestContainers)

## Project Structure

```
src/
├── main/
│   ├── java/org/elavationlab/
│   │   ├── domain/              # JPA entities (Wallet, Transaction, PendingWithdrawal)
│   │   ├── repository/           # Spring Data JPA repositories
│   │   ├── service/              # Business logic services
│   │   ├── controller/           # REST controllers
│   │   ├── dto/                  # Data Transfer Objects
│   │   └── exception/            # Custom exceptions
│   └── resources/
│       ├── db/migration/         # Flyway migration scripts
│       └── application.yml       # Application configuration
└── test/
    ├── java/org/elavationlab/
    │   ├── service/              # Unit tests
    │   └── integration/          # Integration tests with TestContainers
    └── resources/
        └── application-test.yml  # Test configuration
```

## Database Setup

### Option 1: Using Docker Compose (Recommended)

Create a `docker-compose.yml` file:

```yaml
version: '3.8'
services:
  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: walletdb
      POSTGRES_USER: walletuser
      POSTGRES_PASSWORD: walletpass
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

Run: `docker-compose up -d`

### Option 2: Local PostgreSQL

1. Install PostgreSQL 15+
2. Create database: `CREATE DATABASE walletdb;`
3. Create user: `CREATE USER walletuser WITH PASSWORD 'walletpass';`
4. Grant privileges: `GRANT ALL PRIVILEGES ON DATABASE walletdb TO walletuser;`

## Running the Application

1. **Build the project:**
   ```bash
   mvn clean install
   ```

2. **Run the application:**
   ```bash
   mvn spring-boot:run
   ```

   Or run the main class: `org.elavationlab.WalletManagerApplication`

3. **The application will start on:** `http://localhost:8080`

## Web UI

A beautiful, modern web interface is available at:
- **Main UI:** `http://localhost:8080/`

The UI provides:
- 💰 **Deposit** - Add funds to player wallet
- 💸 **Withdraw** - Withdraw funds from wallet
- 🎲 **Place Bet** - Place bets on games
- 🎉 **Win** - Credit winnings
- 🎁 **Add Bonus** - Add bonus credits
- 💱 **Convert Currency** - Convert between currencies
- 📊 **Get Balance** - View current balance
- 📜 **Transaction History** - View transaction history

The UI features:
- Real-time balance display
- Responsive design (works on mobile and desktop)
- Modern gradient design
- Error handling and success notifications
- Transaction history viewer

## Running Tests

### Unit Tests
```bash
mvn test
```

### Integration Tests
Integration tests use TestContainers and will automatically start a PostgreSQL container:
```bash
mvn test
```

## API Endpoints

### Wallet Operations

- `POST /api/wallets/{playerId}/deposit` - Deposit funds
- `POST /api/wallets/{playerId}/withdraw` - Withdraw funds
- `POST /api/wallets/{playerId}/bet` - Place a bet
- `POST /api/wallets/{playerId}/win` - Credit a win
- `POST /api/wallets/{playerId}/bonus` - Add bonus balance
- `POST /api/wallets/{playerId}/convert` - Convert currency
- `GET /api/wallets/{playerId}/balance?currency=USD` - Get balance

### Transaction Operations

- `GET /api/transactions/{playerId}?currency=USD&days=30` - Get transaction history

## Example API Calls

### Deposit
```bash
curl -X POST http://localhost:8080/api/wallets/1001/deposit \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 500.00,
    "currency": "USD"
  }'
```

### Get Balance
```bash
curl http://localhost:8080/api/wallets/1001/balance?currency=USD
```

### Place Bet
```bash
curl -X POST http://localhost:8080/api/wallets/1001/bet \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 50.00,
    "currency": "USD",
    "gameId": "SLOT_001"
  }'
```

## Features Implemented

✅ **Clean Architecture**
- Separation of concerns (Controller → Service → Repository)
- SOLID principles applied
- Dependency injection

✅ **Domain Model**
- JPA entities with proper relationships
- BigDecimal for monetary values (precision)
- Enum types for transaction types

✅ **Data Layer**
- Spring Data JPA repositories
- Flyway database migrations
- PostgreSQL support

✅ **Business Logic**
- Wallet operations (deposit, withdraw, bet, win, bonus)
- Currency conversion with exchange rates
- Transaction history

✅ **API Layer**
- RESTful endpoints
- Request/Response DTOs
- Input validation
- Global exception handling

✅ **Testing**
- Unit tests with Mockito (TDD approach)
- Integration tests with TestContainers
- Test isolation and independence

✅ **Configuration**
- Application properties
- Database connection pooling
- Flyway migration management

## Business Rules

- **Deposit Limit:** Maximum 10,000 per transaction
- **Withdrawal Limit:** Maximum 5,000 per transaction
- **Supported Currencies:** USD, EUR, GBP, CAD
- **Bet Logic:** Deducts from bonus balance first, then real balance
- **Transaction History:** Returns last N days (default 30)

## Migration from Legacy Code

The legacy `WalletManager.java` has been refactored into:

1. **Domain Layer:** Entities representing core business concepts
2. **Repository Layer:** Data access abstraction
3. **Service Layer:** Business logic with transaction management
4. **Controller Layer:** REST API endpoints
5. **DTO Layer:** Request/Response objects
6. **Exception Layer:** Custom exceptions with proper error handling

## Next Steps (Future Enhancements)

- Add authentication and authorization
- Implement pending withdrawal approval workflow
- Add currency exchange rate service integration
- Implement reporting service
- Add monitoring and metrics (Prometheus/Grafana)
- Add API documentation (Swagger/OpenAPI)
- Implement caching for frequently accessed data
- Add event-driven architecture for async operations


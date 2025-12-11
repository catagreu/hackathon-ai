# Challenge 01: Player Wallet Service

## Domain Context

In online gaming platforms, the player wallet is the core financial component that manages player balances, deposits, withdrawals, and bet transactions. This system must handle real-time balance updates, transaction history, and currency conversions while maintaining strict consistency and audit trails.

## Legacy Code Description

**What it does:**
A monolithic Java application (`WalletManager.java`) that handles all wallet operations including deposits, withdrawals, balance checks, transaction logging, and reporting. Originally designed as a quick prototype that evolved into production code.

**Why it's problematic:**
- Single 650-line class mixing persistence, business logic, validation, and presentation
- Direct JDBC calls with hardcoded SQL throughout the codebase
- No transaction management or rollback capabilities
- Synchronous blocking operations causing performance bottlenecks
- No separation between currency types (fiat vs bonus credits)
- Manual CSV report generation with string concatenation
- No tests or documentation
- Thread-safety issues in balance calculations

## Representative Legacy Code Snippet

```java
public class WalletManager {
    private Connection dbConnection;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/wallets";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "admin123";
    
    public WalletManager() throws SQLException {
        dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
    
    public String processDeposit(int playerId, double amount, String currency) {
        try {
            // Validate amount
            if (amount <= 0) return "ERROR: Invalid amount";
            if (amount > 10000) return "ERROR: Exceeds deposit limit";
            
            // Check if wallet exists
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT balance FROM wallets WHERE player_id=" + playerId + 
                " AND currency='" + currency + "'"
            );
            
            double currentBalance = 0;
            boolean walletExists = false;
            if (rs.next()) {
                currentBalance = rs.getDouble("balance");
                walletExists = true;
            }
            
            // Update or create wallet
            if (walletExists) {
                double newBalance = currentBalance + amount;
                stmt.executeUpdate(
                    "UPDATE wallets SET balance=" + newBalance + 
                    ", updated_at=NOW() WHERE player_id=" + playerId + 
                    " AND currency='" + currency + "'"
                );
            } else {
                stmt.executeUpdate(
                    "INSERT INTO wallets (player_id, currency, balance, created_at) " +
                    "VALUES (" + playerId + ",'" + currency + "'," + amount + ", NOW())"
                );
            }
            
            // Log transaction
            stmt.executeUpdate(
                "INSERT INTO transactions (player_id, type, amount, currency, timestamp) " +
                "VALUES (" + playerId + ",'DEPOSIT'," + amount + ",'" + currency + "', NOW())"
            );
            
            return "SUCCESS: Deposited " + amount + " " + currency;
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }
    
    public String processWithdrawal(int playerId, double amount, String currency) {
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT balance FROM wallets WHERE player_id=" + playerId + 
                " AND currency='" + currency + "'"
            );
            
            if (!rs.next()) {
                return "ERROR: Wallet not found";
            }
            
            double currentBalance = rs.getDouble("balance");
            if (currentBalance < amount) {
                return "ERROR: Insufficient funds";
            }
            
            double newBalance = currentBalance - amount;
            stmt.executeUpdate(
                "UPDATE wallets SET balance=" + newBalance + 
                ", updated_at=NOW() WHERE player_id=" + playerId + 
                " AND currency='" + currency + "'"
            );
            
            stmt.executeUpdate(
                "INSERT INTO transactions (player_id, type, amount, currency, timestamp) " +
                "VALUES (" + playerId + ",'WITHDRAWAL'," + amount + ",'" + currency + "', NOW())"
            );
            
            return "SUCCESS: Withdrawn " + amount + " " + currency;
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }
}
```

## Full Legacy Code

[WalletManager.java - 650 Lines](wallet-manager/src/main/java/org/elavationlab/WalletManager.java)

---

## SDLC Phases & Tasks

### Phase 1: Analysis & Specification (45 min)
**Tool Category:** Spec-Driven Tool (Amazon Kiro, GitHub Spec-Kit, or Tessl)

**Task:**
Extract functional and non-functional requirements from the legacy code. Identify:
- Core entities (Wallet, Transaction, Withdrawal)
- Business rules and constraints
- API endpoints needed
- Data models and relationships

**Deliverables:**
1. Data dictionary with entity definitions
2. OpenAPI 3.0 specification with all endpoints
3. Business rules documentation
4. Non-functional requirements (performance, security)

**Tool Evaluation Focus:**
- How well does the tool understand legacy code?
- Quality of generated specifications
- Learning curve for the tool

---

### Phase 2: Design (30 min)
**Tool Category:** Terminal Coding Tool (Claude Code, OpenAI Codex, Gemini CLI, etc.)

**Task:**
Design the microservices architecture:
- Wallet Service (balance management)
- Transaction Service (audit log)
- Payment Gateway Interface (deposits/withdrawals)

**Deliverables:**
1. Architecture diagram (C4 model or similar)
2. Service interaction diagrams
3. Database schema design
4. API contracts between services

**Tool Evaluation Focus:**
- Diagram generation quality
- Architecture recommendations
- Speed of iteration

---

### Phase 3: Implementation (90 min)
**Tool Category:** IDE Coding Tool (Cursor, WindSurf, VS Code + Copilot, etc.)

**Task:**
Rewrite the monolith as clean microservices using modern frameworks:
- Java → Kotlin with Spring Boot or Quarkus
- Apply SOLID principles
- Implement proper dependency injection
- Separate concerns (controllers, services, repositories)

**Deliverables:**
1. Git repository with modular code structure
2. README with setup instructions
3. Configuration files (application.yml/properties)
4. Separated services with clear boundaries

**Tool Evaluation Focus:**
- Code generation quality
- Refactoring suggestions
- Integration with project structure

---

### Phase 4: Data Layer (30 min)
**Tool Category:** IDE Coding Tool

**Task:**
Design and implement the persistence layer:
- PostgreSQL for transactional data (ACID compliance)
- Database migration scripts (Flyway or Liquibase)
- Repository pattern implementation

**Deliverables:**
1. Database schema SQL files
2. Migration scripts (V1__initial_schema.sql, etc.)
3. JPA entities or data access objects
4. Connection pooling configuration

**Tool Evaluation Focus:**
- Database design recommendations
- Migration script generation
- ORM mapping suggestions

---

### Phase 5: Testing (45 min)
**Tool Category:** Terminal or IDE Coding Tool

**Task:**
Implement comprehensive testing following TDD:
1. Write unit tests first (manually or with AI)
2. Ask AI to implement features that pass the tests
3. Integration tests using TestContainers

**Deliverables:**
1. Unit test suite (JUnit 5 or similar)
2. Integration test suite with TestContainers
3. Test execution reports (coverage, results)
4. Mocking strategy documentation

**Tool Evaluation Focus:**
- Test generation quality
- TDD workflow support
- Test maintenance suggestions

---

### Phase 6: DevOps (30 min)
**Tool Category:** Local LLM + Terminal AI Tool

**Task:**
Containerize the services for local deployment:
- Multi-stage Dockerfiles for each service
- Docker Compose orchestration
- Volume mounts for data persistence

**Deliverables:**
1. Dockerfile for each service
2. docker-compose.yml with all services
3. Environment variable configuration
4. Local deployment instructions

**Tool Evaluation Focus:**
- Dockerfile optimization suggestions
- Orchestration best practices
- Local development workflow

---

### Phase 7: Code Review (20 min)
**Tool Category:** Code Review Tool (Coderabbit, SonarQube, etc.)

**Task:**
Run automated code review on all implemented code:
- Static analysis
- Security vulnerability scanning
- Code quality metrics
- Best practice violations

**Deliverables:**
1. Code review report with findings
2. Prioritized remediation list
3. Team's response: which issues to fix vs. ignore
4. Justification for ignored issues

**Tool Evaluation Focus:**
- Accuracy of findings
- False positive rate
- Actionability of suggestions

---

### Phase 8: Visual Assets (20 min)
**Tool Category:** Art/Motion Tool (Freepik Flux, Stable Diffusion, etc.)

**Task:**
Generate UI assets for the wallet dashboard:
- Wallet icons (different currencies)
- Transaction type icons (deposit, withdrawal, bet, win)
- Background graphics or promotional banners

**Deliverables:**
1. Generated image files (PNG/SVG)
2. Prompt documentation
3. Asset usage guidelines
4. Tool evaluation notes

**Tool Evaluation Focus:**
- Image quality and relevance
- Prompt iteration requirements
- Commercial usage rights

---

### Phase 9: UI Testing & Documentation (20 min)
**Tool Category:** Agentic Browser (Arc, Perplexity Comet, Edge Copilot)

**Task:**
If time permits, create a simple React dashboard. Use the agentic browser to:
- Navigate the deployed UI
- Identify all interactive elements
- Document user flows
- Report any UI/UX issues

**Deliverables:**
1. UI navigation documentation
2. User flow diagrams
3. Accessibility issues report
4. Improvement recommendations

**Tool Evaluation Focus:**
- UI understanding capabilities
- Documentation quality
- Navigation accuracy

---

### Phase 10: Monitoring (20 min)
**Tool Category:** IDE Coding Tool

**Task:**
Define observability for the wallet services:
- Prometheus metrics endpoints
- Grafana dashboard configuration (mock acceptable)
- Key metrics: transaction rate, balance updates, error rate

**Deliverables:**
1. Prometheus configuration (prometheus.yml)
2. Grafana dashboard JSON
3. Metrics documentation
4. Alerting rules (optional)

**Tool Evaluation Focus:**
- Monitoring setup guidance
- Dashboard generation quality
- Best practice recommendations

---

## Success Criteria

### Primary: Tool Assessment Quality (60%)
- Honest evaluation of each tool used
- Evidence provided (screenshots, videos)
- Detailed pros/cons for each tool
- Comparative analysis where applicable
- Recommendations for company adoption

### Secondary: SDLC Coverage (30%)
- Evidence of attempting all phases
- Clear mapping of tools to phases
- Documentation of workflow

### Tertiary: Implementation Quality (10%)
- Code compiles and runs
- Demonstrates understanding of clean architecture
- Git repository is organized

### Not Evaluated:
- Production-ready code
- Complete feature implementation
- Perfect tool usage

---

## Constraints & Resources

**Allowed:**
- Local Docker containers
- Free trial accounts for AI tools
- Public documentation
- Open-source libraries

**Prohibited:**
- Company resources or proprietary information
- Cloud deployments
- Paid-only tools without trials
- Production/staging database access

**Time Budget:**
Total: 6 hours (10:30 - 16:30 CET)
- Setup & planning: 15 min
- Core phases: 5 hours
- Documentation & packaging: 45 min

---

## Evaluation Template

For each tool used, provide:

```markdown
## Tool: [Tool Name]
**Category:** [Tool Category]
**Phase Used:** [SDLC Phase]

### Usage Evidence
- Screenshots: [links]
- Video demo: [link, max 5 min]
- Sample prompts used:
  ```
  [prompt 1]
  [prompt 2]
  ```

### Evaluation

**What worked well:**
- [Point 1]
- [Point 2]

**Limitations encountered:**
- [Point 1]
- [Point 2]

**Learning Curve:** [1-5, where 1=easy, 5=steep]

**Would you recommend it?** [Yes/No]
**Why/Why not:** [Explanation]

**Compared to alternatives:** [If applicable]

**Cost-benefit analysis:** [Time invested vs. value delivered]
```

---

## Questions?

Contact the organizing team or refer to the [Hackathon Rules](https://wiki.prod.corpcenter.tech/wiki/spaces/Workplace/pages/573931521/Hackathon+Rules).

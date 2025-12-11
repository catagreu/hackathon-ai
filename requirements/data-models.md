# Data Models and Relationships

## Overview

This document defines all data entities, their relationships, and constraints extracted from the legacy wallet management system.

## Core Entities

### Player
Represents a user in the system who can own multiple wallets.

**Fields:**
- `player_id` (INT, PRIMARY KEY): Unique identifier for the player
- `created_at` (TIMESTAMP): Account creation timestamp
- `updated_at` (TIMESTAMP): Last modification timestamp

**Business Rules:**
- Each player can have multiple wallets (one per currency)
- Player ID must be unique across the system

### Wallet
Represents a currency-specific account for a player.

**Fields:**
- `id` (INT, PRIMARY KEY, AUTO_INCREMENT): Unique wallet identifier
- `player_id` (INT, NOT NULL, FOREIGN KEY): References Player.player_id
- `currency` (VARCHAR(3), NOT NULL): ISO currency code (USD, EUR, GBP, CAD)
- `balance` (DECIMAL(15,2), NOT NULL, DEFAULT 0.00): Real money balance
- `bonus_balance` (DECIMAL(15,2), NOT NULL, DEFAULT 0.00): Bonus funds balance
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Wallet creation time
- `updated_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE): Last balance update

**Constraints:**
- UNIQUE KEY `unique_player_currency` (player_id, currency)
- Balance must be >= 0.00
- Bonus balance must be >= 0.00
- Currency must be in supported list: USD, EUR, GBP, CAD

**Business Rules:**
- One wallet per player per currency
- Real balance can be withdrawn
- Bonus balance used first for bets
- Total available = balance + bonus_balance

### Transaction
Records all financial operations performed on wallets.

**Fields:**
- `id` (INT, PRIMARY KEY, AUTO_INCREMENT): Unique transaction identifier
- `player_id` (INT, NOT NULL): References Player.player_id
- `type` (VARCHAR(20), NOT NULL): Transaction type
- `amount` (DECIMAL(15,2), NOT NULL): Transaction amount
- `currency` (VARCHAR(3), NOT NULL): Currency code
- `balance_before` (DECIMAL(15,2)): Balance before transaction
- `balance_after` (DECIMAL(15,2)): Balance after transaction
- `timestamp` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Transaction time
- `description` (TEXT): Additional transaction details

**Transaction Types:**
- `DEPOSIT`: Funds added to wallet
- `WITHDRAWAL`: Funds removed from wallet
- `BET`: Funds used for gaming
- `WIN`: Winnings credited to wallet
- `BONUS`: Bonus funds added
- `CONVERSION`: Currency exchange operation

**Business Rules:**
- All transactions must be recorded
- Amount must be > 0 for all types
- Balance changes must be tracked
- Immutable once created

### Pending_Withdrawal
Tracks withdrawal requests awaiting approval.

**Fields:**
- `id` (INT, PRIMARY KEY, AUTO_INCREMENT): Unique withdrawal identifier
- `player_id` (INT, NOT NULL): References Player.player_id
- `amount` (DECIMAL(15,2), NOT NULL): Withdrawal amount
- `currency` (VARCHAR(3), NOT NULL): Currency code
- `status` (VARCHAR(20), DEFAULT 'PENDING'): Withdrawal status
- `requested_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP): Request time
- `processed_at` (TIMESTAMP, NULL): Processing completion time

**Status Values:**
- `PENDING`: Awaiting approval
- `APPROVED`: Approved for processing
- `REJECTED`: Rejected by administrator
- `COMPLETED`: Funds disbursed

**Business Rules:**
- Funds deducted immediately upon request
- Requires administrative approval
- Maximum withdrawal: 5,000 per transaction

## Entity Relationships

### Player → Wallet (One-to-Many)
- One player can have multiple wallets
- Each wallet belongs to exactly one player
- Relationship enforced by foreign key: Wallet.player_id → Player.player_id

### Player → Transaction (One-to-Many)
- One player can have multiple transactions
- Each transaction belongs to exactly one player
- Relationship tracked by: Transaction.player_id → Player.player_id

### Player → Pending_Withdrawal (One-to-Many)
- One player can have multiple pending withdrawals
- Each withdrawal belongs to exactly one player
- Relationship tracked by: Pending_Withdrawal.player_id → Player.player_id

### Wallet → Transaction (Implicit)
- Transactions affect wallet balances
- Relationship established through player_id + currency combination
- No direct foreign key due to legacy design

## Data Constraints

### Amount Validation
- All monetary amounts: DECIMAL(15,2) for precision
- Deposit limit: 10,000 per transaction
- Withdrawal limit: 5,000 per transaction
- Minimum balance: 0.00 (no negative balances)

### Currency Support
- Supported currencies: USD, EUR, GBP, CAD
- Exchange rates (hardcoded in legacy):
  - USD: 1.0 (base)
  - EUR: 0.85
  - GBP: 0.73
  - CAD: 1.25

### Referential Integrity
- All player_id references must exist in Player table
- Currency codes must be from supported list
- Transaction types must be from defined enum
- Withdrawal statuses must be from defined enum

## Indexes and Performance

### Required Indexes
- PRIMARY KEY on all id fields
- UNIQUE INDEX on (player_id, currency) for Wallets
- INDEX on player_id for all tables (foreign key performance)
- INDEX on timestamp for Transactions (history queries)
- INDEX on status for Pending_Withdrawals (admin queries)

### Query Patterns
- Balance lookups: WHERE player_id = ? AND currency = ?
- Transaction history: WHERE player_id = ? AND currency = ? ORDER BY timestamp DESC
- Pending withdrawals: WHERE status = 'PENDING'
- Daily reports: GROUP BY DATE(timestamp), currency

## Data Migration Considerations

### Legacy Issues Identified
- SQL injection vulnerabilities (string concatenation)
- No proper transaction management
- Hardcoded exchange rates
- Missing proper error handling
- No audit trail for administrative actions

### Modernization Requirements
- Parameterized queries for security
- Database transactions for consistency
- Dynamic exchange rate service
- Comprehensive error handling
- Administrative action logging
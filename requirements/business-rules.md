# Business Rules Documentation

## Overview

This document defines all business rules, constraints, and operational policies extracted from the legacy wallet management system. These rules govern how the system processes financial transactions and maintains data integrity.

## Transaction Limits and Constraints

### Deposit Rules

**BR-001: Maximum Deposit Limit**
- **Rule**: Single deposit transactions cannot exceed 10,000 units in any currency
- **Rationale**: Risk management and regulatory compliance
- **Implementation**: Validate amount <= 10,000 before processing
- **Error Response**: "ERROR: Exceeds deposit limit of 10,000"

**BR-002: Minimum Deposit Amount**
- **Rule**: Deposit amounts must be greater than zero
- **Rationale**: Prevent invalid or fraudulent transactions
- **Implementation**: Validate amount > 0
- **Error Response**: "ERROR: Invalid amount"

**BR-003: Deposit Currency Validation**
- **Rule**: Deposits only accepted in supported currencies (USD, EUR, GBP, CAD)
- **Rationale**: System limitation and exchange rate management
- **Implementation**: Validate currency against supported list
- **Error Response**: "ERROR: Unsupported currency"

### Withdrawal Rules

**BR-004: Maximum Withdrawal Limit**
- **Rule**: Single withdrawal transactions cannot exceed 5,000 units in any currency
- **Rationale**: Risk management and fraud prevention
- **Implementation**: Validate amount <= 5,000 before processing
- **Error Response**: "ERROR: Exceeds withdrawal limit of 5,000"

**BR-005: Minimum Withdrawal Amount**
- **Rule**: Withdrawal amounts must be greater than zero
- **Rationale**: Prevent invalid transactions
- **Implementation**: Validate amount > 0
- **Error Response**: "ERROR: Invalid amount"

**BR-006: Sufficient Balance Requirement**
- **Rule**: Withdrawals can only be made from real balance (not bonus balance)
- **Rationale**: Bonus funds have usage restrictions
- **Implementation**: Validate real_balance >= withdrawal_amount
- **Error Response**: "ERROR: Insufficient funds. Current balance: {amount}"

**BR-007: Withdrawal Approval Process**
- **Rule**: All withdrawals require administrative approval before processing
- **Rationale**: Fraud prevention and compliance
- **Implementation**: Create pending_withdrawal record, deduct balance immediately
- **Status Flow**: PENDING → APPROVED/REJECTED → COMPLETED

## Balance Management Rules

### Balance Types

**BR-008: Real Balance Definition**
- **Rule**: Real balance represents withdrawable funds
- **Sources**: Deposits, winnings, approved bonuses
- **Usage**: Can be withdrawn or used for betting
- **Constraints**: Must be >= 0, precision to 2 decimal places

**BR-009: Bonus Balance Definition**
- **Rule**: Bonus balance represents promotional funds with restrictions
- **Sources**: Promotional bonuses, loyalty rewards
- **Usage**: Can only be used for betting, not withdrawable
- **Constraints**: Must be >= 0, precision to 2 decimal places

**BR-010: Balance Calculation**
- **Rule**: Total available balance = real_balance + bonus_balance
- **Usage**: Used for bet eligibility checks
- **Display**: Show all three values (real, bonus, total)

### Balance Updates

**BR-011: Atomic Balance Updates**
- **Rule**: All balance changes must be atomic and recorded
- **Implementation**: Update balance and create transaction record in same database transaction
- **Rollback**: If transaction logging fails, rollback balance change

**BR-012: Balance Precision**
- **Rule**: All monetary amounts stored with 2 decimal place precision
- **Data Type**: DECIMAL(15,2)
- **Rounding**: Round to nearest cent using standard rounding rules

## Betting and Gaming Rules

### Bet Processing

**BR-013: Bet Amount Validation**
- **Rule**: Bet amounts must be greater than zero
- **Implementation**: Validate amount > 0
- **Error Response**: "ERROR: Invalid bet amount"

**BR-014: Sufficient Funds for Betting**
- **Rule**: Total available balance must cover bet amount
- **Calculation**: (real_balance + bonus_balance) >= bet_amount
- **Error Response**: "ERROR: Insufficient funds. Available: {total_available}"

**BR-015: Balance Deduction Priority**
- **Rule**: Deduct from bonus balance first, then real balance
- **Logic**: 
  1. If bonus_balance >= bet_amount: deduct from bonus only
  2. If bonus_balance < bet_amount: deduct all bonus, remainder from real
- **Rationale**: Encourage use of promotional funds

### Win Processing

**BR-016: Win Amount Validation**
- **Rule**: Win amounts must be greater than zero
- **Implementation**: Validate amount > 0
- **Error Response**: "ERROR: Invalid win amount"

**BR-017: Win Credit Destination**
- **Rule**: All winnings credited to real balance (withdrawable)
- **Rationale**: Winnings are player's earned money
- **Implementation**: real_balance += win_amount

## Currency and Conversion Rules

### Supported Currencies

**BR-018: Currency Support**
- **Supported**: USD, EUR, GBP, CAD
- **Base Currency**: USD (rate = 1.0)
- **Validation**: All currency codes must be 3-character ISO codes
- **Case Sensitivity**: Currency codes are case-sensitive uppercase

### Exchange Rates

**BR-019: Exchange Rate Application**
- **Current Rates** (legacy hardcoded values):
  - USD: 1.0 (base)
  - EUR: 0.85
  - GBP: 0.73
  - CAD: 1.25
- **Conversion Formula**: converted_amount = (amount / from_rate) * to_rate
- **Rounding**: Round to 2 decimal places

**BR-020: Currency Conversion Process**
- **Rule**: Conversion requires sufficient balance in source currency
- **Process**:
  1. Validate sufficient source balance
  2. Calculate converted amount using exchange rates
  3. Deduct from source wallet
  4. Credit to destination wallet (create if doesn't exist)
  5. Record conversion transactions for both currencies

## Wallet Management Rules

### Wallet Creation

**BR-021: Wallet Uniqueness**
- **Rule**: One wallet per player per currency
- **Constraint**: UNIQUE(player_id, currency)
- **Auto-Creation**: Wallets created automatically on first transaction

**BR-022: Initial Wallet State**
- **Real Balance**: 0.00
- **Bonus Balance**: 0.00
- **Status**: Active
- **Timestamps**: Set created_at and updated_at

### Wallet Operations

**BR-023: Wallet Existence Validation**
- **Rule**: Operations require existing wallet except for deposits
- **Deposits**: Create wallet if doesn't exist
- **Other Operations**: Return error if wallet not found
- **Error Response**: "ERROR: Wallet not found"

## Transaction Recording Rules

### Transaction Logging

**BR-024: Mandatory Transaction Recording**
- **Rule**: All financial operations must be logged
- **Required Fields**: player_id, type, amount, currency, timestamp
- **Optional Fields**: balance_before, balance_after, description
- **Immutability**: Transaction records cannot be modified after creation

**BR-025: Transaction Types**
- **DEPOSIT**: Funds added to wallet
- **WITHDRAWAL**: Funds removed from wallet (pending approval)
- **BET**: Funds used for gaming
- **WIN**: Winnings credited to wallet
- **BONUS**: Bonus funds added
- **CONVERSION**: Currency exchange operation

**BR-026: Transaction Descriptions**
- **Format**: Human-readable description of operation
- **Examples**:
  - "Deposit via payment gateway"
  - "Withdrawal requested"
  - "Bet on game {gameId}"
  - "Win from game {gameId}"
  - "Bonus credited: {bonusCode}"
  - "Converted to {amount} {currency}"

## Administrative Rules

### Withdrawal Approval

**BR-027: Approval Authority**
- **Rule**: Only authorized administrators can approve withdrawals
- **Authentication**: Require admin-level authentication
- **Logging**: Record approval actions with timestamp and admin ID

**BR-028: Approval Process**
- **Status Updates**: PENDING → APPROVED → COMPLETED
- **Balance Impact**: Balance already deducted at request time
- **Rejection**: If rejected, credit amount back to real balance

### Reporting Rules

**BR-029: Report Data Aggregation**
- **Grouping**: By date and currency
- **Metrics**: Total deposits, withdrawals, bets, wins, unique players
- **Period**: Default 30 days, configurable
- **Format**: CSV or JSON output

**BR-030: Report Data Accuracy**
- **Source**: Transaction table aggregation
- **Real-time**: Reports reflect current data state
- **Consistency**: Ensure data consistency across time periods

## Data Validation Rules

### Input Validation

**BR-031: Player ID Validation**
- **Rule**: Player IDs must be positive integers
- **Range**: 1 to 2,147,483,647 (INT max)
- **Required**: Cannot be null or empty

**BR-032: Amount Validation**
- **Rule**: All monetary amounts must be positive numbers
- **Precision**: Maximum 2 decimal places
- **Range**: 0.01 to 999,999,999,999.99
- **Format**: Decimal format, no scientific notation

**BR-033: Currency Code Validation**
- **Rule**: Must be exactly 3 uppercase letters
- **Pattern**: [A-Z]{3}
- **Whitelist**: Only USD, EUR, GBP, CAD accepted

### Data Integrity Rules

**BR-034: Referential Integrity**
- **Rule**: All player_id references must exist
- **Foreign Keys**: Enforce at database level
- **Cascading**: Define appropriate cascade rules

**BR-035: Balance Consistency**
- **Rule**: Balances must always be non-negative
- **Constraint**: CHECK (balance >= 0 AND bonus_balance >= 0)
- **Validation**: Validate before and after all operations

## Security and Compliance Rules

### Authentication and Authorization

**BR-036: API Authentication**
- **Rule**: All API endpoints require valid authentication
- **Method**: Bearer token authentication
- **Expiration**: Tokens must have reasonable expiration times

**BR-037: Operation Authorization**
- **Player Operations**: Require player-level authorization
- **Admin Operations**: Require admin-level authorization
- **Cross-Player Access**: Prevent unauthorized access to other players' data

### Data Protection

**BR-038: Sensitive Data Handling**
- **Rule**: Financial data must be encrypted at rest and in transit
- **PII Protection**: Protect personally identifiable information
- **Audit Trail**: Maintain audit logs for all operations

**BR-039: Error Message Security**
- **Rule**: Error messages must not expose sensitive system information
- **Generic Errors**: Use generic error messages for security issues
- **Logging**: Log detailed errors server-side only

## Performance and Scalability Rules

### Response Time Requirements

**BR-040: API Response Times**
- **Target**: 95% of requests complete within 500ms
- **Timeout**: Maximum 30 seconds for any operation
- **Monitoring**: Track and alert on performance degradation

### Concurrency Rules

**BR-041: Concurrent Transaction Handling**
- **Rule**: System must handle concurrent transactions safely
- **Locking**: Use appropriate database locking mechanisms
- **Isolation**: Ensure transaction isolation to prevent race conditions

**BR-042: Scalability Targets**
- **Concurrent Users**: Support minimum 1000 concurrent transactions
- **Throughput**: Handle peak loads without degradation
- **Availability**: Maintain 99.9% uptime during business hours
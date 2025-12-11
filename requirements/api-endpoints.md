# API Endpoints Specification

## Overview

This document defines all REST API endpoints required for the Wallet Management System, extracted from the legacy code analysis.

## Base URL
```
https://api.wallet-system.com/v1
```

## Authentication
All endpoints require Bearer token authentication in the Authorization header.

## Endpoints

### Player Wallet Operations

#### Deposit Funds
```
POST /api/wallets/{playerId}/deposit
```
**Parameters:**
- `playerId` (path): Player identifier
- `amount` (body): Deposit amount (max: 10,000)
- `currency` (body): Currency code (USD, EUR, GBP, CAD)

**Response:** Transaction confirmation with new balance

#### Withdraw Funds
```
POST /api/wallets/{playerId}/withdraw
```
**Parameters:**
- `playerId` (path): Player identifier
- `amount` (body): Withdrawal amount (max: 5,000)
- `currency` (body): Currency code

**Response:** Withdrawal request confirmation with pending status

#### Place Bet
```
POST /api/wallets/{playerId}/bet
```
**Parameters:**
- `playerId` (path): Player identifier
- `amount` (body): Bet amount
- `currency` (body): Currency code
- `gameId` (body): Game identifier

**Response:** Bet confirmation with updated balance

#### Credit Winnings
```
POST /api/wallets/{playerId}/win
```
**Parameters:**
- `playerId` (path): Player identifier
- `amount` (body): Win amount
- `currency` (body): Currency code
- `gameId` (body): Game identifier

**Response:** Win confirmation with updated balance

#### Add Bonus
```
POST /api/wallets/{playerId}/bonus
```
**Parameters:**
- `playerId` (path): Player identifier
- `amount` (body): Bonus amount
- `currency` (body): Currency code
- `bonusCode` (body): Bonus code identifier

**Response:** Bonus confirmation with updated bonus balance

#### Get Balance
```
GET /api/wallets/{playerId}/balance/{currency}
```
**Parameters:**
- `playerId` (path): Player identifier
- `currency` (path): Currency code

**Response:** Current real balance, bonus balance, and total balance

#### Get Transaction History
```
GET /api/wallets/{playerId}/transactions
```
**Parameters:**
- `playerId` (path): Player identifier
- `currency` (query): Currency code
- `days` (query): Number of days to retrieve (default: 30, max: 100 transactions)

**Response:** List of transactions with details

#### Convert Currency
```
POST /api/wallets/{playerId}/convert
```
**Parameters:**
- `playerId` (path): Player identifier
- `fromCurrency` (body): Source currency code
- `toCurrency` (body): Destination currency code
- `amount` (body): Amount to convert

**Response:** Conversion confirmation with exchange rate and new balances

### Administrative Operations

#### Approve Withdrawal
```
PUT /api/admin/withdrawals/{withdrawalId}/approve
```
**Parameters:**
- `withdrawalId` (path): Withdrawal request identifier

**Response:** Approval confirmation with updated status

#### Generate Daily Report
```
GET /api/admin/reports/daily
```
**Parameters:**
- `startDate` (query, optional): Report start date
- `endDate` (query, optional): Report end date
- `format` (query, optional): Output format (json, csv)

**Response:** Aggregated financial data by date and currency

## Error Responses

All endpoints return standardized error responses:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human readable error message",
    "details": "Additional error context"
  }
}
```

## Common Error Codes

- `INVALID_AMOUNT`: Amount is zero, negative, or exceeds limits
- `INSUFFICIENT_FUNDS`: Not enough balance for operation
- `WALLET_NOT_FOUND`: Wallet doesn't exist for player/currency
- `UNSUPPORTED_CURRENCY`: Currency not supported
- `WITHDRAWAL_NOT_FOUND`: Withdrawal ID doesn't exist
- `VALIDATION_ERROR`: Request data validation failed
- `AUTHENTICATION_ERROR`: Invalid or missing authentication
- `AUTHORIZATION_ERROR`: Insufficient permissions
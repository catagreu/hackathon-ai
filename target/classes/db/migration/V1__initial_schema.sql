CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    player_id INTEGER NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    bonus_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT unique_player_currency UNIQUE (player_id, currency)
);

CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    player_id INTEGER NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_before DECIMAL(15,2),
    balance_after DECIMAL(15,2),
    timestamp TIMESTAMP NOT NULL,
    description TEXT
);

CREATE TABLE pending_withdrawals (
    id BIGSERIAL PRIMARY KEY,
    player_id INTEGER NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE INDEX idx_wallets_player_currency ON wallets(player_id, currency);
CREATE INDEX idx_transactions_player_currency ON transactions(player_id, currency);
CREATE INDEX idx_transactions_timestamp ON transactions(timestamp);
CREATE INDEX idx_pending_withdrawals_status ON pending_withdrawals(status);


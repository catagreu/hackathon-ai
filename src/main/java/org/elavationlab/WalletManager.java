package org.elavationlab;

import java.sql.*;
import java.util.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Monolithic Wallet Management System
 * WARNING: This is legacy code with multiple anti-patterns
 * DO NOT use as reference for best practices
 */
public class WalletManager {

    private Connection dbConnection;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/wallets";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "admin123";

    private static final double MAX_DEPOSIT = 10000.0;
    private static final double MAX_WITHDRAWAL = 5000.0;
    private static final double MIN_BALANCE = 0.0;

    // Currency exchange rates (hardcoded, never updated)
    private static final Map<String, Double> EXCHANGE_RATES = new HashMap<String, Double>() {{
        put("USD", 1.0);
        put("EUR", 0.85);
        put("GBP", 0.73);
        put("CAD", 1.25);
    }};

    public WalletManager() throws SQLException {
        dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
        initializeDatabase();
    }

    private void initializeDatabase() {
        try {
            Statement stmt = dbConnection.createStatement();
            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS wallets (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "player_id INT NOT NULL," +
                            "currency VARCHAR(3) NOT NULL," +
                            "balance DECIMAL(15,2) NOT NULL DEFAULT 0.00," +
                            "bonus_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00," +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                            "UNIQUE KEY unique_player_currency (player_id, currency))"
            );

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS transactions (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "player_id INT NOT NULL," +
                            "type VARCHAR(20) NOT NULL," +
                            "amount DECIMAL(15,2) NOT NULL," +
                            "currency VARCHAR(3) NOT NULL," +
                            "balance_before DECIMAL(15,2)," +
                            "balance_after DECIMAL(15,2)," +
                            "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "description TEXT)"
            );

            stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS pending_withdrawals (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY," +
                            "player_id INT NOT NULL," +
                            "amount DECIMAL(15,2) NOT NULL," +
                            "currency VARCHAR(3) NOT NULL," +
                            "status VARCHAR(20) DEFAULT 'PENDING'," +
                            "requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                            "processed_at TIMESTAMP NULL)"
            );
        } catch (SQLException e) {
            System.err.println("Database initialization failed: " + e.getMessage());
        }
    }

    public String processDeposit(int playerId, double amount, String currency) {
        try {
            // Validation
            if (amount <= 0) return "ERROR: Invalid amount";
            if (amount > MAX_DEPOSIT) return "ERROR: Exceeds deposit limit of " + MAX_DEPOSIT;
            if (!EXCHANGE_RATES.containsKey(currency)) return "ERROR: Unsupported currency";

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

            double newBalance = currentBalance + amount;

            // Update or create wallet
            if (walletExists) {
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
                    "INSERT INTO transactions (player_id, type, amount, currency, balance_before, balance_after, description) " +
                            "VALUES (" + playerId + ",'DEPOSIT'," + amount + ",'" + currency + "'," +
                            currentBalance + "," + newBalance + ",'Deposit via payment gateway')"
            );

            return "SUCCESS: Deposited " + amount + " " + currency + ". New balance: " + newBalance;
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String processWithdrawal(int playerId, double amount, String currency) {
        try {
            if (amount <= 0) return "ERROR: Invalid amount";
            if (amount > MAX_WITHDRAWAL) return "ERROR: Exceeds withdrawal limit of " + MAX_WITHDRAWAL;

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
                return "ERROR: Insufficient funds. Current balance: " + currentBalance;
            }

            // Create pending withdrawal
            stmt.executeUpdate(
                    "INSERT INTO pending_withdrawals (player_id, amount, currency) " +
                            "VALUES (" + playerId + "," + amount + ",'" + currency + "')"
            );

            double newBalance = currentBalance - amount;
            stmt.executeUpdate(
                    "UPDATE wallets SET balance=" + newBalance +
                            ", updated_at=NOW() WHERE player_id=" + playerId +
                            " AND currency='" + currency + "'"
            );

            stmt.executeUpdate(
                    "INSERT INTO transactions (player_id, type, amount, currency, balance_before, balance_after, description) " +
                            "VALUES (" + playerId + ",'WITHDRAWAL'," + amount + ",'" + currency + "'," +
                            currentBalance + "," + newBalance + ",'Withdrawal requested')"
            );

            return "SUCCESS: Withdrawal of " + amount + " " + currency + " requested. New balance: " + newBalance;
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String processBet(int playerId, double amount, String currency, String gameId) {
        try {
            if (amount <= 0) return "ERROR: Invalid bet amount";

            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT balance, bonus_balance FROM wallets WHERE player_id=" + playerId +
                            " AND currency='" + currency + "'"
            );

            if (!rs.next()) {
                return "ERROR: Wallet not found";
            }

            double balance = rs.getDouble("balance");
            double bonusBalance = rs.getDouble("bonus_balance");
            double totalAvailable = balance + bonusBalance;

            if (totalAvailable < amount) {
                return "ERROR: Insufficient funds. Available: " + totalAvailable;
            }

            // Deduct from bonus first, then real balance
            double remainingAmount = amount;
            double newBonusBalance = bonusBalance;
            double newBalance = balance;

            if (bonusBalance > 0) {
                if (bonusBalance >= remainingAmount) {
                    newBonusBalance = bonusBalance - remainingAmount;
                    remainingAmount = 0;
                } else {
                    remainingAmount -= bonusBalance;
                    newBonusBalance = 0;
                }
            }

            if (remainingAmount > 0) {
                newBalance = balance - remainingAmount;
            }

            stmt.executeUpdate(
                    "UPDATE wallets SET balance=" + newBalance +
                            ", bonus_balance=" + newBonusBalance +
                            ", updated_at=NOW() WHERE player_id=" + playerId +
                            " AND currency='" + currency + "'"
            );

            stmt.executeUpdate(
                    "INSERT INTO transactions (player_id, type, amount, currency, balance_before, balance_after, description) " +
                            "VALUES (" + playerId + ",'BET'," + amount + ",'" + currency + "'," +
                            balance + "," + newBalance + ",'Bet on game " + gameId + "')"
            );

            return "SUCCESS: Bet placed. Amount: " + amount + " " + currency;
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String processWin(int playerId, double amount, String currency, String gameId) {
        try {
            if (amount <= 0) return "ERROR: Invalid win amount";

            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT balance FROM wallets WHERE player_id=" + playerId +
                            " AND currency='" + currency + "'"
            );

            if (!rs.next()) {
                return "ERROR: Wallet not found";
            }

            double currentBalance = rs.getDouble("balance");
            double newBalance = currentBalance + amount;

            stmt.executeUpdate(
                    "UPDATE wallets SET balance=" + newBalance +
                            ", updated_at=NOW() WHERE player_id=" + playerId +
                            " AND currency='" + currency + "'"
            );

            stmt.executeUpdate(
                    "INSERT INTO transactions (player_id, type, amount, currency, balance_before, balance_after, description) " +
                            "VALUES (" + playerId + ",'WIN'," + amount + ",'" + currency + "'," +
                            currentBalance + "," + newBalance + ",'Win from game " + gameId + "')"
            );

            return "SUCCESS: Win credited. Amount: " + amount + " " + currency;
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String addBonusBalance(int playerId, double amount, String currency, String bonusCode) {
        try {
            if (amount <= 0) return "ERROR: Invalid bonus amount";

            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT bonus_balance FROM wallets WHERE player_id=" + playerId +
                            " AND currency='" + currency + "'"
            );

            if (!rs.next()) {
                return "ERROR: Wallet not found";
            }

            double currentBonus = rs.getDouble("bonus_balance");
            double newBonus = currentBonus + amount;

            stmt.executeUpdate(
                    "UPDATE wallets SET bonus_balance=" + newBonus +
                            ", updated_at=NOW() WHERE player_id=" + playerId +
                            " AND currency='" + currency + "'"
            );

            stmt.executeUpdate(
                    "INSERT INTO transactions (player_id, type, amount, currency, description) " +
                            "VALUES (" + playerId + ",'BONUS'," + amount + ",'" + currency + "'," +
                            "'Bonus credited: " + bonusCode + "')"
            );

            return "SUCCESS: Bonus credited. Amount: " + amount + " " + currency;
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String getBalance(int playerId, String currency) {
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT balance, bonus_balance FROM wallets WHERE player_id=" + playerId +
                            " AND currency='" + currency + "'"
            );

            if (!rs.next()) {
                return "ERROR: Wallet not found";
            }

            double balance = rs.getDouble("balance");
            double bonusBalance = rs.getDouble("bonus_balance");

            return "Balance: " + balance + " " + currency +
                    " | Bonus: " + bonusBalance + " " + currency +
                    " | Total: " + (balance + bonusBalance) + " " + currency;
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String convertCurrency(int playerId, String fromCurrency, String toCurrency, double amount) {
        try {
            if (!EXCHANGE_RATES.containsKey(fromCurrency) || !EXCHANGE_RATES.containsKey(toCurrency)) {
                return "ERROR: Unsupported currency";
            }

            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT balance FROM wallets WHERE player_id=" + playerId +
                            " AND currency='" + fromCurrency + "'"
            );

            if (!rs.next()) {
                return "ERROR: Source wallet not found";
            }

            double sourceBalance = rs.getDouble("balance");
            if (sourceBalance < amount) {
                return "ERROR: Insufficient funds in " + fromCurrency + " wallet";
            }

            // Calculate conversion
            double fromRate = EXCHANGE_RATES.get(fromCurrency);
            double toRate = EXCHANGE_RATES.get(toCurrency);
            double convertedAmount = (amount / fromRate) * toRate;
            convertedAmount = Math.round(convertedAmount * 100.0) / 100.0; // Round to 2 decimals

            // Deduct from source
            double newSourceBalance = sourceBalance - amount;
            stmt.executeUpdate(
                    "UPDATE wallets SET balance=" + newSourceBalance +
                            ", updated_at=NOW() WHERE player_id=" + playerId +
                            " AND currency='" + fromCurrency + "'"
            );

            // Add to destination
            ResultSet rs2 = stmt.executeQuery(
                    "SELECT balance FROM wallets WHERE player_id=" + playerId +
                            " AND currency='" + toCurrency + "'"
            );

            double destBalance = 0;
            boolean destExists = rs2.next();
            if (destExists) {
                destBalance = rs2.getDouble("balance");
            }

            double newDestBalance = destBalance + convertedAmount;

            if (destExists) {
                stmt.executeUpdate(
                        "UPDATE wallets SET balance=" + newDestBalance +
                                ", updated_at=NOW() WHERE player_id=" + playerId +
                                " AND currency='" + toCurrency + "'"
                );
            } else {
                stmt.executeUpdate(
                        "INSERT INTO wallets (player_id, currency, balance) " +
                                "VALUES (" + playerId + ",'" + toCurrency + "'," + newDestBalance + ")"
                );
            }

            // Log transactions
            stmt.executeUpdate(
                    "INSERT INTO transactions (player_id, type, amount, currency, description) " +
                            "VALUES (" + playerId + ",'CONVERSION'," + amount + ",'" + fromCurrency + "'," +
                            "'Converted to " + convertedAmount + " " + toCurrency + "')"
            );

            return "SUCCESS: Converted " + amount + " " + fromCurrency +
                    " to " + convertedAmount + " " + toCurrency;
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public String getTransactionHistory(int playerId, String currency, int days) {
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT type, amount, balance_before, balance_after, timestamp, description " +
                            "FROM transactions WHERE player_id=" + playerId +
                            " AND currency='" + currency + "'" +
                            " AND timestamp >= DATE_SUB(NOW(), INTERVAL " + days + " DAY)" +
                            " ORDER BY timestamp DESC LIMIT 100"
            );

            StringBuilder history = new StringBuilder();
            history.append("Transaction History for Player ").append(playerId)
                    .append(" (").append(currency).append(") - Last ").append(days).append(" days:\n");
            history.append("=".repeat(80)).append("\n");

            int count = 0;
            while (rs.next()) {
                count++;
                history.append(String.format("%s | %s | Amount: %.2f | Before: %.2f | After: %.2f | %s\n",
                        rs.getTimestamp("timestamp"),
                        rs.getString("type"),
                        rs.getDouble("amount"),
                        rs.getDouble("balance_before"),
                        rs.getDouble("balance_after"),
                        rs.getString("description")
                ));
            }

            if (count == 0) {
                return "No transactions found for the specified period";
            }

            history.append("=".repeat(80)).append("\n");
            history.append("Total transactions: ").append(count);

            return history.toString();
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public void generateDailyReport(String outputFile) {
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(
                    "SELECT DATE(timestamp) as date, currency, " +
                            "SUM(CASE WHEN type='DEPOSIT' THEN amount ELSE 0 END) as total_deposits, " +
                            "SUM(CASE WHEN type='WITHDRAWAL' THEN amount ELSE 0 END) as total_withdrawals, " +
                            "SUM(CASE WHEN type='BET' THEN amount ELSE 0 END) as total_bets, " +
                            "SUM(CASE WHEN type='WIN' THEN amount ELSE 0 END) as total_wins, " +
                            "COUNT(DISTINCT player_id) as unique_players " +
                            "FROM transactions " +
                            "WHERE timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY) " +
                            "GROUP BY DATE(timestamp), currency " +
                            "ORDER BY date DESC, currency"
            );

            FileWriter writer = new FileWriter(outputFile);
            writer.write("Date,Currency,Deposits,Withdrawals,Bets,Wins,Unique Players\n");

            while (rs.next()) {
                writer.write(String.format("%s,%s,%.2f,%.2f,%.2f,%.2f,%d\n",
                        rs.getDate("date"),
                        rs.getString("currency"),
                        rs.getDouble("total_deposits"),
                        rs.getDouble("total_withdrawals"),
                        rs.getDouble("total_bets"),
                        rs.getDouble("total_wins"),
                        rs.getInt("unique_players")
                ));
            }

            writer.close();
            System.out.println("Report generated: " + outputFile);
        } catch (Exception e) {
            System.err.println("Report generation failed: " + e.getMessage());
        }
    }

    public String approveWithdrawal(int withdrawalId) {
        try {
            Statement stmt = dbConnection.createStatement();
            stmt.executeUpdate(
                    "UPDATE pending_withdrawals SET status='APPROVED', processed_at=NOW() " +
                            "WHERE id=" + withdrawalId
            );
            return "SUCCESS: Withdrawal approved";
        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public void close() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }

    // Main method for testing
    public static void main(String[] args) {
        try {
            WalletManager wm = new WalletManager();

            System.out.println(wm.processDeposit(1001, 500.0, "USD"));
            System.out.println(wm.getBalance(1001, "USD"));
            System.out.println(wm.processBet(1001, 50.0, "USD", "SLOT_001"));
            System.out.println(wm.processWin(1001, 150.0, "USD", "SLOT_001"));
            System.out.println(wm.getBalance(1001, "USD"));

            wm.generateDailyReport("wallet_report.csv");

            wm.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
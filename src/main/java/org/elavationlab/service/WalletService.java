package org.elavationlab.service;

import org.elavationlab.domain.Transaction;
import org.elavationlab.domain.Wallet;
import org.elavationlab.dto.MultiCurrencyBalanceResponse;
import org.elavationlab.dto.WalletBalanceResponse;
import org.elavationlab.exception.InsufficientFundsException;
import org.elavationlab.exception.InvalidAmountException;
import org.elavationlab.exception.UnsupportedCurrencyException;
import org.elavationlab.exception.WalletNotFoundException;
import org.elavationlab.repository.TransactionRepository;
import org.elavationlab.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final BigDecimal MAX_DEPOSIT = new BigDecimal("10000.00");
    private static final BigDecimal MAX_WITHDRAWAL = new BigDecimal("5000.00");
    private static final Map<String, BigDecimal> EXCHANGE_RATES = Map.of(
            "USD", BigDecimal.ONE,
            "EUR", new BigDecimal("0.85"),
            "GBP", new BigDecimal("0.73"),
            "CAD", new BigDecimal("1.25")
    );

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public WalletService(WalletRepository walletRepository, TransactionRepository transactionRepository) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public WalletBalanceResponse processDeposit(Integer playerId, BigDecimal amount, String currency) {
        validateAmount(amount);
        validateCurrency(currency);
        
        if (amount.compareTo(MAX_DEPOSIT) > 0) {
            throw new InvalidAmountException("Exceeds deposit limit of " + MAX_DEPOSIT);
        }

        Wallet wallet = walletRepository.findByPlayerIdAndCurrency(playerId, currency)
                .orElse(Wallet.builder()
                        .playerId(playerId)
                        .currency(currency)
                        .balance(BigDecimal.ZERO)
                        .bonusBalance(BigDecimal.ZERO)
                        .build());

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet savedWallet = walletRepository.save(wallet);

        createTransaction(playerId, Transaction.TransactionType.DEPOSIT, amount, currency,
                balanceBefore, savedWallet.getBalance(), "Deposit via payment gateway");

        return mapToResponse(savedWallet);
    }

    @Transactional
    public WalletBalanceResponse processWithdrawal(Integer playerId, BigDecimal amount, String currency) {
        validateAmount(amount);
        
        if (amount.compareTo(MAX_WITHDRAWAL) > 0) {
            throw new InvalidAmountException("Exceeds withdrawal limit of " + MAX_WITHDRAWAL);
        }

        Wallet wallet = walletRepository.findByPlayerIdAndCurrency(playerId, currency)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for player " + playerId + " and currency " + currency));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds. Current balance: " + wallet.getBalance());
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance().subtract(amount));
        Wallet savedWallet = walletRepository.save(wallet);

        createTransaction(playerId, Transaction.TransactionType.WITHDRAWAL, amount, currency,
                balanceBefore, savedWallet.getBalance(), "Withdrawal requested");

        return mapToResponse(savedWallet);
    }

    @Transactional
    public void processBet(Integer playerId, BigDecimal amount, String currency, String gameId) {
        validateAmount(amount);

        Wallet wallet = walletRepository.findByPlayerIdAndCurrency(playerId, currency)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for player " + playerId + " and currency " + currency));

        BigDecimal totalAvailable = wallet.getBalance().add(wallet.getBonusBalance());
        if (totalAvailable.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds. Available: " + totalAvailable);
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal remainingAmount = amount;

        // Deduct from bonus first, then from balance
        if (wallet.getBonusBalance().compareTo(BigDecimal.ZERO) > 0) {
            if (wallet.getBonusBalance().compareTo(remainingAmount) >= 0) {
                wallet.setBonusBalance(wallet.getBonusBalance().subtract(remainingAmount));
                remainingAmount = BigDecimal.ZERO;
            } else {
                remainingAmount = remainingAmount.subtract(wallet.getBonusBalance());
                wallet.setBonusBalance(BigDecimal.ZERO);
            }
        }

        if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
            wallet.setBalance(wallet.getBalance().subtract(remainingAmount));
        }

        Wallet savedWallet = walletRepository.save(wallet);

        createTransaction(playerId, Transaction.TransactionType.BET, amount, currency,
                balanceBefore, savedWallet.getBalance(), "Bet on game " + gameId);
    }

    @Transactional
    public WalletBalanceResponse processWin(Integer playerId, BigDecimal amount, String currency, String gameId) {
        validateAmount(amount);

        Wallet wallet = walletRepository.findByPlayerIdAndCurrency(playerId, currency)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for player " + playerId + " and currency " + currency));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet savedWallet = walletRepository.save(wallet);

        createTransaction(playerId, Transaction.TransactionType.WIN, amount, currency,
                balanceBefore, savedWallet.getBalance(), "Win from game " + gameId);

        return mapToResponse(savedWallet);
    }

    @Transactional
    public WalletBalanceResponse addBonusBalance(Integer playerId, BigDecimal amount, String currency, String bonusCode) {
        validateAmount(amount);

        Wallet wallet = walletRepository.findByPlayerIdAndCurrency(playerId, currency)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for player " + playerId + " and currency " + currency));

        wallet.setBonusBalance(wallet.getBonusBalance().add(amount));
        Wallet savedWallet = walletRepository.save(wallet);

        createTransaction(playerId, Transaction.TransactionType.BONUS, amount, currency,
                null, null, "Bonus credited: " + bonusCode);

        return mapToResponse(savedWallet);
    }

    @Transactional
    public WalletBalanceResponse convertCurrency(Integer playerId, String fromCurrency, String toCurrency, BigDecimal amount) {
        validateAmount(amount);
        validateCurrency(fromCurrency);
        validateCurrency(toCurrency);

        Wallet sourceWallet = walletRepository.findByPlayerIdAndCurrency(playerId, fromCurrency)
                .orElseThrow(() -> new WalletNotFoundException("Source wallet not found for player " + playerId + " and currency " + fromCurrency));

        if (sourceWallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException("Insufficient funds in " + fromCurrency + " wallet");
        }

        BigDecimal fromRate = EXCHANGE_RATES.get(fromCurrency);
        BigDecimal toRate = EXCHANGE_RATES.get(toCurrency);
        BigDecimal convertedAmount = amount.divide(fromRate, 10, RoundingMode.HALF_UP)
                .multiply(toRate)
                .setScale(2, RoundingMode.HALF_UP);

        sourceWallet.setBalance(sourceWallet.getBalance().subtract(amount));
        walletRepository.save(sourceWallet);

        Wallet targetWallet = walletRepository.findByPlayerIdAndCurrency(playerId, toCurrency)
                .orElse(Wallet.builder()
                        .playerId(playerId)
                        .currency(toCurrency)
                        .balance(BigDecimal.ZERO)
                        .bonusBalance(BigDecimal.ZERO)
                        .build());

        targetWallet.setBalance(targetWallet.getBalance().add(convertedAmount));
        walletRepository.save(targetWallet);

        createTransaction(playerId, Transaction.TransactionType.CONVERSION, amount, fromCurrency,
                null, null, "Converted to " + convertedAmount + " " + toCurrency);

        return mapToResponse(targetWallet);
    }

    public WalletBalanceResponse getBalance(Integer playerId, String currency) {
        Wallet wallet = walletRepository.findByPlayerIdAndCurrency(playerId, currency)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for player " + playerId + " and currency " + currency));

        return mapToResponse(wallet);
    }

    public MultiCurrencyBalanceResponse getAllBalances(Integer playerId) {
        List<Wallet> wallets = walletRepository.findByPlayerId(playerId);
        
        List<MultiCurrencyBalanceResponse.CurrencyBalance> currencyBalances = wallets.stream()
                .map(wallet -> MultiCurrencyBalanceResponse.CurrencyBalance.builder()
                        .currency(wallet.getCurrency())
                        .balance(wallet.getBalance())
                        .bonusBalance(wallet.getBonusBalance())
                        .totalBalance(wallet.getTotalBalance())
                        .build())
                .collect(Collectors.toList());

        // Calculate total balance in USD
        BigDecimal totalBalanceInUSD = currencyBalances.stream()
                .map(cb -> {
                    BigDecimal rate = EXCHANGE_RATES.getOrDefault(cb.getCurrency(), BigDecimal.ONE);
                    return cb.getTotalBalance().divide(rate, 10, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return MultiCurrencyBalanceResponse.builder()
                .playerId(playerId)
                .currencies(currencyBalances)
                .totalBalanceInUSD(totalBalanceInUSD)
                .build();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be greater than 0");
        }
    }

    private void validateCurrency(String currency) {
        if (!EXCHANGE_RATES.containsKey(currency)) {
            throw new UnsupportedCurrencyException("Unsupported currency: " + currency);
        }
    }

    private void createTransaction(Integer playerId, Transaction.TransactionType type, BigDecimal amount,
                                   String currency, BigDecimal balanceBefore, BigDecimal balanceAfter, String description) {
        Transaction transaction = Transaction.builder()
                .playerId(playerId)
                .type(type)
                .amount(amount)
                .currency(currency)
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .description(description)
                .build();

        transactionRepository.save(transaction);
    }

    private WalletBalanceResponse mapToResponse(Wallet wallet) {
        return WalletBalanceResponse.builder()
                .playerId(wallet.getPlayerId())
                .currency(wallet.getCurrency())
                .balance(wallet.getBalance())
                .bonusBalance(wallet.getBonusBalance())
                .totalBalance(wallet.getTotalBalance())
                .build();
    }
}


package org.elavationlab.service;

import org.elavationlab.domain.Transaction;
import org.elavationlab.domain.Wallet;
import org.elavationlab.dto.TransactionResponse;
import org.elavationlab.exception.WalletNotFoundException;
import org.elavationlab.repository.TransactionRepository;
import org.elavationlab.repository.WalletRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionService(TransactionRepository transactionRepository, WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    public List<TransactionResponse> getTransactionHistory(Integer playerId, String currency, int days) {
        // Verify wallet exists
        walletRepository.findByPlayerIdAndCurrency(playerId, currency)
                .orElseThrow(() -> new WalletNotFoundException("Wallet not found for player " + playerId + " and currency " + currency));

        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Transaction> transactions = transactionRepository.findRecentTransactions(playerId, currency, since);

        return transactions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        return TransactionResponse.builder()
                .id(transaction.getId())
                .playerId(transaction.getPlayerId())
                .type(transaction.getType())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .balanceBefore(transaction.getBalanceBefore())
                .balanceAfter(transaction.getBalanceAfter())
                .timestamp(transaction.getTimestamp())
                .description(transaction.getDescription())
                .build();
    }
}


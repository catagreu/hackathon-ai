package org.elavationlab.service;

import org.elavationlab.domain.Transaction;
import org.elavationlab.dto.TransactionResponse;
import org.elavationlab.exception.WalletNotFoundException;
import org.elavationlab.repository.TransactionRepository;
import org.elavationlab.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private TransactionService transactionService;

    private static final Integer PLAYER_ID = 1001;
    private static final String CURRENCY = "USD";

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(transactionRepository, walletRepository);
    }

    @Test
    void getTransactionHistory_shouldReturnTransactions_whenWalletExists() {
        // Given
        Transaction transaction1 = Transaction.builder()
                .id(1L)
                .playerId(PLAYER_ID)
                .type(Transaction.TransactionType.DEPOSIT)
                .amount(new BigDecimal("100.00"))
                .currency(CURRENCY)
                .timestamp(LocalDateTime.now().minusDays(1))
                .build();

        Transaction transaction2 = Transaction.builder()
                .id(2L)
                .playerId(PLAYER_ID)
                .type(Transaction.TransactionType.BET)
                .amount(new BigDecimal("50.00"))
                .currency(CURRENCY)
                .timestamp(LocalDateTime.now())
                .build();

        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.of(mock(org.elavationlab.domain.Wallet.class)));
        when(transactionRepository.findRecentTransactions(eq(PLAYER_ID), eq(CURRENCY), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(transaction2, transaction1));

        // When
        List<TransactionResponse> result = transactionService.getTransactionHistory(PLAYER_ID, CURRENCY, 30);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo(Transaction.TransactionType.BET);
        verify(transactionRepository).findRecentTransactions(eq(PLAYER_ID), eq(CURRENCY), any(LocalDateTime.class));
    }

    @Test
    void getTransactionHistory_shouldThrowException_whenWalletNotFound() {
        // Given
        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> transactionService.getTransactionHistory(PLAYER_ID, CURRENCY, 30))
                .isInstanceOf(WalletNotFoundException.class);
    }
}


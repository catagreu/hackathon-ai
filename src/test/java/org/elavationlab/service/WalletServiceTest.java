package org.elavationlab.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.elavationlab.domain.Transaction;
import org.elavationlab.domain.Wallet;
import org.elavationlab.dto.WalletBalanceResponse;
import org.elavationlab.exception.InsufficientFundsException;
import org.elavationlab.exception.InvalidAmountException;
import org.elavationlab.exception.UnsupportedCurrencyException;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    private MeterRegistry meterRegistry;

    private WalletService walletService;

    private static final Integer PLAYER_ID = 1001;
    private static final String CURRENCY = "USD";

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        walletService = new WalletService(walletRepository, transactionRepository, meterRegistry);
    }

    @Test
    void processDeposit_shouldCreateNewWallet_whenWalletDoesNotExist() {
        // Given
        BigDecimal amount = new BigDecimal("500.00");
        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        WalletBalanceResponse response = walletService.processDeposit(PLAYER_ID, amount, CURRENCY);

        // Then
        assertThat(response.getBalance()).isEqualByComparingTo(amount);
        assertThat(response.getBonusBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getTotalBalance()).isEqualByComparingTo(amount);
        verify(walletRepository).save(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void processDeposit_shouldUpdateExistingWallet_whenWalletExists() {
        // Given
        BigDecimal existingBalance = new BigDecimal("1000.00");
        BigDecimal depositAmount = new BigDecimal("500.00");
        Wallet existingWallet = Wallet.builder()
                .id(1L)
                .playerId(PLAYER_ID)
                .currency(CURRENCY)
                .balance(existingBalance)
                .bonusBalance(BigDecimal.ZERO)
                .build();

        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        WalletBalanceResponse response = walletService.processDeposit(PLAYER_ID, depositAmount, CURRENCY);

        // Then
        assertThat(response.getBalance()).isEqualByComparingTo(existingBalance.add(depositAmount));
        verify(walletRepository).save(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void processDeposit_shouldThrowException_whenAmountIsZero() {
        // Given
        BigDecimal amount = BigDecimal.ZERO;

        // When/Then
        assertThatThrownBy(() -> walletService.processDeposit(PLAYER_ID, amount, CURRENCY))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessageContaining("Amount must be greater than 0");
    }

    @Test
    void processDeposit_shouldThrowException_whenAmountIsNegative() {
        // Given
        BigDecimal amount = new BigDecimal("-100.00");

        // When/Then
        assertThatThrownBy(() -> walletService.processDeposit(PLAYER_ID, amount, CURRENCY))
                .isInstanceOf(InvalidAmountException.class);
    }

    @Test
    void processDeposit_shouldThrowException_whenAmountExceedsLimit() {
        // Given
        BigDecimal amount = new BigDecimal("10001.00");

        // When/Then
        assertThatThrownBy(() -> walletService.processDeposit(PLAYER_ID, amount, CURRENCY))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessageContaining("Exceeds deposit limit");
    }

    @Test
    void processDeposit_shouldThrowException_whenCurrencyIsUnsupported() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        String unsupportedCurrency = "XYZ";

        // When/Then
        assertThatThrownBy(() -> walletService.processDeposit(PLAYER_ID, amount, unsupportedCurrency))
                .isInstanceOf(UnsupportedCurrencyException.class);
    }

    @Test
    void processWithdrawal_shouldUpdateBalance_whenSufficientFunds() {
        // Given
        BigDecimal existingBalance = new BigDecimal("1000.00");
        BigDecimal withdrawalAmount = new BigDecimal("300.00");
        Wallet wallet = Wallet.builder()
                .id(1L)
                .playerId(PLAYER_ID)
                .currency(CURRENCY)
                .balance(existingBalance)
                .bonusBalance(BigDecimal.ZERO)
                .build();

        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        WalletBalanceResponse response = walletService.processWithdrawal(PLAYER_ID, withdrawalAmount, CURRENCY);

        // Then
        assertThat(response.getBalance()).isEqualByComparingTo(existingBalance.subtract(withdrawalAmount));
        verify(walletRepository).save(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void processWithdrawal_shouldThrowException_whenWalletNotFound() {
        // Given
        BigDecimal amount = new BigDecimal("100.00");
        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> walletService.processWithdrawal(PLAYER_ID, amount, CURRENCY))
                .isInstanceOf(WalletNotFoundException.class);
    }

    @Test
    void processWithdrawal_shouldThrowException_whenInsufficientFunds() {
        // Given
        BigDecimal existingBalance = new BigDecimal("100.00");
        BigDecimal withdrawalAmount = new BigDecimal("200.00");
        Wallet wallet = Wallet.builder()
                .id(1L)
                .playerId(PLAYER_ID)
                .currency(CURRENCY)
                .balance(existingBalance)
                .bonusBalance(BigDecimal.ZERO)
                .build();

        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.of(wallet));

        // When/Then
        assertThatThrownBy(() -> walletService.processWithdrawal(PLAYER_ID, withdrawalAmount, CURRENCY))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void processWithdrawal_shouldThrowException_whenAmountExceedsLimit() {
        // Given
        BigDecimal amount = new BigDecimal("5001.00");

        // When/Then
        assertThatThrownBy(() -> walletService.processWithdrawal(PLAYER_ID, amount, CURRENCY))
                .isInstanceOf(InvalidAmountException.class)
                .hasMessageContaining("Exceeds withdrawal limit");
    }

    @Test
    void processBet_shouldDeductFromBonusFirst_thenFromBalance() {
        // Given
        BigDecimal balance = new BigDecimal("100.00");
        BigDecimal bonusBalance = new BigDecimal("50.00");
        BigDecimal betAmount = new BigDecimal("75.00");
        Wallet wallet = Wallet.builder()
                .id(1L)
                .playerId(PLAYER_ID)
                .currency(CURRENCY)
                .balance(balance)
                .bonusBalance(bonusBalance)
                .build();

        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        walletService.processBet(PLAYER_ID, betAmount, CURRENCY, "GAME_001");

        // Then
        verify(walletRepository).save(argThat(w -> 
            w.getBonusBalance().compareTo(BigDecimal.ZERO) == 0 &&
            w.getBalance().compareTo(new BigDecimal("75.00")) == 0
        ));
    }

    @Test
    void processBet_shouldThrowException_whenInsufficientFunds() {
        // Given
        BigDecimal balance = new BigDecimal("50.00");
        BigDecimal bonusBalance = new BigDecimal("20.00");
        BigDecimal betAmount = new BigDecimal("100.00");
        Wallet wallet = Wallet.builder()
                .id(1L)
                .playerId(PLAYER_ID)
                .currency(CURRENCY)
                .balance(balance)
                .bonusBalance(bonusBalance)
                .build();

        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.of(wallet));

        // When/Then
        assertThatThrownBy(() -> walletService.processBet(PLAYER_ID, betAmount, CURRENCY, "GAME_001"))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void processWin_shouldAddToBalance() {
        // Given
        BigDecimal existingBalance = new BigDecimal("100.00");
        BigDecimal winAmount = new BigDecimal("50.00");
        Wallet wallet = Wallet.builder()
                .id(1L)
                .playerId(PLAYER_ID)
                .currency(CURRENCY)
                .balance(existingBalance)
                .bonusBalance(BigDecimal.ZERO)
                .build();

        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        WalletBalanceResponse response = walletService.processWin(PLAYER_ID, winAmount, CURRENCY, "GAME_001");

        // Then
        assertThat(response.getBalance()).isEqualByComparingTo(existingBalance.add(winAmount));
        verify(walletRepository).save(any(Wallet.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void getBalance_shouldReturnBalance_whenWalletExists() {
        // Given
        BigDecimal balance = new BigDecimal("1000.00");
        BigDecimal bonusBalance = new BigDecimal("200.00");
        Wallet wallet = Wallet.builder()
                .id(1L)
                .playerId(PLAYER_ID)
                .currency(CURRENCY)
                .balance(balance)
                .bonusBalance(bonusBalance)
                .build();

        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.of(wallet));

        // When
        WalletBalanceResponse response = walletService.getBalance(PLAYER_ID, CURRENCY);

        // Then
        assertThat(response.getBalance()).isEqualByComparingTo(balance);
        assertThat(response.getBonusBalance()).isEqualByComparingTo(bonusBalance);
        assertThat(response.getTotalBalance()).isEqualByComparingTo(balance.add(bonusBalance));
    }

    @Test
    void getBalance_shouldThrowException_whenWalletNotFound() {
        // Given
        when(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> walletService.getBalance(PLAYER_ID, CURRENCY))
                .isInstanceOf(WalletNotFoundException.class);
    }
}


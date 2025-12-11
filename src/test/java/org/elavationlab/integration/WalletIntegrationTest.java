package org.elavationlab.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elavationlab.dto.*;
import org.elavationlab.repository.TransactionRepository;
import org.elavationlab.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class WalletIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true)
            .withStartupTimeout(java.time.Duration.ofMinutes(2));

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final Integer PLAYER_ID = 1001;
    private static final String CURRENCY = "USD";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        objectMapper = new ObjectMapper();
        walletRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void deposit_shouldCreateWalletAndReturnBalance() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency(CURRENCY);

        mockMvc.perform(post("/api/wallets/{playerId}/deposit", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00))
                .andExpect(jsonPath("$.currency").value(CURRENCY));

        assertThat(walletRepository.findByPlayerIdAndCurrency(PLAYER_ID, CURRENCY)).isPresent();
        assertThat(transactionRepository.findAll()).hasSize(1);
    }

    @Test
    void deposit_shouldUpdateExistingWallet() throws Exception {
        // First deposit
        DepositRequest request1 = new DepositRequest();
        request1.setAmount(new BigDecimal("300.00"));
        request1.setCurrency(CURRENCY);

        mockMvc.perform(post("/api/wallets/{playerId}/deposit", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk());

        // Second deposit
        DepositRequest request2 = new DepositRequest();
        request2.setAmount(new BigDecimal("200.00"));
        request2.setCurrency(CURRENCY);

        mockMvc.perform(post("/api/wallets/{playerId}/deposit", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00));

        assertThat(transactionRepository.findAll()).hasSize(2);
    }

    @Test
    void withdraw_shouldDeductFromBalance() throws Exception {
        // First deposit
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal("1000.00"));
        depositRequest.setCurrency(CURRENCY);

        mockMvc.perform(post("/api/wallets/{playerId}/deposit", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // Then withdraw
        WithdrawalRequest withdrawalRequest = new WithdrawalRequest();
        withdrawalRequest.setAmount(new BigDecimal("300.00"));
        withdrawalRequest.setCurrency(CURRENCY);

        mockMvc.perform(post("/api/wallets/{playerId}/withdraw", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawalRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(700.00));
    }

    @Test
    void withdraw_shouldReturnError_whenInsufficientFunds() throws Exception {
        WithdrawalRequest request = new WithdrawalRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency(CURRENCY);

        mockMvc.perform(post("/api/wallets/{playerId}/withdraw", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void bet_shouldDeductFromBonusFirst() throws Exception {
        // Deposit
        DepositRequest depositRequest = new DepositRequest();
        depositRequest.setAmount(new BigDecimal("100.00"));
        depositRequest.setCurrency(CURRENCY);

        mockMvc.perform(post("/api/wallets/{playerId}/deposit", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // Add bonus
        BonusRequest bonusRequest = new BonusRequest();
        bonusRequest.setAmount(new BigDecimal("50.00"));
        bonusRequest.setCurrency(CURRENCY);
        bonusRequest.setBonusCode("WELCOME");

        mockMvc.perform(post("/api/wallets/{playerId}/bonus", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bonusRequest)))
                .andExpect(status().isOk());

        // Place bet
        BetRequest betRequest = new BetRequest();
        betRequest.setAmount(new BigDecimal("75.00"));
        betRequest.setCurrency(CURRENCY);
        betRequest.setGameId("SLOT_001");

        mockMvc.perform(post("/api/wallets/{playerId}/bet", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(betRequest)))
                .andExpect(status().isOk());

        // Check balance - should have used bonus first
        mockMvc.perform(get("/api/wallets/{playerId}/balance", PLAYER_ID)
                        .param("currency", CURRENCY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bonusBalance").value(0.00))
                .andExpect(jsonPath("$.balance").value(75.00));
    }

    @Test
    void getBalance_shouldReturnCurrentBalance() throws Exception {
        // Deposit first
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("500.00"));
        request.setCurrency(CURRENCY);

        mockMvc.perform(post("/api/wallets/{playerId}/deposit", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Get balance
        mockMvc.perform(get("/api/wallets/{playerId}/balance", PLAYER_ID)
                        .param("currency", CURRENCY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(500.00))
                .andExpect(jsonPath("$.totalBalance").value(500.00));
    }

    @Test
    void deposit_shouldReturnError_whenAmountExceedsLimit() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("10001.00"));
        request.setCurrency(CURRENCY);

        mockMvc.perform(post("/api/wallets/{playerId}/deposit", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deposit_shouldReturnError_whenCurrencyIsUnsupported() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAmount(new BigDecimal("100.00"));
        request.setCurrency("XYZ");

        mockMvc.perform(post("/api/wallets/{playerId}/deposit", PLAYER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}


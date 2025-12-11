package org.elavationlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceResponse {
    private Integer playerId;
    private String currency;
    private BigDecimal balance;
    private BigDecimal bonusBalance;
    private BigDecimal totalBalance;
}


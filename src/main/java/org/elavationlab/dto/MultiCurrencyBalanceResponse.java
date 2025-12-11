package org.elavationlab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiCurrencyBalanceResponse {
    private Integer playerId;
    private List<CurrencyBalance> currencies;
    private BigDecimal totalBalanceInUSD;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencyBalance {
        private String currency;
        private BigDecimal balance;
        private BigDecimal bonusBalance;
        private BigDecimal totalBalance;
    }
}


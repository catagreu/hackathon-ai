package org.elavationlab.controller;

import jakarta.validation.Valid;
import org.elavationlab.dto.*;
import org.elavationlab.service.WalletService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/{playerId}/deposit")
    public ResponseEntity<WalletBalanceResponse> deposit(
            @PathVariable Integer playerId,
            @Valid @RequestBody DepositRequest request) {
        WalletBalanceResponse response = walletService.processDeposit(
                playerId, request.getAmount(), request.getCurrency());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{playerId}/withdraw")
    public ResponseEntity<WalletBalanceResponse> withdraw(
            @PathVariable Integer playerId,
            @Valid @RequestBody WithdrawalRequest request) {
        WalletBalanceResponse response = walletService.processWithdrawal(
                playerId, request.getAmount(), request.getCurrency());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{playerId}/bet")
    public ResponseEntity<Void> bet(
            @PathVariable Integer playerId,
            @Valid @RequestBody BetRequest request) {
        walletService.processBet(playerId, request.getAmount(), request.getCurrency(), request.getGameId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{playerId}/win")
    public ResponseEntity<WalletBalanceResponse> win(
            @PathVariable Integer playerId,
            @Valid @RequestBody WinRequest request) {
        WalletBalanceResponse response = walletService.processWin(
                playerId, request.getAmount(), request.getCurrency(), request.getGameId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{playerId}/bonus")
    public ResponseEntity<WalletBalanceResponse> addBonus(
            @PathVariable Integer playerId,
            @Valid @RequestBody BonusRequest request) {
        WalletBalanceResponse response = walletService.addBonusBalance(
                playerId, request.getAmount(), request.getCurrency(), request.getBonusCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{playerId}/convert")
    public ResponseEntity<WalletBalanceResponse> convertCurrency(
            @PathVariable Integer playerId,
            @Valid @RequestBody CurrencyConversionRequest request) {
        WalletBalanceResponse response = walletService.convertCurrency(
                playerId, request.getFromCurrency(), request.getToCurrency(), request.getAmount());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{playerId}/balance")
    public ResponseEntity<WalletBalanceResponse> getBalance(
            @PathVariable Integer playerId,
            @RequestParam String currency) {
        WalletBalanceResponse response = walletService.getBalance(playerId, currency);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{playerId}/balances")
    public ResponseEntity<MultiCurrencyBalanceResponse> getAllBalances(
            @PathVariable Integer playerId) {
        MultiCurrencyBalanceResponse response = walletService.getAllBalances(playerId);
        return ResponseEntity.ok(response);
    }
}


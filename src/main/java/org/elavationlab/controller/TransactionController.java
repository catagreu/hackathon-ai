package org.elavationlab.controller;

import org.elavationlab.dto.TransactionResponse;
import org.elavationlab.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/{playerId}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @PathVariable Integer playerId,
            @RequestParam String currency,
            @RequestParam(defaultValue = "30") int days) {
        List<TransactionResponse> transactions = transactionService.getTransactionHistory(playerId, currency, days);
        return ResponseEntity.ok(transactions);
    }
}


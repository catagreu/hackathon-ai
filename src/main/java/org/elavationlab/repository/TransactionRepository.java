package org.elavationlab.repository;

import org.elavationlab.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByPlayerIdAndCurrencyOrderByTimestampDesc(Integer playerId, String currency);
    
    @Query("SELECT t FROM Transaction t WHERE t.playerId = :playerId " +
           "AND t.currency = :currency AND t.timestamp >= :since " +
           "ORDER BY t.timestamp DESC")
    List<Transaction> findRecentTransactions(
        @Param("playerId") Integer playerId,
        @Param("currency") String currency,
        @Param("since") LocalDateTime since
    );
}


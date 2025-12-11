package org.elavationlab.repository;

import org.elavationlab.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByPlayerIdAndCurrency(Integer playerId, String currency);
    List<Wallet> findByPlayerId(Integer playerId);
}


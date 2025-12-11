package org.elavationlab.repository;

import org.elavationlab.domain.PendingWithdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PendingWithdrawalRepository extends JpaRepository<PendingWithdrawal, Long> {
}


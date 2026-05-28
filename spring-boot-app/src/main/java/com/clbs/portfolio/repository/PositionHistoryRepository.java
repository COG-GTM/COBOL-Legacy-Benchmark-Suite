package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PositionHistory entities.
 * Replaces DB2 POSHIST table access patterns.
 */
@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    List<PositionHistory> findByAccountNoAndPortfolioId(String accountNo, String portfolioId);
}

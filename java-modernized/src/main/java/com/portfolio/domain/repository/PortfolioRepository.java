package com.portfolio.domain.repository;

import com.portfolio.domain.enums.PortfolioStatus;
import com.portfolio.domain.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByClientIdAndStatus(String clientId, PortfolioStatus status);

    @Query("SELECT p FROM Portfolio p WHERE p.status = :status AND (p.closeDate IS NULL OR p.closeDate > :date)")
    List<Portfolio> findActiveByStatusAndCloseDate(
            @Param("status") PortfolioStatus status,
            @Param("date") LocalDate date);
}

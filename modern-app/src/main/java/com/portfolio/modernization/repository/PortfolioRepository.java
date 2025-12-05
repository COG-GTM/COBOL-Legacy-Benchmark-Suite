package com.portfolio.modernization.repository;

import com.portfolio.modernization.model.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByClientId(String clientId);

    List<Portfolio> findByStatus(Portfolio.PortfolioStatus status);

    Optional<Portfolio> findByAccountNumber(String accountNumber);

    @Query("SELECT p FROM Portfolio p WHERE p.status = :status AND (p.closeDate IS NULL OR p.closeDate > CURRENT_DATE)")
    List<Portfolio> findActivePortfolios(@Param("status") Portfolio.PortfolioStatus status);

    @Query("SELECT p FROM Portfolio p WHERE p.clientId = :clientId AND p.status = :status")
    List<Portfolio> findByClientIdAndStatus(@Param("clientId") String clientId, @Param("status") Portfolio.PortfolioStatus status);
}

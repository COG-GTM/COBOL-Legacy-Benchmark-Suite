package com.portfolio.repository;

import com.portfolio.entity.Portfolio;
import com.portfolio.entity.Portfolio.PortfolioStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Portfolio entity.
 * Replaces VSAM PORTMSTR file access operations.
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    Optional<Portfolio> findByPortfolioId(String portfolioId);

    Optional<Portfolio> findByPortfolioIdAndAccountTypeAndBranchId(
            String portfolioId, String accountType, String branchId);

    List<Portfolio> findByClientId(String clientId);

    Page<Portfolio> findByStatus(PortfolioStatus status, Pageable pageable);

    Page<Portfolio> findByBranchId(String branchId, Pageable pageable);

    @Query("SELECT p FROM Portfolio p WHERE p.status = :status AND p.closeDate IS NULL")
    List<Portfolio> findActivePortfolios(@Param("status") PortfolioStatus status);

    @Query("SELECT p FROM Portfolio p WHERE p.clientId = :clientId AND p.status = 'ACTIVE'")
    List<Portfolio> findActivePortfoliosByClient(@Param("clientId") String clientId);

    @Query("SELECT COUNT(p) FROM Portfolio p WHERE p.status = :status")
    long countByStatus(@Param("status") PortfolioStatus status);

    boolean existsByPortfolioId(String portfolioId);
}

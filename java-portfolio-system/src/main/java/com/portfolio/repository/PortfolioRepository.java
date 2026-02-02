package com.portfolio.repository;

import com.portfolio.domain.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Portfolio entity
 * Provides data access operations for portfolio management
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    Optional<Portfolio> findByAccountNo(String accountNo);

    List<Portfolio> findByStatus(Portfolio.PortfolioStatus status);

    List<Portfolio> findByClientType(Portfolio.ClientType clientType);

    List<Portfolio> findByClientNameContainingIgnoreCase(String clientName);

    @Query("SELECT p FROM Portfolio p WHERE p.status = :status ORDER BY p.totalValue DESC")
    List<Portfolio> findActivePortfoliosByValue(@Param("status") Portfolio.PortfolioStatus status);

    @Query("SELECT p FROM Portfolio p WHERE p.clientType = :clientType AND p.status = 'A'")
    List<Portfolio> findActiveByClientType(@Param("clientType") Portfolio.ClientType clientType);

    boolean existsByAccountNo(String accountNo);
}

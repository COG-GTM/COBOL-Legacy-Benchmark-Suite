package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Portfolio entities.
 * Replaces VSAM KSDS access patterns for PORTFLIO file.
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByStatus(Portfolio.PortfolioStatus status);

    List<Portfolio> findByClientType(Portfolio.ClientType clientType);
}

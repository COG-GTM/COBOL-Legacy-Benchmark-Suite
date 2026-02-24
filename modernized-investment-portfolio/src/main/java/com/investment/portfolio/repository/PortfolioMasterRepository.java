package com.investment.portfolio.repository;

import com.investment.portfolio.entity.PortfolioMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA Repository for the Portfolio Master entity.
 *
 * Provides CRUD operations and custom queries for portfolio management,
 * replacing the COBOL VSAM and DB2 access patterns.
 */
@Repository
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, String> {

    /**
     * Find all portfolios for a given client, ordered by status.
     * Replaces COBOL access via IDX_PORT_MASTER_CLIENT index.
     */
    List<PortfolioMaster> findByClientIdOrderByStatus(String clientId);

    /**
     * Find all portfolios by status.
     * Supports the COBOL 88-level condition checks (PORT-ACTIVE, PORT-CLOSED, PORT-SUSPENDED).
     */
    List<PortfolioMaster> findByStatus(String status);

    /**
     * Find active portfolios (mirrors the ACTIVE_PORTFOLIOS DB2 view).
     */
    @Query("SELECT p FROM PortfolioMaster p WHERE p.status = 'A' AND (p.closeDate IS NULL OR p.closeDate > CURRENT_DATE)")
    List<PortfolioMaster> findActivePortfolios();

    /**
     * Find portfolios by client ID and status.
     */
    List<PortfolioMaster> findByClientIdAndStatus(String clientId, String status);
}

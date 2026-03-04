package com.cobolbenchmark.db;

import com.cobolbenchmark.model.PortfolioMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Portfolio Master Repository - replaces VSAM portfolio master file operations.
 * Provides CRUD access to PORTFOLIO_MASTER table.
 */
@Repository
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, String> {

    @Query("SELECT p FROM PortfolioMaster p WHERE p.status = :status")
    List<PortfolioMaster> findByStatus(@Param("status") String status);

    @Query("SELECT p FROM PortfolioMaster p WHERE p.clientId = :clientId")
    List<PortfolioMaster> findByClientId(@Param("clientId") String clientId);

    @Query("SELECT p FROM PortfolioMaster p WHERE p.clientId = :clientId AND p.status = :status")
    List<PortfolioMaster> findByClientIdAndStatus(
            @Param("clientId") String clientId,
            @Param("status") String status);
}

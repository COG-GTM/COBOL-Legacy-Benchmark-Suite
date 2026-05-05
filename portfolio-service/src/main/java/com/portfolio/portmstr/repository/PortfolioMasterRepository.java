package com.portfolio.portmstr.repository;

import com.portfolio.portmstr.model.PortfolioMaster;
import com.portfolio.portmstr.model.enums.PortfolioStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Portfolio Master records.
 * Replaces COBOL VSAM KSDS file I/O operations (OPEN, READ, WRITE, REWRITE, DELETE)
 * on the PORTMSTR file and DB2 queries on PORTFOLIO_MASTER table.
 */
@Repository
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, String> {

    List<PortfolioMaster> findByStatus(PortfolioStatus status);

    List<PortfolioMaster> findByClientId(String clientId);

    @Query("SELECT p FROM PortfolioMaster p WHERE p.clientId = :clientId AND p.status = :status")
    List<PortfolioMaster> findByClientIdAndStatus(
            @Param("clientId") String clientId,
            @Param("status") PortfolioStatus status);

    @Query("SELECT p FROM PortfolioMaster p WHERE p.status = 'ACTIVE' AND (p.closeDate IS NULL OR p.closeDate > CURRENT_DATE)")
    List<PortfolioMaster> findActivePortfolios();

    Optional<PortfolioMaster> findByPortfolioIdAndAccountNo(String portfolioId, String accountNo);

    boolean existsByPortfolioId(String portfolioId);
}

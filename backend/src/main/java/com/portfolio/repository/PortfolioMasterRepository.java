package com.portfolio.repository;

import com.portfolio.entity.PortfolioMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository for PortfolioMaster entity.
 * Replaces VSAM KSDS read operations and DB2 queries from INQPORT.cbl.
 */
@Repository
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, String> {

    List<PortfolioMaster> findByClientId(String clientId);

    List<PortfolioMaster> findByStatus(String status);

    @Query("SELECT p FROM PortfolioMaster p WHERE p.status = 'A' AND (p.closeDate IS NULL OR p.closeDate > CURRENT_DATE)")
    List<PortfolioMaster> findActivePortfolios();

    List<PortfolioMaster> findByClientIdAndStatus(String clientId, String status);
}

package com.portfolio.support;

import com.portfolio.model.PortfolioMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Portfolio Master table.
 * Replaces COBOL direct DB2 SQL access in online and batch programs.
 */
@Repository
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, String> {

    List<PortfolioMaster> findByClientId(String clientId);

    List<PortfolioMaster> findByStatus(String status);

    List<PortfolioMaster> findByClientIdAndStatus(String clientId, String status);
}

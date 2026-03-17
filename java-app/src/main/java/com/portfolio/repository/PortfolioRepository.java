package com.portfolio.repository;

import com.portfolio.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Portfolio Master entity.
 * Replaces: DB2 embedded SQL (EXEC SQL ... END-EXEC) and VSAM PORTMSTR file I/O
 * found in INQPORT.cbl, PORTREAD.cbl, PORTADD.cbl, PORTUPDT.cbl, PORTDEL.cbl.
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByClientId(String clientId);

    List<Portfolio> findByStatus(String status);

    List<Portfolio> findByBranchId(String branchId);

    List<Portfolio> findByClientIdAndStatus(String clientId, String status);

    List<Portfolio> findByBranchIdAndAccountType(String branchId, String accountType);
}

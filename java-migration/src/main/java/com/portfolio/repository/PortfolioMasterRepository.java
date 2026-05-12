package com.portfolio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.portfolio.entity.PortfolioMaster;

/**
 * Spring Data JPA repository for PortfolioMaster entity.
 * <p>
 * Mirrors COBOL access patterns from PORTMSTR.cbl (keyed READ),
 * PORTREAD.cbl (sequential READ), PORTUPDT.cbl (REWRITE),
 * PORTADD.cbl (WRITE), and PORTDEL.cbl (DELETE).
 */
@Repository
public interface PortfolioMasterRepository extends JpaRepository<PortfolioMaster, String> {

    /**
     * Find portfolios by status (mirrors COBOL level-88 conditions:
     * PORT-ACTIVE='A', PORT-CLOSED='C', PORT-SUSPENDED='S').
     */
    List<PortfolioMaster> findByPortStatus(String portStatus);

    /**
     * Find portfolios by client type (mirrors COBOL level-88 conditions:
     * PORT-INDIVIDUAL='I', PORT-CORPORATE='C', PORT-TRUST='T').
     */
    List<PortfolioMaster> findByPortClientType(String portClientType);

    /**
     * Find portfolio by account number (mirrors COBOL keyed access
     * using PORT-ACCOUNT-NO as alternate lookup).
     */
    List<PortfolioMaster> findByPortAccountNo(String portAccountNo);

    /**
     * Find portfolios by client name containing a search term.
     */
    List<PortfolioMaster> findByPortClientNameContainingIgnoreCase(String name);
}

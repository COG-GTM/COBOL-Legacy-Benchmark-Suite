package com.portfolio.repository;

import com.portfolio.model.Portfolio;
import com.portfolio.model.PortfolioKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Portfolio entities.
 * Replaces VSAM KSDS (Key-Sequenced Data Set) file access in the original
 * COBOL programs (INQPORT, PORTMSTR).
 *
 * In the COBOL system, portfolio records were stored in a VSAM file
 * accessed via READ/WRITE/REWRITE/DELETE verbs with FILE STATUS checking.
 * Spring Data JPA handles all the persistence plumbing automatically.
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, PortfolioKey> {

    /**
     * Find all portfolios by portfolio ID (may span multiple accounts).
     * This is the primary lookup used by P300-PORTFOLIO-INQUIRY in INQONLN.cbl.
     */
    List<Portfolio> findByKeyPortfolioId(String portfolioId);
}

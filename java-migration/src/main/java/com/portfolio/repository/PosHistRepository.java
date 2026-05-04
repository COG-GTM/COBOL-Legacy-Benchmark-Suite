package com.portfolio.repository;

import com.portfolio.domain.PosHistId;
import com.portfolio.domain.PosHistRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Position History repository - replaces DB2 POSHIST table access.
 */
@Repository
public interface PosHistRepository extends JpaRepository<PosHistRecord, PosHistId> {

    List<PosHistRecord> findByPortfolioId(String portfolioId);

    List<PosHistRecord> findByAccountNo(String accountNo);

    List<PosHistRecord> findBySecurityIdAndTransDateBetween(
            String securityId, LocalDate startDate, LocalDate endDate);

    List<PosHistRecord> findByProcessDateAndProgramId(LocalDate processDate, String programId);
}

package com.benchmark.portfolio.common.repository;

import com.benchmark.portfolio.common.entity.PortfolioPosition;
import com.benchmark.portfolio.common.entity.PortfolioPositionId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link PortfolioPosition}, replacing VSAM KSDS POSFILE
 * (POSREC.cpy, RECORD KEY POS-KEY = POS-PORTFOLIO-ID + POS-DATE +
 * POS-INVESTMENT-ID).
 *
 * <p>Primary-key CRUD inherited from {@link JpaRepository} covers the keyed
 * position maintenance performed by the batch position-update job
 * (POSUPDT step; read-modify-REWRITE of POSFILE records).
 */
public interface PortfolioPositionRepository
        extends JpaRepository<PortfolioPosition, PortfolioPositionId> {

    /**
     * Partial-key (key-prefix) read on POS-PORTFOLIO-ID, replicating
     * INQPORT.cbl P200-GET-POSITION ({@code EXEC CICS READ FILE('POSFILE')
     * RIDFLD(POSITION-ACCOUNT)} - positioning on the leading portion of
     * POS-KEY to retrieve the account's positions).
     */
    List<PortfolioPosition> findByIdPortfolioIdOrderByIdPositionDateAscIdInvestmentIdAsc(
            String portfolioId);

    /**
     * Keyed range scan within a portfolio from a starting position date, the
     * STARTBR/READNEXT equivalent ({@code START ... KEY >= ...} +
     * {@code READ NEXT} browse over POSFILE within the POS-PORTFOLIO-ID
     * prefix).
     */
    List<PortfolioPosition> findByIdPortfolioIdAndIdPositionDateGreaterThanEqualOrderByIdPositionDateAscIdInvestmentIdAsc(
            String portfolioId, LocalDate positionDate);

    /**
     * Most recent position for one investment within a portfolio, replicating
     * the current-position lookup of INQPORT.cbl P200-GET-POSITION (the KSDS
     * holds the current record; under the date-versioned relational key the
     * latest POSITION_DATE row is the current position).
     */
    Optional<PortfolioPosition> findFirstByIdPortfolioIdAndIdInvestmentIdOrderByIdPositionDateDesc(
            String portfolioId, String investmentId);
}

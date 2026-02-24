package com.investment.portfolio.repository;

import com.investment.portfolio.entity.InvestmentPosition;
import com.investment.portfolio.entity.InvestmentPositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data JPA Repository for the Investment Position entity.
 *
 * Uses the composite key {@link InvestmentPositionId} matching the VSAM
 * Position Master key structure (Portfolio-ID + Symbol-ID + Date).
 */
@Repository
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, InvestmentPositionId> {

    /**
     * Find all positions for a given portfolio.
     * Replaces COBOL VSAM KSDS read by partial key (POS-PORTFOLIO-ID).
     */
    List<InvestmentPosition> findByIdPortfolioId(String portfolioId);

    /**
     * Find positions for a portfolio on a specific date.
     */
    List<InvestmentPosition> findByIdPortfolioIdAndIdPositionDate(String portfolioId, LocalDate positionDate);

    /**
     * Find all positions for a given date across all portfolios.
     * Replaces the DB2 IDX_POSITIONS_DATE index access pattern.
     */
    List<InvestmentPosition> findByIdPositionDate(LocalDate positionDate);

    /**
     * Find current positions (previous business day) with portfolio details.
     * Mirrors the CURRENT_POSITIONS DB2 view.
     */
    @Query("SELECT ip FROM InvestmentPosition ip JOIN FETCH ip.portfolio " +
           "WHERE ip.id.positionDate = :positionDate")
    List<InvestmentPosition> findPositionsWithPortfolio(@Param("positionDate") LocalDate positionDate);
}

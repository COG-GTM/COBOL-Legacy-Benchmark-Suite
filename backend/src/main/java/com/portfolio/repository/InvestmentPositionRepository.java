package com.portfolio.repository;

import com.portfolio.entity.InvestmentPosition;
import com.portfolio.entity.InvestmentPositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

/**
 * Repository for InvestmentPosition entity.
 * Replaces VSAM Position Master (POSMSTRE) read operations and
 * DB2 INVESTMENT_POSITIONS queries.
 */
@Repository
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, InvestmentPositionId> {

    List<InvestmentPosition> findByPortfolioId(String portfolioId);

    List<InvestmentPosition> findByPortfolioIdAndStatus(String portfolioId, String status);

    List<InvestmentPosition> findByPortfolioIdAndPositionDate(String portfolioId, LocalDate positionDate);

    @Query("SELECT ip FROM InvestmentPosition ip WHERE ip.portfolioId = :portfolioId AND ip.status = 'A'")
    List<InvestmentPosition> findActivePositions(@Param("portfolioId") String portfolioId);

    List<InvestmentPosition> findByPositionDateAndPortfolioId(LocalDate positionDate, String portfolioId);

    /**
     * Find active position by portfolio and investment ID regardless of date.
     * Used by PositionUpdateStep to find existing positions to update.
     */
    @Query("SELECT ip FROM InvestmentPosition ip WHERE ip.portfolioId = :portfolioId AND ip.investmentId = :investmentId AND ip.status = 'A' ORDER BY ip.positionDate DESC")
    List<InvestmentPosition> findActiveByPortfolioAndInvestment(@Param("portfolioId") String portfolioId, @Param("investmentId") String investmentId);
}

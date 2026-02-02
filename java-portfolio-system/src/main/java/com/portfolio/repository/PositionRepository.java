package com.portfolio.repository;

import com.portfolio.domain.Position;
import com.portfolio.domain.PositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Position entity
 * Provides data access operations for portfolio positions
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, PositionId> {

    List<Position> findByPortfolioId(String portfolioId);

    List<Position> findByPortfolioIdAndStatus(String portfolioId, Position.PositionStatus status);

    List<Position> findByPortfolioIdAndPositionDate(String portfolioId, LocalDate positionDate);

    List<Position> findByInvestmentId(String investmentId);

    @Query("SELECT p FROM Position p WHERE p.portfolioId = :portfolioId AND p.status = 'A' ORDER BY p.marketValue DESC")
    List<Position> findActivePositionsByPortfolio(@Param("portfolioId") String portfolioId);

    @Query("SELECT SUM(p.marketValue) FROM Position p WHERE p.portfolioId = :portfolioId AND p.status = 'A'")
    java.math.BigDecimal calculateTotalMarketValue(@Param("portfolioId") String portfolioId);

    @Query("SELECT SUM(p.costBasis) FROM Position p WHERE p.portfolioId = :portfolioId AND p.status = 'A'")
    java.math.BigDecimal calculateTotalCostBasis(@Param("portfolioId") String portfolioId);
}

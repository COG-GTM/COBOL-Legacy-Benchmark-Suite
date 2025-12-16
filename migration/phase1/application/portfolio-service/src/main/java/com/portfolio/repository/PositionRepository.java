package com.portfolio.repository;

import com.portfolio.entity.Position;
import com.portfolio.entity.Position.PositionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Position entity.
 * Replaces VSAM POSFILE access operations (PORTFOLIO.POSITION.VSAM).
 * 
 * @see src/cics/PORTDFN.csd - POSFILE definition
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {

    List<Position> findByPortfolioId(String portfolioId);

    Page<Position> findByPortfolioId(String portfolioId, Pageable pageable);

    Optional<Position> findByPortfolioIdAndPositionDateAndInvestmentId(
            String portfolioId, LocalDate positionDate, String investmentId);

    List<Position> findByPortfolioIdAndStatus(String portfolioId, PositionStatus status);

    List<Position> findByInvestmentId(String investmentId);

    @Query("SELECT p FROM Position p WHERE p.portfolioId = :portfolioId AND p.status = 'ACTIVE' " +
           "ORDER BY p.positionDate DESC")
    List<Position> findCurrentPositions(@Param("portfolioId") String portfolioId);

    @Query("SELECT DISTINCT p FROM Position p WHERE p.portfolioId = :portfolioId " +
           "AND p.positionDate = (SELECT MAX(p2.positionDate) FROM Position p2 " +
           "WHERE p2.portfolioId = p.portfolioId AND p2.investmentId = p.investmentId)")
    List<Position> findLatestPositionsByPortfolio(@Param("portfolioId") String portfolioId);

    @Query("SELECT SUM(p.marketValue) FROM Position p WHERE p.portfolioId = :portfolioId AND p.status = 'ACTIVE'")
    BigDecimal calculateTotalMarketValue(@Param("portfolioId") String portfolioId);

    @Query("SELECT SUM(p.costBasis) FROM Position p WHERE p.portfolioId = :portfolioId AND p.status = 'ACTIVE'")
    BigDecimal calculateTotalCostBasis(@Param("portfolioId") String portfolioId);

    @Query("SELECT COUNT(DISTINCT p.investmentId) FROM Position p WHERE p.portfolioId = :portfolioId AND p.status = 'ACTIVE'")
    long countDistinctInvestments(@Param("portfolioId") String portfolioId);
}

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
 * Position repository - replaces VSAM POSHIST KSDS file access.
 * VSAM READ FILE('POSFILE') -> findById / findByPortfolioId
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, PositionId> {

    List<Position> findByPortfolioId(String portfolioId);

    List<Position> findByPortfolioIdAndStatus(String portfolioId, String status);

    List<Position> findByPortfolioIdAndPositionDate(String portfolioId, LocalDate positionDate);

    @Query("SELECT p FROM Position p WHERE p.portfolioId = :portfolioId AND p.status = 'A'")
    List<Position> findActiveByPortfolioId(@Param("portfolioId") String portfolioId);

    @Query("SELECT p FROM Position p JOIN Portfolio pm ON p.portfolioId = pm.portfolioId " +
           "WHERE p.positionDate = :date")
    List<Position> findCurrentPositions(@Param("date") LocalDate date);
}

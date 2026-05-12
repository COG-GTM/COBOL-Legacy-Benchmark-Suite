package com.portfolio.repository;

import com.portfolio.model.entity.Position;
import com.portfolio.model.entity.PositionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<Position, PositionKey> {

    @Query("SELECT p FROM Position p WHERE p.id.portfolioId = :portfolioId ORDER BY p.id.positionDate DESC")
    List<Position> findByPortfolioId(@Param("portfolioId") String portfolioId);

    @Query("SELECT p FROM Position p JOIN Portfolio pm ON p.id.portfolioId = pm.portfolioId " +
            "WHERE p.id.positionDate = :asOfDate")
    List<Position> findCurrentPositions(@Param("asOfDate") LocalDate asOfDate);
}

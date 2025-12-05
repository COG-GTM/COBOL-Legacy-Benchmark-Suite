package com.portfolio.modernization.repository;

import com.portfolio.modernization.model.entity.Position;
import com.portfolio.modernization.model.entity.PositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<Position, PositionId> {

    List<Position> findByPortfolioId(String portfolioId);

    List<Position> findByPortfolioIdAndPositionDate(String portfolioId, LocalDate positionDate);

    List<Position> findByInvestmentId(String investmentId);

    @Query("SELECT p FROM Position p WHERE p.portfolioId = :portfolioId AND p.positionDate = (SELECT MAX(p2.positionDate) FROM Position p2 WHERE p2.portfolioId = :portfolioId)")
    List<Position> findLatestPositionsByPortfolioId(@Param("portfolioId") String portfolioId);

    @Query("SELECT p FROM Position p WHERE p.positionDate BETWEEN :startDate AND :endDate")
    List<Position> findByPositionDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}

package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.Position;
import com.clbs.portfolio.enums.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByStatus(EntityStatus status);

    List<Position> findByPortfolioId(String portfolioId);

    List<Position> findByPortfolioIdAndStatus(String portfolioId, EntityStatus status);

    @Query("SELECT p FROM Position p WHERE p.status = :status AND p.positionDate <= :date")
    List<Position> findActivePositionsAsOfDate(@Param("status") EntityStatus status,
                                               @Param("date") LocalDate date);

    @Query("SELECT p FROM Position p WHERE p.portfolioId NOT IN " +
           "(SELECT pf.portfolioId FROM Portfolio pf)")
    List<Position> findOrphanedPositions();

    @Query("SELECT DISTINCT p.portfolioId FROM Position p WHERE p.status = :status")
    List<String> findDistinctPortfolioIdsByStatus(@Param("status") EntityStatus status);
}

package com.portfolio.repository;

import com.portfolio.entity.PositionRecord;
import com.portfolio.entity.PositionRecordId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionRepository extends JpaRepository<PositionRecord, PositionRecordId> {

    List<PositionRecord> findByPortfolioId(String portfolioId);

    List<PositionRecord> findByPortfolioIdAndStatus(String portfolioId, String status);

    List<PositionRecord> findByPortfolioIdAndPositionDate(String portfolioId, LocalDate positionDate);

    @Query("SELECT p FROM PositionRecord p WHERE p.portfolioId = :portfolioId AND p.status = 'A'")
    List<PositionRecord> findActivePositions(@Param("portfolioId") String portfolioId);

    @Query("SELECT p FROM PositionRecord p WHERE p.positionDate = :date")
    List<PositionRecord> findByDate(@Param("date") LocalDate date);
}

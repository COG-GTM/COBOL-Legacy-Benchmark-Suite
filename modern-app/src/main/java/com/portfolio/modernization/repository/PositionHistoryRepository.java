package com.portfolio.modernization.repository;

import com.portfolio.modernization.model.entity.PositionHistory;
import com.portfolio.modernization.model.entity.PositionHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, PositionHistoryId> {

    List<PositionHistory> findByPortfolioId(String portfolioId);

    List<PositionHistory> findBySecurityId(String securityId);

    @Query("SELECT ph FROM PositionHistory ph WHERE ph.portfolioId = :portfolioId AND ph.transactionDate BETWEEN :startDate AND :endDate ORDER BY ph.transactionDate DESC, ph.transactionTime DESC")
    List<PositionHistory> findByPortfolioIdAndDateRange(@Param("portfolioId") String portfolioId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT ph FROM PositionHistory ph WHERE ph.processDate = :processDate AND ph.programId = :programId")
    List<PositionHistory> findByProcessDateAndProgramId(@Param("processDate") LocalDate processDate, @Param("programId") String programId);
}

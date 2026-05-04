package com.portfolio.repository;

import com.portfolio.entity.PositionHistory;
import com.portfolio.entity.PositionHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, PositionHistoryId> {

    List<PositionHistory> findBySecurityIdAndTransDateBetween(
            String securityId, LocalDate startDate, LocalDate endDate);

    List<PositionHistory> findByProcessDateAndProgramId(
            LocalDate processDate, String programId);

    List<PositionHistory> findByAccountNoAndPortfolioId(String accountNo, String portfolioId);

    @Query("SELECT ph FROM PositionHistory ph " +
            "WHERE ph.securityId = :securityId AND ph.transDate = :transDate")
    List<PositionHistory> findBySecurityIdAndTransDate(
            @Param("securityId") String securityId,
            @Param("transDate") LocalDate transDate);
}

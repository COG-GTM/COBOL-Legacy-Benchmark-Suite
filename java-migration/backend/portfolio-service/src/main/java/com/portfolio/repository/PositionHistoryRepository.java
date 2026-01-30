package com.portfolio.repository;

import com.portfolio.model.entity.PositionHistory;
import com.portfolio.model.enums.ActionCode;
import com.portfolio.model.enums.HistoryRecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    List<PositionHistory> findByPortfolioId(String portfolioId);

    List<PositionHistory> findByPortfolioIdAndRecordType(String portfolioId, HistoryRecordType recordType);

    List<PositionHistory> findByPortfolioIdAndActionCode(String portfolioId, ActionCode actionCode);

    List<PositionHistory> findByHistoryDateBetween(LocalDate startDate, LocalDate endDate);

    List<PositionHistory> findByPortfolioIdAndHistoryDateBetween(
            String portfolioId, LocalDate startDate, LocalDate endDate);

    List<PositionHistory> findByAccountNo(String accountNo);

    long countByPortfolioId(String portfolioId);
}

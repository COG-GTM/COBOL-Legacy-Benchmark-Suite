package com.portfolio.repository;

import com.portfolio.entity.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    List<PositionHistory> findByPortfolioId(String portfolioId);

    List<PositionHistory> findByAccountNoAndPortfolioId(String accountNo, String portfolioId);

    List<PositionHistory> findByTransDateBetween(LocalDate startDate, LocalDate endDate);

    List<PositionHistory> findByProgramId(String programId);
}

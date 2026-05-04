package com.coggtm.portfolio.repository;

import com.coggtm.portfolio.domain.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, Long> {

    List<PositionHistory> findByPortfolioId(String portfolioId);

    List<PositionHistory> findByAccountNo(String accountNo);
}

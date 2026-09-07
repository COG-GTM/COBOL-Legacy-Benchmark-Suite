package com.portfolio.repository;

import com.portfolio.domain.InvestmentPosition;
import com.portfolio.domain.InvestmentPositionId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvestmentPositionRepository
    extends JpaRepository<InvestmentPosition, InvestmentPositionId> {
  List<InvestmentPosition> findByIdPortfolioIdOrderByIdInvestmentId(String portfolioId);

  List<InvestmentPosition> findByIdPortfolioIdAndIdPositionDate(
      String portfolioId, java.time.LocalDate positionDate);

  @Query(
      "select position from InvestmentPosition position "
          + "join Portfolio portfolio on position.id.portfolioId=portfolio.portfolioId "
          + "where portfolio.accountNo=:accountNo")
  List<InvestmentPosition> findByAccountNo(@Param("accountNo") String accountNo);
}

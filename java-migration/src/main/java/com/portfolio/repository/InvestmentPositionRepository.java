package com.portfolio.repository;

import com.portfolio.domain.*;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface InvestmentPositionRepository
    extends JpaRepository<InvestmentPosition, InvestmentPositionId> {
  List<InvestmentPosition> findByIdPortfolioIdOrderByIdInvestmentId(String portfolioId);

  @Query(
      "select position from InvestmentPosition position "
          + "join Portfolio portfolio on position.id.portfolioId=portfolio.portfolioId "
          + "where portfolio.accountNo=:accountNo")
  List<InvestmentPosition> findByAccountNo(@Param("accountNo") String accountNo);
}

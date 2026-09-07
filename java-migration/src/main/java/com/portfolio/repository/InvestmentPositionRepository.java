package com.portfolio.repository;
import com.portfolio.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.*;
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition,InvestmentPositionId> {
 List<InvestmentPosition> findByIdPortfolioIdOrderByIdInvestmentId(String portfolioId);
 @Query("select p from InvestmentPosition p join Portfolio f on p.id.portfolioId=f.portfolioId where f.accountNo=:accountNo") List<InvestmentPosition> findByAccountNo(@Param("accountNo") String accountNo);
}

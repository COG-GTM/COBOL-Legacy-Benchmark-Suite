package com.portfolio.repository;

import com.portfolio.entity.InvestmentPosition;
import com.portfolio.entity.InvestmentPositionId;
import com.portfolio.entity.PositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, InvestmentPositionId> {

    List<InvestmentPosition> findByPortfolioId(String portfolioId);

    List<InvestmentPosition> findByPortfolioIdAndStatus(String portfolioId, PositionStatus status);

    List<InvestmentPosition> findByPortfolioIdAndPositionDate(String portfolioId, LocalDate positionDate);

    @Query("SELECT p FROM InvestmentPosition p " +
            "JOIN PortfolioMaster pm ON p.portfolioId = pm.portfolioId " +
            "WHERE pm.accountNo = :accountNo")
    List<InvestmentPosition> findByAccountNo(@Param("accountNo") String accountNo);

    @Query("SELECT p FROM InvestmentPosition p " +
            "JOIN PortfolioMaster pm ON p.portfolioId = pm.portfolioId " +
            "WHERE pm.accountNo = :accountNo AND p.positionDate = :positionDate")
    List<InvestmentPosition> findByAccountNoAndPositionDate(
            @Param("accountNo") String accountNo,
            @Param("positionDate") LocalDate positionDate);

    @Query("SELECT p FROM InvestmentPosition p WHERE p.positionDate = :positionDate")
    List<InvestmentPosition> findByPositionDate(@Param("positionDate") LocalDate positionDate);
}

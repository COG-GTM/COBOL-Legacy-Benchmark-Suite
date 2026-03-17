package com.portfolio.repository;

import com.portfolio.model.InvestmentPosition;
import com.portfolio.model.InvestmentPositionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for Investment Position entity.
 * Replaces: DB2 INVESTMENT_POSITIONS SQL and VSAM position file access
 * in POSUPDT.cbl, INQPORT.cbl, RPTPOS00.cbl.
 */
@Repository
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, InvestmentPositionKey> {

    List<InvestmentPosition> findByKeyPortfolioId(String portfolioId);

    List<InvestmentPosition> findByKeyPortfolioIdAndKeyPositionDate(String portfolioId, LocalDate positionDate);

    List<InvestmentPosition> findByKeyPositionDate(LocalDate positionDate);

    List<InvestmentPosition> findByKeyPortfolioIdAndStatus(String portfolioId, String status);

    java.util.Optional<InvestmentPosition> findByKeyPortfolioIdAndKeyInvestmentId(String portfolioId, String investmentId);
}

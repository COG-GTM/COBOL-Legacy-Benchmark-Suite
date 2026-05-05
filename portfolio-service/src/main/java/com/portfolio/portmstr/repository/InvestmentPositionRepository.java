package com.portfolio.portmstr.repository;

import com.portfolio.portmstr.model.InvestmentPosition;
import com.portfolio.portmstr.model.InvestmentPositionId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Investment Position records.
 * Replaces COBOL VSAM KSDS file I/O on the POSHIST file.
 */
@Repository
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, InvestmentPositionId> {

    List<InvestmentPosition> findByPortfolioId(String portfolioId);

    List<InvestmentPosition> findByPortfolioIdAndPositionDate(String portfolioId, LocalDate positionDate);

    @Query("SELECT ip FROM InvestmentPosition ip WHERE ip.positionDate = :date")
    List<InvestmentPosition> findByPositionDate(@Param("date") LocalDate date);
}

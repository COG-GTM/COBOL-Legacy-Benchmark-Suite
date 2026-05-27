package com.portfolio.domain.repository;

import com.portfolio.domain.model.InvestmentPosition;
import com.portfolio.domain.model.InvestmentPositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, InvestmentPositionId> {

    List<InvestmentPosition> findByIdPositionDateAndIdPortfolioId(LocalDate positionDate, String portfolioId);
}

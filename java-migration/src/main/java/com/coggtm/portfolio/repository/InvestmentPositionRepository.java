package com.coggtm.portfolio.repository;

import com.coggtm.portfolio.domain.InvestmentPosition;
import com.coggtm.portfolio.domain.InvestmentPositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, InvestmentPositionId> {

    List<InvestmentPosition> findByIdPortfolioId(String portfolioId);
}

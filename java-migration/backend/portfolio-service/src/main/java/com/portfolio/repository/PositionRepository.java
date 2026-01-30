package com.portfolio.repository;

import com.portfolio.model.entity.Position;
import com.portfolio.model.enums.PositionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByPortfolioId(String portfolioId);

    Optional<Position> findByPortfolioIdAndInvestmentId(String portfolioId, String investmentId);

    List<Position> findByPortfolioIdAndStatus(String portfolioId, PositionStatus status);

    List<Position> findByStatus(PositionStatus status);

    List<Position> findByInvestmentId(String investmentId);

    boolean existsByPortfolioIdAndInvestmentId(String portfolioId, String investmentId);
}

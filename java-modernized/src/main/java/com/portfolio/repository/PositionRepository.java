package com.portfolio.repository;

import com.portfolio.model.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Position entity.
 * Mirrors VSAM POSHIST file access patterns.
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByPortfolioId(String portfolioId);

    List<Position> findByPortfolioIdAndStatus(String portfolioId, String status);

    List<Position> findByPortfolioIdAndInvestmentId(String portfolioId, String investmentId);
}

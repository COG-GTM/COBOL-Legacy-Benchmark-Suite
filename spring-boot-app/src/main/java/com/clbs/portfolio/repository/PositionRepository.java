package com.clbs.portfolio.repository;

import com.clbs.portfolio.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findByPortfolioIdAndInvestmentId(String portfolioId, String investmentId);
}

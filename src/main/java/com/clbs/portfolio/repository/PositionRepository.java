package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    Optional<Position> findFirstByPortfolioIdAndInvestmentIdOrderByPositionDateDesc(String portfolioId,
                                                                                   String investmentId);

    List<Position> findByPortfolioIdInOrderByPortfolioIdAscInvestmentIdAscPositionDateDesc(
            Collection<String> portfolioIds);
}

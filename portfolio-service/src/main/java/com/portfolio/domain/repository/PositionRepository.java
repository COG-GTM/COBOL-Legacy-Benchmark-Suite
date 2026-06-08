package com.portfolio.domain.repository;

import com.portfolio.domain.model.Position;
import com.portfolio.domain.model.PositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Position entities.
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, PositionId> {

    List<Position> findByPortfolioId(String portfolioId);
}

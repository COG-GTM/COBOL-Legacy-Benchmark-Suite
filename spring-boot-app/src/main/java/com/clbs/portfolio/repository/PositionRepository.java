package com.clbs.portfolio.repository;

import com.clbs.portfolio.model.Position;
import com.clbs.portfolio.model.PositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Position entities.
 * Replaces VSAM KSDS access patterns for POSREC file.
 */
@Repository
public interface PositionRepository extends JpaRepository<Position, PositionId> {

    List<Position> findByPortfolioIdAndPosDate(String portfolioId, String posDate);
}

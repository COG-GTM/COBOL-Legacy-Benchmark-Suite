package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.PositionMaster;
import com.cognition.clbs.common.persistence.entity.PositionMasterKey;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link PositionMaster}. */
public interface PositionMasterRepository extends JpaRepository<PositionMaster, PositionMasterKey> {
  /** Positions of one portfolio on one date. */
  List<PositionMaster> findByIdPortfolioIdAndIdPositionDateOrderByIdInvestmentId(
      String portfolioId, LocalDate positionDate);
}

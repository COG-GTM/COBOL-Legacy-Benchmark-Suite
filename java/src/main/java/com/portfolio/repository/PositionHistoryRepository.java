package com.portfolio.repository;

import com.portfolio.domain.PositionHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repository over the POSHIST table (target of the HISTLD00 load). */
public interface PositionHistoryRepository extends JpaRepository<PositionHistory, PositionHistory.Key> {
}

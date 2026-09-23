package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.PositionHistory;
import com.cognition.clbs.common.persistence.entity.PositionHistoryKey;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link PositionHistory}. */
public interface PositionHistoryRepository
    extends JpaRepository<PositionHistory, PositionHistoryKey> {
  /** Rows for one security on one day (index {@code POSHIST_IX1}). */
  List<PositionHistory> findBySecurityIdAndIdTransactionDate(
      String securityId, LocalDate transactionDate);
}

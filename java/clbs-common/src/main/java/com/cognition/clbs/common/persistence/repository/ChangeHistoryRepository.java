package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.ChangeHistory;
import com.cognition.clbs.common.persistence.entity.ChangeHistoryKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link ChangeHistory}. */
public interface ChangeHistoryRepository extends JpaRepository<ChangeHistory, ChangeHistoryKey> {
  /** History of one portfolio, in key order (mirrors a START/READ NEXT on TRANHIST). */
  List<ChangeHistory> findByIdPortfolioIdOrderByIdHistoryDateAscIdHistoryTimeAscIdSequenceNumberAsc(
      String portfolioId);
}

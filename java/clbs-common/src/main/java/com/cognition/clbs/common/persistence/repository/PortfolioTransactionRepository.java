package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.PortfolioTransaction;
import com.cognition.clbs.common.persistence.entity.PortfolioTransactionKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link PortfolioTransaction}. */
public interface PortfolioTransactionRepository
    extends JpaRepository<PortfolioTransaction, PortfolioTransactionKey> {
  /** Transactions of one portfolio, in key order (mirrors a START/READ NEXT on TRANFILE). */
  List<PortfolioTransaction>
      findByIdPortfolioIdOrderByIdTransactionDateAscIdTransactionTimeAscIdSequenceNumberAsc(
          String portfolioId);
}

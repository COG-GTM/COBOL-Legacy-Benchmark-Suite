package com.cognition.clbs.common.persistence.repository;

import com.cognition.clbs.common.persistence.entity.Portfolio;
import com.cognition.clbs.common.persistence.entity.PortfolioKey;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data repository for {@link Portfolio}. */
public interface PortfolioRepository extends JpaRepository<Portfolio, PortfolioKey> {
  /** Generic-key read: all accounts of one portfolio, in key order. */
  List<Portfolio> findByIdPortfolioIdOrderByIdAccountNumber(String portfolioId);
}

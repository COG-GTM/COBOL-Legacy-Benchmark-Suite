package com.portfolio.repository;

import com.portfolio.domain.Portfolio;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioRepository extends JpaRepository<Portfolio, String> {
  Optional<Portfolio> findByAccountNo(String accountNo);

  boolean existsByAccountNo(String accountNo);
}

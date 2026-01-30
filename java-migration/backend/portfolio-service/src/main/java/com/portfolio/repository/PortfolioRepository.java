package com.portfolio.repository;

import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.enums.PortfolioStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    Optional<Portfolio> findByPortfolioId(String portfolioId);

    Optional<Portfolio> findByAccountNo(String accountNo);

    List<Portfolio> findByStatus(PortfolioStatus status);

    List<Portfolio> findByClientNameContainingIgnoreCase(String clientName);

    boolean existsByPortfolioId(String portfolioId);

    boolean existsByAccountNo(String accountNo);
}

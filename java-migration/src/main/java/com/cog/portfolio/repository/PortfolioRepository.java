package com.cog.portfolio.repository;

import com.cog.portfolio.domain.Portfolio;
import com.cog.portfolio.domain.PortfolioId;
import com.cog.portfolio.domain.PortfolioStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Replaces VSAM PORTMSTR access (PORTMSTR/PORTADD/PORTDEL/PORTREAD). */
public interface PortfolioRepository extends JpaRepository<Portfolio, PortfolioId> {

    Optional<Portfolio> findFirstByIdPortfolioId(String portfolioId);

    List<Portfolio> findByIdAccountNo(String accountNo);

    List<Portfolio> findByStatus(PortfolioStatus status);

    List<Portfolio> findAllByOrderByIdPortfolioIdAsc();
}

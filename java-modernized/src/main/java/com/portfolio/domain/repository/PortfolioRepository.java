package com.portfolio.domain.repository;

import com.portfolio.domain.enums.PortfolioStatus;
import com.portfolio.domain.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByClientIdAndStatus(String clientId, PortfolioStatus status);

    List<Portfolio> findByStatusAndCloseDateIsNullOrCloseDateAfter(PortfolioStatus status, LocalDate date);
}

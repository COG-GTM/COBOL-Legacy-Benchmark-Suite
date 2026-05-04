package com.coggtm.portfolio.repository;

import com.coggtm.portfolio.domain.Portfolio;
import com.coggtm.portfolio.domain.enums.PortfolioStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByClientId(String clientId);

    List<Portfolio> findByStatus(PortfolioStatus status);

    List<Portfolio> findByClientIdAndStatus(String clientId, PortfolioStatus status);
}

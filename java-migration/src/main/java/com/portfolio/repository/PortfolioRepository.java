package com.portfolio.repository;

import com.portfolio.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByStatus(String status);

    List<Portfolio> findByClientId(String clientId);

    @Query("SELECT p FROM Portfolio p WHERE p.status = 'A' AND (p.closeDate IS NULL OR p.closeDate > CURRENT_DATE)")
    List<Portfolio> findActivePortfolios();

    List<Portfolio> findByClientIdAndStatus(String clientId, String status);

    List<Portfolio> findByAccountNo(String accountNo);

    @Query("SELECT p FROM Portfolio p WHERE p.portfolioId LIKE :prefix%")
    List<Portfolio> findByPortfolioIdPrefix(@Param("prefix") String prefix);
}

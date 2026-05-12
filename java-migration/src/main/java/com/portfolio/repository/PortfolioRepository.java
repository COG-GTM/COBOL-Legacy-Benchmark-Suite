package com.portfolio.repository;

import com.portfolio.model.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    Optional<Portfolio> findByClientId(String clientId);

    List<Portfolio> findByStatus(Character status);

    @Query("SELECT p FROM Portfolio p WHERE p.status = 'A' AND (p.closeDate IS NULL OR p.closeDate > CURRENT_DATE)")
    List<Portfolio> findActivePortfolios();

    List<Portfolio> findByClientIdAndStatus(String clientId, Character status);
}

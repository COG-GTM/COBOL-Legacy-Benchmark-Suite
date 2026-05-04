package com.portfolio.repository;

import com.portfolio.domain.Portfolio;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Portfolio repository - replaces VSAM PORTMSTR KSDS file access.
 * VSAM READ ... RIDFLD(key) -> findById(key)
 * VSAM REWRITE -> save(entity)
 * VSAM sequential access -> findAll(Sort.by(...))
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByStatus(String status);

    List<Portfolio> findByClientId(String clientId);

    List<Portfolio> findByClientIdAndStatus(String clientId, String status);

    @Query("SELECT p FROM Portfolio p WHERE p.status = 'A' AND (p.closeDate IS NULL OR p.closeDate > CURRENT_DATE)")
    List<Portfolio> findActivePortfolios();

    Page<Portfolio> findByStatusOrderByPortfolioIdAsc(String status, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Portfolio p WHERE p.status = :status")
    long countByStatus(@Param("status") String status);
}

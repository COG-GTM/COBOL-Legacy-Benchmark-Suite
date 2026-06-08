package com.portfolio.domain.repository;

import com.portfolio.domain.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Portfolio aggregate root.
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    Optional<Portfolio> findByAccountNumber(String accountNumber);
}

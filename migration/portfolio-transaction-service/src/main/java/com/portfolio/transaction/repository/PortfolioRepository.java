package com.portfolio.transaction.repository;

import com.portfolio.transaction.domain.entity.Portfolio;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Portfolio p WHERE p.portfolioId = :id")
    Optional<Portfolio> findByIdWithLock(@Param("id") String portfolioId);

    boolean existsByPortfolioId(String portfolioId);
}

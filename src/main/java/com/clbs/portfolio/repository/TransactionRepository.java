package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.PortfolioTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<PortfolioTransaction, Long> {

    List<PortfolioTransaction> findByPortfolioIdOrderByTransactionDateDescTransactionTimeDescIdDesc(String portfolioId);
}

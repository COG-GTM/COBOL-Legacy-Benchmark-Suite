package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.Portfolio;
import com.clbs.portfolio.domain.PortfolioKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PortfolioRepository extends JpaRepository<Portfolio, PortfolioKey> {

    /** Sequential browse in key order, as PORTREAD does with READ NEXT on the KSDS. */
    List<Portfolio> findAllByOrderByKeyPortfolioIdAscKeyAccountNoAsc();

    List<Portfolio> findByKeyPortfolioId(String portfolioId);

    List<Portfolio> findByKeyAccountNo(String accountNo);
}

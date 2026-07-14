package com.cog.gtm.clbs.migration.repository;

import com.cog.gtm.clbs.migration.domain.portfolio.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {
}

package com.portfolio.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the POSHIST table (DB2 DBTBLS.cpy).
 */
@Repository
public interface PositionHistoryRepository extends JpaRepository<PositionHistoryEntity, Long> {

    List<PositionHistoryEntity> findByPortfolioIdOrderByTransDateDesc(String portfolioId);

    List<PositionHistoryEntity> findByAccountNo(String accountNo);

    List<PositionHistoryEntity> findBySecurityId(String securityId);
}

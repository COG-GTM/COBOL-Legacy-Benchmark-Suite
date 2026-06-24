package com.portfolio.repository;

import com.portfolio.model.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA repository for Portfolio entity.
 * Mirrors COBOL VSAM access patterns:
 * <ul>
 *   <li>findById() — keyed READ by PORT-KEY (PORTMSTR 3000-READ-PORTFOLIO)</li>
 *   <li>findAll() — sequential READ NEXT (PORTREAD 2000-PROCESS)</li>
 *   <li>save() — WRITE (PORTMSTR 2000-CREATE-PORTFOLIO) / REWRITE (4000-UPDATE-PORTFOLIO)</li>
 *   <li>deleteById() — DELETE (PORTMSTR 5000-DELETE-PORTFOLIO)</li>
 *   <li>findByStatus() — filter by PORT-STATUS (88-level conditions)</li>
 *   <li>findByClientType() — filter by PORT-CLIENT-TYPE (88-level conditions)</li>
 * </ul>
 */
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByStatus(String status);

    List<Portfolio> findByClientType(String clientType);

    List<Portfolio> findByAccountNo(String accountNo);

    boolean existsByPortId(String portId);
}

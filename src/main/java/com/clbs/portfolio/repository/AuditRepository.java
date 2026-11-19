package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByPortfolioIdOrderByIdAsc(String portfolioId);

    List<AuditRecord> findAllByOrderByIdAsc();
}

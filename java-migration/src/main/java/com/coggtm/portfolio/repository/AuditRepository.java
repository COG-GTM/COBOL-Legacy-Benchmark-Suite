package com.coggtm.portfolio.repository;

import com.coggtm.portfolio.domain.AuditRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByPortfolioId(String portfolioId);
}

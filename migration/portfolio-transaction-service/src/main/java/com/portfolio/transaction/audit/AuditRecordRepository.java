package com.portfolio.transaction.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditRecordRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findByPortfolioIdOrderByTimestampDesc(String portfolioId);
}

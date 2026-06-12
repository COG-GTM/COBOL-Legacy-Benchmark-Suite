package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.HistoryKey;
import com.clbs.portfolio.domain.HistoryRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the history file (audit before/after images).
 *
 * <p>Primary key is portfolio+date+time+seq; methods cover audit-trail browse by
 * portfolio and filtering by record type (PT/PS/TR).
 */
@Repository
public interface HistoryRecordRepository extends JpaRepository<HistoryRecord, HistoryKey> {

    List<HistoryRecord> findByKeyPortfolioIdOrderByKeyHistDateAscKeyHistTimeAscKeySeqNoAsc(
            String portfolioId);

    List<HistoryRecord> findByRecordType(String recordType);
}

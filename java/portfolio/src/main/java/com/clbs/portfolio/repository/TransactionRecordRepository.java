package com.clbs.portfolio.repository;

import com.clbs.portfolio.domain.TransactionKey;
import com.clbs.portfolio.domain.TransactionRecord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for the TRANHIST VSAM KSDS (transaction history file).
 *
 * <p>The KSDS primary key is date+time+portfolio+sequence; these methods cover
 * the common browse patterns (all transactions for a portfolio, by date range).
 */
@Repository
public interface TransactionRecordRepository extends JpaRepository<TransactionRecord, TransactionKey> {

    List<TransactionRecord> findByKeyPortfolioIdOrderByKeyTrnDateAscKeyTrnTimeAscKeySequenceNoAsc(
            String portfolioId);

    List<TransactionRecord> findByKeyPortfolioIdAndKeyTrnDateBetweenOrderByKeyTrnDateAscKeyTrnTimeAsc(
            String portfolioId, String fromDate, String toDate);

    List<TransactionRecord> findByStatus(String status);
}

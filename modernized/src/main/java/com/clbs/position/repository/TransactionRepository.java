package com.clbs.position.repository;

import com.clbs.position.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for input transactions &mdash; the modern equivalent of
 * the sequential {@code TRANFILE} read loop
 * ({@code PORTTRAN.cbl 2000-PROCESS-TRANSACTIONS}).
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Pending transactions in COBOL transaction-key order
     * ({@code TRN-DATE, TRN-TIME, TRN-PORTFOLIO-ID, TRN-SEQUENCE-NO}) &mdash;
     * the order the batch reader consumes records in.
     */
    List<Transaction> findByStatusOrderByTrnDateAscTrnTimeAscPortfolioIdAscSequenceNoAsc(String status);

    List<Transaction> findByPortfolioId(String portfolioId);
}

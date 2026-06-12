package com.benchmark.portfolio.common.repository;

import com.benchmark.portfolio.common.entity.PortfolioTransaction;
import com.benchmark.portfolio.common.entity.PortfolioTransactionId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link PortfolioTransaction}, replacing the sequential
 * transaction file TRANFILE (TRNREC.cpy, key TRN-KEY = TRN-DATE + TRN-TIME +
 * TRN-PORTFOLIO-ID + TRN-SEQUENCE-NO) and the DB2 POSHIST history table.
 *
 * <p>Primary-key CRUD inherited from {@link JpaRepository} covers the keyed
 * record handling of PORTTRAN.cbl (transaction ingest writes/updates).
 */
public interface PortfolioTransactionRepository
        extends JpaRepository<PortfolioTransaction, PortfolioTransactionId> {

    /**
     * Per-portfolio history in reverse chronological order, replicating
     * INQHIST.cbl P200-GET-HISTORY ({@code SELECT ... FROM POSHIST WHERE
     * ACCOUNT_NO = ? ORDER BY TRANS_DATE DESC}). The {@link Pageable} parameter
     * replicates the CURSMGR.cbl P300-FETCH-DATA array fetch (block of rows per
     * cursor FETCH).
     */
    Page<PortfolioTransaction> findByIdPortfolioIdOrderByIdTransDateDescIdTransTimeDescIdSequenceNoDesc(
            String portfolioId, Pageable pageable);

    /**
     * Full sequential read of the transaction file in key order, replicating
     * PORTTRAN.cbl 2000-PROCESS-TRANSACTIONS ({@code READ TRANSACTION-FILE}
     * loop over the daily transaction batch).
     */
    List<PortfolioTransaction> findAllByOrderByIdTransDateAscIdTransTimeAscIdPortfolioIdAscIdSequenceNoAsc();

    /**
     * Keyed range scan from a starting date (leading field of TRN-KEY), the
     * STARTBR/READNEXT equivalent ({@code START ... KEY >= ...} +
     * {@code READ NEXT} as in PRCSEQ00.cbl 1200-BUILD-SEQUENCE) for replaying
     * transactions from a checkpoint date.
     */
    List<PortfolioTransaction> findByIdTransDateGreaterThanEqualOrderByIdTransDateAscIdTransTimeAscIdPortfolioIdAscIdSequenceNoAsc(
            LocalDate transDate);
}

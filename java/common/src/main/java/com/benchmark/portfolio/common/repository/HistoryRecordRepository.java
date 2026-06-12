package com.benchmark.portfolio.common.repository;

import com.benchmark.portfolio.common.entity.HistoryRecord;
import com.benchmark.portfolio.common.entity.HistoryRecordId;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for {@link HistoryRecord}, replacing the VSAM transaction-history
 * KSDS TRANHIST (HISTREC.cpy, RECORD KEY HIST-KEY = HIST-PORTFOLIO-ID +
 * HIST-DATE + HIST-TIME + HIST-SEQ-NO) loaded to DB2 by HISTLD00.
 *
 * <p>{@code save} replicates the history-load insert of HISTLD00.cbl
 * 2200-LOAD-TO-DB2 ({@code EXEC SQL INSERT INTO POSHIST}).
 */
public interface HistoryRecordRepository
        extends JpaRepository<HistoryRecord, HistoryRecordId> {

    /**
     * Full sequential read in ascending HIST-KEY order, replicating
     * HISTLD00.cbl 2100-READ-HISTORY ({@code READ TRANSACTION-HISTORY}
     * sequential loop over the KSDS in key sequence).
     */
    List<HistoryRecord> findAllByOrderByIdPortfolioIdAscIdHistDateAscIdHistTimeAscIdSeqNoAsc();

    /**
     * Per-portfolio history newest first with scrolling, replicating
     * INQHIST.cbl P200-GET-HISTORY ({@code ORDER BY TRANS_DATE DESC} cursor)
     * with {@link Pageable} standing in for the CURSMGR.cbl P300-FETCH-DATA
     * array fetch used to scroll through history pages.
     */
    Page<HistoryRecord> findByIdPortfolioIdOrderByIdHistDateDescIdHistTimeDescIdSeqNoDesc(
            String portfolioId, Pageable pageable);

    /**
     * Keyed range scan within a portfolio from a starting date, the
     * STARTBR/READNEXT equivalent ({@code START ... KEY >= ...} +
     * {@code READ NEXT RECORD} as in PRCSEQ00.cbl 1200-BUILD-SEQUENCE) over the
     * HIST-PORTFOLIO-ID key prefix.
     */
    List<HistoryRecord> findByIdPortfolioIdAndIdHistDateGreaterThanEqualOrderByIdHistDateAscIdHistTimeAscIdSeqNoAsc(
            String portfolioId, LocalDate histDate);
}

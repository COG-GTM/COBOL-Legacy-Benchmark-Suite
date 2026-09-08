package com.clbs.store;

import com.clbs.domain.AuditRecord;
import com.clbs.domain.PortfolioRecord;
import com.clbs.domain.PositionRecord;
import com.clbs.domain.TransactionHistoryRecord;
import com.clbs.domain.TransactionRecord;
import com.clbs.batch.BatchControlRecord;
import com.clbs.batch.ProcessSequenceRecord;
import org.springframework.stereotype.Component;

/**
 * The VSAM dataset inventory from the JCL DD statements: PORTFILE, POSFILE/POSMSTR, TRANHIST,
 * BCHCTL, PRCSEQ and AUDITLOG. One catalog instance replaces the shared mainframe datasets.
 */
@Component
public class DatasetCatalog {

    private final IndexedFile<PortfolioRecord> portfolioFile =
            new IndexedFile<>("PORTFILE", PortfolioRecord::key);
    private final IndexedFile<PositionRecord> positionFile =
            new IndexedFile<>("POSFILE", PositionRecord::key);
    private final IndexedFile<TransactionHistoryRecord> transactionHistory =
            new IndexedFile<>("TRANHIST", TransactionHistoryRecord::key);
    private final IndexedFile<TransactionRecord> transactionFile =
            new IndexedFile<>("TRANFILE", TransactionRecord::key);
    private final IndexedFile<BatchControlRecord> batchControlFile =
            new IndexedFile<>("BCHCTL", BatchControlRecord::key);
    private final IndexedFile<ProcessSequenceRecord> processSequenceFile =
            new IndexedFile<>("PRCSEQ", ProcessSequenceRecord::key);
    private final SequentialFile<AuditRecord> auditFile = new SequentialFile<>("AUDITLOG");

    public IndexedFile<PortfolioRecord> portfolioFile() {
        return portfolioFile;
    }

    public IndexedFile<PositionRecord> positionFile() {
        return positionFile;
    }

    public IndexedFile<TransactionHistoryRecord> transactionHistory() {
        return transactionHistory;
    }

    public IndexedFile<TransactionRecord> transactionFile() {
        return transactionFile;
    }

    public IndexedFile<BatchControlRecord> batchControlFile() {
        return batchControlFile;
    }

    public IndexedFile<ProcessSequenceRecord> processSequenceFile() {
        return processSequenceFile;
    }

    public SequentialFile<AuditRecord> auditFile() {
        return auditFile;
    }

    public void clear() {
        portfolioFile.clear();
        positionFile.clear();
        transactionHistory.clear();
        transactionFile.clear();
        batchControlFile.clear();
        processSequenceFile.clear();
        auditFile.clear();
    }
}

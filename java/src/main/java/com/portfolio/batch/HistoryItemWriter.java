package com.portfolio.batch;

import com.portfolio.domain.PositionHistory;
import com.portfolio.repository.PositionHistoryRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Insert step of the HISTLD00 migration (COBOL
 * {@code EXEC SQL INSERT INTO POSHIST VALUES (:POSHIST-RECORD)}).
 *
 * <p>Duplicate handling: HISTLD00 treats SQLCODE -803 (duplicate key) as a
 * no-op ({@code IF SQLCODE = -803 CONTINUE}); here a record whose key already
 * exists is skipped without counting as written or as an error. Chunk-based
 * commits by Spring Batch replace the manual WS-COMMIT-THRESHOLD (1000)
 * commit logic in 2300-CHECK-COMMIT.
 *
 * <p>Other insert failures propagate as {@code DataAccessException} and are
 * handled by the step's skip policy, which counts them toward WS-ERROR-COUNT
 * like COBOL's DB2-ERROR-ROUTINE. Each item is flushed individually so a
 * failure is attributed to the correct record, and the written counter is
 * updated only after the chunk transaction commits so rolled-back chunks are
 * not counted.
 */
@Component
public class HistoryItemWriter implements ItemWriter<PositionHistory> {

    private final PositionHistoryRepository repository;
    private final HistoryLoadStats stats;

    public HistoryItemWriter(PositionHistoryRepository repository, HistoryLoadStats stats) {
        this.repository = repository;
        this.stats = stats;
    }

    @Override
    public void write(Chunk<? extends PositionHistory> chunk) {
        long written = 0;
        for (PositionHistory item : chunk) {
            if (repository.existsById(item.getKey())) {
                continue;
            }
            repository.saveAndFlush(item);
            written++;
        }
        final long delta = written;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                stats.addRecordsWritten(delta);
            }
        });
    }
}

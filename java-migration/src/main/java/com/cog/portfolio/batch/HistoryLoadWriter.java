package com.cog.portfolio.batch;

import com.cog.portfolio.domain.PositionHistory;
import com.cog.portfolio.repository.PositionHistoryRepository;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * HISTLD00 INSERT INTO POSHIST. {@code SQLCODE = -803} (duplicate key) is
 * ignored and processing continues; every other failure propagates so Spring
 * Batch rolls the chunk back (the legacy 9000-ERROR-ROUTINE ROLLBACK WORK).
 */
@Component
public class HistoryLoadWriter implements ItemWriter<PositionHistory> {

    private static final Logger log = LoggerFactory.getLogger(HistoryLoadWriter.class);

    private final PositionHistoryRepository repository;
    private final AtomicLong recordsWritten = new AtomicLong();
    private final AtomicLong duplicatesSkipped = new AtomicLong();

    public HistoryLoadWriter(PositionHistoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends PositionHistory> chunk) {
        for (PositionHistory ph : chunk) {
            if (repository.existsById(ph.getId())) {
                duplicatesSkipped.incrementAndGet();
                log.debug("Duplicate POSHIST key {} ignored (SQLCODE -803 rule)", ph.getId());
                continue;
            }
            repository.save(ph);
            recordsWritten.incrementAndGet();
        }
    }

    public long getRecordsWritten() {
        return recordsWritten.get();
    }

    public long getDuplicatesSkipped() {
        return duplicatesSkipped.get();
    }
}

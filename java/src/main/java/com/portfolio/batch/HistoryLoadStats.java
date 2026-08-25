package com.portfolio.batch;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Migration of HISTLD00's WS-COUNTERS working storage: records read, records
 * written, and error count. The error count becomes the process exit code
 * (COBOL {@code MOVE WS-ERROR-COUNT TO RETURN-CODE}).
 */
@Component
public class HistoryLoadStats {

    /** WS-ERROR-COUNT limit: HISTLD00 stops processing when the count exceeds 100. */
    public static final int MAX_ERRORS = 100;

    private final AtomicLong recordsRead = new AtomicLong();
    private final AtomicLong recordsWritten = new AtomicLong();
    private final AtomicLong errorCount = new AtomicLong();

    public void reset() {
        recordsRead.set(0);
        recordsWritten.set(0);
        errorCount.set(0);
    }

    public long incrementRecordsRead() {
        return recordsRead.incrementAndGet();
    }

    public long addRecordsWritten(long delta) {
        return recordsWritten.addAndGet(delta);
    }

    public long incrementErrorCount() {
        return errorCount.incrementAndGet();
    }

    public long getRecordsRead() {
        return recordsRead.get();
    }

    public long getRecordsWritten() {
        return recordsWritten.get();
    }

    public long getErrorCount() {
        return errorCount.get();
    }

    /** COBOL: UNTIL ... WS-ERROR-COUNT > 100. */
    public boolean errorLimitExceeded() {
        return errorCount.get() > MAX_ERRORS;
    }
}

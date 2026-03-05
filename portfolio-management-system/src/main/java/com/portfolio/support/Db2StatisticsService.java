package com.portfolio.support;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * DB2 Statistics Service.
 * Migrated from COBOL DB2STAT program.
 * Uses Micrometer MeterRegistry for performance counters.
 */
@Service
public class Db2StatisticsService {

    private final MeterRegistry meterRegistry;

    private final Counter queryCounter;
    private final Counter insertCounter;
    private final Counter updateCounter;
    private final Counter errorCounter;
    private final Timer queryTimer;

    public Db2StatisticsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.queryCounter = Counter.builder("db2.operations.query")
                .description("Number of DB2 query operations")
                .register(meterRegistry);

        this.insertCounter = Counter.builder("db2.operations.insert")
                .description("Number of DB2 insert operations")
                .register(meterRegistry);

        this.updateCounter = Counter.builder("db2.operations.update")
                .description("Number of DB2 update operations")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("db2.operations.error")
                .description("Number of DB2 error operations")
                .register(meterRegistry);

        this.queryTimer = Timer.builder("db2.operations.duration")
                .description("Duration of DB2 operations")
                .register(meterRegistry);
    }

    public void recordQuery() {
        queryCounter.increment();
    }

    public void recordInsert() {
        insertCounter.increment();
    }

    public void recordUpdate() {
        updateCounter.increment();
    }

    public void recordError() {
        errorCounter.increment();
    }

    public void recordDuration(long durationMs) {
        queryTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    public double getQueryCount() {
        return queryCounter.count();
    }

    public double getInsertCount() {
        return insertCounter.count();
    }

    public double getUpdateCount() {
        return updateCounter.count();
    }

    public double getErrorCount() {
        return errorCounter.count();
    }
}

package com.clbs.portfolio.service.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MonitoringServiceTest {

    private MonitoringService monitoringService;
    private AtomicInteger activeJobsGauge;
    private Counter recordsProcessedCounter;
    private Counter batchErrorsCounter;
    private Timer dbQueryTimer;

    @BeforeEach
    void setUp() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        activeJobsGauge = new AtomicInteger(0);
        recordsProcessedCounter = Counter.builder("batch.records.processed").register(registry);
        batchErrorsCounter = Counter.builder("batch.errors.total").register(registry);
        dbQueryTimer = Timer.builder("db.queries.duration").register(registry);

        monitoringService = new MonitoringService(registry, activeJobsGauge,
                recordsProcessedCounter, batchErrorsCounter, dbQueryTimer);
    }

    @Test
    void incrementAndDecrementActiveJobs() {
        monitoringService.incrementActiveJobs();
        assertThat(activeJobsGauge.get()).isEqualTo(1);

        monitoringService.incrementActiveJobs();
        assertThat(activeJobsGauge.get()).isEqualTo(2);

        monitoringService.decrementActiveJobs();
        assertThat(activeJobsGauge.get()).isEqualTo(1);
    }

    @Test
    void recordProcessed() {
        monitoringService.recordProcessed(100);
        assertThat(recordsProcessedCounter.count()).isEqualTo(100.0);
    }

    @Test
    void recordError() {
        monitoringService.recordError();
        monitoringService.recordError();
        assertThat(batchErrorsCounter.count()).isEqualTo(2.0);
    }

    @Test
    void getCurrentMetrics_returnsAllKeys() {
        Map<String, Object> metrics = monitoringService.getCurrentMetrics();

        assertThat(metrics).containsKeys(
                "activeJobs", "recordsProcessed", "totalErrors",
                "jvmTotalMemory", "jvmFreeMemory", "jvmUsedMemory",
                "jvmMaxMemory", "availableProcessors");
    }

    @Test
    void checkThresholds_returnsAlerts() {
        List<MonitoringService.Alert> alerts = monitoringService.checkThresholds();

        assertThat(alerts).isNotEmpty();
        // Alerts may be INFO (all clear) or WARNING/CRITICAL (memory usage during tests)
        assertThat(alerts.get(0).level()).isIn("INFO", "WARNING", "CRITICAL");
    }

    @Test
    void checkThresholds_highErrorRate() {
        recordsProcessedCounter.increment(100);
        for (int i = 0; i < 20; i++) {
            batchErrorsCounter.increment();
        }

        List<MonitoringService.Alert> alerts = monitoringService.checkThresholds();

        boolean hasErrorAlert = alerts.stream()
                .anyMatch(a -> "ERROR_RATE".equals(a.resource()));
        assertThat(hasErrorAlert).isTrue();
    }
}

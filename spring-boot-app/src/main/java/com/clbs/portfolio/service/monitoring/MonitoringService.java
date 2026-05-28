package com.clbs.portfolio.service.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeJobsGauge;
    private final Counter recordsProcessedCounter;
    private final Counter batchErrorsCounter;
    private final Timer dbQueryTimer;

    @Value("${monitoring.thresholds.batch-job-duration-warning-ms:300000}")
    private long batchJobDurationWarningMs;

    @Value("${monitoring.thresholds.batch-job-duration-critical-ms:600000}")
    private long batchJobDurationCriticalMs;

    @Value("${monitoring.thresholds.error-rate-warning:5.0}")
    private double errorRateWarning;

    @Value("${monitoring.thresholds.error-rate-critical:10.0}")
    private double errorRateCritical;

    @Value("${monitoring.thresholds.db-query-duration-warning-ms:1000}")
    private long dbQueryDurationWarningMs;

    @Value("${monitoring.thresholds.db-query-duration-critical-ms:5000}")
    private long dbQueryDurationCriticalMs;

    public MonitoringService(MeterRegistry meterRegistry,
                              AtomicInteger activeJobsGauge,
                              Counter recordsProcessedCounter,
                              Counter batchErrorsCounter,
                              Timer dbQueryTimer) {
        this.meterRegistry = meterRegistry;
        this.activeJobsGauge = activeJobsGauge;
        this.recordsProcessedCounter = recordsProcessedCounter;
        this.batchErrorsCounter = batchErrorsCounter;
        this.dbQueryTimer = dbQueryTimer;
    }

    public void incrementActiveJobs() {
        activeJobsGauge.incrementAndGet();
    }

    public void decrementActiveJobs() {
        activeJobsGauge.decrementAndGet();
    }

    public void recordProcessed(long count) {
        recordsProcessedCounter.increment(count);
    }

    public void recordError() {
        batchErrorsCounter.increment();
    }

    public Map<String, Object> getCurrentMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();

        metrics.put("activeJobs", activeJobsGauge.get());
        metrics.put("recordsProcessed", recordsProcessedCounter.count());
        metrics.put("totalErrors", batchErrorsCounter.count());
        metrics.put("dbQueryMeanMs", dbQueryTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
        metrics.put("dbQueryMaxMs", dbQueryTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS));
        metrics.put("dbQueryCount", dbQueryTimer.count());

        // JVM metrics
        Runtime runtime = Runtime.getRuntime();
        metrics.put("jvmTotalMemory", runtime.totalMemory());
        metrics.put("jvmFreeMemory", runtime.freeMemory());
        metrics.put("jvmUsedMemory", runtime.totalMemory() - runtime.freeMemory());
        metrics.put("jvmMaxMemory", runtime.maxMemory());
        metrics.put("availableProcessors", runtime.availableProcessors());

        return metrics;
    }

    public List<Alert> checkThresholds() {
        List<Alert> alerts = new ArrayList<>();

        // Check error rate
        double totalErrors = batchErrorsCounter.count();
        double totalProcessed = recordsProcessedCounter.count();
        if (totalProcessed > 0) {
            double errorRate = (totalErrors / totalProcessed) * 100;
            if (errorRate >= errorRateCritical) {
                Alert alert = new Alert("CRITICAL", "ERROR_RATE",
                        String.format("Error rate %.2f%% exceeds critical threshold %.2f%%",
                                errorRate, errorRateCritical));
                alerts.add(alert);
                log.error(alert.message());
            } else if (errorRate >= errorRateWarning) {
                Alert alert = new Alert("WARNING", "ERROR_RATE",
                        String.format("Error rate %.2f%% exceeds warning threshold %.2f%%",
                                errorRate, errorRateWarning));
                alerts.add(alert);
                log.warn(alert.message());
            }
        }

        // Check DB query duration
        double meanQueryMs = dbQueryTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS);
        if (meanQueryMs >= dbQueryDurationCriticalMs) {
            Alert alert = new Alert("CRITICAL", "DB_QUERY",
                    String.format("Mean DB query duration %.2f ms exceeds critical threshold %d ms",
                            meanQueryMs, dbQueryDurationCriticalMs));
            alerts.add(alert);
            log.error(alert.message());
        } else if (meanQueryMs >= dbQueryDurationWarningMs) {
            Alert alert = new Alert("WARNING", "DB_QUERY",
                    String.format("Mean DB query duration %.2f ms exceeds warning threshold %d ms",
                            meanQueryMs, dbQueryDurationWarningMs));
            alerts.add(alert);
            log.warn(alert.message());
        }

        // Check memory utilization
        Runtime runtime = Runtime.getRuntime();
        double memoryUtilization = ((double) (runtime.totalMemory() - runtime.freeMemory())
                / runtime.maxMemory()) * 100;
        if (memoryUtilization >= 90) {
            Alert alert = new Alert("CRITICAL", "MEMORY",
                    String.format("Memory utilization %.2f%% exceeds 90%%", memoryUtilization));
            alerts.add(alert);
            log.error(alert.message());
        } else if (memoryUtilization >= 75) {
            Alert alert = new Alert("WARNING", "MEMORY",
                    String.format("Memory utilization %.2f%% exceeds 75%%", memoryUtilization));
            alerts.add(alert);
            log.warn(alert.message());
        }

        if (alerts.isEmpty()) {
            alerts.add(new Alert("INFO", "SYSTEM", "All metrics within normal thresholds"));
        }

        return alerts;
    }

    public record Alert(String level, String resource, String message) {}
}

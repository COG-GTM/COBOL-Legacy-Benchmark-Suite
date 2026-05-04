package com.portfolio.service.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * System Monitor Service - migrated from COBOL UTLMON00.cbl.
 * CPU/memory/DASD monitoring -> JVM metrics via Micrometer.
 * Threshold alerts -> Actuator health indicators.
 */
@Service
public class SystemMonitorService {

    private static final Logger log = LoggerFactory.getLogger(SystemMonitorService.class);

    private static final double CPU_THRESHOLD = 0.90;
    private static final double MEMORY_THRESHOLD = 0.85;

    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsage = (double) usedMemory / maxMemory;

        metrics.put("maxMemoryMB", maxMemory / (1024 * 1024));
        metrics.put("usedMemoryMB", usedMemory / (1024 * 1024));
        metrics.put("memoryUsagePercent", String.format("%.1f%%", memoryUsage * 100));
        metrics.put("availableProcessors", osBean.getAvailableProcessors());
        metrics.put("systemLoadAverage", osBean.getSystemLoadAverage());
        metrics.put("heapMemoryUsage", memoryBean.getHeapMemoryUsage().toString());
        metrics.put("uptimeSeconds", ManagementFactory.getRuntimeMXBean().getUptime() / 1000);

        return metrics;
    }

    public void checkThresholds() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        double memoryUsage = (double) usedMemory / maxMemory;

        if (memoryUsage > MEMORY_THRESHOLD) {
            log.warn("ALERT: Memory usage at {:.1f}% exceeds threshold of {:.1f}%",
                    memoryUsage * 100, MEMORY_THRESHOLD * 100);
        }

        double cpuLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        int processors = Runtime.getRuntime().availableProcessors();
        if (cpuLoad / processors > CPU_THRESHOLD) {
            log.warn("ALERT: CPU load at {:.1f} exceeds threshold", cpuLoad);
        }
    }
}

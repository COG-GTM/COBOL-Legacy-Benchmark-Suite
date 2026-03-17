package com.portfolio.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitoring Utility.
 * Replaces: UTLMON00.cbl utility helper functions.
 * Provides system metrics and resource monitoring utilities.
 */
public final class MonitoringUtil {

    private MonitoringUtil() {
        // Utility class - no instantiation
    }

    /**
     * Gets JVM memory metrics.
     * Replaces the resource utilization tracking in UTLMON00.cbl.
     */
    public static Map<String, Long> getMemoryMetrics() {
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        Map<String, Long> metrics = new HashMap<>();

        metrics.put("heapUsed", memBean.getHeapMemoryUsage().getUsed());
        metrics.put("heapMax", memBean.getHeapMemoryUsage().getMax());
        metrics.put("heapCommitted", memBean.getHeapMemoryUsage().getCommitted());
        metrics.put("nonHeapUsed", memBean.getNonHeapMemoryUsage().getUsed());

        return metrics;
    }

    /**
     * Gets JVM uptime in milliseconds.
     */
    public static long getUptimeMillis() {
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
        return runtimeBean.getUptime();
    }

    /**
     * Formats bytes to a human-readable string.
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Formats milliseconds to a human-readable uptime string.
     */
    public static String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        return String.format("%dd %dh %dm %ds",
                days, hours % 24, minutes % 60, seconds % 60);
    }
}

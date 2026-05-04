package com.portfolio.service;

import org.springframework.stereotype.Service;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemMonitorService {

    private final DatabaseStatisticsService statsService;

    public SystemMonitorService(DatabaseStatisticsService statsService) {
        this.statsService = statsService;
    }

    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new LinkedHashMap<>();

        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        status.put("uptime", runtime.getUptime() / 1000 + " seconds");
        status.put("jvmName", runtime.getVmName());
        status.put("jvmVersion", runtime.getVmVersion());

        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        long heapUsed = memory.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = memory.getHeapMemoryUsage().getMax() / (1024 * 1024);
        status.put("heapUsedMB", heapUsed);
        status.put("heapMaxMB", heapMax);
        status.put("heapUsagePercent", heapMax > 0 ? (heapUsed * 100) / heapMax : 0);

        status.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        status.put("tableCounts", statsService.getTableCounts());
        status.put("transactionStats", statsService.getTransactionStats());

        return status;
    }
}

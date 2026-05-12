package com.portfolio.service.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class SystemMonitorService {

    private static final Logger log = LoggerFactory.getLogger(SystemMonitorService.class);

    private final HealthEndpoint healthEndpoint;

    public SystemMonitorService(HealthEndpoint healthEndpoint) {
        this.healthEndpoint = healthEndpoint;
    }

    @Scheduled(fixedRate = 60000)
    public void monitorResources() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        double memoryUsagePercent = ((double) usedMemory / maxMemory) * 100;

        if (memoryUsagePercent > 90) {
            log.warn("ALERT: Memory usage at {}%", String.format("%.1f", memoryUsagePercent));
        } else if (memoryUsagePercent > 75) {
            log.info("Memory usage at {}%", String.format("%.1f", memoryUsagePercent));
        }
    }

    public String getSystemStatus() {
        Status status = healthEndpoint.health().getStatus();
        return status.getCode();
    }
}

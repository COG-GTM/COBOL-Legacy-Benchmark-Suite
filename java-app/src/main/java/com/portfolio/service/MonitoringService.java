package com.portfolio.service;

import com.portfolio.repository.BatchControlRepository;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Monitoring Service.
 * Replaces: UTLMON00.cbl - Health checks and performance metrics.
 * Integrates with Spring Boot Actuator for additional monitoring.
 */
@Service
public class MonitoringService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringService.class);

    private final PortfolioRepository portfolioRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final BatchControlRepository batchControlRepository;

    public MonitoringService(PortfolioRepository portfolioRepository,
                             TransactionHistoryRepository transactionHistoryRepository,
                             BatchControlRepository batchControlRepository) {
        this.portfolioRepository = portfolioRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.batchControlRepository = batchControlRepository;
    }

    /**
     * Gets system health metrics.
     * Replaces the monitoring checks in UTLMON00.cbl.
     */
    public Map<String, Object> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("totalPortfolios", portfolioRepository.count());
        metrics.put("activePortfolios", portfolioRepository.findByStatus("A").size());
        metrics.put("totalTransactions", transactionHistoryRepository.count());
        metrics.put("pendingBatchJobs", batchControlRepository.findByStatus("R").size());
        metrics.put("activeBatchJobs", batchControlRepository.findByStatus("A").size());
        metrics.put("errorBatchJobs", batchControlRepository.findByStatus("E").size());
        metrics.put("timestamp", LocalDate.now().toString());

        log.debug("System metrics retrieved: {}", metrics);
        return metrics;
    }

    /**
     * Checks overall system health.
     */
    public HealthStatus checkHealth() {
        try {
            portfolioRepository.count();
            long errorJobs = batchControlRepository.findByStatus("E").size();

            if (errorJobs > 0) {
                return new HealthStatus("DEGRADED",
                        errorJobs + " batch jobs in error state");
            }
            return new HealthStatus("HEALTHY", "All systems operational");
        } catch (Exception e) {
            log.error("Health check failed: {}", e.getMessage());
            return new HealthStatus("UNHEALTHY", "Database connectivity issue: " + e.getMessage());
        }
    }

    public record HealthStatus(String status, String message) {
    }
}

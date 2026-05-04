package com.portfolio.service;

import com.portfolio.repository.*;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DatabaseStatisticsService {

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final ErrorLogRepository errorLogRepository;
    private final BatchControlRepository batchControlRepository;

    public DatabaseStatisticsService(PortfolioRepository portfolioRepository,
                                     PositionRepository positionRepository,
                                     TransactionRepository transactionRepository,
                                     AuditLogRepository auditLogRepository,
                                     ErrorLogRepository errorLogRepository,
                                     BatchControlRepository batchControlRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
        this.errorLogRepository = errorLogRepository;
        this.batchControlRepository = batchControlRepository;
    }

    public Map<String, Long> getTableCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("Portfolios", portfolioRepository.count());
        counts.put("Positions", positionRepository.count());
        counts.put("Transactions", transactionRepository.count());
        counts.put("Audit Logs", auditLogRepository.count());
        counts.put("Error Logs", errorLogRepository.count());
        counts.put("Batch Controls", batchControlRepository.count());
        return counts;
    }

    public Map<String, Long> getTransactionStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("Pending", transactionRepository.countByStatus("P"));
        stats.put("Done", transactionRepository.countByStatus("D"));
        stats.put("Failed", transactionRepository.countByStatus("F"));
        stats.put("Reversed", transactionRepository.countByStatus("R"));
        return stats;
    }

    public Map<String, Long> getErrorStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("Info", errorLogRepository.countByErrorSeverity(1));
        stats.put("Warning", errorLogRepository.countByErrorSeverity(2));
        stats.put("Error", errorLogRepository.countByErrorSeverity(3));
        stats.put("Severe", errorLogRepository.countByErrorSeverity(4));
        return stats;
    }
}

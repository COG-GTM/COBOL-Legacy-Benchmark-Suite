package com.portfolio.service;

import com.portfolio.repository.ErrorLogRepository;
import com.portfolio.repository.PortfolioMasterRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MonitoringService {

    private final PortfolioMasterRepository portfolioRepository;
    private final TransactionHistoryRepository transactionRepository;
    private final ErrorLogRepository errorLogRepository;

    public MonitoringService(PortfolioMasterRepository portfolioRepository,
                             TransactionHistoryRepository transactionRepository,
                             ErrorLogRepository errorLogRepository) {
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.errorLogRepository = errorLogRepository;
    }

    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("timestamp", LocalDateTime.now());
        status.put("status", "UP");

        Map<String, Object> database = new HashMap<>();
        database.put("portfolioCount", portfolioRepository.count());
        database.put("transactionCount", transactionRepository.count());
        database.put("errorLogCount", errorLogRepository.count());
        status.put("database", database);

        Map<String, Object> todaysActivity = new HashMap<>();
        LocalDate today = LocalDate.now();
        todaysActivity.put("transactions", transactionRepository
                .findByTransactionDateBetween(today, today).size());
        todaysActivity.put("errors", errorLogRepository
                .findByProcessDateBetween(today, today).size());
        status.put("todaysActivity", todaysActivity);

        return status;
    }
}

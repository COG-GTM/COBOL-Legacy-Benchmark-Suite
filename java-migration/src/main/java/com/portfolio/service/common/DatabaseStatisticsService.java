package com.portfolio.service.common;

import com.portfolio.repository.ErrorLogRepository;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PosHistRepository;
import com.portfolio.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Database Statistics Service - migrated from COBOL DB2STAT.cbl.
 */
@Service
public class DatabaseStatisticsService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStatisticsService.class);

    private final PortfolioRepository portfolioRepository;
    private final TransactionRepository transactionRepository;
    private final PosHistRepository posHistRepository;
    private final ErrorLogRepository errorLogRepository;

    public DatabaseStatisticsService(PortfolioRepository portfolioRepository,
                                     TransactionRepository transactionRepository,
                                     PosHistRepository posHistRepository,
                                     ErrorLogRepository errorLogRepository) {
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.posHistRepository = posHistRepository;
        this.errorLogRepository = errorLogRepository;
    }

    public Map<String, Long> getTableCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("PORTFOLIO_MASTER", portfolioRepository.count());
        counts.put("TRANSACTION_HISTORY", transactionRepository.count());
        counts.put("POSHIST", posHistRepository.count());
        counts.put("ERRLOG", errorLogRepository.count());
        return counts;
    }

    public void displayStatistics() {
        Map<String, Long> counts = getTableCounts();
        log.info("=== DATABASE STATISTICS ===");
        counts.forEach((table, count) ->
                log.info("Table {}: {} records", table, count));
        log.info("===========================");
    }
}

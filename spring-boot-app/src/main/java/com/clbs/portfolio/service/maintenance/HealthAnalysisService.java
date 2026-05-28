package com.clbs.portfolio.service.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.List;

@Service
public class HealthAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(HealthAnalysisService.class);

    private static final List<String> TABLES = List.of(
            "portfolio", "position", "transaction_record",
            "audit_record", "error_log", "batch_control_record",
            "position_history", "checkpoint_control"
    );

    @PersistenceContext
    private EntityManager entityManager;

    public MaintenanceResult analyze() {
        MaintenanceResult result = new MaintenanceResult("ANALYZE");

        for (String table : TABLES) {
            try {
                Query countQuery = entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM " + table);
                Object countResult = countQuery.getSingleResult();
                long rowCount = ((Number) countResult).longValue();

                result.incrementRecordsAffected();
                result.addDetail(String.format("Table %-30s: %,10d rows", table, rowCount));
            } catch (Exception e) {
                log.warn("Failed to analyze table {}: {}", table, e.getMessage());
                result.incrementErrors();
                result.addDetail("Failed to analyze table " + table + ": " + e.getMessage());
            }
        }

        result.setRecordsProcessed(TABLES.size());
        log.info("Health analysis complete: {} tables analyzed", TABLES.size());
        return result;
    }
}

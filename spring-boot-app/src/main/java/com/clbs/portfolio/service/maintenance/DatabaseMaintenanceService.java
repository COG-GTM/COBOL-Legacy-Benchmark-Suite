package com.clbs.portfolio.service.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DatabaseMaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMaintenanceService.class);

    private static final List<String> TABLES = List.of(
            "portfolio", "position", "transaction_record",
            "audit_record", "error_log", "batch_control_record",
            "position_history", "checkpoint_control"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public MaintenanceResult reorg() {
        MaintenanceResult result = new MaintenanceResult("REORG");

        for (String table : TABLES) {
            try {
                // H2 uses ANALYZE for table statistics; REINDEX is not directly supported
                // but we can use ANALYZE TABLE for H2
                entityManager.createNativeQuery("ANALYZE TABLE " + table).executeUpdate();
                result.incrementRecordsAffected();
                result.addDetail("Analyzed table: " + table);
            } catch (Exception e) {
                log.warn("Failed to analyze table {}: {}", table, e.getMessage());
                result.incrementErrors();
                result.addDetail("Failed to analyze table " + table + ": " + e.getMessage());
            }
        }

        result.setRecordsProcessed(TABLES.size());
        log.info("Database reorg complete: {} tables processed", TABLES.size());
        return result;
    }
}

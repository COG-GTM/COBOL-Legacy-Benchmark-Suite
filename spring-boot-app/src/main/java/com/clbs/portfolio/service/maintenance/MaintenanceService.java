package com.clbs.portfolio.service.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaintenanceService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceService.class);

    private final ArchiveService archiveService;
    private final CleanupService cleanupService;
    private final DatabaseMaintenanceService databaseMaintenanceService;
    private final HealthAnalysisService healthAnalysisService;

    public MaintenanceService(ArchiveService archiveService,
                               CleanupService cleanupService,
                               DatabaseMaintenanceService databaseMaintenanceService,
                               HealthAnalysisService healthAnalysisService) {
        this.archiveService = archiveService;
        this.cleanupService = cleanupService;
        this.databaseMaintenanceService = databaseMaintenanceService;
        this.healthAnalysisService = healthAnalysisService;
    }

    public Map<String, MaintenanceResult> executeMaintenance(List<String> functions) {
        Map<String, MaintenanceResult> results = new LinkedHashMap<>();

        for (String function : functions) {
            String upperFunction = function.toUpperCase();
            log.info("Executing maintenance function: {}", upperFunction);

            MaintenanceResult result = switch (upperFunction) {
                case "ARCHIVE" -> archiveService.archive();
                case "CLEANUP" -> cleanupService.cleanup();
                case "REORG" -> databaseMaintenanceService.reorg();
                case "ANALYZE" -> healthAnalysisService.analyze();
                default -> {
                    MaintenanceResult unknown = new MaintenanceResult(upperFunction);
                    unknown.addDetail("Unknown maintenance function: " + upperFunction);
                    yield unknown;
                }
            };

            results.put(upperFunction, result);
            log.info("{} complete: processed={}, affected={}, errors={}",
                    upperFunction, result.getRecordsProcessed(),
                    result.getRecordsAffected(), result.getErrorsEncountered());
        }

        return results;
    }
}

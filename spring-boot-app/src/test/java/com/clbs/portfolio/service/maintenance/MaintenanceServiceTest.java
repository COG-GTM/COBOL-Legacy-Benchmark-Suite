package com.clbs.portfolio.service.maintenance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private ArchiveService archiveService;

    @Mock
    private CleanupService cleanupService;

    @Mock
    private DatabaseMaintenanceService databaseMaintenanceService;

    @Mock
    private HealthAnalysisService healthAnalysisService;

    @InjectMocks
    private MaintenanceService maintenanceService;

    @Test
    void executeMaintenance_archive() {
        MaintenanceResult archiveResult = new MaintenanceResult("ARCHIVE");
        archiveResult.setRecordsAffected(5);
        when(archiveService.archive()).thenReturn(archiveResult);

        Map<String, MaintenanceResult> results = maintenanceService.executeMaintenance(
                List.of("ARCHIVE"));

        assertThat(results).containsKey("ARCHIVE");
        assertThat(results.get("ARCHIVE").getRecordsAffected()).isEqualTo(5);
        verify(archiveService).archive();
    }

    @Test
    void executeMaintenance_cleanup() {
        MaintenanceResult cleanupResult = new MaintenanceResult("CLEANUP");
        cleanupResult.setRecordsAffected(10);
        when(cleanupService.cleanup()).thenReturn(cleanupResult);

        Map<String, MaintenanceResult> results = maintenanceService.executeMaintenance(
                List.of("CLEANUP"));

        assertThat(results).containsKey("CLEANUP");
        verify(cleanupService).cleanup();
    }

    @Test
    void executeMaintenance_reorg() {
        MaintenanceResult reorgResult = new MaintenanceResult("REORG");
        when(databaseMaintenanceService.reorg()).thenReturn(reorgResult);

        Map<String, MaintenanceResult> results = maintenanceService.executeMaintenance(
                List.of("REORG"));

        assertThat(results).containsKey("REORG");
        verify(databaseMaintenanceService).reorg();
    }

    @Test
    void executeMaintenance_analyze() {
        MaintenanceResult analyzeResult = new MaintenanceResult("ANALYZE");
        when(healthAnalysisService.analyze()).thenReturn(analyzeResult);

        Map<String, MaintenanceResult> results = maintenanceService.executeMaintenance(
                List.of("ANALYZE"));

        assertThat(results).containsKey("ANALYZE");
        verify(healthAnalysisService).analyze();
    }

    @Test
    void executeMaintenance_allFunctions() {
        when(archiveService.archive()).thenReturn(new MaintenanceResult("ARCHIVE"));
        when(cleanupService.cleanup()).thenReturn(new MaintenanceResult("CLEANUP"));
        when(databaseMaintenanceService.reorg()).thenReturn(new MaintenanceResult("REORG"));
        when(healthAnalysisService.analyze()).thenReturn(new MaintenanceResult("ANALYZE"));

        Map<String, MaintenanceResult> results = maintenanceService.executeMaintenance(
                List.of("ARCHIVE", "CLEANUP", "REORG", "ANALYZE"));

        assertThat(results).hasSize(4);
        assertThat(results).containsKeys("ARCHIVE", "CLEANUP", "REORG", "ANALYZE");
    }

    @Test
    void executeMaintenance_unknownFunction() {
        Map<String, MaintenanceResult> results = maintenanceService.executeMaintenance(
                List.of("INVALID"));

        assertThat(results).containsKey("INVALID");
        assertThat(results.get("INVALID").getDetails()).isNotEmpty();
    }
}

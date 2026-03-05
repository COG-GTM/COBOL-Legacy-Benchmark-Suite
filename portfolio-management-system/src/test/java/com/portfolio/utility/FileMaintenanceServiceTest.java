package com.portfolio.utility;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for FileMaintenanceService.
 * Validates maintenance operations replacing COBOL UTLMNT00.
 */
@SpringBootTest
@ActiveProfiles("test")
class FileMaintenanceServiceTest {

    @Autowired
    private FileMaintenanceService maintenanceService;

    @Test
    void testPerformMaintenance() {
        Map<String, Object> result = maintenanceService.performMaintenance();

        assertThat(result).containsKey("maintenanceDate");
        assertThat(result).containsKey("status");
        assertThat(result.get("status")).isEqualTo("COMPLETED");
    }
}

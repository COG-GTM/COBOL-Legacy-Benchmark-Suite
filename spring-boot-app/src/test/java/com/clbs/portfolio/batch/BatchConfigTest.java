package com.clbs.portfolio.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class BatchConfigTest {

    @Autowired
    @Qualifier("positionReportJob")
    private Job positionReportJob;

    @Autowired
    @Qualifier("auditReportJob")
    private Job auditReportJob;

    @Autowired
    @Qualifier("systemStatsReportJob")
    private Job systemStatsReportJob;

    @Autowired
    @Qualifier("maintenanceJob")
    private Job maintenanceJob;

    @Test
    void positionReportJob_isConfigured() {
        assertThat(positionReportJob).isNotNull();
        assertThat(positionReportJob.getName()).isEqualTo("positionReportJob");
    }

    @Test
    void auditReportJob_isConfigured() {
        assertThat(auditReportJob).isNotNull();
        assertThat(auditReportJob.getName()).isEqualTo("auditReportJob");
    }

    @Test
    void systemStatsReportJob_isConfigured() {
        assertThat(systemStatsReportJob).isNotNull();
        assertThat(systemStatsReportJob.getName()).isEqualTo("systemStatsReportJob");
    }

    @Test
    void maintenanceJob_isConfigured() {
        assertThat(maintenanceJob).isNotNull();
        assertThat(maintenanceJob.getName()).isEqualTo("maintenanceJob");
    }
}

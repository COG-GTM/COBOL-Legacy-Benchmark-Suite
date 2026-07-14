package com.cognition.clbs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the Spring context boots with the Web, Batch, and JPA layers wired.
 */
@SpringBootTest
class ClbsApplicationTests {

    @Autowired
    private Job sampleJob;

    @Test
    void contextLoads() {
        assertThat(sampleJob).isNotNull();
    }
}

package com.clbs.posval;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Spring Boot entry point for the position valuation and update slice.
 *
 * <p>The COBOL step this replaces is a batch job, not a service, so the application context exists
 * to wire and configure the components; a job runner or an HTTP adapter can be layered on without
 * touching the business logic.
 */
@SpringBootApplication
public class PositionValuationApplication {

    public static void main(String[] args) {
        SpringApplication.run(PositionValuationApplication.class, args);
    }

    /**
     * {@code FUNCTION CURRENT-DATE}, injected so that audit timestamps are deterministic under
     * test.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}

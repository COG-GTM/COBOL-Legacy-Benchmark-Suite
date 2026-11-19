package com.clbs.portfolio;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Fixes "today" at 2024-03-20 10:00 UTC so dates are deterministic. */
@TestConfiguration
public class TestClockConfig {

    public static final Instant NOW = Instant.parse("2024-03-20T10:00:00Z");

    @Bean
    @Primary
    public Clock fixedClock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }
}

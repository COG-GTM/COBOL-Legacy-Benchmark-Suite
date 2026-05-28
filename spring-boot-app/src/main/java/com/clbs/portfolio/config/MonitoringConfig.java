package com.clbs.portfolio.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Configuration
public class MonitoringConfig {

    @Bean
    public AtomicInteger activeJobsGauge(MeterRegistry registry) {
        AtomicInteger activeJobs = new AtomicInteger(0);
        Gauge.builder("batch.jobs.active", activeJobs, AtomicInteger::get)
                .description("Number of currently running batch jobs")
                .register(registry);
        return activeJobs;
    }

    @Bean
    public Counter recordsProcessedCounter(MeterRegistry registry) {
        return Counter.builder("batch.records.processed")
                .description("Total number of processed records")
                .register(registry);
    }

    @Bean
    public Counter batchErrorsCounter(MeterRegistry registry) {
        return Counter.builder("batch.errors.total")
                .description("Total number of batch errors")
                .register(registry);
    }

    @Bean
    public Timer dbQueryTimer(MeterRegistry registry) {
        return Timer.builder("db.queries.duration")
                .description("Database query duration")
                .register(registry);
    }
}

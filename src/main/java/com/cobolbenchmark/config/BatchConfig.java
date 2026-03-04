package com.cobolbenchmark.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.context.annotation.Configuration;

/**
 * Batch Configuration - enables Spring Batch for batch processing jobs.
 * Replaces JCL batch job scheduling and COBOL batch infrastructure.
 */
@Configuration
@EnableBatchProcessing
public class BatchConfig {
    // Spring Batch auto-configuration handles most setup.
    // Job-specific configuration is in each *JobConfig class.
}

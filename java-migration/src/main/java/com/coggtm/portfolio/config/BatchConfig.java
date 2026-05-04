package com.coggtm.portfolio.config;

import org.springframework.context.annotation.Configuration;

/**
 * Spring Batch infrastructure configuration.
 * <p>
 * Replaces the JCL batch orchestration layer ({@code BCHCTL00}) and
 * checkpoint/restart logic from {@code CKPRST.cpy}. Spring Batch
 * handles job repository, step execution, and chunk-oriented
 * processing natively.
 * </p>
 */
@Configuration
public class BatchConfig {
    // TODO: Configure custom JobRepository, TaskExecutor, or
    //       PlatformTransactionManager if defaults are insufficient.
}

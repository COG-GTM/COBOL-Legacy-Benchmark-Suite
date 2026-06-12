package com.benchmark.portfolio.common.repository;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Test-only Spring Boot configuration so {@code @DataJpaTest} slices can
 * bootstrap the common module, which is a library without an application
 * class.
 */
@SpringBootApplication
@EntityScan("com.benchmark.portfolio.common.entity")
@EnableJpaRepositories("com.benchmark.portfolio.common.repository")
public class RepositoryTestApplication {
}

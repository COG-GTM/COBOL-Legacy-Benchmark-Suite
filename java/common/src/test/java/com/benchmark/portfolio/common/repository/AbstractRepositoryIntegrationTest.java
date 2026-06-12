package com.benchmark.portfolio.common.repository;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Base configuration for repository integration tests: runs the real Flyway
 * baseline schema (V1__baseline_schema.sql) against H2 in PostgreSQL mode,
 * matching the pattern used by EntityPersistenceTest, and disables Hibernate
 * DDL so all queries execute against the migrated schema.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:repository_it;MODE=PostgreSQL;"
                + "DATABASE_TO_LOWER=FALSE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true"
})
public abstract class AbstractRepositoryIntegrationTest {
}

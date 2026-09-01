package com.clbs.portfolio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class ContextLoadsTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndFlywayAppliesMigration() {
        Integer migrations = jdbcTemplate.queryForObject(
                "select count(*) from \"flyway_schema_history\" where \"version\" = '1'", Integer.class);
        assertEquals(1, migrations);
    }
}

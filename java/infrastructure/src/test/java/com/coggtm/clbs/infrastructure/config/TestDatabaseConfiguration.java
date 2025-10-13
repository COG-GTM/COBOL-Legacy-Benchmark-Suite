package com.coggtm.clbs.infrastructure.config;

import com.coggtm.clbs.infrastructure.util.TestDataBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@TestConfiguration
@Profile("test")
public class TestDatabaseConfiguration {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("testdb")
                .build();
    }

    @Bean
    public TestDataBuilder testDataBuilder() {
        return new TestDataBuilder();
    }
}

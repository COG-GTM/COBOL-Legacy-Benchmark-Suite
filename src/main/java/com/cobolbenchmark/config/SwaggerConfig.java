package com.cobolbenchmark.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI Configuration - API documentation.
 * Documents REST API endpoints that replace BMS maps (MENMAP, POSMAP, HISMAP, ERRMAP).
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI portfolioManagementOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Portfolio Management System API")
                        .description("REST API migrated from COBOL Legacy Benchmark Suite. " +
                                "Replaces CICS online programs (INQONLN, INQPORT, INQHIST) " +
                                "and BMS maps (MENMAP, POSMAP, HISMAP, ERRMAP).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("COG-GTM")
                                .url("https://github.com/COG-GTM/COBOL-Legacy-Benchmark-Suite")));
    }
}

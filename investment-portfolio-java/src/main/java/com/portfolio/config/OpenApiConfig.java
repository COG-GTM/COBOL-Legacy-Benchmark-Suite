package com.portfolio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Investment Portfolio Management System API")
                        .description("REST API for the Investment Portfolio Management System, "
                                + "modernized from COBOL/CICS/DB2 to Spring Boot 3.x")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Portfolio Team")
                                .email("portfolio@example.com"))
                        .license(new License()
                                .name("MIT")));
    }
}

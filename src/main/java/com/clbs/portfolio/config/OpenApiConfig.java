package com.clbs.portfolio.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI portfolioOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CLBS Portfolio Service API")
                        .version("1.0.0")
                        .description("Java 21 port of the COBOL Legacy Benchmark Suite investment portfolio system. "
                                + "The authoritative contract is api-spec/openapi.yaml."))
                .components(new Components().addSecuritySchemes("basicAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"));
    }
}

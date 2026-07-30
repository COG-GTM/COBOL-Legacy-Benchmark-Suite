package com.cognition.portfolio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3 contract published by the service, so the API can be onboarded to the API gateway
 * (Apigee) the same way the target architecture expects.
 */
@Configuration
public class OpenApiConfiguration {

  @Bean
  public OpenAPI portfolioTransactionOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Portfolio Transaction Service")
                .version("1.0.0")
                .description(
                    """
                    Java 21 / Spring Boot 3 migration of the COBOL portfolio transaction entity.

                    Source of truth: copybook TRNREC.cpy (VSAM KSDS TRANHIST); business rules
                    extracted from PORTTRAN.cbl, PORTVALD.cbl and PRCSEQ00.cbl. Every operation
                    below documents the COBOL paragraph it replaces; see MIGRATION-NOTES.md for the
                    numbered rule list and the field mapping table.
                    """)
                .contact(new Contact().name("Portfolio Modernization"))
                .license(new License().name("Internal")));
  }
}

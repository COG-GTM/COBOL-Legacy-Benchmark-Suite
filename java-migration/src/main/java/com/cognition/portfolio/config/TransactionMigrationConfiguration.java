package com.cognition.portfolio.config;

import com.cognition.portfolio.transaction.validation.FormatOnlyPortfolioReferenceValidator;
import com.cognition.portfolio.transaction.validation.PortfolioFieldValidator;
import com.cognition.portfolio.transaction.validation.PortfolioReferenceValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Wiring for the collaborators that stand in for not-yet-migrated COBOL files. */
@Configuration
public class TransactionMigrationConfiguration {

  /**
   * Default portfolio existence check used until the portfolio master ({@code PORTMSTR}) is
   * migrated. A deployment that owns the portfolio table supplies its own bean.
   */
  @Bean
  @ConditionalOnMissingBean(PortfolioReferenceValidator.class)
  public PortfolioReferenceValidator portfolioReferenceValidator(PortfolioFieldValidator fieldValidator) {
    return new FormatOnlyPortfolioReferenceValidator(fieldValidator);
  }
}

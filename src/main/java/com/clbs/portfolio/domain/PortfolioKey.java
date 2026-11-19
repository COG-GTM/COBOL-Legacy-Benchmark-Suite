package com.clbs.portfolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;

/** PORT-KEY from PORTFLIO.cpy: PORT-ID X(8) + PORT-ACCOUNT-NO X(10). */
@Embeddable
public record PortfolioKey(
        @Column(name = "PORTFOLIO_ID", length = 8, nullable = false) String portfolioId,
        @Column(name = "ACCOUNT_NO", length = 10, nullable = false) String accountNo) implements Serializable {
}

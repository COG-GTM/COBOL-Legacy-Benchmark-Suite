package com.cognition.clbs.common.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Composite primary key of {@link Portfolio}. */
@Embeddable
public record PortfolioKey(
    @Column(name = "PORTFOLIO_ID", nullable = false, length = 8) String portfolioId,
    @Column(name = "ACCOUNT_NO", nullable = false, length = 10) String accountNumber) {}

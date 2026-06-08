package com.portfolio.domain.event;

import java.time.LocalDateTime;

/**
 * Domain event emitted when a new portfolio is created.
 */
public record PortfolioCreatedEvent(
        String portfolioId,
        String accountNumber,
        String userId,
        LocalDateTime timestamp
) {}

package com.portfolio.service;

import com.portfolio.exception.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class PortfolioValidator {

    private static final String PORTFOLIO_ID_PATTERN = "PORT\\d{4}";

    public void validatePortfolioId(String portfolioId) {
        if (portfolioId == null || !portfolioId.matches(PORTFOLIO_ID_PATTERN)) {
            throw new ValidationException(
                    "Portfolio ID must start with 'PORT' followed by 4 numeric digits");
        }
    }

    public void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Portfolio name must not be blank");
        }
    }

    public void validateStatus(String status) {
        if (status == null || !status.matches("[ACS]")) {
            throw new ValidationException(
                    "Status must be A (Active), C (Closed), or S (Suspended)");
        }
    }
}

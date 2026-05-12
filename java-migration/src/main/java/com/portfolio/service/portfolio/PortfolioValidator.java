package com.portfolio.service.portfolio;

import com.portfolio.exception.InvalidPortfolioException;
import com.portfolio.model.entity.Portfolio;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PortfolioValidator {

    public static final String VAL_ID_PREFIX = "PORT";
    public static final BigDecimal VAL_MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    public static final BigDecimal VAL_MAX_AMOUNT = new BigDecimal("9999999999999.99");

    public void validate(Portfolio portfolio) {
        validateId(portfolio.getPortfolioId());
        validateName(portfolio.getPortfolioName());
        validateStatus(portfolio.getStatus());
        validateAmounts(portfolio);
    }

    private void validateId(String portfolioId) {
        if (portfolioId == null || portfolioId.length() < 4) {
            throw new InvalidPortfolioException("Invalid Portfolio ID format");
        }
        if (!portfolioId.startsWith(VAL_ID_PREFIX)) {
            throw new InvalidPortfolioException("Portfolio ID must start with 'PORT'");
        }
        String suffix = portfolioId.substring(4);
        if (!suffix.isEmpty() && !suffix.chars().allMatch(Character::isDigit)) {
            throw new InvalidPortfolioException("Invalid Portfolio ID format");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidPortfolioException("Portfolio Name is required");
        }
    }

    private void validateStatus(Character status) {
        if (status == null) {
            return;
        }
        if (status != 'A' && status != 'C' && status != 'S') {
            throw new InvalidPortfolioException("Invalid Portfolio Status: " + status);
        }
    }

    private void validateAmounts(Portfolio portfolio) {
        if (portfolio.getTotalValue() != null) {
            if (portfolio.getTotalValue().compareTo(VAL_MIN_AMOUNT) < 0
                    || portfolio.getTotalValue().compareTo(VAL_MAX_AMOUNT) > 0) {
                throw new InvalidPortfolioException("Amount outside valid range");
            }
        }
    }
}

package com.portfolio.transaction.service.validation;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.ValidationResult;
import com.portfolio.transaction.repository.PortfolioRepository;
import org.springframework.stereotype.Component;

@Component
public class PortfolioValidator implements TransactionValidator {

    private final PortfolioRepository portfolioRepository;

    public PortfolioValidator(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    public ValidationResult validate(TransactionRequest request) {
        if (request.getPortfolioId() == null || request.getPortfolioId().isBlank()) {
            return ValidationResult.failure("Portfolio ID is required");
        }

        if (!portfolioRepository.existsById(request.getPortfolioId())) {
            return ValidationResult.failure("Invalid Portfolio ID: " + request.getPortfolioId());
        }

        return ValidationResult.success();
    }

    @Override
    public int getOrder() {
        return 1;
    }
}

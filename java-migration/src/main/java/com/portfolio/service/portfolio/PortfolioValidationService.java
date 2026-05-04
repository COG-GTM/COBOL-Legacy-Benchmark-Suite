package com.portfolio.service.portfolio;

import com.portfolio.domain.Portfolio;
import com.portfolio.domain.PortfolioValidation;
import com.portfolio.exception.ValidationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Portfolio Validation Service - migrated from COBOL PORTVALD.cbl.
 * Translates COBOL validation rules exactly.
 */
@Service
public class PortfolioValidationService {

    public void validatePortfolio(Portfolio portfolio) {
        validatePortfolioId(portfolio.getPortfolioId());
        validateClientId(portfolio.getClientId());
        validateClientType(portfolio.getClientType());
        validateAmounts(portfolio.getTotalValue(), portfolio.getCashBalance());
        validateStatus(portfolio.getStatus());
    }

    public void validatePortfolioId(String portfolioId) {
        if (portfolioId == null || portfolioId.trim().isEmpty()) {
            throw new ValidationException(PortfolioValidation.ERR_ID, "E001");
        }
        if (portfolioId.length() > 8) {
            throw new ValidationException(PortfolioValidation.ERR_ID, "E001");
        }
    }

    public void validateClientId(String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new ValidationException(PortfolioValidation.ERR_ACCT, "E001");
        }
        if (clientId.length() > 10) {
            throw new ValidationException(PortfolioValidation.ERR_ACCT, "E001");
        }
    }

    public void validateClientType(String clientType) {
        if (clientType == null || clientType.trim().isEmpty()) {
            throw new ValidationException(PortfolioValidation.ERR_TYPE, "E001");
        }
        if (!"I".equals(clientType) && !"C".equals(clientType) && !"T".equals(clientType)) {
            throw new ValidationException(PortfolioValidation.ERR_TYPE, "E001");
        }
    }

    public void validateAmounts(BigDecimal totalValue, BigDecimal cashBalance) {
        if (totalValue != null) {
            if (totalValue.compareTo(PortfolioValidation.MIN_AMOUNT) < 0
                    || totalValue.compareTo(PortfolioValidation.MAX_AMOUNT) > 0) {
                throw new ValidationException(PortfolioValidation.ERR_AMT, "E001");
            }
        }
        if (cashBalance != null) {
            if (cashBalance.compareTo(PortfolioValidation.MIN_AMOUNT) < 0
                    || cashBalance.compareTo(PortfolioValidation.MAX_AMOUNT) > 0) {
                throw new ValidationException(PortfolioValidation.ERR_AMT, "E001");
            }
        }
    }

    public void validateStatus(String status) {
        if (status == null) return;
        if (!"A".equals(status) && !"C".equals(status) && !"S".equals(status)) {
            throw new ValidationException("Invalid portfolio status: " + status, "E001");
        }
    }
}

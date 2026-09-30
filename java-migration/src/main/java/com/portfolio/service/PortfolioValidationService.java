package com.portfolio.service;

import com.portfolio.common.ValidationConstants;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PortfolioValidationService {
  public record ValidationResult(int code, String message) {
    public boolean valid() {
      return code == 0;
    }
  }

  private static final Set<String> TYPES = Set.of("STK", "BND", "MMF", "ETF");

  public ValidationResult validatePortfolioId(String portfolioId) {
    return portfolioId != null && portfolioId.matches("PORT\\d{4}")
        ? new ValidationResult(0, "")
        : new ValidationResult(1, ValidationConstants.INVALID_ID_MESSAGE);
  }

  public ValidationResult validateAccountNo(String accountNumber) {
    return accountNumber != null
            && accountNumber.matches("\\d{10}")
            && !accountNumber.matches("0{10}")
        ? new ValidationResult(0, "")
        : new ValidationResult(2, ValidationConstants.INVALID_ACCOUNT_MESSAGE);
  }

  public ValidationResult validateInvestmentType(String investmentType) {
    return investmentType != null && TYPES.contains(investmentType)
        ? new ValidationResult(0, "")
        : new ValidationResult(3, ValidationConstants.INVALID_TYPE_MESSAGE);
  }

  public ValidationResult validateAmount(BigDecimal amount) {
    return amount != null
            && amount.compareTo(ValidationConstants.MIN_AMOUNT) >= 0
            && amount.compareTo(ValidationConstants.MAX_AMOUNT) <= 0
        ? new ValidationResult(0, "")
        : new ValidationResult(4, ValidationConstants.INVALID_AMOUNT_MESSAGE);
  }

  public void requirePortfolioId(String portfolioId) {
    require(validatePortfolioId(portfolioId));
  }

  public void requireAccountNo(String accountNumber) {
    require(validateAccountNo(accountNumber));
  }

  public void requireInvestmentType(String investmentType) {
    require(validateInvestmentType(investmentType));
  }

  public void requireAmount(BigDecimal amount) {
    require(validateAmount(amount));
  }

  private void require(ValidationResult validationResult) {
    if (!validationResult.valid()) {
      throw new BusinessException("E008", validationResult.message());
    }
  }

  public String investmentTypeOf(String investmentId) {
    return investmentId != null && investmentId.length() >= 3 ? investmentId.substring(0, 3) : "";
  }
}

package com.cognition.portfolio.transaction.validation;

import com.cognition.portfolio.traceability.CobolOrigin;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.domain.TransactionType;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Transaction-level validation, migrated from {@code PORTTRAN 2100-VALIDATE-TRANSACTION} and the
 * three paragraphs it performs. The COBOL only performs the next check while {@code ERR-TEXT =
 * SPACES}, so the Java port also stops at the first failure (BR-07).
 */
@Component
@CobolOrigin(program = "PORTTRAN", paragraph = "2100-VALIDATE-TRANSACTION")
public class TransactionValidator {

  private final PortfolioReferenceValidator portfolioReferenceValidator;

  public TransactionValidator(PortfolioReferenceValidator portfolioReferenceValidator) {
    this.portfolioReferenceValidator = portfolioReferenceValidator;
  }

  /**
   * BR-07 — {@code 2100-VALIDATE-TRANSACTION}: runs 2110, then 2120, then 2130, short-circuiting on
   * the first paragraph that sets {@code ERR-TEXT}.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2100-VALIDATE-TRANSACTION", rules = {"BR-07"})
  public ValidationOutcome validate(PortfolioTransaction transaction) {
    ValidationOutcome portfolio = checkPortfolio(transaction);
    if (!portfolio.isValid()) {
      return portfolio;
    }
    ValidationOutcome type = checkTransactionType(transaction.getTrnType());
    if (!type.isValid()) {
      return type;
    }
    return checkAmounts(transaction);
  }

  /**
   * BR-01/BR-02 — {@code 2110-CHECK-PORTFOLIO}: {@code TRN-PORTFOLIO-ID} is required, and the
   * portfolio record must be readable on {@code PORTFILE}.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2110-CHECK-PORTFOLIO", rules = {"BR-01", "BR-02"})
  public ValidationOutcome checkPortfolio(PortfolioTransaction transaction) {
    String portfolioId = transaction.getPortfolioId();
    if (portfolioId == null || portfolioId.isBlank()) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_ID,
          "Portfolio ID is required",
          "BR-01",
          "PORTTRAN 2110-CHECK-PORTFOLIO");
    }
    if (!portfolioReferenceValidator.exists(portfolioId)) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_ID,
          "Invalid Portfolio ID: " + portfolioId,
          "BR-02",
          "PORTTRAN 2110-CHECK-PORTFOLIO");
    }
    return ValidationOutcome.success("PORTTRAN 2110-CHECK-PORTFOLIO");
  }

  /** BR-03 — {@code 2120-CHECK-TRANSACTION-TYPE}: {@code TRN-TYPE} must be BU, SL, TR or FE. */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2120-CHECK-TRANSACTION-TYPE", rules = {"BR-03"})
  public ValidationOutcome checkTransactionType(TransactionType type) {
    if (type == null) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_TYPE,
          "Invalid Transaction Type: ",
          "BR-03",
          "PORTTRAN 2120-CHECK-TRANSACTION-TYPE");
    }
    return ValidationOutcome.success("PORTTRAN 2120-CHECK-TRANSACTION-TYPE");
  }

  /**
   * BR-04/BR-05/BR-06 — {@code 2130-CHECK-AMOUNTS}:
   *
   * <pre>
   * IF TRN-QUANTITY &lt;= ZERO                          -&gt; 'Quantity must be greater than zero'
   * IF TRN-PRICE  &lt;= ZERO AND TRN-TYPE NOT = 'TR'    -&gt; 'Price must be greater than zero'
   * IF TRN-AMOUNT &lt;= ZERO AND TRN-TYPE NOT = 'TR'    -&gt; 'Amount must be greater than zero'
   * </pre>
   *
   * <p>Note the literal reading: the quantity check has no {@code TR} exemption and applies to fee
   * transactions too (open question OQ-3).
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2130-CHECK-AMOUNTS", rules = {"BR-04", "BR-05", "BR-06"})
  public ValidationOutcome checkAmounts(PortfolioTransaction transaction) {
    if (isNotPositive(transaction.getTrnQuantity())) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_AMT,
          "Quantity must be greater than zero",
          "BR-04",
          "PORTTRAN 2130-CHECK-AMOUNTS");
    }
    boolean transfer = transaction.getTrnType() == TransactionType.TRANSFER;
    if (isNotPositive(transaction.getTrnPrice()) && !transfer) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_AMT,
          "Price must be greater than zero",
          "BR-05",
          "PORTTRAN 2130-CHECK-AMOUNTS");
    }
    if (isNotPositive(transaction.getTrnAmount()) && !transfer) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_AMT,
          "Amount must be greater than zero",
          "BR-06",
          "PORTTRAN 2130-CHECK-AMOUNTS");
    }
    return ValidationOutcome.success("PORTTRAN 2130-CHECK-AMOUNTS");
  }

  private boolean isNotPositive(BigDecimal value) {
    return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
  }
}

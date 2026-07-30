package com.cognition.portfolio.transaction.validation;

import com.cognition.portfolio.traceability.CobolOrigin;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Field-level validation subroutine, migrated from {@code PORTVALD.cbl} with the constants from
 * {@code PORTVAL.cpy}. One Java method per COBOL paragraph.
 */
@Component
@CobolOrigin(program = "PORTVALD", paragraph = "0000-MAIN")
public class PortfolioFieldValidator {

  /** {@code VAL-ID-PREFIX PIC X(4) VALUE 'PORT'}. */
  public static final String ID_PREFIX = "PORT";

  /** {@code VAL-MIN-AMOUNT PIC S9(13)V99 VALUE -9999999999999.99}. */
  public static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");

  /** {@code VAL-MAX-AMOUNT PIC S9(13)V99 VALUE +9999999999999.99}. */
  public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");

  /** Investment type values accepted by {@code 3000-VALIDATE-TYPE}. */
  public static final Set<String> VALID_INVESTMENT_TYPES = Set.of("STK", "BND", "MMF", "ETF");

  /** {@code VAL-ERR-ID}. */
  public static final String ERR_ID = "Invalid Portfolio ID format";

  /** {@code VAL-ERR-ACCT}. */
  public static final String ERR_ACCT = "Invalid Account Number format";

  /** {@code VAL-ERR-TYPE}. */
  public static final String ERR_TYPE = "Invalid Investment Type";

  /** {@code VAL-ERR-AMT}. */
  public static final String ERR_AMT = "Amount outside valid range";

  /**
   * BR-15 — {@code 1000-VALIDATE-ID}: portfolio ID must start with {@code 'PORT'} and be followed
   * by four numeric digits, which exactly fills {@code TRN-PORTFOLIO-ID PIC X(08)}.
   */
  @CobolOrigin(program = "PORTVALD", paragraph = "1000-VALIDATE-ID", rules = {"BR-15"})
  public ValidationOutcome validatePortfolioId(String portfolioId) {
    String value = portfolioId == null ? "" : portfolioId;
    if (value.length() < 8 || !value.startsWith(ID_PREFIX)) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_ID, ERR_ID, "BR-15", "PORTVALD 1000-VALIDATE-ID");
    }
    String digits = value.substring(4, 8);
    if (!digits.chars().allMatch(Character::isDigit)) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_ID, ERR_ID, "BR-15", "PORTVALD 1000-VALIDATE-ID");
    }
    return ValidationOutcome.success("PORTVALD 1000-VALIDATE-ID");
  }

  /**
   * BR-16 — {@code 2000-VALIDATE-ACCOUNT}: account number must be numeric and non-zero.
   *
   * <p>The COBOL applies {@code IS NOT NUMERIC} to the whole {@code LS-INPUT-VALUE PIC X(50)}; the
   * Java port applies it to the account number the caller supplies. See open question OQ-8.
   */
  @CobolOrigin(program = "PORTVALD", paragraph = "2000-VALIDATE-ACCOUNT", rules = {"BR-16"})
  public ValidationOutcome validateAccountNumber(String accountNumber) {
    String value = accountNumber == null ? "" : accountNumber.trim();
    boolean numeric = !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    boolean zeros = numeric && value.chars().allMatch(c -> c == '0');
    if (!numeric || zeros) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_ACCT, ERR_ACCT, "BR-16", "PORTVALD 2000-VALIDATE-ACCOUNT");
    }
    return ValidationOutcome.success("PORTVALD 2000-VALIDATE-ACCOUNT");
  }

  /** BR-17 — {@code 3000-VALIDATE-TYPE}: investment type must be STK, BND, MMF or ETF. */
  @CobolOrigin(program = "PORTVALD", paragraph = "3000-VALIDATE-TYPE", rules = {"BR-17"})
  public ValidationOutcome validateInvestmentType(String investmentType) {
    String value = investmentType == null ? "" : investmentType.trim();
    if (!VALID_INVESTMENT_TYPES.contains(value)) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_TYPE, ERR_TYPE, "BR-17", "PORTVALD 3000-VALIDATE-TYPE");
    }
    return ValidationOutcome.success("PORTVALD 3000-VALIDATE-TYPE");
  }

  /** BR-18 — {@code 4000-VALIDATE-AMOUNT}: amount must be within {@code VAL-MIN/MAX-AMOUNT}. */
  @CobolOrigin(program = "PORTVALD", paragraph = "4000-VALIDATE-AMOUNT", rules = {"BR-18"})
  public ValidationOutcome validateAmount(BigDecimal amount) {
    if (amount == null
        || amount.compareTo(MIN_AMOUNT) < 0
        || amount.compareTo(MAX_AMOUNT) > 0) {
      return ValidationOutcome.failure(
          ValidationReturnCode.INVALID_AMT, ERR_AMT, "BR-18", "PORTVALD 4000-VALIDATE-AMOUNT");
    }
    return ValidationOutcome.success("PORTVALD 4000-VALIDATE-AMOUNT");
  }

  /**
   * BR-19 — {@code 0000-MAIN}: dispatch on {@code LS-VALIDATE-TYPE} (I/A/T/M); any other value
   * returns {@code VAL-INVALID-ID} with {@code 'Invalid validation type'}.
   */
  @CobolOrigin(program = "PORTVALD", paragraph = "0000-MAIN", rules = {"BR-19"})
  public ValidationOutcome validate(char validateType, String inputValue) {
    return switch (validateType) {
      case 'I' -> validatePortfolioId(inputValue);
      case 'A' -> validateAccountNumber(inputValue);
      case 'T' -> validateInvestmentType(inputValue);
      case 'M' -> validateAmount(parseAmount(inputValue));
      default ->
          ValidationOutcome.failure(
              ValidationReturnCode.INVALID_ID,
              "Invalid validation type",
              "BR-19",
              "PORTVALD 0000-MAIN");
    };
  }

  private BigDecimal parseAmount(String inputValue) {
    try {
      return new BigDecimal(inputValue == null ? "" : inputValue.trim());
    } catch (NumberFormatException e) {
      // MOVE of a non-numeric alphanumeric field into VAL-TEMP-NUM leaves an out-of-range value.
      return null;
    }
  }
}

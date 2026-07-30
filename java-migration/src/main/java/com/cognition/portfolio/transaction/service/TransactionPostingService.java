package com.cognition.portfolio.transaction.service;

import com.cognition.portfolio.traceability.CobolOrigin;
import com.cognition.portfolio.transaction.domain.PortfolioPostingEffect;
import com.cognition.portfolio.transaction.domain.PortfolioTransaction;
import com.cognition.portfolio.transaction.exception.TransactionProcessingException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Position update logic, migrated from {@code PORTTRAN 2200-UPDATE-POSITIONS} and its four
 * type-specific paragraphs. Each COBOL paragraph becomes one method with the same name.
 */
@Service
@CobolOrigin(program = "PORTTRAN", paragraph = "2200-UPDATE-POSITIONS")
public class TransactionPostingService {

  /**
   * {@code 2200-UPDATE-POSITIONS}: dispatch on {@code TRN-TYPE} to the buy/sell/transfer/fee
   * paragraph.
   *
   * @param availableUnits current {@code PORT-TOTAL-UNITS}, required by the sell path (BR-10)
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2200-UPDATE-POSITIONS", rules = {"BR-09", "BR-10", "BR-11", "BR-12"})
  public PortfolioPostingEffect updatePositions(PortfolioTransaction transaction, BigDecimal availableUnits) {
    return switch (transaction.getTrnType()) {
      case BUY -> processBuy(transaction);
      case SELL -> processSell(transaction, availableUnits);
      case TRANSFER -> processTransfer(transaction);
      case FEE -> processFee(transaction);
    };
  }

  /**
   * BR-09 — {@code 2210-PROCESS-BUY}: {@code ADD TRN-QUANTITY TO PORT-TOTAL-UNITS} and
   * {@code ADD TRN-AMOUNT TO PORT-TOTAL-COST}.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2210-PROCESS-BUY", rules = {"BR-09"})
  public PortfolioPostingEffect processBuy(PortfolioTransaction transaction) {
    return new PortfolioPostingEffect(
        transaction.getTrnQuantity(),
        transaction.getTrnAmount(),
        transaction.getTrnType().getAuditAction());
  }

  /**
   * BR-10 — {@code 2220-PROCESS-SELL}: reject when {@code PORT-TOTAL-UNITS < TRN-QUANTITY}
   * ('Insufficient units for sale'), otherwise {@code SUBTRACT TRN-QUANTITY FROM PORT-TOTAL-UNITS}
   * and {@code SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST}.
   *
   * <p>The COBOL reads {@code PORT-TOTAL-UNITS} from {@code PORTFILE}, so "position not supplied"
   * is not a legacy state: a missing {@code availableUnits} is a caller error and is reported as
   * such rather than as the business rejection.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2220-PROCESS-SELL", rules = {"BR-10"})
  public PortfolioPostingEffect processSell(PortfolioTransaction transaction, BigDecimal availableUnits) {
    if (availableUnits == null) {
      throw new IllegalArgumentException(
          "PORT-TOTAL-UNITS must be supplied to process an SL transaction; the legacy program reads "
              + "it from PORTFILE (MIGRATION-NOTES OQ-2)");
    }
    if (availableUnits.compareTo(transaction.getTrnQuantity()) < 0) {
      throw new TransactionProcessingException(
          "Insufficient units for sale", "BR-10", "PORTTRAN 2220-PROCESS-SELL");
    }
    return new PortfolioPostingEffect(
        transaction.getTrnQuantity().negate(),
        transaction.getTrnAmount().negate(),
        transaction.getTrnType().getAuditAction());
  }

  /**
   * BR-11 — {@code 2230-PROCESS-TRANSFER}: the paragraph contains only
   * {@code MOVE 'Transfer processing not implemented' TO ERR-TEXT}, so every {@code TR} transaction
   * fails in the legacy system. The literal behaviour is preserved.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2230-PROCESS-TRANSFER", rules = {"BR-11"})
  public PortfolioPostingEffect processTransfer(PortfolioTransaction transaction) {
    throw new TransactionProcessingException(
        "Transfer processing not implemented", "BR-11", "PORTTRAN 2230-PROCESS-TRANSFER");
  }

  /**
   * BR-12 — {@code 2240-PROCESS-FEE}: {@code SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST}; units are
   * untouched.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2240-PROCESS-FEE", rules = {"BR-12"})
  public PortfolioPostingEffect processFee(PortfolioTransaction transaction) {
    return new PortfolioPostingEffect(
        BigDecimal.ZERO.setScale(TransactionAmountCalculator.QUANTITY_SCALE),
        transaction.getTrnAmount().negate(),
        transaction.getTrnType().getAuditAction());
  }
}

package com.cognition.portfolio.transaction.service;

import com.cognition.portfolio.traceability.CobolOrigin;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/**
 * Derives {@code TRN-AMOUNT} from {@code TRN-QUANTITY} and {@code TRN-PRICE}.
 *
 * <p>No paragraph in {@code PORTTRAN.cbl} computes the amount — the batch only validates the value
 * it receives on the input file ({@code 2130-CHECK-AMOUNTS}). The calculation below therefore
 * reproduces what {@code COMPUTE TRN-AMOUNT = TRN-QUANTITY * TRN-PRICE} would do given the
 * copybook PIC clauses: the product of two {@code S9(11)V9(4)} fields is stored into an
 * {@code S9(13)V9(2)} field, and because the statement carries no {@code ROUNDED} phrase COBOL
 * <em>truncates</em> the excess decimal positions. Recorded as open question OQ-1.
 */
@Component
@CobolOrigin(program = "PORTTRAN", paragraph = "2130-CHECK-AMOUNTS", rules = {"BR-22"}, derived = true)
public class TransactionAmountCalculator {

  /** Scale of {@code TRN-AMOUNT PIC S9(13)V9(2)}. */
  public static final int AMOUNT_SCALE = 2;

  /** Scale of {@code TRN-QUANTITY} / {@code TRN-PRICE} {@code PIC S9(11)V9(4)}. */
  public static final int QUANTITY_SCALE = 4;

  /**
   * BR-22 — amount = quantity × price, truncated (never rounded up) to two decimal places, matching
   * a COBOL {@code COMPUTE} without the {@code ROUNDED} phrase.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2130-CHECK-AMOUNTS", rules = {"BR-22"}, derived = true)
  public BigDecimal computeAmount(BigDecimal quantity, BigDecimal price) {
    if (quantity == null || price == null) {
      throw new IllegalArgumentException("TRN-QUANTITY and TRN-PRICE are required to compute TRN-AMOUNT");
    }
    return quantity.multiply(price).setScale(AMOUNT_SCALE, RoundingMode.DOWN);
  }

  /**
   * True when the supplied {@code TRN-AMOUNT} equals quantity × price under BR-22. Used to flag
   * input records whose amount does not reconcile with the quantity and price on the same record.
   */
  @CobolOrigin(program = "PORTTRAN", paragraph = "2130-CHECK-AMOUNTS", rules = {"BR-22"}, derived = true)
  public boolean isConsistent(BigDecimal quantity, BigDecimal price, BigDecimal amount) {
    return amount != null
        && computeAmount(quantity, price).compareTo(amount.setScale(AMOUNT_SCALE, RoundingMode.DOWN)) == 0;
  }
}

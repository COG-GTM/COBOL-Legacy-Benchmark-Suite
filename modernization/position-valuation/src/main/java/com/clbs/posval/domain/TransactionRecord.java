package com.clbs.posval.domain;

import com.clbs.posval.cobol.PackedField;
import java.math.BigDecimal;

/**
 * {@code TRANSACTION-RECORD} of {@code src/copybook/common/TRNREC.cpy}, the input record of
 * {@code PORTTRAN}.
 *
 * <p>Layout, in copybook order:
 *
 * <pre>
 * 05 TRN-KEY.
 *    10 TRN-DATE          PIC X(08)              transaction date, YYYYMMDD
 *    10 TRN-TIME          PIC X(06)              transaction time, HHMMSS
 *    10 TRN-PORTFOLIO-ID  PIC X(08)
 *    10 TRN-SEQUENCE-NO   PIC X(06)
 * 05 TRN-DATA.
 *    10 TRN-INVESTMENT-ID PIC X(10)
 *    10 TRN-TYPE          PIC X(02)
 *    10 TRN-QUANTITY      PIC S9(11)V9(4) COMP-3
 *    10 TRN-PRICE         PIC S9(11)V9(4) COMP-3
 *    10 TRN-AMOUNT        PIC S9(13)V9(2) COMP-3
 *    10 TRN-CURRENCY      PIC X(03)
 *    10 TRN-STATUS        PIC X(01)
 * 05 TRN-AUDIT.
 *    10 TRN-PROCESS-DATE  PIC X(26)
 *    10 TRN-PROCESS-USER  PIC X(08)
 * 05 TRN-FILLER           PIC X(50)
 * </pre>
 *
 * <p>{@code TRN-AMOUNT} is an independent input field, not a derived one: nothing in the slice
 * checks that it equals quantity times price (spec open question OQ-6).
 */
public record TransactionRecord(
        String date,
        String time,
        String portfolioId,
        String sequenceNo,
        String investmentId,
        String type,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal amount,
        String currency,
        String status) {

    public TransactionRecord {
        quantity = PackedField.QUANTITY.store(quantity);
        price = PackedField.QUANTITY.store(price);
        amount = PackedField.AMOUNT.store(amount);
    }

    /** Convenience factory for the fields the valuation and update path actually reads. */
    public static TransactionRecord of(
            String portfolioId, String type, BigDecimal quantity, BigDecimal price, BigDecimal amount) {
        return new TransactionRecord(
                "20240320", "153045", portfolioId, "000001", "IBM0000001",
                type, quantity, price, amount, "USD", "P");
    }
}

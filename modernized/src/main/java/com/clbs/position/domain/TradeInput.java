package com.clbs.position.domain;

import java.math.BigDecimal;

/**
 * Immutable view of the calculable fields of a transaction, mirroring the
 * {@code TRN-DATA} group of {@code TRANSACTION-RECORD}
 * (copybook {@code src/copybook/common/TRNREC.cpy}):
 *
 * <pre>
 *   05  TRN-DATA.
 *       10  TRN-TYPE           PIC X(02).
 *       10  TRN-QUANTITY       PIC S9(11)V9(4) COMP-3.
 *       10  TRN-PRICE          PIC S9(11)V9(4) COMP-3.
 *       10  TRN-AMOUNT         PIC S9(13)V9(2) COMP-3.
 * </pre>
 *
 * @param type     transaction type ({@code TRN-TYPE})
 * @param quantity transaction quantity ({@code TRN-QUANTITY}), scale 4
 * @param price    transaction unit price ({@code TRN-PRICE}), scale 4
 * @param amount   transaction gross amount ({@code TRN-AMOUNT}), scale 2
 */
public record TradeInput(TransactionType type, BigDecimal quantity, BigDecimal price, BigDecimal amount) {

    public TradeInput {
        if (type == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }
        quantity = scale(quantity, MoneyScale.QUANTITY_SCALE);
        price = scale(price, MoneyScale.PRICE_SCALE);
        amount = scale(amount, MoneyScale.AMOUNT_SCALE);
    }

    private static BigDecimal scale(BigDecimal value, int scale) {
        return value == null
                ? BigDecimal.ZERO.setScale(scale)
                : value.setScale(scale, MoneyScale.ROUNDING);
    }
}

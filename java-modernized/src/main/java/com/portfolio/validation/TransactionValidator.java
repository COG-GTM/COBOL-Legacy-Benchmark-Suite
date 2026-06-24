package com.portfolio.validation;

import com.portfolio.dto.TransactionRequest;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.math.BigDecimal;

/**
 * Custom validator for transaction business rules.
 * Translated from PORTTRAN.cbl paragraph 2130-CHECK-AMOUNTS:
 * <pre>
 *   IF TRN-QUANTITY <= ZERO
 *       MOVE 'Quantity must be greater than zero' TO ERR-TEXT
 *   IF TRN-PRICE <= ZERO AND TRN-TYPE NOT = 'TR'
 *       MOVE 'Price must be greater than zero' TO ERR-TEXT
 *   IF TRN-AMOUNT <= ZERO AND TRN-TYPE NOT = 'TR'
 *       MOVE 'Amount must be greater than zero' TO ERR-TEXT
 * </pre>
 */
@Component
public class TransactionValidator implements Validator {

    @Override
    public boolean supports(Class<?> clazz) {
        return TransactionRequest.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        TransactionRequest request = (TransactionRequest) target;
        checkAmounts(request, errors);
    }

    /**
     * Mirrors 2130-CHECK-AMOUNTS from PORTTRAN.cbl.
     * Transfers (TR) are exempt from price/amount positivity checks.
     */
    private void checkAmounts(TransactionRequest request, Errors errors) {
        boolean isTransfer = "TR".equals(request.getTransactionType());

        if (!isTransfer && request.getPrice() != null
                && request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.rejectValue("price", "price.positive",
                    "Price must be greater than zero");
        }

        if (!isTransfer && request.getAmount() != null
                && request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.rejectValue("amount", "amount.positive",
                    "Amount must be greater than zero");
        }
    }
}

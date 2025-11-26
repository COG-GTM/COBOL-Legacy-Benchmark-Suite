package com.portfolio.transaction.service.validation;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.ValidationResult;
import com.portfolio.transaction.domain.enums.TransactionType;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class AmountQuantityValidator implements TransactionValidator {

    @Override
    public ValidationResult validate(TransactionRequest request) {
        if (request.getQuantity() == null || 
            request.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return ValidationResult.failure("Quantity must be greater than zero");
        }

        TransactionType type = TransactionType.fromCode(request.getTransactionType());

        if (type != TransactionType.TRANSFER) {
            if (request.getPrice() == null || 
                request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                return ValidationResult.failure("Price must be greater than zero");
            }

            if (request.getAmount() == null || 
                request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ValidationResult.failure("Amount must be greater than zero");
            }
        }

        return ValidationResult.success();
    }

    @Override
    public int getOrder() {
        return 3;
    }
}

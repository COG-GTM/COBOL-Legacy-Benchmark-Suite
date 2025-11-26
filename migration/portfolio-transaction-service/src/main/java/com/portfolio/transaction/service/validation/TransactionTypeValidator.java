package com.portfolio.transaction.service.validation;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.ValidationResult;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class TransactionTypeValidator implements TransactionValidator {

    private static final Set<String> VALID_TYPES = Set.of("BU", "SL", "TR", "FE");

    @Override
    public ValidationResult validate(TransactionRequest request) {
        String transactionType = request.getTransactionType();

        if (transactionType == null || !VALID_TYPES.contains(transactionType.toUpperCase())) {
            return ValidationResult.failure("Invalid Transaction Type: " + transactionType);
        }

        return ValidationResult.success();
    }

    @Override
    public int getOrder() {
        return 2;
    }
}

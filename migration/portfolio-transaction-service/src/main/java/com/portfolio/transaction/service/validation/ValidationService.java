package com.portfolio.transaction.service.validation;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.ValidationResult;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ValidationService {

    private final List<TransactionValidator> validators;

    public ValidationService(List<TransactionValidator> validators) {
        this.validators = validators.stream()
            .sorted(Comparator.comparingInt(TransactionValidator::getOrder))
            .collect(Collectors.toList());
    }

    public ValidationResult validateTransaction(TransactionRequest request) {
        for (TransactionValidator validator : validators) {
            ValidationResult result = validator.validate(request);
            if (!result.isValid()) {
                return result;
            }
        }
        return ValidationResult.success();
    }
}

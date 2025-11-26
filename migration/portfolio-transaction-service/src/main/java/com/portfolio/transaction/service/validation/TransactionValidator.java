package com.portfolio.transaction.service.validation;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.ValidationResult;

public interface TransactionValidator {

    ValidationResult validate(TransactionRequest request);

    int getOrder();
}

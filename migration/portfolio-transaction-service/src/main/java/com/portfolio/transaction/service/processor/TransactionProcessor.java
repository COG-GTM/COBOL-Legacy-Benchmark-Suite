package com.portfolio.transaction.service.processor;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResult;
import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.domain.enums.TransactionType;

public interface TransactionProcessor {

    TransactionType getSupportedType();

    TransactionResult process(TransactionRequest request, Portfolio portfolio);
}

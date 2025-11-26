package com.portfolio.transaction.service.processor;

import com.portfolio.transaction.domain.dto.TransactionRequest;
import com.portfolio.transaction.domain.dto.TransactionResult;
import com.portfolio.transaction.domain.entity.Portfolio;
import com.portfolio.transaction.domain.enums.TransactionType;
import org.springframework.stereotype.Component;

@Component
public class TransferTransactionProcessor implements TransactionProcessor {

    @Override
    public TransactionType getSupportedType() {
        return TransactionType.TRANSFER;
    }

    @Override
    public TransactionResult process(TransactionRequest request, Portfolio portfolio) {
        throw new UnsupportedOperationException("Transfer processing not implemented");
    }
}

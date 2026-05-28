package com.clbs.portfolio.batch.rules;

import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ValidationRuleService {

    public AdjudicationResult apply(TransactionRecord transaction) {
        if (!"DONE".equals(transaction.getStatus())) {
            log.warn("Transaction {} failed re-validation: status is not DONE", transaction.getId());
            return AdjudicationResult.DENIED;
        }

        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().isBlank()) {
            return AdjudicationResult.DENIED;
        }

        if (transaction.getTrnType() == null) {
            return AdjudicationResult.DENIED;
        }

        if (transaction.getAmount() == null || transaction.getQuantity() == null || transaction.getPrice() == null) {
            return AdjudicationResult.DENIED;
        }

        return AdjudicationResult.APPROVED;
    }
}

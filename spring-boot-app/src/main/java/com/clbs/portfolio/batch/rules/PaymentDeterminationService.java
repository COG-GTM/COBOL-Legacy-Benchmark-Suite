package com.clbs.portfolio.batch.rules;

import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
public class PaymentDeterminationService {

    public AdjudicationResult apply(TransactionRecord transaction) {
        BigDecimal amount = transaction.getAmount().abs();
        BigDecimal fees = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
        BigDecimal adjustment = transaction.getCostBasisAdjustment() != null
                ? transaction.getCostBasisAdjustment() : BigDecimal.ZERO;

        BigDecimal settlementAmount = amount.add(fees).subtract(adjustment);
        transaction.setSettlementAmount(settlementAmount);

        log.debug("Settlement amount for transaction {}: amount={}, fees={}, adj={}, settlement={}",
                transaction.getId(), amount, fees, adjustment, settlementAmount);

        return AdjudicationResult.APPROVED;
    }
}

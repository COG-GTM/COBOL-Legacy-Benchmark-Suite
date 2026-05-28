package com.clbs.portfolio.batch.rules;

import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import com.clbs.portfolio.enums.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
public class FeeScheduleService {

    private static final BigDecimal BUY_FEE_RATE = new BigDecimal("0.001");
    private static final BigDecimal SELL_FEE_RATE = new BigDecimal("0.0015");
    private static final BigDecimal TRANSFER_FEE = new BigDecimal("25.00");
    private static final BigDecimal BUY_MIN_FEE = new BigDecimal("5.00");
    private static final BigDecimal BUY_MAX_FEE = new BigDecimal("500.00");
    private static final BigDecimal SELL_MIN_FEE = new BigDecimal("5.00");
    private static final BigDecimal SELL_MAX_FEE = new BigDecimal("750.00");

    public AdjudicationResult apply(TransactionRecord transaction) {
        BigDecimal fee = calculateFee(transaction);
        transaction.setFeeAmount(fee);
        log.debug("Fee calculated for transaction {}: {}", transaction.getId(), fee);
        return AdjudicationResult.APPROVED;
    }

    public BigDecimal calculateFee(TransactionRecord transaction) {
        BigDecimal amount = transaction.getAmount().abs();
        TransactionType type = transaction.getTrnType();

        return switch (type) {
            case BU -> {
                BigDecimal fee = amount.multiply(BUY_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
                yield fee.max(BUY_MIN_FEE).min(BUY_MAX_FEE);
            }
            case SL -> {
                BigDecimal fee = amount.multiply(SELL_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
                yield fee.max(SELL_MIN_FEE).min(SELL_MAX_FEE);
            }
            case TR -> TRANSFER_FEE;
            case FE -> BigDecimal.ZERO;
        };
    }
}

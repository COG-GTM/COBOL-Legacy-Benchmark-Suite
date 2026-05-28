package com.clbs.portfolio.batch.rules;

import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.AdjudicationResult;
import com.clbs.portfolio.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class FeeScheduleServiceTest {

    private FeeScheduleService feeScheduleService;

    @BeforeEach
    void setUp() {
        feeScheduleService = new FeeScheduleService();
    }

    @Test
    void shouldCalculateBuyFeeWithMinimum() {
        TransactionRecord txn = createTransaction(TransactionType.BU, new BigDecimal("1000.00"));
        BigDecimal fee = feeScheduleService.calculateFee(txn);
        // 1000 * 0.001 = 1.00, but min is 5.00
        assertThat(fee).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void shouldCalculateBuyFeeNormal() {
        TransactionRecord txn = createTransaction(TransactionType.BU, new BigDecimal("50000.00"));
        BigDecimal fee = feeScheduleService.calculateFee(txn);
        // 50000 * 0.001 = 50.00
        assertThat(fee).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void shouldCalculateBuyFeeWithMaximum() {
        TransactionRecord txn = createTransaction(TransactionType.BU, new BigDecimal("1000000.00"));
        BigDecimal fee = feeScheduleService.calculateFee(txn);
        // 1000000 * 0.001 = 1000.00, but max is 500.00
        assertThat(fee).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void shouldCalculateSellFeeWithMinimum() {
        TransactionRecord txn = createTransaction(TransactionType.SL, new BigDecimal("500.00"));
        BigDecimal fee = feeScheduleService.calculateFee(txn);
        // 500 * 0.0015 = 0.75, but min is 5.00
        assertThat(fee).isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void shouldCalculateSellFeeNormal() {
        TransactionRecord txn = createTransaction(TransactionType.SL, new BigDecimal("50000.00"));
        BigDecimal fee = feeScheduleService.calculateFee(txn);
        // 50000 * 0.0015 = 75.00
        assertThat(fee).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void shouldCalculateSellFeeWithMaximum() {
        TransactionRecord txn = createTransaction(TransactionType.SL, new BigDecimal("1000000.00"));
        BigDecimal fee = feeScheduleService.calculateFee(txn);
        // 1000000 * 0.0015 = 1500.00, but max is 750.00
        assertThat(fee).isEqualByComparingTo(new BigDecimal("750.00"));
    }

    @Test
    void shouldCalculateTransferFee() {
        TransactionRecord txn = createTransaction(TransactionType.TR, new BigDecimal("100000.00"));
        BigDecimal fee = feeScheduleService.calculateFee(txn);
        assertThat(fee).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void shouldCalculateZeroFeeForFeeType() {
        TransactionRecord txn = createTransaction(TransactionType.FE, new BigDecimal("100.00"));
        BigDecimal fee = feeScheduleService.calculateFee(txn);
        assertThat(fee).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnApprovedAndSetFeeOnTransaction() {
        TransactionRecord txn = createTransaction(TransactionType.BU, new BigDecimal("50000.00"));
        AdjudicationResult result = feeScheduleService.apply(txn);
        assertThat(result).isEqualTo(AdjudicationResult.APPROVED);
        assertThat(txn.getFeeAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void shouldUseBigDecimalPrecisionForFees() {
        TransactionRecord txn = createTransaction(TransactionType.BU, new BigDecimal("7777.77"));
        BigDecimal fee = feeScheduleService.calculateFee(txn);
        // 7777.77 * 0.001 = 7.77777 -> rounded to 7.78
        assertThat(fee).isEqualByComparingTo(new BigDecimal("7.78"));
    }

    private TransactionRecord createTransaction(TransactionType type, BigDecimal amount) {
        return TransactionRecord.builder()
                .id(1L)
                .trnType(type)
                .amount(amount)
                .build();
    }
}

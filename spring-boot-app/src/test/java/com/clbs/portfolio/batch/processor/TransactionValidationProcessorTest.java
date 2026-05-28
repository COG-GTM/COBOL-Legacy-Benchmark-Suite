package com.clbs.portfolio.batch.processor;

import com.clbs.portfolio.entity.TransactionRecord;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.ErrorLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TransactionValidationProcessorTest {

    @Mock
    private ErrorLogRepository errorLogRepository;

    private TransactionValidationProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new TransactionValidationProcessor(errorLogRepository);
    }

    private TransactionRecord createValidBuyTransaction() {
        return TransactionRecord.builder()
                .id(1L)
                .trnDate("20240315")
                .trnTime("143025")
                .portfolioId("PORT1234")
                .sequenceNo("000001")
                .investmentId("AAPL000001")
                .trnType(TransactionType.BU)
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("150.0000"))
                .amount(new BigDecimal("15000.00"))
                .currency("USD")
                .status("PENDING")
                .build();
    }

    @Test
    void shouldValidateCorrectBuyTransaction() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("DONE");
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void shouldValidateCorrectSellTransaction() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setTrnType(TransactionType.SL);
        transaction.setQuantity(new BigDecimal("-100.0000"));
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("DONE");
    }

    @Test
    void shouldRejectInvalidDateFormat() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setTrnDate("ABCDEFGH");
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldRejectInvalidTimeFormat() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setTrnTime("ABCDEF");
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldRejectInvalidPortfolioIdPrefix() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setPortfolioId("ACCT1234");
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).contains("Invalid Portfolio ID");
    }

    @Test
    void shouldRejectNullPortfolioId() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setPortfolioId(null);
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldRejectZeroSequenceNo() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setSequenceNo("000000");
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldRejectAmountOutOfRange() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setAmount(new BigDecimal("99999999999999.99"));
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage()).contains("Amount outside valid range");
    }

    @Test
    void shouldRejectNegativeQuantityForBuy() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setQuantity(new BigDecimal("-100.0000"));
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldRejectPositiveQuantityForSell() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setTrnType(TransactionType.SL);
        transaction.setQuantity(new BigDecimal("100.0000"));
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldRejectNegativePrice() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setPrice(new BigDecimal("-150.0000"));
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldRejectAmountMismatch() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setAmount(new BigDecimal("99999.00"));
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void shouldAcceptMaxValidAmount() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setQuantity(new BigDecimal("1.0000"));
        transaction.setPrice(new BigDecimal("9999999999999.9900"));
        transaction.setAmount(new BigDecimal("9999999999999.99"));
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("DONE");
    }

    @Test
    void shouldAcceptMinValidAmount() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setTrnType(TransactionType.SL);
        transaction.setQuantity(new BigDecimal("-1.0000"));
        transaction.setPrice(new BigDecimal("9999999999999.9900"));
        transaction.setAmount(new BigDecimal("-9999999999999.99"));
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("DONE");
    }

    @Test
    void shouldUseBigDecimalPrecision() throws Exception {
        TransactionRecord transaction = createValidBuyTransaction();
        transaction.setQuantity(new BigDecimal("33.3333"));
        transaction.setPrice(new BigDecimal("10.0000"));
        transaction.setAmount(new BigDecimal("333.33"));
        TransactionRecord result = processor.process(transaction);
        assertThat(result.getStatus()).isEqualTo("DONE");
    }
}

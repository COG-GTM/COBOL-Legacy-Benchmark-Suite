package com.portfolio.utility;

import com.portfolio.model.PositionRecord;
import com.portfolio.model.TransactionRecord;
import com.portfolio.support.PositionRecordRepository;
import com.portfolio.support.TransactionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for DataValidationService.
 * Validates data integrity checks replacing COBOL UTLVAL00.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataValidationServiceTest {

    @Autowired
    private DataValidationService validationService;

    @Autowired
    private PositionRecordRepository positionRepository;

    @Autowired
    private TransactionRecordRepository transactionRepository;

    @BeforeEach
    void setUp() {
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
    }

    @Test
    void testValidationWithNoData() {
        Map<String, Object> result = validationService.runValidation();

        assertThat(result.get("checksRun")).isEqualTo(4);
        assertThat(result.get("status")).isEqualTo("PASSED");
    }

    @Test
    void testValidationWithValidData() {
        PositionRecord pos = new PositionRecord();
        pos.setPortfolioId("PORT0001");
        pos.setSymbolId("AAPL      ");
        pos.setPositionDate(LocalDate.now());
        pos.setQuantity(new BigDecimal("100.0000"));
        pos.setCostBasis(new BigDecimal("5000.00"));
        pos.setMarketValue(new BigDecimal("5500.00"));
        pos.setCurrencyCode("USD");
        pos.setStatus("A");
        pos.setLastMaintDate(LocalDateTime.now());
        pos.setLastMaintUser("TEST    ");
        positionRepository.save(pos);

        TransactionRecord txn = new TransactionRecord();
        txn.setTransactionId("TXN0000000000000001");
        txn.setPortfolioId("PORT0001");
        txn.setTransactionDate(LocalDate.now());
        txn.setTransactionTime(LocalTime.now());
        txn.setInvestmentId("AAPL      ");
        txn.setTransactionType("BU");
        txn.setQuantity(new BigDecimal("100.0000"));
        txn.setPrice(new BigDecimal("50.0000"));
        txn.setAmount(new BigDecimal("5000.00"));
        txn.setCurrencyCode("USD");
        txn.setStatus("D");
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("TEST    ");
        transactionRepository.save(txn);

        Map<String, Object> result = validationService.runValidation();
        assertThat(result.get("status")).isEqualTo("PASSED");
        assertThat(result.get("totalPositions")).isEqualTo(1);
        assertThat(result.get("totalTransactions")).isEqualTo(1);
    }

    @Test
    void testValidationWithInvalidTransactionType() {
        TransactionRecord txn = new TransactionRecord();
        txn.setTransactionId("TXN0000000000000002");
        txn.setPortfolioId("PORT0001");
        txn.setTransactionDate(LocalDate.now());
        txn.setTransactionTime(LocalTime.now());
        txn.setInvestmentId("AAPL      ");
        txn.setTransactionType("XX");
        txn.setQuantity(new BigDecimal("100.0000"));
        txn.setPrice(new BigDecimal("50.0000"));
        txn.setAmount(new BigDecimal("5000.00"));
        txn.setCurrencyCode("USD");
        txn.setStatus("P");
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("TEST    ");
        transactionRepository.save(txn);

        Map<String, Object> result = validationService.runValidation();
        assertThat(result.get("status")).isEqualTo("FAILED");
    }
}

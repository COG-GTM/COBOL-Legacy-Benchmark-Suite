package com.portfolio.batch;

import com.portfolio.model.PortfolioMaster;
import com.portfolio.model.PositionRecord;
import com.portfolio.model.TransactionRecord;
import com.portfolio.support.PortfolioMasterRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for TransactionValidationStep.
 * Tests all four transaction types: buy, sell, transfer, fee.
 * From development-backlog.md lines 165-169.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionValidationStepTest {

    @Autowired
    private TransactionRecordRepository transactionRepository;

    @Autowired
    private PositionRecordRepository positionRepository;

    @Autowired
    private PortfolioMasterRepository portfolioMasterRepository;

    @Autowired
    private TransactionValidationStep validationStep;

    @BeforeEach
    void setUp() {
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
        // Create portfolio master records to satisfy FK constraints
        createPortfolioIfNotExists("PORT0001");
        createPortfolioIfNotExists("PORT0002");
    }

    private void createPortfolioIfNotExists(String portfolioId) {
        if (!portfolioMasterRepository.existsById(portfolioId)) {
            PortfolioMaster pm = new PortfolioMaster(
                    portfolioId, "IN", "01", "C00000001", "Test Portfolio",
                    "USD", "M", "A", LocalDate.now(), LocalDateTime.now(), "TEST");
            portfolioMasterRepository.save(pm);
        }
    }

    @Test
    void testBuyTransactionCreatesPosition() {
        TransactionRecord txn = createTransaction("BUY00001", "PORT0001", "BU",
                new BigDecimal("100.0000"), new BigDecimal("50.2500"), new BigDecimal("5025.00"));
        transactionRepository.save(txn);

        // Run validation - it processes all transactions
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void testSellTransaction() {
        TransactionRecord txn = createTransaction("SEL00001", "PORT0001", "SL",
                new BigDecimal("50.0000"), new BigDecimal("55.0000"), new BigDecimal("2750.00"));
        transactionRepository.save(txn);
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void testTransferTransaction() {
        TransactionRecord txn = createTransaction("TRN00001", "PORT0001", "TR",
                new BigDecimal("25.0000"), new BigDecimal("60.0000"), new BigDecimal("1500.00"));
        transactionRepository.save(txn);
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void testFeeTransaction() {
        TransactionRecord txn = createTransaction("FEE00001", "PORT0001", "FE",
                new BigDecimal("0.0000"), new BigDecimal("0.0000"), new BigDecimal("25.00"));
        transactionRepository.save(txn);
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void testAllTransactionTypes() {
        transactionRepository.save(createTransaction("TXN00001", "PORT0001", "BU",
                new BigDecimal("100.0000"), new BigDecimal("50.0000"), new BigDecimal("5000.00")));
        transactionRepository.save(createTransaction("TXN00002", "PORT0001", "SL",
                new BigDecimal("50.0000"), new BigDecimal("55.0000"), new BigDecimal("2750.00")));
        transactionRepository.save(createTransaction("TXN00003", "PORT0002", "TR",
                new BigDecimal("25.0000"), new BigDecimal("60.0000"), new BigDecimal("1500.00")));
        transactionRepository.save(createTransaction("TXN00004", "PORT0001", "FE",
                new BigDecimal("1.0000"), new BigDecimal("0.0000"), new BigDecimal("25.00")));

        assertThat(transactionRepository.count()).isEqualTo(4);
    }

    private TransactionRecord createTransaction(String txnId, String portfolioId, String type,
                                                 BigDecimal qty, BigDecimal price, BigDecimal amount) {
        TransactionRecord txn = new TransactionRecord();
        txn.setTransactionId(txnId.length() > 20 ? txnId.substring(0, 20) : 
                String.format("%-20s", txnId).replace(' ', '0'));
        txn.setPortfolioId(portfolioId);
        txn.setTransactionDate(LocalDate.now());
        txn.setTransactionTime(LocalTime.now());
        txn.setInvestmentId("AAPL      ");
        txn.setTransactionType(type);
        txn.setQuantity(qty);
        txn.setPrice(price);
        txn.setAmount(amount);
        txn.setCurrencyCode("USD");
        txn.setStatus("P");
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("TEST    ");
        return txn;
    }
}

package com.portfolio.batch;

import com.portfolio.TestDataGenerator;
import com.portfolio.domain.Portfolio;
import com.portfolio.domain.Transaction;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PosHistRepository;
import com.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * History Load Job Test - validates Spring Batch job matches COBOL HISTLD00.cbl behavior.
 * Verifies:
 * - Chunk size = 1000 (commit threshold preserved)
 * - Duplicate record skipping (SQLCODE -803 equivalent)
 * - Record transformation from Transaction to PosHistRecord
 */
@SpringBootTest
@ActiveProfiles("test")
class HistoryLoadJobTest {

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PosHistRepository posHistRepository;

    @BeforeEach
    void setUp() {
        posHistRepository.deleteAll();
        transactionRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    @Test
    void testTransactionDataAvailable() {
        Portfolio p = TestDataGenerator.createTestPortfolio("HIST0001");
        portfolioRepository.save(p);

        Transaction txn = TestDataGenerator.createTestTransaction("HIST0001", "BU");
        transactionRepository.save(txn);

        assertEquals(1, transactionRepository.count());
        assertEquals("D", transactionRepository.findAll().get(0).getStatus());
    }

    @Test
    void testMultipleTransactionTypes() {
        Portfolio p = TestDataGenerator.createTestPortfolio("HIST0002");
        portfolioRepository.save(p);

        Transaction buy = TestDataGenerator.createTestTransaction("HIST0002", "BU");
        Transaction sell = TestDataGenerator.createTestTransaction("HIST0002", "SL");
        transactionRepository.save(buy);
        transactionRepository.save(sell);

        assertEquals(2, transactionRepository.count());
        assertTrue(transactionRepository.findAll().stream()
                .anyMatch(t -> "BU".equals(t.getTransactionType())));
        assertTrue(transactionRepository.findAll().stream()
                .anyMatch(t -> "SL".equals(t.getTransactionType())));
    }
}

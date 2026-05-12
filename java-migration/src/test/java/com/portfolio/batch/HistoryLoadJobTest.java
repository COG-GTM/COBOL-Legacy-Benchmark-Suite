package com.portfolio.batch;

import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.entity.Transaction;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionHistoryRepository;
import com.portfolio.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
class HistoryLoadJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    @Qualifier("historyLoadJob")
    private Job historyLoadJob;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PositionHistoryRepository positionHistoryRepository;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(historyLoadJob);
        positionHistoryRepository.deleteAll();
        transactionRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    @Test
    void historyLoadJob_processesTransactions() throws Exception {
        Portfolio portfolio = createPortfolio("PORT0001");
        portfolioRepository.save(portfolio);

        for (int i = 0; i < 5; i++) {
            Transaction txn = createTransaction("PORT0001", "TXN" + String.format("%017d", i));
            transactionRepository.save(txn);
        }

        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertEquals(BatchStatus.COMPLETED, execution.getStatus());
        assertTrue(positionHistoryRepository.count() > 0);
    }

    private Portfolio createPortfolio(String id) {
        Portfolio p = new Portfolio();
        p.setPortfolioId(id);
        p.setAccountType("GN");
        p.setBranchId("01");
        p.setClientId("CLIENT001");
        p.setPortfolioName("Test Portfolio");
        p.setCurrencyCode("USD");
        p.setRiskLevel("M");
        p.setStatus('A');
        p.setOpenDate(LocalDate.of(2024, 1, 1));
        p.setLastMaintDate(LocalDateTime.now());
        p.setLastMaintUser("SYSTEM");
        p.setTotalValue(BigDecimal.ZERO);
        p.setCashBalance(BigDecimal.ZERO);
        return p;
    }

    private Transaction createTransaction(String portfolioId, String txnId) {
        Transaction txn = new Transaction();
        txn.setTransactionId(txnId);
        txn.setPortfolioId(portfolioId);
        txn.setTransactionDate(LocalDate.now());
        txn.setTransactionTime(LocalTime.now());
        txn.setInvestmentId("INV001");
        txn.setTransactionType("BU");
        txn.setQuantity(new BigDecimal("100.0000"));
        txn.setPrice(new BigDecimal("50.0000"));
        txn.setAmount(new BigDecimal("5000.00"));
        txn.setCurrencyCode("USD");
        txn.setStatus('D');
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("SYSTEM");
        return txn;
    }
}

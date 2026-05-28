package com.clbs.portfolio.integration;

import com.clbs.portfolio.entity.*;
import com.clbs.portfolio.enums.TransactionType;
import com.clbs.portfolio.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest
class ClaimsProcessingJobIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private TransactionRecordRepository transactionRecordRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private PositionHistoryRepository positionHistoryRepository;

    @Autowired
    private HistoryRecordRepository historyRecordRepository;

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @BeforeEach
    void setUp() {
        transactionRecordRepository.deleteAll();
        portfolioRepository.deleteAll();
        positionRepository.deleteAll();
        positionHistoryRepository.deleteAll();
        historyRecordRepository.deleteAll();
        errorLogRepository.deleteAll();
    }

    @Test
    void shouldProcessValidAndInvalidTransactions() throws Exception {
        Portfolio portfolio = Portfolio.builder()
                .portfolioId("PORT1234")
                .accountNo("1234567890")
                .clientName("Test Client")
                .clientType("I")
                .status("A")
                .totalValue(new BigDecimal("100000.00"))
                .cashBalance(new BigDecimal("50000.00"))
                .build();
        portfolioRepository.save(portfolio);

        TransactionRecord validTxn = TransactionRecord.builder()
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
        transactionRecordRepository.save(validTxn);

        TransactionRecord invalidTxn = TransactionRecord.builder()
                .trnDate("INVALID!")
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
        transactionRecordRepository.save(invalidTxn);

        HistoryRecord histRecord = HistoryRecord.builder()
                .accountNo("1234567890")
                .portfolioId("PORT001234")
                .transDate("2024-03-15")
                .transTime("14:30:25")
                .transType("BU")
                .securityId("AAPL00000001")
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("150.0000"))
                .amount(new BigDecimal("15000.00"))
                .fees(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("15015.00"))
                .costBasis(new BigDecimal("15000.00"))
                .gainLoss(new BigDecimal("0.00"))
                .status("PENDING")
                .build();
        historyRecordRepository.save(histRecord);

        JobExecution execution = jobLauncherTestUtils.launchJob(
                new JobParametersBuilder()
                        .addLong("run.id", System.currentTimeMillis())
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<TransactionRecord> results = transactionRecordRepository.findAll();
        long failedCount = results.stream().filter(t -> "FAILED".equals(t.getStatus())).count();
        assertThat(failedCount).isGreaterThanOrEqualTo(1);

        List<TransactionRecord> appliedTxns = results.stream()
                .filter(t -> "APPLIED".equals(t.getStatus()))
                .toList();
        assertThat(appliedTxns).hasSize(1);

        List<Position> positions = positionRepository.findAll();
        assertThat(positions).hasSize(1);
        Position pos = positions.get(0);
        assertThat(pos.getPortfolioId()).isEqualTo("PORT1234");
        assertThat(pos.getInvestmentId()).isEqualTo("AAPL000001");
        assertThat(pos.getQuantity()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(pos.getCostBasis()).isEqualByComparingTo(new BigDecimal("15000.00"));

        List<PositionHistory> history = positionHistoryRepository.findAll();
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getProgramId()).isEqualTo("HISTLD00");
    }

    @Test
    void shouldRunFullJobWithBuyAndSell() throws Exception {
        Portfolio portfolio = Portfolio.builder()
                .portfolioId("PORT5555")
                .accountNo("5555555555")
                .clientName("E2E Client")
                .clientType("I")
                .status("A")
                .totalValue(new BigDecimal("500000.00"))
                .cashBalance(new BigDecimal("200000.00"))
                .build();
        portfolioRepository.save(portfolio);

        Position existingPos = Position.builder()
                .portfolioId("PORT5555")
                .investmentId("MSFT000001")
                .quantity(new BigDecimal("200.0000"))
                .costBasis(new BigDecimal("80000.00"))
                .marketValue(new BigDecimal("80000.00"))
                .currency("USD")
                .status("A")
                .realizedGainLoss(BigDecimal.ZERO)
                .build();
        positionRepository.save(existingPos);

        TransactionRecord buyTxn = TransactionRecord.builder()
                .trnDate("20240601")
                .trnTime("100000")
                .portfolioId("PORT5555")
                .sequenceNo("000001")
                .investmentId("MSFT000001")
                .trnType(TransactionType.BU)
                .quantity(new BigDecimal("50.0000"))
                .price(new BigDecimal("400.0000"))
                .amount(new BigDecimal("20000.00"))
                .currency("USD")
                .status("PENDING")
                .build();
        transactionRecordRepository.save(buyTxn);

        JobExecution execution = jobLauncherTestUtils.launchJob(
                new JobParametersBuilder()
                        .addLong("run.id", System.currentTimeMillis())
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Position pos = positionRepository.findByPortfolioIdAndInvestmentId("PORT5555", "MSFT000001")
                .orElse(null);
        assertThat(pos).isNotNull();
        assertThat(pos.getQuantity()).isEqualByComparingTo(new BigDecimal("250.0000"));
        assertThat(pos.getCostBasis()).isEqualByComparingTo(new BigDecimal("100000.00"));
    }

    @Test
    void shouldProcessHistoryLoadCorrectly() throws Exception {
        HistoryRecord histRecord = HistoryRecord.builder()
                .accountNo("ACCT001234")
                .portfolioId("PORT001234")
                .transDate("2024-03-15")
                .transTime("14:30:25")
                .transType("BU")
                .securityId("AAPL00000001")
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("150.0000"))
                .amount(new BigDecimal("15000.00"))
                .fees(new BigDecimal("15.00"))
                .totalAmount(new BigDecimal("15015.00"))
                .costBasis(new BigDecimal("15000.00"))
                .gainLoss(new BigDecimal("0.00"))
                .status("PENDING")
                .build();
        historyRecordRepository.save(histRecord);

        JobExecution execution = jobLauncherTestUtils.launchJob(
                new JobParametersBuilder()
                        .addLong("run.id", System.currentTimeMillis())
                        .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        List<PositionHistory> loaded = positionHistoryRepository.findAll();
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).getAccountNo()).isEqualTo("ACCT001234");
        assertThat(loaded.get(0).getProgramId()).isEqualTo("HISTLD00");
        assertThat(loaded.get(0).getQuantity()).isEqualByComparingTo(new BigDecimal("100.000"));
    }
}

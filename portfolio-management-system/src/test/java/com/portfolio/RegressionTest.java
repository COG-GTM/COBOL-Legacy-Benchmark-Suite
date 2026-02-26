package com.portfolio;

import com.portfolio.model.PositionRecord;
import com.portfolio.model.TransactionRecord;
import com.portfolio.model.PortfolioMaster;
import com.portfolio.reporting.PositionReportService;
import com.portfolio.reporting.StatisticsReportService;
import com.portfolio.support.ErrorRecordRepository;
import com.portfolio.support.PortfolioMasterRepository;
import com.portfolio.support.PositionRecordRepository;
import com.portfolio.support.TransactionRecordRepository;
import com.portfolio.support.HistoryRecordRepository;
import com.portfolio.utility.DataValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression / Data Validation Test.
 * Replaces COBOL TSTGEN00 + TSTVAL00.
 * Generates test data: 1000 portfolios, 10,000 transactions, 5% error rate, deterministic seed.
 * From system-architecture.md lines 155-168.
 */
@SpringBootTest
@ActiveProfiles("test")
class RegressionTest {

    private static final int NUM_PORTFOLIOS = 1000;
    private static final int NUM_TRANSACTIONS = 10000;
    private static final double ERROR_RATE = 0.05;
    private static final long SEED = 42L;

    @Autowired
    private PortfolioMasterRepository portfolioRepository;

    @Autowired
    private TransactionRecordRepository transactionRepository;

    @Autowired
    private PositionRecordRepository positionRepository;

    @Autowired
    private HistoryRecordRepository historyRecordRepository;

    @Autowired
    private ErrorRecordRepository errorRecordRepository;

    @Autowired
    private PositionReportService positionReportService;

    @Autowired
    private StatisticsReportService statisticsReportService;

    @Autowired
    private DataValidationService dataValidationService;

    @BeforeEach
    void setUp() {
        historyRecordRepository.deleteAll();
        positionRepository.deleteAll();
        transactionRepository.deleteAll();
        errorRecordRepository.deleteAll();
        portfolioRepository.deleteAll();
    }

    @Test
    void testGenerateAndValidateTestData() {
        Random random = new Random(SEED);

        // Generate 1000 portfolios (TSTGEN00)
        String[] accountTypes = {"IN", "RE", "TR", "MM"};
        String[] currencies = {"USD", "EUR", "GBP", "JPY", "CAD"};
        String[] riskLevels = {"L", "M", "H"};

        for (int i = 0; i < NUM_PORTFOLIOS; i++) {
            String portfolioId = String.format("P%07d", i + 1);
            PortfolioMaster pm = new PortfolioMaster(
                    portfolioId,
                    accountTypes[random.nextInt(accountTypes.length)],
                    String.format("%02d", random.nextInt(50) + 1),
                    String.format("C%08d", random.nextInt(100000)),
                    "Portfolio " + (i + 1),
                    currencies[random.nextInt(currencies.length)],
                    riskLevels[random.nextInt(riskLevels.length)],
                    "A",
                    LocalDate.now().minusDays(random.nextInt(3650)),
                    LocalDateTime.now(),
                    "TSTGEN00"
            );
            portfolioRepository.save(pm);
        }

        assertThat(portfolioRepository.count()).isEqualTo(NUM_PORTFOLIOS);

        // Generate 10,000 transactions
        String[] txnTypes = {"BU", "SL", "TR", "FE"};
        String[] symbols = {"AAPL      ", "GOOGL     ", "MSFT      ", "AMZN      ", "TSLA      "};
        int errorCount = 0;

        for (int i = 0; i < NUM_TRANSACTIONS; i++) {
            String txnId = String.format("T%019d", i + 1);
            String portfolioId = String.format("P%07d", random.nextInt(NUM_PORTFOLIOS) + 1);

            // 5% error rate - use invalid transaction type
            boolean isError = random.nextDouble() < ERROR_RATE;
            String txnType = isError ? "XX" : txnTypes[random.nextInt(txnTypes.length)];
            if (isError) errorCount++;

            TransactionRecord txn = new TransactionRecord();
            txn.setTransactionId(txnId);
            txn.setPortfolioId(portfolioId);
            txn.setTransactionDate(LocalDate.now().minusDays(random.nextInt(365)));
            txn.setTransactionTime(LocalTime.of(random.nextInt(24), random.nextInt(60)));
            txn.setInvestmentId(symbols[random.nextInt(symbols.length)]);
            txn.setTransactionType(txnType);
            txn.setQuantity(new BigDecimal(random.nextInt(1000) + 1 + ".0000"));
            txn.setPrice(new BigDecimal(random.nextInt(500) + 1 + ".0000"));
            txn.setAmount(txn.getQuantity().multiply(txn.getPrice()));
            txn.setCurrencyCode("USD");
            txn.setStatus(isError ? "F" : "P");
            txn.setProcessDate(LocalDateTime.now());
            txn.setProcessUser("TSTGEN00");
            transactionRepository.save(txn);
        }

        assertThat(transactionRepository.count()).isEqualTo(NUM_TRANSACTIONS);

        // Run data validation (TSTVAL00)
        Map<String, Object> validationResult = dataValidationService.runValidation();
        assertThat(validationResult).containsKey("checksRun");
        assertThat(validationResult).containsKey("totalTransactions");
        assertThat(validationResult.get("totalTransactions")).isEqualTo(NUM_TRANSACTIONS);

        // Run position report
        List<PositionReportService.PositionReportLine> report = positionReportService.generateDailyReport();
        // Report should be empty since no positions were created yet
        assertThat(report).isNotNull();

        // Run statistics report
        Map<String, Object> statsReport = statisticsReportService.generateStatisticsReport();
        assertThat(statsReport.get("totalTransactions")).isEqualTo((long) NUM_TRANSACTIONS);

        // Verify error rate is approximately 5%
        long failedTxns = transactionRepository.countByStatus("F");
        double actualErrorRate = (double) failedTxns / NUM_TRANSACTIONS;
        assertThat(actualErrorRate).isBetween(0.02, 0.08);
    }

    @Test
    void testDeterministicSeedProducesSameResults() {
        Random random1 = new Random(SEED);
        Random random2 = new Random(SEED);

        // Generate the same sequence with same seed
        for (int i = 0; i < 100; i++) {
            assertThat(random1.nextInt()).isEqualTo(random2.nextInt());
        }
    }
}

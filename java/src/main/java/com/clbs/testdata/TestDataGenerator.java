package com.clbs.testdata;

import com.clbs.common.Inputs;
import com.clbs.common.ProgramResult;
import com.clbs.domain.PortfolioRecord;
import com.clbs.domain.TransactionRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Service;

/**
 * TSTGEN00 — test data generator.
 *
 * <p>The COBOL source defines the CONFIG-RECORD layout, the
 * PORTFOLIO/TRANSACTN/ERROR/VOLUME dispatch, the seeded WS-RANDOM-VALUES fields and the
 * generate-then-write loop bounded by CFG-VOLUME. The paragraphs that actually build a record
 * (2210/2220, 2310/2320, 2410/2420, 2510/2520) are PERFORMed but never defined, so the field
 * values below follow the documented test-data specifications and are deterministic for a given
 * seed rather than reproducing unwritten COBOL logic.
 */
@Service
public class TestDataGenerator {

    public static final String PROGRAM = "TSTGEN00";

    public static final String PORTFOLIO = "PORTFOLIO";
    public static final String TRANSACTION = "TRANSACTN";
    public static final String ERROR_TEST = "ERROR";
    public static final String VOLUME_TEST = "VOLUME";
    public static final String ERR_INVALID_TYPE = "INVALID TEST TYPE";

    /** CONFIG-RECORD. */
    public record GeneratorConfig(String testType, int volume, String parameters) {
    }

    /** What one generation run produced. */
    public record GeneratedData(ProgramResult result, List<PortfolioRecord> portfolios,
            List<TransactionRecord> transactions) {
    }

    private static final String[] SECURITIES = {"IBM0000001", "MSFT000001", "AAPL000001",
            "GOOG000001", "AMZN000001"};

    /** 2000-PROCESS, driven by SEED-RECORD from the RANDOM-SEED file. */
    public GeneratedData run(List<GeneratorConfig> configs, long seed) {
        ProgramResult result = new ProgramResult(PROGRAM);
        Random random = new Random(seed);
        List<PortfolioRecord> portfolios = new ArrayList<>();
        List<TransactionRecord> transactions = new ArrayList<>();
        long errors = 0;

        for (GeneratorConfig config : Inputs.records(configs)) {
            switch (config.testType() == null ? "" : config.testType().trim()) {
                case PORTFOLIO -> generatePortfolios(config.volume(), random, portfolios);
                case TRANSACTION -> generateTransactions(config.volume(), random, transactions);
                case VOLUME_TEST -> {
                    generatePortfolios(config.volume(), random, portfolios);
                    generateTransactions(config.volume(), random, transactions);
                }
                case ERROR_TEST -> generateErrorData(config.volume(), portfolios, transactions);
                default -> {
                    errors++;
                    result.display(ERR_INVALID_TYPE);
                }
            }
        }

        result.count("portfolios", portfolios.size())
                .count("transactions", transactions.size())
                .count("errors", errors);
        return new GeneratedData(result, portfolios, transactions);
    }

    /** 2200-GEN-PORTFOLIO. */
    private void generatePortfolios(int volume, Random random, List<PortfolioRecord> target) {
        int start = target.size() + 1;
        for (int i = start; i < start + volume; i++) {
            PortfolioRecord record = new PortfolioRecord();
            record.setPortId(String.format("PORT%05d", i));
            record.setAccountNo(String.format("ACCT%06d", i));
            record.setClientName("TEST PORTFOLIO " + i);
            record.setCreateDate(20240320);
            record.setStatus("AIC".charAt(random.nextInt(3)));
            record.setTotalValue(amount(random, 1_000_000));
            target.add(record);
        }
    }

    /** 2300-GEN-TRANSACTION. */
    private void generateTransactions(int volume, Random random, List<TransactionRecord> target) {
        int start = target.size() + 1;
        for (int i = start; i < start + volume; i++) {
            TransactionRecord record = new TransactionRecord();
            record.setSequenceNo(String.format("%08d", i));
            record.setPortfolioId(
                    String.format("PORT%05d", 1 + random.nextInt(Math.max(1, volume))));
            record.setType(random.nextBoolean() ? "BU" : "SL");
            record.setInvestmentId(SECURITIES[random.nextInt(SECURITIES.length)]);
            record.setQuantity(quantity(random));
            record.setPrice(amount(random, 500));
            record.setAmount(record.getQuantity().multiply(record.getPrice())
                    .setScale(2, RoundingMode.HALF_UP));
            record.setDate("20240320");
            target.add(record);
        }
    }

    /**
     * 2400-GEN-ERROR-DATA: records that violate the documented validation criteria — a malformed
     * portfolio ID and an unsupported transaction type.
     */
    private void generateErrorData(int volume, List<PortfolioRecord> portfolios,
            List<TransactionRecord> transactions) {
        for (int i = 1; i <= volume; i++) {
            PortfolioRecord portfolio = new PortfolioRecord();
            portfolio.setPortId("BAD" + i);
            portfolio.setClientName("");
            portfolio.setStatus('X');
            portfolios.add(portfolio);

            TransactionRecord transaction = new TransactionRecord();
            transaction.setSequenceNo(String.format("%08d", i));
            transaction.setPortfolioId("BAD" + i);
            transaction.setType("ZZ");
            transaction.setQuantity(BigDecimal.ZERO);
            transaction.setPrice(BigDecimal.ZERO);
            transaction.setAmount(BigDecimal.ZERO);
            transactions.add(transaction);
        }
    }

    private static BigDecimal amount(Random random, int bound) {
        return BigDecimal.valueOf(random.nextInt(bound * 100) + 1L, 2);
    }

    private static BigDecimal quantity(Random random) {
        return BigDecimal.valueOf(random.nextInt(10_000) + 1L, 4)
                .setScale(4, RoundingMode.HALF_UP);
    }
}

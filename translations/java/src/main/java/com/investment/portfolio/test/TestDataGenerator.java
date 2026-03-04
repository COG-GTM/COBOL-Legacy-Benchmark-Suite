package com.investment.portfolio.test;

import com.investment.portfolio.common.ErrorHandler;
import com.investment.portfolio.common.FileHandler;
import com.investment.portfolio.common.ReturnCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Test Data Generator (TSTGEN00) - Java equivalent of TSTGEN00.cbl
 *
 * Original COBOL: src/programs/test/TSTGEN00.cbl
 *
 * Responsibilities:
 * - Generates test data for system testing
 * - Creates portfolio, transaction, error, and volume test data
 * - Uses configurable random seed for reproducible test runs
 * - Writes generated data to output files
 *
 * Test types (from WS-TEST-TYPE):
 * - PORTFOLIO:  Generate portfolio master test records
 * - TRANSACTN:  Generate transaction test records
 * - ERROR:      Generate error condition test data
 * - VOLUME:     Generate high-volume test data
 *
 * Files:
 * - TEST-CONFIG      (input):  Test generation configuration
 * - PORTFOLIO-OUT    (output): Generated portfolio records
 * - TRANSACTION-OUT  (output): Generated transaction records
 * - RANDOM-SEED      (input):  Random seed for reproducibility
 */
public class TestDataGenerator {

    private static final Logger LOGGER = Logger.getLogger(TestDataGenerator.class.getName());
    private static final String PROGRAM_ID = "TSTGEN00";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    /** Test types matching WS-TEST-TYPE */
    public enum TestType {
        PORTFOLIO, TRANSACTN, ERROR, VOLUME
    }

    private final Path configFilePath;
    private final Path portfolioOutputPath;
    private final Path transactionOutputPath;
    private final Path seedFilePath;

    private final ErrorHandler errorHandler;
    private final ReturnCode returnCode;
    private Random random;

    /** Generation counters */
    private long portfolioCount;
    private long transactionCount;
    private long errorCount;
    private long totalGenerated;

    /** Configuration defaults */
    private int numPortfolios = 100;
    private int transactionsPerPortfolio = 10;
    private int volumeMultiplier = 10;
    private long randomSeed = 12345L;

    /** Data generation constants */
    private static final String[] CLIENT_TYPES = {"I", "C", "T"};
    private static final String[] TRANSACTION_TYPES = {"BU", "SL", "TR", "FE"};
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP", "JPY", "CAD"};
    private static final String[] INVESTMENT_PREFIXES = {"STK", "BND", "FND", "ETF", "OPT"};

    public TestDataGenerator(Path configFilePath, Path portfolioOutputPath,
                             Path transactionOutputPath, Path seedFilePath) {
        this.configFilePath = configFilePath;
        this.portfolioOutputPath = portfolioOutputPath;
        this.transactionOutputPath = transactionOutputPath;
        this.seedFilePath = seedFilePath;
        this.errorHandler = new ErrorHandler(PROGRAM_ID);
        this.returnCode = new ReturnCode(PROGRAM_ID);
    }

    /**
     * Main entry point - maps to COBOL 0000-MAIN.
     *
     * PERFORM 1000-INITIALIZE
     * EVALUATE WS-TEST-TYPE
     *   WHEN 'PORTFOLIO' PERFORM 2000-GENERATE-PORTFOLIOS
     *   WHEN 'TRANSACTN' PERFORM 3000-GENERATE-TRANSACTIONS
     *   WHEN 'ERROR'     PERFORM 4000-GENERATE-ERRORS
     *   WHEN 'VOLUME'    PERFORM 5000-GENERATE-VOLUME
     * END-EVALUATE
     * PERFORM 9000-TERMINATE
     */
    public int execute(TestType testType) {
        LOGGER.info(PROGRAM_ID + " - Test Data Generation starting: " + testType);

        try {
            initialize();

            switch (testType) {
                case PORTFOLIO:
                    generatePortfolios();
                    break;
                case TRANSACTN:
                    generateTransactions();
                    break;
                case ERROR:
                    generateErrors();
                    break;
                case VOLUME:
                    generateVolume();
                    break;
            }

            terminate();
        } catch (Exception e) {
            errorHandler.handleSystemError("E999", "Unexpected error in " + PROGRAM_ID, e);
            returnCode.setCode(ReturnCode.SEVERE);
        }

        LOGGER.info(PROGRAM_ID + " - Completed with return code: " + returnCode.getCurrentCode());
        return returnCode.getCurrentCode();
    }

    /**
     * 1000-INITIALIZE: Load config, initialize random seed.
     *
     * Maps to:
     *   OPEN INPUT TEST-CONFIG
     *   READ TEST-CONFIG
     *   OPEN INPUT RANDOM-SEED
     *   READ RANDOM-SEED INTO WS-SEED-VALUE
     *   MOVE FUNCTION RANDOM(WS-SEED-VALUE) TO WS-RANDOM-NUM
     */
    private void initialize() {
        portfolioCount = 0;
        transactionCount = 0;
        errorCount = 0;
        totalGenerated = 0;

        loadConfiguration();
        loadRandomSeed();

        random = new Random(randomSeed);

        LOGGER.info(PROGRAM_ID + " - Initialized with seed: " + randomSeed
                + " portfolios: " + numPortfolios);
    }

    /**
     * Loads test configuration from config file.
     */
    private void loadConfiguration() {
        try (FileHandler configFile = new FileHandler(configFilePath)) {
            if (!FileHandler.STATUS_SUCCESS.equals(configFile.openInput())) {
                LOGGER.info("No config file; using defaults");
                return;
            }

            String line;
            while ((line = configFile.readLine()) != null) {
                if (line.startsWith("NUM_PORTFOLIOS=")) {
                    numPortfolios = Integer.parseInt(line.substring(15).trim());
                } else if (line.startsWith("TRANS_PER_PORT=")) {
                    transactionsPerPortfolio = Integer.parseInt(line.substring(15).trim());
                } else if (line.startsWith("VOLUME_MULT=")) {
                    volumeMultiplier = Integer.parseInt(line.substring(12).trim());
                } else if (line.startsWith("RANDOM_SEED=")) {
                    randomSeed = Long.parseLong(line.substring(12).trim());
                }
            }
        } catch (Exception e) {
            LOGGER.info("Using default configuration");
        }
    }

    /**
     * Loads random seed from seed file.
     * Maps to COBOL FUNCTION RANDOM(seed-value).
     */
    private void loadRandomSeed() {
        try (FileHandler seedFile = new FileHandler(seedFilePath)) {
            if (FileHandler.STATUS_SUCCESS.equals(seedFile.openInput())) {
                String line = seedFile.readLine();
                if (line != null && !line.trim().isEmpty()) {
                    randomSeed = Long.parseLong(line.trim());
                }
            }
        } catch (Exception e) {
            LOGGER.info("Using default random seed: " + randomSeed);
        }
    }

    /**
     * 2000-GENERATE-PORTFOLIOS: Generate portfolio master test records.
     *
     * For each portfolio generates:
     * - Portfolio ID (PORT + sequential number)
     * - Account number (ACCT + sequential number)
     * - Client name, type, dates
     * - Initial positions with random investments
     */
    private void generatePortfolios() {
        LOGGER.info("Generating " + numPortfolios + " portfolio records");

        try (FileHandler portFile = new FileHandler(portfolioOutputPath)) {
            portFile.openOutput();

            for (int i = 1; i <= numPortfolios; i++) {
                // Generate portfolio header
                String portfolioId = String.format("PORT%04d", i);
                String accountNo = String.format("ACCT%06d", i);
                String clientType = CLIENT_TYPES[random.nextInt(CLIENT_TYPES.length)];
                String currency = CURRENCIES[random.nextInt(CURRENCIES.length)];
                LocalDate createDate = LocalDate.now().minusDays(random.nextInt(365 * 5));

                // Generate 1-5 positions per portfolio
                int numPositions = 1 + random.nextInt(5);
                for (int p = 0; p < numPositions; p++) {
                    String investmentId = generateInvestmentId();
                    BigDecimal quantity = generateQuantity();
                    BigDecimal price = generatePrice();
                    BigDecimal costBasis = quantity.multiply(price)
                            .setScale(2, RoundingMode.HALF_UP);
                    BigDecimal marketValue = costBasis.multiply(
                            BigDecimal.ONE.add(generateGainLossPercent()))
                            .setScale(2, RoundingMode.HALF_UP);

                    String record = String.format("%-8s%-8s%-10s%15s%15s%15s%-3s%c",
                            portfolioId,
                            createDate.format(DATE_FMT),
                            investmentId,
                            quantity,
                            costBasis,
                            marketValue,
                            currency,
                            'A');

                    portFile.writeLine(record);
                    portfolioCount++;
                    totalGenerated++;
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E200", "Error generating portfolios", e);
            returnCode.setCode(ReturnCode.ERROR);
        }

        LOGGER.info("Generated " + portfolioCount + " portfolio position records");
    }

    /**
     * 3000-GENERATE-TRANSACTIONS: Generate transaction test records.
     */
    private void generateTransactions() {
        LOGGER.info("Generating transactions for " + numPortfolios + " portfolios");

        try (FileHandler trnFile = new FileHandler(transactionOutputPath)) {
            trnFile.openOutput();

            for (int i = 1; i <= numPortfolios; i++) {
                String portfolioId = String.format("PORT%04d", i);

                for (int t = 0; t < transactionsPerPortfolio; t++) {
                    String txnType = TRANSACTION_TYPES[random.nextInt(TRANSACTION_TYPES.length)];
                    LocalDate txnDate = LocalDate.now().minusDays(random.nextInt(90));
                    String txnTime = String.format("%02d%02d%02d",
                            random.nextInt(24), random.nextInt(60), random.nextInt(60));
                    String seqNo = String.format("%06d", t + 1);
                    String investmentId = generateInvestmentId();
                    BigDecimal quantity = generateQuantity();
                    BigDecimal price = generatePrice();
                    BigDecimal amount = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
                    String currency = CURRENCIES[random.nextInt(CURRENCIES.length)];

                    String record = String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                            txnDate.format(DATE_FMT),
                            txnTime,
                            portfolioId,
                            seqNo,
                            investmentId,
                            txnType,
                            quantity,
                            price,
                            amount,
                            currency,
                            'P');

                    trnFile.writeLine(record);
                    transactionCount++;
                    totalGenerated++;
                }
            }
        } catch (Exception e) {
            errorHandler.handleSystemError("E300", "Error generating transactions", e);
            returnCode.setCode(ReturnCode.ERROR);
        }

        LOGGER.info("Generated " + transactionCount + " transaction records");
    }

    /**
     * 4000-GENERATE-ERRORS: Generate error condition test data.
     *
     * Creates intentionally malformed records to test error handling:
     * - Invalid dates
     * - Missing required fields
     * - Out-of-range amounts
     * - Invalid transaction types
     * - Negative quantities for sells exceeding balance
     */
    private void generateErrors() {
        LOGGER.info("Generating error condition test data");

        try (FileHandler trnFile = new FileHandler(transactionOutputPath)) {
            trnFile.openOutput();

            // Error: Future date
            trnFile.writeLine(String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                    LocalDate.now().plusDays(30).format(DATE_FMT),
                    "120000", "PORT0001", "000001", "STK00001  ", "BU",
                    "100.0000", "50.0000", "5000.00", "USD", 'P'));
            errorCount++;

            // Error: Invalid transaction type
            trnFile.writeLine(String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                    LocalDate.now().format(DATE_FMT),
                    "120000", "PORT0001", "000002", "STK00001  ", "XX",
                    "100.0000", "50.0000", "5000.00", "USD", 'P'));
            errorCount++;

            // Error: Zero quantity for buy
            trnFile.writeLine(String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                    LocalDate.now().format(DATE_FMT),
                    "120000", "PORT0001", "000003", "STK00001  ", "BU",
                    "0.0000", "50.0000", "0.00", "USD", 'P'));
            errorCount++;

            // Error: Negative price
            trnFile.writeLine(String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                    LocalDate.now().format(DATE_FMT),
                    "120000", "PORT0001", "000004", "STK00001  ", "BU",
                    "100.0000", "-50.0000", "-5000.00", "USD", 'P'));
            errorCount++;

            // Error: Missing portfolio ID
            trnFile.writeLine(String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                    LocalDate.now().format(DATE_FMT),
                    "120000", "        ", "000005", "STK00001  ", "BU",
                    "100.0000", "50.0000", "5000.00", "USD", 'P'));
            errorCount++;

            // Error: Missing investment ID
            trnFile.writeLine(String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                    LocalDate.now().format(DATE_FMT),
                    "120000", "PORT0001", "000006", "          ", "BU",
                    "100.0000", "50.0000", "5000.00", "USD", 'P'));
            errorCount++;

            // Error: Zero amount for fee
            trnFile.writeLine(String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                    LocalDate.now().format(DATE_FMT),
                    "120000", "PORT0001", "000007", "STK00001  ", "FE",
                    "0.0000", "0.0000", "0.00", "USD", 'P'));
            errorCount++;

            // Error: Truncated record
            trnFile.writeLine("20260101120000PORT");
            errorCount++;

            // Error: Invalid date format
            trnFile.writeLine(String.format("%-8s%-6s%-8s%-6s%-10s%-2s%15s%15s%15s%-3s%c",
                    "99991301",
                    "120000", "PORT0001", "000009", "STK00001  ", "BU",
                    "100.0000", "50.0000", "5000.00", "USD", 'P'));
            errorCount++;

            totalGenerated += errorCount;
        } catch (Exception e) {
            errorHandler.handleSystemError("E400", "Error generating error test data", e);
            returnCode.setCode(ReturnCode.ERROR);
        }

        LOGGER.info("Generated " + errorCount + " error condition records");
    }

    /**
     * 5000-GENERATE-VOLUME: Generate high-volume test data.
     *
     * Generates volumeMultiplier * normal volume of data
     * for performance and stress testing.
     */
    private void generateVolume() {
        LOGGER.info("Generating volume test data (multiplier: " + volumeMultiplier + "x)");

        int originalPortfolios = numPortfolios;
        int originalTrans = transactionsPerPortfolio;

        numPortfolios = originalPortfolios * volumeMultiplier;
        transactionsPerPortfolio = originalTrans;

        generatePortfolios();
        generateTransactions();

        // Restore original values
        numPortfolios = originalPortfolios;
        transactionsPerPortfolio = originalTrans;

        LOGGER.info("Volume generation complete: " + totalGenerated + " total records");
    }

    /**
     * 9000-TERMINATE: Display generation statistics.
     */
    private void terminate() {
        displayStatistics();
    }

    // --- Random data generation helpers ---

    private String generateInvestmentId() {
        String prefix = INVESTMENT_PREFIXES[random.nextInt(INVESTMENT_PREFIXES.length)];
        return String.format("%s%05d  ", prefix, random.nextInt(10000));
    }

    private BigDecimal generateQuantity() {
        return BigDecimal.valueOf(1 + random.nextInt(10000))
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal generatePrice() {
        return BigDecimal.valueOf(1.0 + random.nextDouble() * 999.0)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal generateGainLossPercent() {
        // Generate gain/loss between -30% and +50%
        return BigDecimal.valueOf(-0.30 + random.nextDouble() * 0.80)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private void displayStatistics() {
        LOGGER.info(PROGRAM_ID + " Generation Statistics:");
        LOGGER.info("  Portfolio Records: " + portfolioCount);
        LOGGER.info("  Transaction Records: " + transactionCount);
        LOGGER.info("  Error Records: " + errorCount);
        LOGGER.info("  Total Generated: " + totalGenerated);
        LOGGER.info("  Random Seed: " + randomSeed);
        LOGGER.info("  Return Code: " + returnCode.getCurrentCode());
    }
}

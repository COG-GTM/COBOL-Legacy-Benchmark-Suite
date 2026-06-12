package com.clbs.portfolio.testdata;

import com.clbs.common.cobol.CobolField;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic golden test-data generator (Phase 0, task 0.7).
 *
 * <p>{@code TSTGEN00.cbl} is a non-runnable skeleton — it {@code PERFORM}s eight
 * paragraphs (2210/2220, 2310/2320, 2410/2420, 2510/2520) that are never
 * defined, so it cannot produce data. This class reproduces the intent of
 * TSTGEN00 (portfolio + transaction fixtures) deterministically, emitting
 * fixed-width records in the exact copybook layout so both Java tests and the
 * parallel-run comparison framework can consume them.
 *
 * <p>Numeric COMP-3 fields are rendered as zoned DISPLAY digits (implied decimal,
 * non-negative) per {@link CobolField}; see {@code java/docs/field-mappings.md}.
 */
public final class GoldenFixtures {

    public static final int PORTFOLIO_RECORD_LENGTH = 162;
    public static final int TRANSACTION_RECORD_LENGTH = 173;

    private static final String[] CLIENT_NAMES = {
            "GROWTH PORTFOLIO", "INCOME PORTFOLIO", "BALANCED PORTFOLIO",
            "AGGRESSIVE FUND", "CONSERVATIVE FUND"
    };
    private static final String[] CLIENT_TYPES = {"I", "C", "T"};
    private static final String[] INVESTMENTS = {
            "AAPL000001", "MSFT000001", "IBM0000001", "GOOG000001", "TSLA000001"
    };
    private static final String[] TRN_TYPES = {"BU", "SL", "TR", "FE"};

    private GoldenFixtures() {
    }

    /** A generated portfolio fixture row with both its fields and fixed-width line. */
    public record PortfolioFixture(
            String portId, String accountNo, String clientName, String clientType,
            int createDate, int lastMaint, String status, BigDecimal totalValue,
            BigDecimal cashBalance, String lastUser, int lastTrans, String filler) {

        public String toFixedWidth() {
            return CobolField.alphanumeric(portId, 8)
                    + CobolField.alphanumeric(accountNo, 10)
                    + CobolField.alphanumeric(clientName, 30)
                    + CobolField.alphanumeric(clientType, 1)
                    + CobolField.integer(createDate, 8)
                    + CobolField.integer(lastMaint, 8)
                    + CobolField.alphanumeric(status, 1)
                    + CobolField.numeric(totalValue, 13, 2)
                    + CobolField.numeric(cashBalance, 13, 2)
                    + CobolField.alphanumeric(lastUser, 8)
                    + CobolField.integer(lastTrans, 8)
                    + CobolField.alphanumeric(filler, 50);
        }
    }

    /** A generated transaction fixture row with both its fields and fixed-width line. */
    public record TransactionFixture(
            String trnDate, String trnTime, String portfolioId, String sequenceNo,
            String investmentId, String type, BigDecimal quantity, BigDecimal price,
            BigDecimal amount, String currency, String status, String processDate,
            String processUser, String filler) {

        public String toFixedWidth() {
            return CobolField.alphanumeric(trnDate, 8)
                    + CobolField.alphanumeric(trnTime, 6)
                    + CobolField.alphanumeric(portfolioId, 8)
                    + CobolField.alphanumeric(sequenceNo, 6)
                    + CobolField.alphanumeric(investmentId, 10)
                    + CobolField.alphanumeric(type, 2)
                    + CobolField.numeric(quantity, 11, 4)
                    + CobolField.numeric(price, 11, 4)
                    + CobolField.numeric(amount, 13, 2)
                    + CobolField.alphanumeric(currency, 3)
                    + CobolField.alphanumeric(status, 1)
                    + CobolField.alphanumeric(processDate, 26)
                    + CobolField.alphanumeric(processUser, 8)
                    + CobolField.alphanumeric(filler, 50);
        }
    }

    public static List<PortfolioFixture> portfolios(int count) {
        List<PortfolioFixture> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            rows.add(new PortfolioFixture(
                    String.format("PORT%04d", i),
                    String.format("ACCT%06d", i),
                    CLIENT_NAMES[(i - 1) % CLIENT_NAMES.length],
                    CLIENT_TYPES[(i - 1) % CLIENT_TYPES.length],
                    20240320,
                    20240321,
                    "A",
                    new BigDecimal("12345678.99").multiply(BigDecimal.valueOf(i)).setScale(2),
                    new BigDecimal("1000000.00").multiply(BigDecimal.valueOf(i)).setScale(2),
                    "TSTGEN00",
                    20240321,
                    ""));
        }
        return rows;
    }

    public static List<TransactionFixture> transactions(int count) {
        List<TransactionFixture> rows = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BigDecimal quantity = BigDecimal.valueOf(100L * i).setScale(4);
            BigDecimal price = new BigDecimal("10.5000").multiply(BigDecimal.valueOf(i)).setScale(4);
            BigDecimal amount = quantity.multiply(price).setScale(2);
            rows.add(new TransactionFixture(
                    "20240320",
                    String.format("1530%02d", i),
                    String.format("PORT%04d", i),
                    String.format("%06d", i),
                    INVESTMENTS[(i - 1) % INVESTMENTS.length],
                    TRN_TYPES[(i - 1) % TRN_TYPES.length],
                    quantity,
                    price,
                    amount,
                    "USD",
                    "D",
                    "2024-03-20-15.30.45.123456",
                    "TSTGEN00",
                    ""));
        }
        return rows;
    }

    public static String portfolioFile(int count) {
        StringBuilder sb = new StringBuilder();
        for (PortfolioFixture row : portfolios(count)) {
            sb.append(row.toFixedWidth()).append('\n');
        }
        return sb.toString();
    }

    public static String transactionFile(int count) {
        StringBuilder sb = new StringBuilder();
        for (TransactionFixture row : transactions(count)) {
            sb.append(row.toFixedWidth()).append('\n');
        }
        return sb.toString();
    }

    /** Regenerates the committed fixtures into the given resources directory. */
    public static void main(String[] args) throws IOException {
        Path dir = Path.of(args.length > 0 ? args[0] : "src/test/resources/fixtures");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("portfolio.dat"), portfolioFile(5), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("transaction.dat"), transactionFile(5), StandardCharsets.UTF_8);
        System.out.println("Wrote fixtures to " + dir.toAbsolutePath());
    }
}

package com.clbs.demo;

import com.clbs.batch.HistoryLoadProcessor;
import com.clbs.model.TransactionHistoryRecord;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Runnable demo of the COBOL HISTLD00 migration to Java.
 * 
 * This demo:
 * 1. Creates an H2 in-memory database (simulating DB2)
 * 2. Generates sample transaction history records (simulating VSAM input)
 * 3. Runs the HistoryLoadProcessor batch job
 * 4. Displays the results
 * 
 * Run with: java -jar cobol-migration-demo-1.0.0.jar
 * 
 * The output shows the same processing flow as the original COBOL program:
 * - Initialize (open files, connect to DB2, init checkpoints)
 * - Process loop (read record, load to DB2, check commit threshold)
 * - Terminate (final commit, display statistics)
 */
public class HistoryLoadDemo {

    private static final String DB_URL = "jdbc:h2:mem:poshist;DB_CLOSE_DELAY=-1";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║     COBOL to Java Migration Demo - HISTLD00 Batch Program      ║");
        System.out.println("║     Position History DB2 Load Program                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝");
        System.out.println();

        int recordCount = 100;
        if (args.length > 0) {
            try {
                recordCount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Usage: java -jar cobol-migration-demo.jar [record_count]");
                System.out.println("Using default: " + recordCount);
            }
        }

        try {
            Connection connection = setupDatabase();
            
            System.out.println("┌────────────────────────────────────────────────────────────────┐");
            System.out.println("│ PHASE 1: Generating Test Data (simulating VSAM input file)    │");
            System.out.println("└────────────────────────────────────────────────────────────────┘");
            List<TransactionHistoryRecord> testData = generateTestData(recordCount);
            System.out.println("  Generated " + testData.size() + " transaction history records");
            System.out.println();

            System.out.println("┌────────────────────────────────────────────────────────────────┐");
            System.out.println("│ PHASE 2: Running HISTLD00 Batch Process                       │");
            System.out.println("│ (This is the Java translation of the COBOL program)           │");
            System.out.println("└────────────────────────────────────────────────────────────────┘");
            System.out.println();
            
            HistoryLoadProcessor processor = new HistoryLoadProcessor(connection);
            int returnCode = processor.execute(testData.iterator());
            
            System.out.println();
            System.out.println("┌────────────────────────────────────────────────────────────────┐");
            System.out.println("│ PHASE 3: Verifying Results (querying DB2/H2 database)         │");
            System.out.println("└────────────────────────────────────────────────────────────────┘");
            displayResults(connection);

            System.out.println();
            System.out.println("┌────────────────────────────────────────────────────────────────┐");
            System.out.println("│ BATCH JOB COMPLETE                                            │");
            System.out.println("├────────────────────────────────────────────────────────────────┤");
            System.out.printf("│ Return Code: %-4d (COBOL equivalent: MOVE %d TO RETURN-CODE)  │%n", 
                returnCode, returnCode);
            System.out.println("└────────────────────────────────────────────────────────────────┘");

            connection.close();

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            System.exit(12);
        }
    }

    private static Connection setupDatabase() throws SQLException {
        System.out.println("┌────────────────────────────────────────────────────────────────┐");
        System.out.println("│ INITIALIZATION: Setting up H2 database (simulating DB2)       │");
        System.out.println("└────────────────────────────────────────────────────────────────┘");
        
        Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        connection.setAutoCommit(false);

        String createTableSQL = """
            CREATE TABLE POSHIST (
                ACCOUNT_NO VARCHAR(8),
                PORTFOLIO_ID VARCHAR(10),
                TRANS_DATE DATE,
                TRANS_TIME TIME,
                TRANS_TYPE VARCHAR(2),
                SECURITY_ID VARCHAR(12),
                QUANTITY DECIMAL(15,3),
                PRICE DECIMAL(15,3),
                AMOUNT DECIMAL(15,2),
                FEES DECIMAL(15,2),
                TOTAL_AMOUNT DECIMAL(15,2),
                COST_BASIS DECIMAL(15,2),
                GAIN_LOSS DECIMAL(15,2),
                PROCESS_DATE DATE,
                PROCESS_TIME TIME,
                PROGRAM_ID VARCHAR(8),
                USER_ID VARCHAR(8),
                AUDIT_TIMESTAMP TIMESTAMP,
                PRIMARY KEY (ACCOUNT_NO, PORTFOLIO_ID, TRANS_DATE, TRANS_TIME)
            )
            """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
        }
        connection.commit();

        System.out.println("  Created POSHIST table (equivalent to DB2 table definition)");
        System.out.println();
        
        return connection;
    }

    private static List<TransactionHistoryRecord> generateTestData(int count) {
        List<TransactionHistoryRecord> records = new ArrayList<>();
        Random random = new Random(42);
        
        String[] portfolioIds = {"PORT00001", "PORT00002", "PORT00003", "PORT00004", "PORT00005"};
        String[] accountNos = {"ACCT0001", "ACCT0002", "ACCT0003", "ACCT0004", "ACCT0005"};
        String[] securityIds = {"IBM", "AAPL", "MSFT", "GOOGL", "AMZN", "META", "NVDA", "TSLA"};
        String[] transTypes = {"BY", "SL", "DV", "IN"};

        for (int i = 0; i < count; i++) {
            TransactionHistoryRecord record = new TransactionHistoryRecord();
            
            record.setPortfolioId(portfolioIds[random.nextInt(portfolioIds.length)]);
            record.setAccountNo(accountNos[random.nextInt(accountNos.length)]);
            record.setSecurityId(securityIds[random.nextInt(securityIds.length)]);
            record.setTransType(transTypes[random.nextInt(transTypes.length)]);
            
            int year = 2024;
            int month = random.nextInt(12) + 1;
            int day = random.nextInt(28) + 1;
            record.setHistDate(String.format("%04d%02d%02d", year, month, day));
            
            int hour = random.nextInt(8) + 9;
            int minute = random.nextInt(60);
            int second = random.nextInt(60);
            record.setHistTime(String.format("%02d%02d%02d", hour, minute, second));
            
            record.setSeqNo(String.format("%04d", i + 1));
            
            BigDecimal quantity = BigDecimal.valueOf(random.nextInt(1000) + 1);
            BigDecimal price = BigDecimal.valueOf(random.nextDouble() * 500 + 10)
                .setScale(3, java.math.RoundingMode.HALF_UP);
            BigDecimal amount = quantity.multiply(price).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal fees = amount.multiply(BigDecimal.valueOf(0.001))
                .setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal totalAmount = amount.add(fees);
            BigDecimal costBasis = amount.multiply(BigDecimal.valueOf(0.95))
                .setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal gainLoss = amount.subtract(costBasis);

            record.setQuantity(quantity);
            record.setPrice(price);
            record.setAmount(amount);
            record.setFees(fees);
            record.setTotalAmount(totalAmount);
            record.setCostBasis(costBasis);
            record.setGainLoss(gainLoss);
            
            record.setRecordType(TransactionHistoryRecord.RecordType.TRANSACTION);
            record.setActionCode(TransactionHistoryRecord.ActionCode.ADD);
            record.setProcessDate(LocalDateTime.now());
            record.setProcessUser("BATCH");

            records.add(record);
        }

        return records;
    }

    private static void displayResults(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM POSHIST");
            rs.next();
            int totalRecords = rs.getInt(1);
            System.out.println("  Total records in POSHIST table: " + totalRecords);

            System.out.println();
            System.out.println("  Records by Portfolio:");
            rs = stmt.executeQuery(
                "SELECT PORTFOLIO_ID, COUNT(*) as CNT, SUM(TOTAL_AMOUNT) as TOTAL " +
                "FROM POSHIST GROUP BY PORTFOLIO_ID ORDER BY PORTFOLIO_ID");
            System.out.println("  ┌────────────┬───────┬──────────────────┐");
            System.out.println("  │ Portfolio  │ Count │ Total Amount     │");
            System.out.println("  ├────────────┼───────┼──────────────────┤");
            while (rs.next()) {
                System.out.printf("  │ %-10s │ %5d │ %16.2f │%n",
                    rs.getString("PORTFOLIO_ID"),
                    rs.getInt("CNT"),
                    rs.getBigDecimal("TOTAL"));
            }
            System.out.println("  └────────────┴───────┴──────────────────┘");

            System.out.println();
            System.out.println("  Sample records (first 5):");
            rs = stmt.executeQuery(
                "SELECT ACCOUNT_NO, PORTFOLIO_ID, SECURITY_ID, TRANS_TYPE, QUANTITY, PRICE, AMOUNT " +
                "FROM POSHIST LIMIT 5");
            System.out.println("  ┌──────────┬────────────┬──────────┬──────┬──────────┬───────────┬──────────────┐");
            System.out.println("  │ Account  │ Portfolio  │ Security │ Type │ Quantity │ Price     │ Amount       │");
            System.out.println("  ├──────────┼────────────┼──────────┼──────┼──────────┼───────────┼──────────────┤");
            while (rs.next()) {
                System.out.printf("  │ %-8s │ %-10s │ %-8s │ %-4s │ %8.0f │ %9.3f │ %12.2f │%n",
                    rs.getString("ACCOUNT_NO"),
                    rs.getString("PORTFOLIO_ID"),
                    rs.getString("SECURITY_ID"),
                    rs.getString("TRANS_TYPE"),
                    rs.getBigDecimal("QUANTITY"),
                    rs.getBigDecimal("PRICE"),
                    rs.getBigDecimal("AMOUNT"));
            }
            System.out.println("  └──────────┴────────────┴──────────┴──────┴──────────┴───────────┴──────────────┘");
        }
    }
}

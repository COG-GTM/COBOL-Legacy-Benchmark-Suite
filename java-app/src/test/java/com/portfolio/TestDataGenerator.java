package com.portfolio;

import com.portfolio.model.*;
import com.portfolio.model.enums.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Test Data Generator.
 * Replaces: TSTGEN00.cbl - Generates deterministic test data for
 * portfolios, positions, and transactions.
 *
 * TSTGEN00 creates synthetic data per the pattern:
 * - Portfolio IDs: PORT0001 through PORT0010
 * - Investment IDs: INV0000001 through INV0000050
 * - Transactions: 100 transactions across all portfolios
 */
@TestConfiguration
public class TestDataGenerator {

    @Bean
    public List<Portfolio> testPortfolios() {
        return generatePortfolios();
    }

    @Bean
    public List<TransactionHistory> testTransactions() {
        return generateTransactions();
    }

    public static List<Portfolio> generatePortfolios() {
        List<Portfolio> portfolios = new ArrayList<>();
        String[] statuses = {"A", "A", "A", "A", "A", "A", "A", "C", "S", "A"};
        String[] branches = {"BR01", "BR01", "BR02", "BR02", "BR03",
                "BR03", "BR04", "BR04", "BR05", "BR05"};

        for (int i = 1; i <= 10; i++) {
            Portfolio p = new Portfolio();
            p.setPortfolioId(String.format("PORT%04d", i));
            p.setAccountType("IN");
            p.setBranchId(branches[i - 1].substring(2, 4));
            p.setClientId(String.format("CLIENT%04d", i));
            p.setPortfolioName("Test Portfolio " + i);
            p.setCurrencyCode("USD");
            p.setRiskLevel(i <= 3 ? "1" : (i <= 7 ? "3" : "5"));
            p.setStatus(statuses[i - 1]);
            p.setOpenDate(LocalDate.of(2020, 1, i));
            if ("C".equals(statuses[i - 1])) {
                p.setCloseDate(LocalDate.of(2024, 6, 15));
            }
            p.setLastMaintDate(LocalDateTime.now());
            p.setLastMaintUser("TESTGEN");
            portfolios.add(p);
        }
        return portfolios;
    }

    public static List<InvestmentPosition> generatePositions() {
        List<InvestmentPosition> positions = new ArrayList<>();
        String[] fundNames = {"US Large Cap Fund", "US Small Cap Fund",
                "International Equity", "Bond Index Fund", "Money Market Fund"};

        for (int portIdx = 1; portIdx <= 10; portIdx++) {
            String portfolioId = String.format("PORT%04d", portIdx);
            for (int invIdx = 1; invIdx <= 5; invIdx++) {
                InvestmentPosition pos = new InvestmentPosition();
                InvestmentPositionKey key = new InvestmentPositionKey();
                key.setPortfolioId(portfolioId);
                key.setInvestmentId(String.format("INV%07d", (portIdx - 1) * 5 + invIdx));
                key.setPositionDate(LocalDate.now());
                pos.setKey(key);
                pos.setInvestmentName(fundNames[invIdx - 1]);
                pos.setQuantity(new BigDecimal(100 * invIdx + ".0000"));
                pos.setCostBasis(new BigDecimal(1000 * invIdx + ".00"));
                pos.setMarketValue(new BigDecimal(1050 * invIdx + ".00"));
                pos.setCurrencyCode("USD");
                pos.setStatus("A");
                pos.setLastMaintDate(LocalDateTime.now());
                pos.setLastMaintUser("TESTGEN");
                pos.setLastActivityDate(LocalDate.now().toString().replace("-", ""));
                positions.add(pos);
            }
        }
        return positions;
    }

    public static List<TransactionHistory> generateTransactions() {
        List<TransactionHistory> transactions = new ArrayList<>();
        String[] types = {"BU", "SL", "BU", "FE", "TR"};

        for (int i = 1; i <= 100; i++) {
            TransactionHistory txn = new TransactionHistory();
            txn.setTransactionId(String.format("TXN%08d", i));
            txn.setPortfolioId(String.format("PORT%04d", ((i - 1) % 10) + 1));
            txn.setInvestmentId(String.format("INV%07d", ((i - 1) % 50) + 1));
            txn.setTransactionType(types[(i - 1) % 5]);
            txn.setTransactionDate(LocalDate.of(2024, ((i - 1) % 12) + 1,
                    ((i - 1) % 28) + 1));
            txn.setTransactionTime(LocalTime.of(10, 0, 0));
            txn.setQuantity(new BigDecimal(10 * ((i % 20) + 1) + ".0000"));
            txn.setPrice(new BigDecimal(25 * ((i % 10) + 1) + ".00"));
            txn.setAmount(txn.getQuantity().multiply(txn.getPrice()));
            txn.setCurrencyCode("USD");
            txn.setStatus(TransactionStatus.PROCESSED.getCode());
            txn.setProcessDate(LocalDateTime.now());
            txn.setProcessUser("TESTGEN");
            transactions.add(txn);
        }
        return transactions;
    }

    public static List<BatchControlRecord> generateBatchRecords() {
        List<BatchControlRecord> records = new ArrayList<>();
        String[] jobNames = {"TRNVAL00", "POSUPD00", "HISTLD00",
                "RPTPOS00", "RPTAUD00", "RPTSTA00"};
        String processDate = LocalDate.now().toString().replace("-", "");

        for (int i = 0; i < jobNames.length; i++) {
            BatchControlRecord record = new BatchControlRecord();
            record.setKey(new BatchControlKey(jobNames[i], processDate, i + 1));
            record.setStatus(BatchStatus.DONE.getCode());
            record.setStartTime("08:00:0" + i);
            record.setEndTime("08:30:0" + i);
            record.setReturnCode(0);
            record.setPrereqCount(i > 0 ? 1 : 0);
            record.setRestartCount(0);
            record.setAttemptTs(LocalDateTime.now().minusHours(1));
            record.setCompleteTs(LocalDateTime.now());
            records.add(record);
        }
        return records;
    }
}

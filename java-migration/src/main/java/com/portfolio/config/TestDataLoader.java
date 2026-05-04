package com.portfolio.config;

import com.portfolio.entity.*;
import com.portfolio.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TestDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TestDataLoader.class);
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;
    private final Random random = new Random(42);
    private final AtomicLong txSequence = new AtomicLong(1);

    private static final String[] CLIENT_NAMES = {
            "Acme Corp", "Global Investments", "Tech Ventures",
            "Blue Horizon Capital", "Pacific Trust", "Summit Holdings",
            "Meridian Partners", "Atlas Financial", "Pinnacle Group", "Vanguard Trust"
    };
    private static final String[] INVESTMENT_IDS = {
            "AAPL", "GOOGL", "MSFT", "AMZN", "TSLA",
            "META", "NVDA", "JPM", "V", "JNJ"
    };
    private static final String[] TXN_TYPES = {"BU", "SL", "BU", "BU", "FE"};

    public TestDataLoader(PortfolioRepository portfolioRepository,
                          PositionRepository positionRepository,
                          TransactionRepository transactionRepository,
                          AuditLogRepository auditLogRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void run(String... args) {
        if (portfolioRepository.count() > 0) {
            log.info("Data already loaded, skipping seed.");
            return;
        }
        log.info("Loading test data (translated from TSTGEN00.cbl)...");
        generatePortfolios();
        generateTransactions();
        generatePositions();
        logStartupAudit();
        log.info("Test data loading complete. Portfolios: {}, Transactions: {}, Positions: {}",
                portfolioRepository.count(), transactionRepository.count(), positionRepository.count());
    }

    private void generatePortfolios() {
        for (int i = 1; i <= 10; i++) {
            Portfolio p = new Portfolio();
            p.setPortfolioId(String.format("PORT%04d", i));
            p.setAccountNo(String.format("ACCT%06d", i));
            p.setAccountType(i % 3 == 0 ? "RA" : "TX");
            p.setBranchId(String.format("%02d", (i % 5) + 1));
            p.setClientId(String.format("CLT%07d", i));
            p.setClientName(CLIENT_NAMES[i - 1]);
            p.setClientType(i <= 5 ? "I" : (i <= 8 ? "C" : "T"));
            p.setPortfolioName(CLIENT_NAMES[i - 1] + " Portfolio");
            p.setCurrencyCode("USD");
            p.setRiskLevel(i <= 3 ? "L" : (i <= 7 ? "M" : "H"));
            p.setStatus("A");
            p.setOpenDate(LocalDate.now().minusDays(random.nextInt(365) + 30));
            p.setTotalValue(BigDecimal.valueOf(random.nextDouble() * 1000000 + 10000)
                    .setScale(2, RoundingMode.HALF_UP));
            p.setCashBalance(BigDecimal.valueOf(random.nextDouble() * 50000 + 1000)
                    .setScale(2, RoundingMode.HALF_UP));
            p.setLastMaintDate(LocalDateTime.now());
            p.setLastMaintUser("TSTGEN00");
            portfolioRepository.save(p);
        }
    }

    private void generateTransactions() {
        for (int i = 1; i <= 10; i++) {
            String portfolioId = String.format("PORT%04d", i);
            for (int j = 0; j < 5; j++) {
                TransactionRecord t = new TransactionRecord();
                String datePart = LocalDateTime.now().minusDays(j)
                        .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                t.setTransactionId(datePart + String.format("%06d", txSequence.getAndIncrement()));
                t.setPortfolioId(portfolioId);
                t.setTransactionDate(LocalDate.now().minusDays(j));
                t.setTransactionTime(LocalTime.of(9 + random.nextInt(8), random.nextInt(60)));
                t.setInvestmentId(INVESTMENT_IDS[random.nextInt(INVESTMENT_IDS.length)]);
                t.setTransactionType(TXN_TYPES[random.nextInt(TXN_TYPES.length)]);
                BigDecimal qty = BigDecimal.valueOf(random.nextInt(1000) + 10)
                        .setScale(4, RoundingMode.HALF_UP);
                BigDecimal price = BigDecimal.valueOf(random.nextDouble() * 500 + 10)
                        .setScale(4, RoundingMode.HALF_UP);
                t.setQuantity(qty);
                t.setPrice(price);
                t.setAmount(qty.multiply(price).setScale(2, RoundingMode.HALF_UP));
                t.setCurrencyCode("USD");
                t.setStatus("C");
                t.setProcessDate(LocalDateTime.now().minusDays(j));
                t.setProcessUser("TSTGEN00");
                transactionRepository.save(t);
            }
        }
    }

    private void generatePositions() {
        for (int i = 1; i <= 10; i++) {
            String portfolioId = String.format("PORT%04d", i);
            for (int j = 0; j < 3; j++) {
                PositionRecord pos = new PositionRecord();
                pos.setPortfolioId(portfolioId);
                pos.setInvestmentId(INVESTMENT_IDS[j]);
                pos.setPositionDate(LocalDate.now());
                BigDecimal qty = BigDecimal.valueOf(random.nextInt(5000) + 100)
                        .setScale(4, RoundingMode.HALF_UP);
                BigDecimal price = BigDecimal.valueOf(random.nextDouble() * 500 + 10)
                        .setScale(4, RoundingMode.HALF_UP);
                pos.setQuantity(qty);
                pos.setCostBasis(qty.multiply(price).setScale(2, RoundingMode.HALF_UP));
                BigDecimal marketPrice = price.multiply(
                        BigDecimal.valueOf(0.9 + random.nextDouble() * 0.3))
                        .setScale(4, RoundingMode.HALF_UP);
                pos.setMarketValue(qty.multiply(marketPrice).setScale(2, RoundingMode.HALF_UP));
                pos.setCurrencyCode("USD");
                pos.setStatus("A");
                pos.setLastMaintDate(LocalDateTime.now());
                pos.setLastMaintUser("TSTGEN00");
                positionRepository.save(pos);
            }
        }
    }

    private void logStartupAudit() {
        AuditLog audit = new AuditLog();
        audit.setAuditTimestamp(LocalDateTime.now());
        audit.setSystemId("PORTMGMT");
        audit.setUserId("SYSTEM");
        audit.setProgramName("STARTUP");
        audit.setTerminalId("CONSOLE");
        audit.setAuditType("SYST");
        audit.setAuditAction("STARTUP");
        audit.setAuditStatus("SUCC");
        audit.setMessage("Application started. Test data loaded successfully.");
        auditLogRepository.save(audit);
    }
}

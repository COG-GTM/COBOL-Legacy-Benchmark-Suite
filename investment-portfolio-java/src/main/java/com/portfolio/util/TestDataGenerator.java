package com.portfolio.util;

import com.portfolio.entity.AuditAction;
import com.portfolio.entity.AuditLog;
import com.portfolio.entity.AuditStatus;
import com.portfolio.entity.AuditType;
import com.portfolio.entity.ClientType;
import com.portfolio.entity.InvestmentPosition;
import com.portfolio.entity.PortfolioMaster;
import com.portfolio.entity.PortfolioStatus;
import com.portfolio.entity.PositionStatus;
import com.portfolio.entity.TransactionHistory;
import com.portfolio.entity.TransactionStatus;
import com.portfolio.entity.TransactionType;
import com.portfolio.repository.AuditLogRepository;
import com.portfolio.repository.InvestmentPositionRepository;
import com.portfolio.repository.PortfolioMasterRepository;
import com.portfolio.repository.TransactionHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TestDataGenerator {

    private static final Logger log = LoggerFactory.getLogger(TestDataGenerator.class);
    private static final Random RANDOM = new Random(42);
    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private static final String[] INVESTMENT_IDS = {
            "AAPL", "GOOGL", "MSFT", "AMZN", "META", "TSLA", "NVDA", "JPM", "V", "JNJ"
    };
    private static final String[] CURRENCIES = {"USD", "EUR", "GBP"};
    private static final ClientType[] CLIENT_TYPES = ClientType.values();

    private final PortfolioMasterRepository portfolioRepository;
    private final InvestmentPositionRepository positionRepository;
    private final TransactionHistoryRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    public TestDataGenerator(PortfolioMasterRepository portfolioRepository,
                             InvestmentPositionRepository positionRepository,
                             TransactionHistoryRepository transactionRepository,
                             AuditLogRepository auditLogRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public List<PortfolioMaster> generatePortfolios(int volume) {
        log.info("Generating {} test portfolios", volume);
        List<PortfolioMaster> portfolios = new ArrayList<>();

        for (int i = 0; i < volume; i++) {
            PortfolioMaster portfolio = new PortfolioMaster();
            portfolio.setPortfolioId(String.format("PORT%04d", i + 1));
            portfolio.setAccountNo(String.format("ACC%07d", i + 1));
            portfolio.setClientName("Client " + (i + 1));
            portfolio.setClientType(CLIENT_TYPES[RANDOM.nextInt(CLIENT_TYPES.length)]);
            portfolio.setPortfolioName("Portfolio " + (i + 1));
            portfolio.setStatus(PortfolioStatus.ACTIVE);
            portfolio.setCreateDate(LocalDate.now().minusDays(RANDOM.nextInt(365)));
            portfolio.setOpenDate(portfolio.getCreateDate());
            portfolio.setLastMaintDate(LocalDateTime.now());
            portfolio.setLastUser("TSTGEN");
            portfolio.setTotalValue(randomDecimal(10000, 1000000));
            portfolio.setCashBalance(randomDecimal(1000, 50000));
            portfolio.setAccountType("SA");
            portfolio.setBranchId(String.format("%02d", RANDOM.nextInt(10) + 1));
            portfolio.setClientId(String.format("CLI%07d", i + 1));
            portfolio.setCurrencyCode(CURRENCIES[RANDOM.nextInt(CURRENCIES.length)]);
            portfolio.setRiskLevel(String.valueOf(RANDOM.nextInt(5) + 1));

            portfolios.add(portfolio);
        }

        return portfolioRepository.saveAll(portfolios);
    }

    @Transactional
    public List<TransactionHistory> generateTransactions(int volume) {
        log.info("Generating {} test transactions", volume);
        List<PortfolioMaster> portfolios = portfolioRepository.findAll();
        if (portfolios.isEmpty()) {
            log.warn("No portfolios found. Generate portfolios first.");
            return List.of();
        }

        List<TransactionHistory> transactions = new ArrayList<>();
        TransactionType[] types = TransactionType.values();

        for (int i = 0; i < volume; i++) {
            PortfolioMaster portfolio = portfolios.get(RANDOM.nextInt(portfolios.size()));
            TransactionType type = types[RANDOM.nextInt(types.length)];

            TransactionHistory txn = new TransactionHistory();
            txn.setTransactionId(generateTxnId());
            txn.setPortfolioId(portfolio.getPortfolioId());
            txn.setTransactionDate(LocalDate.now().minusDays(RANDOM.nextInt(30)));
            txn.setTransactionTime(LocalTime.of(RANDOM.nextInt(24), RANDOM.nextInt(60)));
            txn.setInvestmentId(INVESTMENT_IDS[RANDOM.nextInt(INVESTMENT_IDS.length)]);
            txn.setTransactionType(type);
            txn.setQuantity(randomDecimal(1, 1000));
            txn.setPrice(randomDecimal(10, 5000));
            txn.setAmount(txn.getQuantity().multiply(txn.getPrice())
                    .setScale(2, RoundingMode.HALF_UP));
            txn.setCurrency("USD");
            txn.setStatus(TransactionStatus.DONE);
            txn.setProcessDate(LocalDateTime.now());
            txn.setProcessUser("TSTGEN");

            transactions.add(txn);
        }

        return transactionRepository.saveAll(transactions);
    }

    @Transactional
    public void generateErrorData() {
        log.info("Generating test error data");
        // Error data generation handled by ErrorService when errors occur
    }

    @Transactional
    public void generateVolumeData() {
        log.info("Generating volume test data");
        generatePortfolios(100);
        generateTransactions(500);

        List<PortfolioMaster> portfolios = portfolioRepository.findAll();
        List<InvestmentPosition> positions = new ArrayList<>();
        for (PortfolioMaster portfolio : portfolios) {
            for (int j = 0; j < 3; j++) {
                InvestmentPosition pos = new InvestmentPosition();
                pos.setPortfolioId(portfolio.getPortfolioId());
                pos.setInvestmentId(INVESTMENT_IDS[RANDOM.nextInt(INVESTMENT_IDS.length)]);
                pos.setPositionDate(LocalDate.now().minusDays(j));
                pos.setQuantity(randomDecimal(10, 1000));
                pos.setCostBasis(randomDecimal(1000, 100000));
                pos.setMarketValue(randomDecimal(1000, 120000));
                pos.setCurrency("USD");
                pos.setStatus(PositionStatus.ACTIVE);
                pos.setLastMaintDate(LocalDateTime.now());
                pos.setLastMaintUser("TSTGEN");
                positions.add(pos);
            }
        }
        positionRepository.saveAll(positions);

        AuditLog audit = new AuditLog();
        audit.setTimestamp(LocalDateTime.now());
        audit.setSystemId("TESTGEN");
        audit.setUserId("TSTGEN");
        audit.setProgram("TSTGEN");
        audit.setType(AuditType.SYSTEM_EVENT);
        audit.setAction(AuditAction.CREATE);
        audit.setStatus(AuditStatus.SUCCESS);
        audit.setMessage("Volume test data generated");
        auditLogRepository.save(audit);

        log.info("Volume data generation complete");
    }

    private BigDecimal randomDecimal(int min, int max) {
        double value = min + (max - min) * RANDOM.nextDouble();
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateTxnId() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int seq = SEQUENCE.incrementAndGet() % 1000000;
        return datePart + String.format("%06d", seq);
    }
}

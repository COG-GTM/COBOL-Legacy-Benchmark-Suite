package com.portfolio.demo;

import com.portfolio.domain.AppUser;
import com.portfolio.domain.AuthEntry;
import com.portfolio.domain.PortfolioStatus;
import com.portfolio.domain.PositionHistory;
import com.portfolio.domain.PositionHistoryId;
import com.portfolio.domain.PositionStatus;
import com.portfolio.domain.TransactionType;
import com.portfolio.dto.PortfolioDto;
import com.portfolio.dto.TransactionDto;
import com.portfolio.repository.AppUserRepository;
import com.portfolio.repository.AuthEntryRepository;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.PositionHistoryRepository;
import com.portfolio.service.PortfolioService;
import com.portfolio.service.PortfolioTransactionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("demo")
@Order(1)
public class DemoDataLoader implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(DemoDataLoader.class);
  private static final List<String> INVESTMENT_IDS =
      List.of("STK000001", "BND000001", "MMF000001", "ETF000001", "STK000002");
  private static final Map<String, BigDecimal> PRICES =
      Map.of(
          "STK", new BigDecimal("125.5000"),
          "BND", new BigDecimal("98.2500"),
          "MMF", new BigDecimal("1.0000"),
          "ETF", new BigDecimal("210.7500"));

  private final PortfolioRepository portfolioRepository;
  private final AppUserRepository userRepository;
  private final AuthEntryRepository authEntryRepository;
  private final PositionHistoryRepository historyRepository;
  private final PortfolioService portfolioService;
  private final PortfolioTransactionService transactionService;
  private final PasswordEncoder passwordEncoder;

  public DemoDataLoader(
      PortfolioRepository portfolioRepository,
      AppUserRepository userRepository,
      AuthEntryRepository authEntryRepository,
      PositionHistoryRepository historyRepository,
      PortfolioService portfolioService,
      PortfolioTransactionService transactionService,
      PasswordEncoder passwordEncoder) {
    this.portfolioRepository = portfolioRepository;
    this.userRepository = userRepository;
    this.authEntryRepository = authEntryRepository;
    this.historyRepository = historyRepository;
    this.portfolioService = portfolioService;
    this.transactionService = transactionService;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (portfolioRepository.count() > 0) {
      return;
    }
    createUsers();
    LocalDate baseDate = LocalDate.now();
    int positions = 0;
    int processedTransactions = 0;
    int pendingTransactions = 0;
    for (int portfolioNumber = 1; portfolioNumber <= 10; portfolioNumber++) {
      String portfolioId = String.format("PORT%04d", portfolioNumber);
      String accountNo = String.format("%010d", 1_000_000_000L + portfolioNumber);
      PortfolioDto portfolioDto =
          portfolio(portfolioId, accountNo, portfolioNumber, baseDate.minusDays(365));
      portfolioService.create(portfolioDto);
      int positionCount = 3 + portfolioNumber % 3;
      createPositions(portfolioId, positionNumber(portfolioNumber), positionCount, baseDate);
      positions += positionCount;
      for (int transactionNumber = 0; transactionNumber < 5; transactionNumber++) {
        boolean pending = transactionNumber >= 3;
        createTransaction(
            portfolioId,
            positionNumber(portfolioNumber).get(transactionNumber % positionCount),
            portfolioNumber,
            transactionNumber,
            baseDate,
            pending);
        if (pending) {
          pendingTransactions++;
        } else {
          processedTransactions++;
        }
      }
    }
    createHistory(baseDate);
    logger.info(
        "Loaded demo data: portfolios=10 positions={} processedTransactions={} pendingTransactions={} history=10",
        positions,
        processedTransactions,
        pendingTransactions);
  }

  private void createUsers() {
    createUser("admin", "admin123", "ADMIN");
    createUser("operator", "operator123", "OPERATOR");
    createUser("user", "user123", "USER");
    grant("admin", "INQONLN", "READ");
    grant("admin", "PORTMSTR", "READ");
    grant("admin", "PORTMSTR", "UPDATE");
    grant("admin", "BATCH", "EXECUTE");
    grant("operator", "INQONLN", "READ");
    grant("operator", "PORTMSTR", "READ");
    grant("operator", "BATCH", "EXECUTE");
    grant("user", "INQONLN", "READ");
    grant("user", "PORTMSTR", "READ");
  }

  private void createUser(String userId, String password, String role) {
    AppUser user = new AppUser();
    user.setUserId(userId);
    user.setPasswordHash(passwordEncoder.encode(password));
    user.setRole(role);
    user.setEnabled(true);
    userRepository.save(user);
  }

  private void grant(String userId, String resource, String accessType) {
    AuthEntry entry = new AuthEntry();
    entry.setUserId(userId);
    entry.setResource(resource);
    entry.setAccessType(accessType);
    authEntryRepository.save(entry);
  }

  private PortfolioDto portfolio(
      String portfolioId, String accountNo, int portfolioNumber, LocalDate createDate) {
    PortfolioDto dto = new PortfolioDto();
    dto.setPortfolioId(portfolioId);
    dto.setAccountNo(accountNo);
    dto.setClientName(String.format("Demo Client %02d", portfolioNumber));
    dto.setClientType(List.of("I", "C", "T").get((portfolioNumber - 1) % 3));
    dto.setCreateDate(createDate);
    dto.setStatus(PortfolioStatus.ACTIVE.getCode());
    dto.setCurrencyCode("USD");
    dto.setBranchId("01");
    dto.setAccountType("IN");
    return dto;
  }

  private List<String> positionNumber(int portfolioNumber) {
    return INVESTMENT_IDS.subList(0, 3 + portfolioNumber % 3);
  }

  private void createPositions(
      String portfolioId, List<String> investmentIds, int positionCount, LocalDate baseDate) {
    for (int positionNumber = 0; positionNumber < positionCount; positionNumber++) {
      String investmentId = investmentIds.get(positionNumber);
      BigDecimal quantity = BigDecimal.valueOf(100L * (positionNumber + 1)).setScale(4);
      BigDecimal price = PRICES.get(investmentId.substring(0, 3));
      BigDecimal costBasis = quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
      com.portfolio.domain.InvestmentPosition position =
          new com.portfolio.domain.InvestmentPosition();
      position.setId(
          new com.portfolio.domain.InvestmentPositionId(portfolioId, investmentId, baseDate));
      position.setQuantity(quantity);
      position.setCostBasis(costBasis);
      position.setMarketValue(
          costBasis.multiply(new BigDecimal("1.05")).setScale(2, RoundingMode.HALF_UP));
      position.setCurrencyCode("USD");
      position.setStatus(PositionStatus.ACTIVE);
      position.setLastMaintDate(LocalDateTime.now());
      position.setLastMaintUser("DEMO");
      portfolioService.savePosition(position);
    }
  }

  private void createTransaction(
      String portfolioId,
      String investmentId,
      int portfolioNumber,
      int transactionNumber,
      LocalDate baseDate,
      boolean pending) {
    TransactionType type =
        List.of(
                TransactionType.BUY,
                TransactionType.BUY,
                TransactionType.SELL,
                TransactionType.FEE,
                TransactionType.BUY)
            .get(transactionNumber);
    BigDecimal quantity =
        type == TransactionType.FEE
            ? BigDecimal.ONE
            : BigDecimal.valueOf(transactionNumber == 2 ? 15 : 10L * (transactionNumber + 1));
    BigDecimal price =
        type == TransactionType.FEE
            ? new BigDecimal("1.0000")
            : PRICES.get(investmentId.substring(0, 3));
    BigDecimal amount =
        type == TransactionType.FEE
            ? new BigDecimal("25.00")
            : quantity.multiply(price).setScale(2, RoundingMode.HALF_UP);
    TransactionDto dto = new TransactionDto();
    dto.setPortfolioId(portfolioId);
    dto.setInvestmentId(investmentId);
    dto.setTransactionType(type.getCode());
    dto.setQuantity(quantity);
    dto.setPrice(price);
    dto.setAmount(amount);
    dto.setCurrencyCode("USD");
    dto.setTransactionDate(baseDate.minusDays((portfolioNumber * 5L + transactionNumber) % 30));
    dto.setTransactionTime(LocalTime.of(9, 30).plusMinutes(transactionNumber * 15L));
    dto.setProcessUser("DEMO");
    var transaction = transactionService.createPending(dto);
    if (!pending) {
      transactionService.applyPending(transaction);
    }
  }

  private void createHistory(LocalDate baseDate) {
    for (int entryNumber = 1; entryNumber <= 10; entryNumber++) {
      LocalDate date = baseDate.minusDays(entryNumber);
      PositionHistory history = new PositionHistory();
      history.setId(
          new PositionHistoryId("1000000001", "PORT0001", date, LocalTime.of(8, entryNumber)));
      boolean buy = entryNumber % 2 == 1;
      history.setTransType(buy ? "BU" : "SL");
      history.setSecurityId(buy ? "STK000001" : "BND000001");
      history.setQuantity(new BigDecimal("10.000").setScale(3));
      history.setPrice(buy ? new BigDecimal("125.500") : new BigDecimal("98.250"));
      history.setAmount((buy ? new BigDecimal("1255.00") : new BigDecimal("982.50")));
      history.setFees(BigDecimal.ZERO.setScale(2));
      history.setTotalAmount(history.getAmount());
      history.setCostBasis(buy ? history.getAmount() : BigDecimal.ZERO.setScale(2));
      history.setGainLoss(BigDecimal.ZERO.setScale(2));
      history.setProcessDate(baseDate);
      history.setProcessTime(LocalTime.NOON);
      history.setProgramId("DEMOLOAD");
      history.setUserId("SYSTEM");
      history.setAuditTimestamp(LocalDateTime.of(baseDate, LocalTime.NOON));
      historyRepository.save(history);
    }
  }
}

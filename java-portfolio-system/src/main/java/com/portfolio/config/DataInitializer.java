package com.portfolio.config;

import com.portfolio.domain.*;
import com.portfolio.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * Data Initializer - creates sample data for demonstration
 * Equivalent to COBOL TSTGEN00 test data generation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            initializeUsers();
            initializePortfolios();
            initializePositions();
            initializeTransactions();
            log.info("Sample data initialized successfully");
        }
    }

    private void initializeUsers() {
        User admin = User.builder()
                .userId("ADMIN001")
                .password(passwordEncoder.encode("admin123"))
                .fullName("System Administrator")
                .email("admin@portfolio.com")
                .status(User.UserStatus.A)
                .roles(Set.of(User.UserRole.ADMIN, User.UserRole.USER))
                .build();
        userRepository.save(admin);

        User user = User.builder()
                .userId("USER0001")
                .password(passwordEncoder.encode("user123"))
                .fullName("John Smith")
                .email("john.smith@portfolio.com")
                .status(User.UserStatus.A)
                .roles(Set.of(User.UserRole.USER))
                .build();
        userRepository.save(user);

        User viewer = User.builder()
                .userId("VIEW0001")
                .password(passwordEncoder.encode("view123"))
                .fullName("Jane Doe")
                .email("jane.doe@portfolio.com")
                .status(User.UserStatus.A)
                .roles(Set.of(User.UserRole.VIEWER))
                .build();
        userRepository.save(viewer);

        log.info("Created 3 sample users");
    }

    private void initializePortfolios() {
        Portfolio portfolio1 = Portfolio.builder()
                .portfolioId("PORT0001")
                .accountNo("ACC1000001")
                .clientName("Acme Corporation")
                .clientType(Portfolio.ClientType.C)
                .status(Portfolio.PortfolioStatus.A)
                .totalValue(new BigDecimal("1500000.00"))
                .cashBalance(new BigDecimal("50000.00"))
                .lastUser("ADMIN001")
                .build();
        portfolioRepository.save(portfolio1);

        Portfolio portfolio2 = Portfolio.builder()
                .portfolioId("PORT0002")
                .accountNo("ACC1000002")
                .clientName("John Smith")
                .clientType(Portfolio.ClientType.I)
                .status(Portfolio.PortfolioStatus.A)
                .totalValue(new BigDecimal("250000.00"))
                .cashBalance(new BigDecimal("15000.00"))
                .lastUser("ADMIN001")
                .build();
        portfolioRepository.save(portfolio2);

        Portfolio portfolio3 = Portfolio.builder()
                .portfolioId("PORT0003")
                .accountNo("ACC1000003")
                .clientName("Smith Family Trust")
                .clientType(Portfolio.ClientType.T)
                .status(Portfolio.PortfolioStatus.A)
                .totalValue(new BigDecimal("750000.00"))
                .cashBalance(new BigDecimal("25000.00"))
                .lastUser("ADMIN001")
                .build();
        portfolioRepository.save(portfolio3);

        log.info("Created 3 sample portfolios");
    }

    private void initializePositions() {
        Position pos1 = Position.builder()
                .portfolioId("PORT0001")
                .positionDate(LocalDate.now())
                .investmentId("AAPL")
                .quantity(new BigDecimal("1000.0000"))
                .costBasis(new BigDecimal("150000.00"))
                .marketValue(new BigDecimal("175000.00"))
                .currency("USD")
                .status(Position.PositionStatus.A)
                .lastMaintUser("ADMIN001")
                .build();
        positionRepository.save(pos1);

        Position pos2 = Position.builder()
                .portfolioId("PORT0001")
                .positionDate(LocalDate.now())
                .investmentId("GOOGL")
                .quantity(new BigDecimal("500.0000"))
                .costBasis(new BigDecimal("600000.00"))
                .marketValue(new BigDecimal("700000.00"))
                .currency("USD")
                .status(Position.PositionStatus.A)
                .lastMaintUser("ADMIN001")
                .build();
        positionRepository.save(pos2);

        Position pos3 = Position.builder()
                .portfolioId("PORT0001")
                .positionDate(LocalDate.now())
                .investmentId("MSFT")
                .quantity(new BigDecimal("1500.0000"))
                .costBasis(new BigDecimal("450000.00"))
                .marketValue(new BigDecimal("575000.00"))
                .currency("USD")
                .status(Position.PositionStatus.A)
                .lastMaintUser("ADMIN001")
                .build();
        positionRepository.save(pos3);

        Position pos4 = Position.builder()
                .portfolioId("PORT0002")
                .positionDate(LocalDate.now())
                .investmentId("AAPL")
                .quantity(new BigDecimal("200.0000"))
                .costBasis(new BigDecimal("30000.00"))
                .marketValue(new BigDecimal("35000.00"))
                .currency("USD")
                .status(Position.PositionStatus.A)
                .lastMaintUser("ADMIN001")
                .build();
        positionRepository.save(pos4);

        Position pos5 = Position.builder()
                .portfolioId("PORT0002")
                .positionDate(LocalDate.now())
                .investmentId("TSLA")
                .quantity(new BigDecimal("300.0000"))
                .costBasis(new BigDecimal("75000.00"))
                .marketValue(new BigDecimal("90000.00"))
                .currency("USD")
                .status(Position.PositionStatus.A)
                .lastMaintUser("ADMIN001")
                .build();
        positionRepository.save(pos5);

        Position pos6 = Position.builder()
                .portfolioId("PORT0003")
                .positionDate(LocalDate.now())
                .investmentId("BRK.B")
                .quantity(new BigDecimal("1000.0000"))
                .costBasis(new BigDecimal("350000.00"))
                .marketValue(new BigDecimal("400000.00"))
                .currency("USD")
                .status(Position.PositionStatus.A)
                .lastMaintUser("ADMIN001")
                .build();
        positionRepository.save(pos6);

        log.info("Created 6 sample positions");
    }

    private void initializeTransactions() {
        Transaction trn1 = Transaction.builder()
                .transactionDate(LocalDate.now().minusDays(5))
                .portfolioId("PORT0001")
                .investmentId("AAPL")
                .transactionType(Transaction.TransactionType.BU)
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("175.00"))
                .amount(new BigDecimal("17500.00"))
                .currency("USD")
                .status(Transaction.TransactionStatus.D)
                .processUser("USER0001")
                .build();
        transactionRepository.save(trn1);

        Transaction trn2 = Transaction.builder()
                .transactionDate(LocalDate.now().minusDays(3))
                .portfolioId("PORT0001")
                .investmentId("GOOGL")
                .transactionType(Transaction.TransactionType.SL)
                .quantity(new BigDecimal("50.0000"))
                .price(new BigDecimal("1400.00"))
                .amount(new BigDecimal("70000.00"))
                .currency("USD")
                .status(Transaction.TransactionStatus.D)
                .processUser("USER0001")
                .build();
        transactionRepository.save(trn2);

        Transaction trn3 = Transaction.builder()
                .transactionDate(LocalDate.now().minusDays(1))
                .portfolioId("PORT0002")
                .investmentId("TSLA")
                .transactionType(Transaction.TransactionType.BU)
                .quantity(new BigDecimal("25.0000"))
                .price(new BigDecimal("300.00"))
                .amount(new BigDecimal("7500.00"))
                .currency("USD")
                .status(Transaction.TransactionStatus.P)
                .processUser("USER0001")
                .build();
        transactionRepository.save(trn3);

        Transaction trn4 = Transaction.builder()
                .transactionDate(LocalDate.now())
                .portfolioId("PORT0003")
                .investmentId("BRK.B")
                .transactionType(Transaction.TransactionType.BU)
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("400.00"))
                .amount(new BigDecimal("40000.00"))
                .currency("USD")
                .status(Transaction.TransactionStatus.P)
                .processUser("USER0001")
                .build();
        transactionRepository.save(trn4);

        log.info("Created 4 sample transactions");
    }
}

package com.portfolio.config;

import com.portfolio.model.entity.Portfolio;
import com.portfolio.model.enums.ClientType;
import com.portfolio.model.enums.PortfolioStatus;
import com.portfolio.repository.PortfolioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PortfolioRepository portfolioRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing sample data...");

        if (portfolioRepository.count() == 0) {
            createSamplePortfolios();
            log.info("Sample data initialized successfully");
        } else {
            log.info("Data already exists, skipping initialization");
        }
    }

    private void createSamplePortfolios() {
        Portfolio portfolio1 = Portfolio.builder()
                .portfolioId("PORT0001")
                .accountNo("ACC0000001")
                .clientName("John Smith")
                .clientType(ClientType.INDIVIDUAL)
                .createDate(LocalDate.now().minusMonths(6))
                .status(PortfolioStatus.ACTIVE)
                .totalValue(new BigDecimal("150000.00"))
                .cashBalance(new BigDecimal("5000.00"))
                .totalUnits(new BigDecimal("1000.0000"))
                .totalCost(new BigDecimal("120000.00"))
                .lastUser("ADMIN")
                .build();
        portfolioRepository.save(portfolio1);

        Portfolio portfolio2 = Portfolio.builder()
                .portfolioId("PORT0002")
                .accountNo("ACC0000002")
                .clientName("Acme Corporation")
                .clientType(ClientType.CORPORATE)
                .createDate(LocalDate.now().minusMonths(12))
                .status(PortfolioStatus.ACTIVE)
                .totalValue(new BigDecimal("500000.00"))
                .cashBalance(new BigDecimal("25000.00"))
                .totalUnits(new BigDecimal("5000.0000"))
                .totalCost(new BigDecimal("450000.00"))
                .lastUser("ADMIN")
                .build();
        portfolioRepository.save(portfolio2);

        Portfolio portfolio3 = Portfolio.builder()
                .portfolioId("PORT0003")
                .accountNo("ACC0000003")
                .clientName("Smith Family Trust")
                .clientType(ClientType.TRUST)
                .createDate(LocalDate.now().minusMonths(3))
                .status(PortfolioStatus.ACTIVE)
                .totalValue(new BigDecimal("250000.00"))
                .cashBalance(new BigDecimal("10000.00"))
                .totalUnits(new BigDecimal("2500.0000"))
                .totalCost(new BigDecimal("200000.00"))
                .lastUser("ADMIN")
                .build();
        portfolioRepository.save(portfolio3);

        Portfolio portfolio4 = Portfolio.builder()
                .portfolioId("PORT0004")
                .accountNo("ACC0000004")
                .clientName("Jane Doe")
                .clientType(ClientType.INDIVIDUAL)
                .createDate(LocalDate.now().minusMonths(9))
                .status(PortfolioStatus.SUSPENDED)
                .totalValue(new BigDecimal("75000.00"))
                .cashBalance(new BigDecimal("2500.00"))
                .totalUnits(new BigDecimal("500.0000"))
                .totalCost(new BigDecimal("60000.00"))
                .lastUser("ADMIN")
                .build();
        portfolioRepository.save(portfolio4);

        Portfolio portfolio5 = Portfolio.builder()
                .portfolioId("PORT0005")
                .accountNo("ACC0000005")
                .clientName("Global Investments LLC")
                .clientType(ClientType.CORPORATE)
                .createDate(LocalDate.now().minusMonths(18))
                .status(PortfolioStatus.CLOSED)
                .totalValue(new BigDecimal("0.00"))
                .cashBalance(new BigDecimal("0.00"))
                .totalUnits(new BigDecimal("0.0000"))
                .totalCost(new BigDecimal("0.00"))
                .lastUser("ADMIN")
                .build();
        portfolioRepository.save(portfolio5);

        log.info("Created 5 sample portfolios");
    }
}

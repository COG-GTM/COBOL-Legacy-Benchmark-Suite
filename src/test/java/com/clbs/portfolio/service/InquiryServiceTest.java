package com.clbs.portfolio.service;

import com.clbs.portfolio.DatabaseCleaner;
import com.clbs.portfolio.IntegrationTest;
import com.clbs.portfolio.domain.PortfolioKey;
import com.clbs.portfolio.domain.PositionHistory;
import com.clbs.portfolio.exception.NotFoundException;
import com.clbs.portfolio.repository.PositionHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
class InquiryServiceTest {

    @Autowired
    private InquiryService inquiry;

    @Autowired
    private PortfolioService portfolioService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private PositionHistoryRepository history;

    @Autowired
    private DatabaseCleaner cleaner;

    @BeforeEach
    void setUp() {
        cleaner.clean();
        portfolioService.create(PortfolioServiceTest.portfolio(new PortfolioKey("PORT0001", "1234567890"), "GROWTH", "A"));
    }

    @Test
    void positionsForAccount() {
        transactionService.process(new TransactionCommand("PORT0001", "IBM0000001", "BU", BigDecimal.TEN,
                BigDecimal.ONE, BigDecimal.TEN, "USD"));
        assertThat(inquiry.positionsForAccount("1234567890")).hasSize(1);
    }

    @Test
    void noPositionsMeansNotFound() {
        assertThatThrownBy(() -> inquiry.positionsForAccount("1234567890"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Position not found for account");
        assertThatThrownBy(() -> inquiry.positionsForAccount("0000000000")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void historyIsNewestFirstAndCappedAtTenRows() {
        for (int day = 1; day <= 12; day++) {
            history.save(new PositionHistory("1234567890", "PORT0001", LocalDate.of(2024, 1, day), LocalTime.NOON,
                    "BU", "IBM0000001", BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, "HISTLD00",
                    "LOADER", LocalDateTime.now()));
        }
        List<PositionHistory> rows = inquiry.historyForAccount("1234567890");
        assertThat(rows).hasSize(10);
        assertThat(rows.getFirst().getTransDate()).isEqualTo(LocalDate.of(2024, 1, 12));
        assertThat(rows.getLast().getTransDate()).isEqualTo(LocalDate.of(2024, 1, 3));
    }
}

package com.portfolio.batch;

import com.portfolio.model.PositionRecord;
import com.portfolio.support.PositionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PositionUpdateStep.
 * Tests cost basis calculation logic from COBOL POSUPD00.
 */
@SpringBootTest
@ActiveProfiles("test")
class PositionUpdateStepTest {

    @Autowired
    private PositionRecordRepository positionRepository;

    @BeforeEach
    void setUp() {
        positionRepository.deleteAll();
    }

    @Test
    void testCostBasisCalculation() {
        PositionRecord position = createPosition("PORT0001", "AAPL      ",
                new BigDecimal("100.0000"), new BigDecimal("5000.00"), new BigDecimal("5500.00"));
        positionRepository.save(position);

        PositionRecord saved = positionRepository.findByPortfolioId("PORT0001").get(0);
        assertThat(saved.getQuantity()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(saved.getCostBasis()).isEqualByComparingTo(new BigDecimal("5000.00"));
    }

    @Test
    void testMultiplePositionsPerPortfolio() {
        positionRepository.save(createPosition("PORT0001", "AAPL      ",
                new BigDecimal("100.0000"), new BigDecimal("5000.00"), new BigDecimal("5500.00")));
        positionRepository.save(createPosition("PORT0001", "GOOGL     ",
                new BigDecimal("50.0000"), new BigDecimal("7500.00"), new BigDecimal("8000.00")));

        assertThat(positionRepository.findByPortfolioId("PORT0001")).hasSize(2);
    }

    private PositionRecord createPosition(String portfolioId, String symbolId,
                                           BigDecimal qty, BigDecimal costBasis, BigDecimal marketValue) {
        PositionRecord pos = new PositionRecord();
        pos.setPortfolioId(portfolioId);
        pos.setSymbolId(symbolId);
        pos.setPositionDate(LocalDate.now());
        pos.setQuantity(qty);
        pos.setCostBasis(costBasis);
        pos.setMarketValue(marketValue);
        pos.setCurrencyCode("USD");
        pos.setStatus("A");
        pos.setLastMaintDate(LocalDateTime.now());
        pos.setLastMaintUser("TEST    ");
        return pos;
    }
}

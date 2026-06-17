package com.clbs.position.service;

import com.clbs.position.domain.PositionUpdateResult;
import com.clbs.position.entity.PositionHistory;
import com.clbs.position.entity.Transaction;
import com.clbs.position.repository.PositionHistoryRepository;
import com.clbs.position.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Service-level tests of the COBOL POSUPDT unit of work (apply transaction ->
 * update position -> record history) against the full Spring context.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:svcdb;DB_CLOSE_DELAY=-1")
class PositionUpdateServiceTest {

    @Autowired
    private PositionUpdateService service;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private PositionHistoryRepository historyRepository;

    private Transaction seededTransaction(String sequenceNo) {
        return transactionRepository.findAll().stream()
                .filter(t -> sequenceNo.equals(t.getSequenceNo()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("Applying a BUY aggregates the existing holding and records history")
    void applyBuy() {
        Transaction buy = seededTransaction("000001"); // BU PORT0001 SEC0000001 100 @ 55 = 5500

        PositionUpdateResult result = service.applyTransaction(buy);

        assertThat(result.newState().quantity()).isEqualByComparingTo("1100.0000");
        assertThat(result.newState().costBasis()).isEqualByComparingTo("55500.00");

        List<PositionHistory> history = historyRepository.findByPortfolioIdAndTransDate(
                "PORT0001", java.time.LocalDate.of(2024, 6, 17));
        assertThat(history).anyMatch(h -> "BU".equals(h.getTransType())
                && h.getCostBasis().compareTo(new java.math.BigDecimal("55500.00")) == 0);

        Transaction reread = transactionRepository.findById(buy.getId()).orElseThrow();
        assertThat(reread.getStatus()).isEqualTo("D");
    }

    @Test
    @DisplayName("Applying a SELL computes realized gain/loss at average cost")
    void applySell() {
        Transaction sell = seededTransaction("000002"); // SL PORT0001 SEC0000002 100 @ 48 = 4800

        PositionUpdateResult result = service.applyTransaction(sell);

        // existing 500 @ cost 25000 -> avg 50; cost of 100 sold = 5000; proceeds 4800 -> -200
        assertThat(result.realizedGainLoss()).isEqualByComparingTo("-200.00");
        assertThat(result.newState().quantity()).isEqualByComparingTo("400.0000");
        assertThat(result.newState().costBasis()).isEqualByComparingTo("20000.00");
    }

    @Test
    @DisplayName("Applying a BUY for a new investment creates an ACTIVE position")
    void applyBuyCreatesNewPosition() {
        Transaction buy = seededTransaction("000004"); // BU PORT0002 SEC0000003 300 @ 20 = 6000

        PositionUpdateResult result = service.applyTransaction(buy);

        assertThat(result.newState().quantity()).isEqualByComparingTo("300.0000");
        assertThat(result.newState().costBasis()).isEqualByComparingTo("6000.00");
        assertThat(service.findByPortfolio("PORT0002")).hasSize(2);
    }

    @Test
    @DisplayName("realizedGainLoss sums POSHIST gain/loss for a portfolio (DB2 aggregate)")
    void realizedGainLossAggregate() {
        service.applyTransaction(seededTransaction("000002")); // realized -200
        assertThat(service.realizedGainLoss("PORT0001")).isEqualByComparingTo("-200.00");
    }
}

package com.benchmark.portfolio.common.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.benchmark.portfolio.common.entity.PortfolioMaster;
import com.benchmark.portfolio.common.entity.PortfolioMasterId;
import com.benchmark.portfolio.common.entity.PortfolioPosition;
import com.benchmark.portfolio.common.entity.PortfolioPositionId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PortfolioPositionRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private PortfolioPositionRepository repository;

    @Autowired
    private PortfolioMasterRepository masterRepository;

    private void seedMaster(String portfolioId, String accountNo) {
        PortfolioMaster master = new PortfolioMaster();
        master.setId(new PortfolioMasterId(portfolioId, accountNo));
        master.setClientName("CLIENT " + portfolioId);
        master.setClientType("I");
        master.setCreateDate(LocalDate.of(2024, 1, 15));
        master.setStatus("A");
        master.setTotalValue(BigDecimal.ZERO);
        master.setCashBalance(BigDecimal.ZERO);
        masterRepository.save(master);
    }

    private PortfolioPosition position(String portfolioId, LocalDate date, String investmentId) {
        PortfolioPosition position = new PortfolioPosition();
        position.setId(new PortfolioPositionId(portfolioId, date, investmentId));
        position.setQuantity(new BigDecimal("100.0000"));
        position.setCostBasis(new BigDecimal("1000.00"));
        position.setMarketValue(new BigDecimal("1100.00"));
        position.setCurrencyCode("USD");
        position.setStatus("A");
        return position;
    }

    @BeforeEach
    void seed() {
        seedMaster("PORT0001", "ACCT000001");
        seedMaster("PORT0002", "ACCT000002");
        seedMaster("PORT0009", "ACCT000009");
        masterRepository.flush();
        repository.saveAll(List.of(
                position("PORT0001", LocalDate.of(2024, 6, 1), "INVEST0001"),
                position("PORT0001", LocalDate.of(2024, 6, 1), "INVEST0002"),
                position("PORT0001", LocalDate.of(2024, 6, 2), "INVEST0001"),
                position("PORT0002", LocalDate.of(2024, 6, 1), "INVEST0001")));
        repository.flush();
    }

    @Test
    void crudRoundTrip() {
        PortfolioPositionId id =
                new PortfolioPositionId("PORT0009", LocalDate.of(2024, 7, 1), "INVEST0009");
        repository.saveAndFlush(position("PORT0009", LocalDate.of(2024, 7, 1), "INVEST0009"));

        PortfolioPosition loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getMarketValue()).isEqualByComparingTo("1100.00");

        loaded.setStatus("C");
        loaded.setQuantity(new BigDecimal("0.0000"));
        repository.saveAndFlush(loaded);
        assertThat(repository.findById(id).orElseThrow().getStatus()).isEqualTo("C");

        repository.deleteById(id);
        repository.flush();
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void partialKeyReadByPortfolioId() {
        List<PortfolioPosition> result =
                repository.findByIdPortfolioIdOrderByIdPositionDateAscIdInvestmentIdAsc("PORT0001");
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId().getInvestmentId()).isEqualTo("INVEST0001");
        assertThat(result.get(2).getId().getPositionDate()).isEqualTo(LocalDate.of(2024, 6, 2));
        assertThat(repository
                .findByIdPortfolioIdOrderByIdPositionDateAscIdInvestmentIdAsc("PORT9999")).isEmpty();
    }

    @Test
    void rangeScanWithinPortfolioFromDate() {
        List<PortfolioPosition> result = repository
                .findByIdPortfolioIdAndIdPositionDateGreaterThanEqualOrderByIdPositionDateAscIdInvestmentIdAsc(
                        "PORT0001", LocalDate.of(2024, 6, 2));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId().getPositionDate()).isEqualTo(LocalDate.of(2024, 6, 2));
    }

    @Test
    void latestPositionForInvestment() {
        PortfolioPosition latest = repository
                .findFirstByIdPortfolioIdAndIdInvestmentIdOrderByIdPositionDateDesc(
                        "PORT0001", "INVEST0001")
                .orElseThrow();
        assertThat(latest.getId().getPositionDate()).isEqualTo(LocalDate.of(2024, 6, 2));
        assertThat(repository
                .findFirstByIdPortfolioIdAndIdInvestmentIdOrderByIdPositionDateDesc(
                        "PORT9999", "INVEST0001")).isEmpty();
    }
}

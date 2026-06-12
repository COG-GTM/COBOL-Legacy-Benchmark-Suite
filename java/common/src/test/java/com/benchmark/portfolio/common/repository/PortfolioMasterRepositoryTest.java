package com.benchmark.portfolio.common.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.benchmark.portfolio.common.entity.PortfolioMaster;
import com.benchmark.portfolio.common.entity.PortfolioMasterId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PortfolioMasterRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private PortfolioMasterRepository repository;

    private PortfolioMaster master(String portfolioId, String accountNo) {
        PortfolioMaster master = new PortfolioMaster();
        master.setId(new PortfolioMasterId(portfolioId, accountNo));
        master.setClientName("CLIENT " + portfolioId);
        master.setClientType("I");
        master.setCreateDate(LocalDate.of(2024, 1, 15));
        master.setStatus("A");
        master.setTotalValue(new BigDecimal("100000.00"));
        master.setCashBalance(new BigDecimal("5000.00"));
        return master;
    }

    @BeforeEach
    void seed() {
        repository.saveAll(List.of(
                master("PORT0001", "ACCT000001"),
                master("PORT0002", "ACCT000002"),
                master("PORT0003", "ACCT000003"),
                master("PORT0004", "ACCT000004")));
        repository.flush();
    }

    @Test
    void crudRoundTrip() {
        PortfolioMasterId id = new PortfolioMasterId("PORT0009", "ACCT000009");
        PortfolioMaster created = master("PORT0009", "ACCT000009");
        repository.saveAndFlush(created);

        PortfolioMaster loaded = repository.findById(id).orElseThrow();
        assertThat(loaded.getClientName()).isEqualTo("CLIENT PORT0009");
        assertThat(loaded.getTotalValue()).isEqualByComparingTo("100000.00");

        loaded.setStatus("S");
        loaded.setCashBalance(new BigDecimal("-123.45"));
        repository.saveAndFlush(loaded);
        PortfolioMaster updated = repository.findById(id).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("S");
        assertThat(updated.getCashBalance()).isEqualByComparingTo("-123.45");

        repository.deleteById(id);
        repository.flush();
        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void sequentialScanReturnsKeyOrder() {
        List<PortfolioMaster> all = repository.findAllByOrderByIdPortfolioIdAscIdAccountNoAsc();
        assertThat(all).extracting(m -> m.getId().getPortfolioId() + m.getId().getAccountNo())
                .containsExactly(
                        "PORT0001ACCT000001",
                        "PORT0002ACCT000002",
                        "PORT0003ACCT000003",
                        "PORT0004ACCT000004");
    }

    @Test
    void partialKeyReadByPortfolioId() {
        List<PortfolioMaster> result =
                repository.findByIdPortfolioIdOrderByIdAccountNoAsc("PORT0001");
        assertThat(result).extracting(m -> m.getId().getAccountNo())
                .containsExactly("ACCT000001");
        assertThat(repository.findByIdPortfolioIdOrderByIdAccountNoAsc("PORT9999")).isEmpty();
    }

    @Test
    void existsByPortfolioIdPrefix() {
        assertThat(repository.existsByIdPortfolioId("PORT0002")).isTrue();
        assertThat(repository.existsByIdPortfolioId("PORT9999")).isFalse();
    }

    @Test
    void rangeScanFromStartingKey() {
        List<PortfolioMaster> result = repository
                .findByIdPortfolioIdGreaterThanEqualOrderByIdPortfolioIdAscIdAccountNoAsc("PORT0003");
        assertThat(result).extracting(m -> m.getId().getPortfolioId())
                .containsExactly("PORT0003", "PORT0004");
    }
}

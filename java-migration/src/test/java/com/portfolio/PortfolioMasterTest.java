package com.portfolio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.portfolio.entity.PortfolioMaster;
import com.portfolio.repository.PortfolioMasterRepository;
import com.portfolio.service.PortfolioMasterService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for PortfolioMaster entity, repository, and service.
 * Uses @DataJpaTest with H2 in-memory database.
 * Flyway migrations (V1 schema + V2 seed data) run automatically.
 */
@DataJpaTest
@Import(PortfolioMasterService.class)
@ActiveProfiles("test")
class PortfolioMasterTest {

    @Autowired
    private PortfolioMasterRepository repository;

    @Autowired
    private PortfolioMasterService service;

    // ---------------------------------------------------------------
    // Seed data verification
    // ---------------------------------------------------------------

    @Test
    void seedDataShouldLoad20Records() {
        List<PortfolioMaster> all = repository.findAll();
        assertThat(all).hasSize(20);
    }

    @Test
    void firstSeedRecordShouldHaveCorrectValues() {
        Optional<PortfolioMaster> opt = repository.findById("PORT0001");
        assertThat(opt).isPresent();

        PortfolioMaster p = opt.get();
        assertThat(p.getPortAccountNo()).isEqualTo("1000000001");
        assertThat(p.getPortClientName()).isEqualTo("TEST CLIENT 001");
        assertThat(p.getPortClientType()).isEqualTo("I");
        assertThat(p.getPortCreateDate()).isEqualTo(LocalDate.of(2024, 3, 20));
        assertThat(p.getPortLastMaint()).isEqualTo(LocalDate.of(2024, 3, 20));
        assertThat(p.getPortStatus()).isEqualTo("A");
        assertThat(p.getPortTotalValue()).isEqualByComparingTo(new BigDecimal("523847.50"));
        assertThat(p.getPortCashBalance()).isEqualByComparingTo(new BigDecimal("52384.75"));
        assertThat(p.getPortLastUser()).isEqualTo("ADMIN001");
        assertThat(p.getPortLastTrans()).isEqualTo(LocalDate.of(2024, 3, 20));
    }

    // ---------------------------------------------------------------
    // CRUD operations
    // ---------------------------------------------------------------

    @Test
    void createShouldPersistNewPortfolio() {
        PortfolioMaster newPort = new PortfolioMaster();
        newPort.setPortId("PORT9999");
        newPort.setPortAccountNo("9999999999");
        newPort.setPortClientName("NEW CLIENT");
        newPort.setPortClientType("I");
        newPort.setPortStatus("A");
        newPort.setPortTotalValue(new BigDecimal("100000.00"));
        newPort.setPortCashBalance(new BigDecimal("10000.00"));
        newPort.setPortLastUser("TESTUSER");

        PortfolioMaster saved = service.create(newPort);

        assertThat(saved.getPortCreateDate()).isNotNull();
        assertThat(saved.getPortLastMaint()).isNotNull();
        assertThat(repository.findById("PORT9999")).isPresent();
    }

    @Test
    void createDuplicateShouldThrow() {
        PortfolioMaster dup = new PortfolioMaster();
        dup.setPortId("PORT0001");
        dup.setPortAccountNo("0000000000");
        dup.setPortClientName("DUPLICATE");
        dup.setPortClientType("I");
        dup.setPortStatus("A");
        dup.setPortTotalValue(BigDecimal.ZERO);
        dup.setPortCashBalance(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.create(dup))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void readShouldReturnExistingPortfolio() {
        Optional<PortfolioMaster> result = service.findById("PORT0005");
        assertThat(result).isPresent();
        assertThat(result.get().getPortClientName()).isEqualTo("TEST CLIENT 005");
        assertThat(result.get().getPortStatus()).isEqualTo("C");
    }

    @Test
    void updateShouldModifyAndSetLastMaint() {
        PortfolioMaster existing = repository.findById("PORT0003").orElseThrow();
        existing.setPortClientName("UPDATED NAME");
        existing.setPortStatus("A");

        PortfolioMaster updated = service.update(existing);

        assertThat(updated.getPortClientName()).isEqualTo("UPDATED NAME");
        assertThat(updated.getPortStatus()).isEqualTo("A");
        assertThat(updated.getPortLastMaint()).isEqualTo(LocalDate.now());
    }

    @Test
    void deleteShouldRemovePortfolio() {
        assertThat(repository.findById("PORT0010")).isPresent();
        service.delete("PORT0010");
        assertThat(repository.findById("PORT0010")).isEmpty();
    }

    @Test
    void deleteNonExistentShouldThrow() {
        assertThatThrownBy(() -> service.delete("PORT9998"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found for deletion");
    }

    // ---------------------------------------------------------------
    // Repository finder methods
    // ---------------------------------------------------------------

    @Test
    void findByStatusShouldReturnMatchingRecords() {
        List<PortfolioMaster> active = service.findByStatus("A");
        assertThat(active).isNotEmpty();
        assertThat(active).allMatch(p -> "A".equals(p.getPortStatus()));

        List<PortfolioMaster> closed = service.findByStatus("C");
        assertThat(closed).isNotEmpty();
        assertThat(closed).allMatch(p -> "C".equals(p.getPortStatus()));

        List<PortfolioMaster> suspended = service.findByStatus("S");
        assertThat(suspended).isNotEmpty();
        assertThat(suspended).allMatch(p -> "S".equals(p.getPortStatus()));
    }

    @Test
    void findByClientTypeShouldReturnMatchingRecords() {
        List<PortfolioMaster> individuals = service.findByClientType("I");
        assertThat(individuals).isNotEmpty();
        assertThat(individuals).allMatch(p -> "I".equals(p.getPortClientType()));

        List<PortfolioMaster> corporates = service.findByClientType("C");
        assertThat(corporates).isNotEmpty();
        assertThat(corporates).allMatch(p -> "C".equals(p.getPortClientType()));

        List<PortfolioMaster> trusts = service.findByClientType("T");
        assertThat(trusts).isNotEmpty();
        assertThat(trusts).allMatch(p -> "T".equals(p.getPortClientType()));
    }

    @Test
    void findByAccountNoShouldReturnMatchingRecords() {
        List<PortfolioMaster> result = service.findByAccountNo("1000000011");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPortClientName()).isEqualTo("ACME CORPORATION");
    }

    @Test
    void findByClientNameShouldSearchCaseInsensitive() {
        List<PortfolioMaster> result = repository.findByPortClientNameContainingIgnoreCase("acme");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPortId()).isEqualTo("PORT0011");
    }

    // ---------------------------------------------------------------
    // Validation
    // ---------------------------------------------------------------

    @Test
    void invalidPortfolioIdShouldThrow() {
        PortfolioMaster bad = new PortfolioMaster();
        bad.setPortId("BADID001");
        bad.setPortAccountNo("0000000000");
        bad.setPortClientType("I");
        bad.setPortStatus("A");
        bad.setPortTotalValue(BigDecimal.ZERO);
        bad.setPortCashBalance(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.create(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start with 'PORT'");
    }

    @Test
    void portfolioIdWithNonNumericSuffixShouldThrow() {
        PortfolioMaster bad = new PortfolioMaster();
        bad.setPortId("PORTABCD");
        bad.setPortAccountNo("0000000000");
        bad.setPortClientType("I");
        bad.setPortStatus("A");
        bad.setPortTotalValue(BigDecimal.ZERO);
        bad.setPortCashBalance(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.create(bad))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digits must follow");
    }
}

package com.clbs.position.repository;

import com.clbs.position.entity.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests against H2 with the Flyway-managed schema and seed data
 * (validates the VSAM-KSDS-to-JPA mapping and the derived finder methods). Uses
 * an isolated in-memory database and rolls back each test.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:repodb;DB_CLOSE_DELAY=-1")
class PositionRepositoryTest {

    @Autowired
    private PositionRepository positionRepository;

    @Test
    @DisplayName("Flyway seed migration loads the expected positions")
    void seedDataLoaded() {
        assertThat(positionRepository.findAll()).hasSize(3);
    }

    @Test
    @DisplayName("Keyed read on the VSAM composite key returns the holding")
    void keyedRead() {
        Optional<Position> pos = positionRepository
                .findByPortfolioIdAndPositionDateAndInvestmentId("PORT0001", "20240617", "SEC0000001");

        assertThat(pos).isPresent();
        assertThat(pos.get().getQuantity()).isEqualByComparingTo("1000.0000");
        assertThat(pos.get().getCostBasis()).isEqualByComparingTo("50000.00");
    }

    @Test
    @DisplayName("findByPortfolioId returns all holdings for a portfolio")
    void findByPortfolio() {
        List<Position> positions = positionRepository.findByPortfolioId("PORT0001");
        assertThat(positions).hasSize(2);
    }

    @Test
    @DisplayName("findByStatus filters by position status")
    void findByStatus() {
        assertThat(positionRepository.findByStatus("A")).hasSize(3);
        assertThat(positionRepository.findByStatus("C")).isEmpty();
    }

    @Test
    @DisplayName("CRUD round-trip persists a new holding")
    void crudRoundTrip() {
        Position created = positionRepository.save(Position.builder()
                .portfolioId("PORT0009")
                .positionDate("20240617")
                .investmentId("SEC0000099")
                .quantity(new BigDecimal("10.0000"))
                .costBasis(new BigDecimal("100.00"))
                .marketValue(new BigDecimal("110.00"))
                .currency("USD")
                .status("A")
                .build());

        assertThat(created.getId()).isNotNull();
        Optional<Position> reread = positionRepository.findById(created.getId());
        assertThat(reread).isPresent();
        assertThat(reread.get().getInvestmentId()).isEqualTo("SEC0000099");

        positionRepository.deleteById(created.getId());
        assertThat(positionRepository.findById(created.getId())).isEmpty();
    }
}

package com.portfolio.domain.model;

import com.portfolio.domain.enums.PortfolioStatus;
import com.portfolio.domain.enums.PositionStatus;
import com.portfolio.domain.repository.InvestmentPositionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class InvestmentPositionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private InvestmentPositionRepository positionRepository;

    @BeforeEach
    void setUp() {
        Portfolio portfolio = new Portfolio();
        portfolio.setPortfolioId("PORT0001");
        portfolio.setAccountType("IN");
        portfolio.setBranchId("01");
        portfolio.setClientId("CLIENT0001");
        portfolio.setPortfolioName("Test Portfolio");
        portfolio.setCurrencyCode("USD");
        portfolio.setRiskLevel("M");
        portfolio.setStatus(PortfolioStatus.ACTIVE);
        portfolio.setOpenDate(LocalDate.of(2024, 1, 15));
        portfolio.setLastMaintDate(LocalDateTime.now());
        portfolio.setLastMaintUser("ADMIN001");
        entityManager.persistAndFlush(portfolio);
    }

    @Test
    void shouldPersistAndReadWithCompositeKey() {
        InvestmentPositionId posId = new InvestmentPositionId("PORT0001", "INV0000001",
                LocalDate.of(2024, 3, 15));
        InvestmentPosition position = createPosition(posId, PositionStatus.ACTIVE);
        entityManager.persistAndFlush(position);
        entityManager.clear();

        Optional<InvestmentPosition> found = positionRepository.findById(posId);
        assertThat(found).isPresent();
        InvestmentPosition p = found.get();
        assertThat(p.getQuantity()).isEqualByComparingTo(new BigDecimal("500.0000"));
        assertThat(p.getCostBasis()).isEqualByComparingTo(new BigDecimal("25000.00"));
        assertThat(p.getMarketValue()).isEqualByComparingTo(new BigDecimal("27500.00"));
        assertThat(p.getStatus()).isEqualTo(PositionStatus.ACTIVE);
    }

    @Test
    void shouldHandleCompositeKeyEquality() {
        InvestmentPositionId id1 = new InvestmentPositionId("PORT0001", "INV0000001",
                LocalDate.of(2024, 3, 15));
        InvestmentPositionId id2 = new InvestmentPositionId("PORT0001", "INV0000001",
                LocalDate.of(2024, 3, 15));
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());

        InvestmentPositionId id3 = new InvestmentPositionId("PORT0001", "INV0000002",
                LocalDate.of(2024, 3, 15));
        assertThat(id1).isNotEqualTo(id3);
    }

    @Test
    void shouldFindByPositionDateAndPortfolioId() {
        LocalDate targetDate = LocalDate.of(2024, 3, 15);

        entityManager.persistAndFlush(createPosition(
                new InvestmentPositionId("PORT0001", "INV0000001", targetDate), PositionStatus.ACTIVE));
        entityManager.persistAndFlush(createPosition(
                new InvestmentPositionId("PORT0001", "INV0000002", targetDate), PositionStatus.ACTIVE));
        entityManager.persistAndFlush(createPosition(
                new InvestmentPositionId("PORT0001", "INV0000003", LocalDate.of(2024, 3, 16)), PositionStatus.CLOSED));
        entityManager.clear();

        List<InvestmentPosition> positions = positionRepository
                .findByIdPositionDateAndIdPortfolioId(targetDate, "PORT0001");
        assertThat(positions).hasSize(2);
    }

    @Test
    void shouldStorePositionStatusAsCobolCode() {
        InvestmentPositionId posId = new InvestmentPositionId("PORT0001", "INV0000001",
                LocalDate.of(2024, 3, 15));
        entityManager.persistAndFlush(createPosition(posId, PositionStatus.PENDING));
        entityManager.clear();

        Object statusValue = entityManager.getEntityManager()
                .createNativeQuery("SELECT status FROM investment_positions WHERE portfolio_id = 'PORT0001' AND investment_id = 'INV0000001'")
                .getSingleResult();
        assertThat(statusValue.toString()).isEqualTo("P");
    }

    private InvestmentPosition createPosition(InvestmentPositionId id, PositionStatus status) {
        InvestmentPosition position = new InvestmentPosition();
        position.setId(id);
        position.setQuantity(new BigDecimal("500.0000"));
        position.setCostBasis(new BigDecimal("25000.00"));
        position.setMarketValue(new BigDecimal("27500.00"));
        position.setCurrencyCode("USD");
        position.setStatus(status);
        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser("SYSTEM01");
        return position;
    }
}

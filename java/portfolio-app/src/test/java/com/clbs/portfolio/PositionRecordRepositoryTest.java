package com.clbs.portfolio;

import com.clbs.portfolio.domain.PositionRecord;
import com.clbs.portfolio.domain.PositionRecordKey;
import com.clbs.portfolio.domain.enums.PositionStatus;
import com.clbs.portfolio.persistence.repository.PositionRecordRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class PositionRecordRepositoryTest {
    @Autowired
    private PositionRecordRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesAndReloadsPositionRecord() {
        PositionRecordKey key = new PositionRecordKey(
                "PORT0002", LocalDate.of(2024, 1, 3), "INVEST0002");
        PositionRecord record = new PositionRecord(key, new BigDecimal("20.5000"),
                new BigDecimal("2000.00"), new BigDecimal("2150.75"), "EUR", PositionStatus.ACTIVE,
                Instant.parse("2024-01-03T12:00:00Z"), "OPS00001");

        repository.saveAndFlush(record);
        entityManager.clear();

        PositionRecord found = repository.findById(key).orElseThrow();
        assertEquals(new BigDecimal("20.5000"), found.getQuantity());
        assertEquals(new BigDecimal("2000.00"), found.getCostBasis());
        assertEquals(new BigDecimal("2150.75"), found.getMarketValue());
        assertEquals("EUR", found.getCurrencyCode());
        assertEquals(PositionStatus.ACTIVE, found.getStatus());
        assertEquals(Instant.parse("2024-01-03T12:00:00Z"), found.getLastMaintAt());
        assertEquals("OPS00001", found.getLastMaintUser());
        assertTrue(repository.findByKeyPortfolioIdAndKeyPositionDate(
                "PORT0002", LocalDate.of(2024, 1, 3)).stream()
                .anyMatch(value -> value.getKey().equals(key)));
    }

}

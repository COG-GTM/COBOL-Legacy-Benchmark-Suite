package com.clbs.portfolio;

import com.clbs.portfolio.domain.TransactionRecord;
import com.clbs.portfolio.domain.TransactionRecordKey;
import com.clbs.portfolio.domain.enums.TransactionStatus;
import com.clbs.portfolio.domain.enums.TransactionType;
import com.clbs.portfolio.persistence.repository.TransactionRecordRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class TransactionRecordRepositoryTest {
    @Autowired
    private TransactionRecordRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndReloadsTransactionRecordUsingCobolCodes() {
        TransactionRecordKey key = new TransactionRecordKey(
                "PORT0001", LocalDate.of(2024, 1, 2), LocalTime.of(9, 30, 15), "000001");
        TransactionRecord record = new TransactionRecord(key, "INVEST0001", TransactionType.BUY,
                new BigDecimal("12.3456"), new BigDecimal("101.2345"), new BigDecimal("1246.24"),
                "USD", TransactionStatus.DONE, Instant.parse("2024-01-02T09:31:00Z"), "BATCH01");

        repository.saveAndFlush(record);
        entityManager.clear();

        TransactionRecord found = repository.findById(key).orElseThrow();
        assertEquals("INVEST0001", found.getInvestmentId());
        assertEquals(TransactionType.BUY, found.getTransactionType());
        assertEquals(new BigDecimal("12.3456"), found.getQuantity());
        assertEquals(new BigDecimal("101.2345"), found.getPrice());
        assertEquals(new BigDecimal("1246.24"), found.getAmount());
        assertEquals("USD", found.getCurrencyCode());
        assertEquals(TransactionStatus.DONE, found.getStatus());
        assertEquals(Instant.parse("2024-01-02T09:31:00Z"), found.getProcessedAt());
        assertEquals("BATCH01", found.getProcessUser());
        assertTrue(repository.findByKeyPortfolioIdOrderByKeyTransactionDateDesc("PORT0001")
                .stream().anyMatch(value -> value.getKey().equals(key)));

        assertEquals("BU", jdbcTemplate.queryForObject(
                "select transaction_type from transaction_record where portfolio_id = ?",
                String.class, "PORT0001"));
        assertEquals("D", jdbcTemplate.queryForObject(
                "select status from transaction_record where portfolio_id = ?",
                String.class, "PORT0001"));
    }

}

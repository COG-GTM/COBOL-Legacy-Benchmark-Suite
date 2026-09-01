package com.clbs.portfolio;

import com.clbs.portfolio.domain.HistoryRecord;
import com.clbs.portfolio.domain.HistoryRecordKey;
import com.clbs.portfolio.domain.enums.HistoryActionCode;
import com.clbs.portfolio.domain.enums.HistoryRecordType;
import com.clbs.portfolio.persistence.repository.HistoryRecordRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class HistoryRecordRepositoryTest {
    @Autowired
    private HistoryRecordRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesAndReloadsHistoryRecordUsingCobolCodes() {
        HistoryRecordKey key = new HistoryRecordKey(
                "PORT0003", LocalDate.of(2024, 1, 4), LocalTime.of(14, 45, 10), "0001");
        HistoryRecord record = new HistoryRecord(key, HistoryRecordType.TRANSACTION,
                HistoryActionCode.CHANGE, "before", "after", "R001",
                Instant.parse("2024-01-04T14:46:00Z"), "AUDIT01");

        repository.saveAndFlush(record);
        entityManager.clear();

        HistoryRecord found = repository.findById(key).orElseThrow();
        assertEquals(HistoryRecordType.TRANSACTION, found.getRecordType());
        assertEquals(HistoryActionCode.CHANGE, found.getActionCode());
        assertEquals("before", found.getBeforeImage());
        assertEquals("after", found.getAfterImage());
        assertEquals("R001", found.getReasonCode());
        assertEquals(Instant.parse("2024-01-04T14:46:00Z"), found.getProcessedAt());
        assertEquals("AUDIT01", found.getProcessUser());
        assertTrue(repository.findByKeyPortfolioId("PORT0003").stream()
                .anyMatch(value -> value.getKey().equals(key)));
        assertEquals("TR", jdbcTemplate.queryForObject(
                "select record_type from history_record where portfolio_id = ?",
                String.class, "PORT0003"));
        assertEquals("C", jdbcTemplate.queryForObject(
                "select action_code from history_record where portfolio_id = ?",
                String.class, "PORT0003"));
    }

}

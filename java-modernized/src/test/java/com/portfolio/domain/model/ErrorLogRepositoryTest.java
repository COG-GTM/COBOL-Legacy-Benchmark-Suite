package com.portfolio.domain.model;

import com.portfolio.domain.enums.ErrorSeverity;
import com.portfolio.domain.enums.ErrorType;
import com.portfolio.domain.repository.ErrorLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ErrorLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @Test
    void shouldPersistAndReadErrorLog() {
        ErrorLog errorLog = createErrorLog(
                LocalDateTime.of(2024, 3, 15, 10, 30, 0),
                "PORTTEST",
                ErrorType.APPLICATION,
                ErrorSeverity.ERROR,
                "ERR00001",
                LocalDate.of(2024, 3, 15));
        entityManager.persistAndFlush(errorLog);
        entityManager.clear();

        ErrorLogId id = new ErrorLogId(LocalDateTime.of(2024, 3, 15, 10, 30, 0), "PORTTEST");
        Optional<ErrorLog> found = errorLogRepository.findById(id);

        assertThat(found).isPresent();
        ErrorLog e = found.get();
        assertThat(e.getErrorType()).isEqualTo(ErrorType.APPLICATION);
        assertThat(e.getErrorSeverity()).isEqualTo(ErrorSeverity.ERROR);
        assertThat(e.getErrorCode()).isEqualTo("ERR00001");
        assertThat(e.getErrorMessage()).isEqualTo("Test error message");
    }

    @Test
    void shouldStoreEnumValuesCorrectly() {
        ErrorLog errorLog = createErrorLog(
                LocalDateTime.of(2024, 3, 15, 11, 0, 0),
                "PORTLOAD",
                ErrorType.SYSTEM,
                ErrorSeverity.SEVERE,
                "SYS00001",
                LocalDate.of(2024, 3, 15));
        entityManager.persistAndFlush(errorLog);
        entityManager.clear();

        Object typeValue = entityManager.getEntityManager()
                .createNativeQuery("SELECT error_type FROM error_log WHERE program_id = 'PORTLOAD'")
                .getSingleResult();
        assertThat(typeValue.toString()).isEqualTo("S");

        Object severityValue = entityManager.getEntityManager()
                .createNativeQuery("SELECT error_severity FROM error_log WHERE program_id = 'PORTLOAD'")
                .getSingleResult();
        assertThat(((Number) severityValue).intValue()).isEqualTo(4);
    }

    @Test
    void shouldHandleCompositeKeyEquality() {
        ErrorLogId id1 = new ErrorLogId(LocalDateTime.of(2024, 3, 15, 10, 30, 0), "PORTTEST");
        ErrorLogId id2 = new ErrorLogId(LocalDateTime.of(2024, 3, 15, 10, 30, 0), "PORTTEST");
        assertThat(id1).isEqualTo(id2);
        assertThat(id1.hashCode()).isEqualTo(id2.hashCode());
    }

    @Test
    @Transactional
    void shouldDeleteByProcessDateBefore() {
        entityManager.persistAndFlush(createErrorLog(
                LocalDateTime.of(2024, 1, 15, 10, 0, 0), "PROG0001",
                ErrorType.DATA, ErrorSeverity.WARNING, "DAT00001",
                LocalDate.of(2024, 1, 15)));
        entityManager.persistAndFlush(createErrorLog(
                LocalDateTime.of(2024, 3, 15, 10, 0, 0), "PROG0002",
                ErrorType.APPLICATION, ErrorSeverity.ERROR, "APP00001",
                LocalDate.of(2024, 3, 15)));
        entityManager.flush();
        entityManager.clear();

        errorLogRepository.deleteByProcessDateBefore(LocalDate.of(2024, 2, 1));

        long count = errorLogRepository.count();
        assertThat(count).isEqualTo(1);
    }

    private ErrorLog createErrorLog(LocalDateTime timestamp, String programId,
            ErrorType type, ErrorSeverity severity, String code, LocalDate processDate) {
        ErrorLog log = new ErrorLog();
        log.setId(new ErrorLogId(timestamp, programId));
        log.setErrorType(type);
        log.setErrorSeverity(severity);
        log.setErrorCode(code);
        log.setErrorMessage("Test error message");
        log.setProcessDate(processDate);
        log.setProcessTime(LocalTime.of(10, 0, 0));
        log.setUserId("TESTUSER");
        log.setAdditionalInfo("Additional test info");
        return log;
    }
}

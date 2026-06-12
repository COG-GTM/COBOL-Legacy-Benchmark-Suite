package com.benchmark.portfolio.common.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.benchmark.portfolio.common.entity.ErrorLog;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ErrorLogRepositoryTest extends AbstractRepositoryIntegrationTest {

    @Autowired
    private ErrorLogRepository repository;

    private ErrorLog error(String programId, LocalDate date, LocalTime time) {
        ErrorLog error = new ErrorLog();
        error.setErrorDate(date);
        error.setErrorTime(time);
        error.setProgramId(programId);
        error.setErrorCategory("VS");
        error.setErrorCode("E001");
        error.setErrorSeverity((short) 8);
        error.setErrorText("VSAM read failed");
        return error;
    }

    @BeforeEach
    void seed() {
        repository.saveAll(List.of(
                error("PORTTRAN", LocalDate.of(2024, 6, 1), LocalTime.of(9, 0)),
                error("PORTTRAN", LocalDate.of(2024, 6, 2), LocalTime.of(10, 0)),
                error("HISTLD00", LocalDate.of(2024, 6, 1), LocalTime.of(11, 0))));
        repository.flush();
    }

    @Test
    void crudRoundTrip() {
        ErrorLog created = repository.saveAndFlush(
                error("INQPORT", LocalDate.of(2024, 7, 1), LocalTime.of(12, 0)));
        assertThat(created.getErrorLogId()).isNotNull();

        ErrorLog loaded = repository.findById(created.getErrorLogId()).orElseThrow();
        assertThat(loaded.getErrorCategory()).isEqualTo("VS");

        loaded.setErrorDetails("Detail text");
        repository.saveAndFlush(loaded);
        assertThat(repository.findById(created.getErrorLogId()).orElseThrow().getErrorDetails())
                .isEqualTo("Detail text");

        repository.deleteById(created.getErrorLogId());
        repository.flush();
        assertThat(repository.findById(created.getErrorLogId())).isEmpty();
    }

    @Test
    void errorsByProgramNewestFirst() {
        List<ErrorLog> result =
                repository.findByProgramIdOrderByErrorDateDescErrorTimeDesc("PORTTRAN");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getErrorDate()).isEqualTo(LocalDate.of(2024, 6, 2));
    }

    @Test
    void errorsByDateInTimeOrder() {
        List<ErrorLog> result =
                repository.findByErrorDateOrderByErrorTimeAsc(LocalDate.of(2024, 6, 1));
        assertThat(result).extracting(ErrorLog::getErrorTime)
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(11, 0));
    }
}

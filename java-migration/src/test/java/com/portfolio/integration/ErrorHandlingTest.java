package com.portfolio.integration;

import com.portfolio.service.common.ErrorProcessingService;
import com.portfolio.repository.ErrorLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ErrorHandlingTest {

    @Autowired
    private ErrorProcessingService errorProcessingService;

    @Autowired
    private ErrorLogRepository errorLogRepository;

    @BeforeEach
    void setUp() {
        errorLogRepository.deleteAll();
    }

    @Test
    void processError_logsToDatabase() {
        int severity = errorProcessingService.processError(
                "TESTPROG", "VS", "E001", 1, "Test error", "Detail info");

        assertEquals(1, severity);
        assertEquals(1, errorLogRepository.count());
    }

    @Test
    void processError_multipleEntries() {
        errorProcessingService.processError("PROG1", "VS", "E001", 1, "Info error", null);
        errorProcessingService.processError("PROG2", "PR", "E003", 3, "Severe error", "Details");
        errorProcessingService.processError("PROG3", "SY", "E005", 4, "System error", null);

        assertEquals(3, errorLogRepository.count());
    }
}

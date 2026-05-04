package com.portfolio.service;

import com.portfolio.entity.ErrorCategory;
import com.portfolio.entity.ErrorSeverity;
import com.portfolio.repository.ErrorLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorServiceTest {

    @Mock
    private ErrorLogRepository errorLogRepository;

    @InjectMocks
    private ErrorService errorService;

    @Test
    void processError_success_returnsZero() {
        when(errorLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int returnCode = errorService.processError(
                "TESTPROG", ErrorCategory.PROCESSING, "E001",
                ErrorSeverity.SUCCESS, "Test message", "Details");

        assertEquals(0, returnCode);
        verify(errorLogRepository).save(any());
    }

    @Test
    void processError_warning_returnsFour() {
        when(errorLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int returnCode = errorService.processError(
                "TESTPROG", ErrorCategory.VALIDATION, "W001",
                ErrorSeverity.WARNING, "Warning message", null);

        assertEquals(4, returnCode);
    }

    @Test
    void processError_error_returnsEight() {
        when(errorLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int returnCode = errorService.processError(
                "TESTPROG", ErrorCategory.SYSTEM, "E002",
                ErrorSeverity.ERROR, "Error message", "Error details");

        assertEquals(8, returnCode);
    }

    @Test
    void processError_terminal_returnsSixteen() {
        when(errorLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int returnCode = errorService.processError(
                "TESTPROG", ErrorCategory.SYSTEM, "T001",
                ErrorSeverity.TERMINAL, "Terminal error", "Critical failure");

        assertEquals(16, returnCode);
    }
}

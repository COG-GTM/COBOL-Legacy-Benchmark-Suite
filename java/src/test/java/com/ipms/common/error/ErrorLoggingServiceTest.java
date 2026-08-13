package com.ipms.common.error;

import com.ipms.domain.ErrorCategory;
import com.ipms.domain.ReturnCodes;
import com.ipms.persistence.entity.ErrorLog;
import com.ipms.persistence.repository.ErrorLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import(ErrorLoggingService.class)
class ErrorLoggingServiceTest {

    @Autowired
    private ErrorLoggingService service;

    @Autowired
    private ErrorLogRepository repository;

    @Test
    void persistsErrorAndReturnsSeverity() {
        ErrorRequest request = new ErrorRequest(
                "PORTMSTR", ErrorCategory.VALIDATION, "E001",
                ReturnCodes.RC_ERROR, "Invalid portfolio ID",
                "Portfolio ID must start with PORT");

        int returnCode = service.processError(request);

        assertEquals(ReturnCodes.RC_ERROR, returnCode);

        List<ErrorLog> entries = repository.findByProgramIdOrderByErrorTimestampDesc("PORTMSTR");
        assertEquals(1, entries.size());
        ErrorLog entry = entries.get(0);
        assertEquals("A", entry.getErrorType());
        assertEquals(3, entry.getErrorSeverity());
        assertEquals("E001", entry.getErrorCode());
        assertEquals("Invalid portfolio ID", entry.getErrorMessage());
        assertEquals("Portfolio ID must start with PORT", entry.getAdditionalInfo());
    }

    @Test
    void mapsCategoriesToErrorTypes() {
        assertEquals("S", ErrorLoggingService.toErrorType(ErrorCategory.SYSTEM));
        assertEquals("D", ErrorLoggingService.toErrorType(ErrorCategory.VSAM));
        assertEquals("A", ErrorLoggingService.toErrorType(ErrorCategory.VALIDATION));
        assertEquals("A", ErrorLoggingService.toErrorType(ErrorCategory.PROCESSING));
    }

    @Test
    void mapsSeverities() {
        assertEquals(1, ErrorLoggingService.toDb2Severity(ReturnCodes.RC_SUCCESS));
        assertEquals(2, ErrorLoggingService.toDb2Severity(ReturnCodes.RC_WARNING));
        assertEquals(3, ErrorLoggingService.toDb2Severity(ReturnCodes.RC_ERROR));
        assertEquals(4, ErrorLoggingService.toDb2Severity(ReturnCodes.RC_SEVERE));
        assertEquals(4, ErrorLoggingService.toDb2Severity(ReturnCodes.RC_CRITICAL));
    }

    @Test
    void rejectsInvalidRequests() {
        assertThrows(IllegalArgumentException.class,
                () -> new ErrorRequest(" ", ErrorCategory.SYSTEM, "E001", 8, "text", "details"));
        assertThrows(IllegalArgumentException.class,
                () -> new ErrorRequest("PGM", null, "E001", 8, "text", "details"));
        assertThrows(IllegalArgumentException.class,
                () -> new ErrorRequest("PGM", ErrorCategory.SYSTEM, "", 8, "text", "details"));
    }
}

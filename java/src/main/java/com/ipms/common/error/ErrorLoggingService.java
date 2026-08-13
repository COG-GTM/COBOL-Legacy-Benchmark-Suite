package com.ipms.common.error;

import com.ipms.domain.ErrorCategory;
import com.ipms.domain.ReturnCodes;
import com.ipms.persistence.entity.ErrorLog;
import com.ipms.persistence.repository.ErrorLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Centralized error logger, ported from {@code src/programs/common/ERRPROC.cbl}.
 *
 * <p>ERRPROC appended a formatted ERR-MESSAGE to the sequential ERRLOG file and displayed
 * it on SYSOUT; here the error is persisted to the ERRLOG table (see ERRLOG.sql) and
 * logged via SLF4J. As in COBOL, the caller's severity is echoed back as the return code.
 */
@Service
public class ErrorLoggingService {

    private static final Logger log = LoggerFactory.getLogger(ErrorLoggingService.class);

    private final ErrorLogRepository errorLogRepository;

    public ErrorLoggingService(ErrorLogRepository errorLogRepository) {
        this.errorLogRepository = errorLogRepository;
    }

    /**
     * Processes an error request (ERRPROC 2000-PROCESS-ERROR): writes the error to the
     * log and returns the severity as the return code.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processError(ErrorRequest request) {
        LocalDateTime now = LocalDateTime.now();

        ErrorLog entry = new ErrorLog();
        entry.setErrorTimestamp(now);
        entry.setProgramId(request.programId());
        entry.setErrorType(toErrorType(request.category()));
        entry.setErrorSeverity(toDb2Severity(request.severity()));
        entry.setErrorCode(request.errorCode());
        entry.setErrorMessage(truncate(request.errorText(), 200));
        entry.setProcessDate(now.toLocalDate());
        entry.setProcessTime(now.toLocalTime());
        entry.setUserId(System.getProperty("user.name", "UNKNOWN"));
        entry.setAdditionalInfo(truncate(request.errorDetails(), 500));
        errorLogRepository.save(entry);

        log.error("ERROR DETECTED: {} PROGRAM: {} CATEGORY: {} CODE: {} SEVERITY: {} MESSAGE: {} DETAILS: {}",
                now, request.programId(), request.category().code(), request.errorCode(),
                request.severity(), request.errorText(), request.errorDetails());

        return request.severity();
    }

    /** Maps ERR-CATEGORY (VS/VL/PR/SY) to ERRLOG.ERROR_TYPE (S=System, A=Application, D=Data). */
    static String toErrorType(ErrorCategory category) {
        return switch (category) {
            case SYSTEM -> "S";
            case VSAM -> "D";
            case VALIDATION, PROCESSING -> "A";
        };
    }

    /** Maps COBOL return-code severities (0/4/8/12/16) to ERRLOG.ERROR_SEVERITY (1-4). */
    static int toDb2Severity(int returnCodeSeverity) {
        if (returnCodeSeverity <= ReturnCodes.RC_SUCCESS) {
            return 1;
        }
        if (returnCodeSeverity <= ReturnCodes.RC_WARNING) {
            return 2;
        }
        if (returnCodeSeverity <= ReturnCodes.RC_ERROR) {
            return 3;
        }
        return 4;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}

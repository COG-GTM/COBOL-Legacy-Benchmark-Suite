package com.portfolio.infrastructure.error;

import com.portfolio.domain.model.ErrorCategory;
import com.portfolio.domain.model.ErrorSeverity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

/**
 * Spring Batch SkipListener that logs errors using COBOL ERRHAND.cpy categories.
 */
@Component
public class BatchErrorListener implements SkipListener<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(BatchErrorListener.class);

    @Override
    public void onSkipInRead(Throwable t) {
        logError("READ", ErrorCategory.VSAM, ErrorSeverity.ERROR, t);
    }

    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        logError("PROCESS", ErrorCategory.PROCESSING, ErrorSeverity.ERROR, t);
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        logError("WRITE", ErrorCategory.VSAM, ErrorSeverity.ERROR, t);
    }

    private void logError(String phase, ErrorCategory category, ErrorSeverity severity, Throwable t) {
        log.error("Batch {} skip [category={}, severity={}]: {}",
                phase, category.getCode(), severity.getCode(), t.getMessage(), t);
    }
}

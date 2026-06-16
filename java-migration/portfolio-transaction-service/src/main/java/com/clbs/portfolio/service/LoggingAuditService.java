package com.clbs.portfolio.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Default {@link AuditService} that writes audit entries to the application log.
 * Stands in for the COBOL {@code AUDPROC} subprogram.
 */
@Service
public class LoggingAuditService implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditService.class);

    @Override
    public void record(AuditRecord record) {
        log.info("AUDIT program={} type={} action={} status={} portfolio={} account={} msg={}",
                record.program(), record.type(), record.action(), record.status(),
                record.portfolioId(), record.accountNo(), record.message());
    }
}

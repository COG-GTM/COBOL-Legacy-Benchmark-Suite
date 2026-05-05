package com.portfolio.portmstr.batch;

import com.portfolio.portmstr.batch.checkpoint.CheckpointService;
import com.portfolio.portmstr.dto.PortfolioRequest;
import com.portfolio.portmstr.model.BatchCheckpoint;
import com.portfolio.portmstr.model.PortfolioMaster;
import com.portfolio.portmstr.model.enums.PortfolioStatus;
import com.portfolio.portmstr.repository.PortfolioMasterRepository;
import com.portfolio.portmstr.service.AuditService;
import com.portfolio.portmstr.service.ErrorLoggingService;
import com.portfolio.portmstr.service.PortfolioMasterService;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * Portfolio batch item processor.
 * Translates COBOL PORTADD.cbl / PORTUPDT.cbl batch processing logic.
 *
 * COBOL flow mapping:
 *   PORTADD.cbl 2000-PROCESS-INPUT -> process() for CREATE operations
 *   PORTUPDT.cbl 2000-PROCESS-INPUT -> process() for UPDATE operations
 *   PORTDEL.cbl 2000-PROCESS-INPUT -> process() for DELETE operations
 *
 * This processor handles checkpoint tracking through the CheckpointService,
 * preserving the COBOL checkpoint/restart pattern from CKPRST.cpy.
 */
@Component
public class PortfolioBatchProcessor implements ItemProcessor<PortfolioRequest, PortfolioMaster> {

    private static final Logger log = LoggerFactory.getLogger(PortfolioBatchProcessor.class);
    private static final String PROGRAM_ID = "PORTBAT";

    private final PortfolioMasterService portfolioService;
    private final CheckpointService checkpointService;
    private final ErrorLoggingService errorLoggingService;

    private long recordsRead = 0;
    private long recordsProcessed = 0;
    private long recordsError = 0;

    public PortfolioBatchProcessor(PortfolioMasterService portfolioService,
                                   CheckpointService checkpointService,
                                   ErrorLoggingService errorLoggingService) {
        this.portfolioService = portfolioService;
        this.checkpointService = checkpointService;
        this.errorLoggingService = errorLoggingService;
    }

    @Override
    public PortfolioMaster process(PortfolioRequest request) {
        recordsRead++;

        try {
            portfolioService.createPortfolio(request);
            recordsProcessed++;

            if (checkpointService.shouldTakeCheckpoint(recordsProcessed)) {
                checkpointService.takeCheckpoint(
                        PROGRAM_ID,
                        request.portfolioId(),
                        recordsRead,
                        recordsProcessed,
                        recordsError,
                        "20"
                );
            }

            return null;
        } catch (Exception e) {
            recordsError++;

            errorLoggingService.logError(
                    PROGRAM_ID,
                    'A',
                    8,
                    "PROC-ERR",
                    e.getMessage(),
                    "SYSTEM",
                    "Portfolio: " + request.portfolioId()
            );

            if (!checkpointService.shouldContinueProcessing(recordsError)) {
                log.error("Maximum error threshold reached. Stopping batch processing.");
                checkpointService.failCheckpoint(PROGRAM_ID,
                        "Max errors exceeded: " + recordsError);
                throw new RuntimeException("Maximum error threshold exceeded", e);
            }

            log.warn("Error processing portfolio {}: {}", request.portfolioId(), e.getMessage());
            return null;
        }
    }

    public long getRecordsRead() {
        return recordsRead;
    }

    public long getRecordsProcessed() {
        return recordsProcessed;
    }

    public long getRecordsError() {
        return recordsError;
    }

    public void resetCounters() {
        recordsRead = 0;
        recordsProcessed = 0;
        recordsError = 0;
    }
}

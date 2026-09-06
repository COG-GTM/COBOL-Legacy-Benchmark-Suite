package com.cog.portfolio.batch;

import com.cog.portfolio.common.PortfolioException;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionStatus;
import com.cog.portfolio.service.PositionUpdateService;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

// PROVISIONAL - requiere validación humana
/**
 * Item processor for the {@code positionUpdateStep} (role of POSUPD00 /
 * "POSUPDT"). {@code src/programs/batch/POSUPDT.cbl} is empty and there is no
 * POSUPD00 source, so this is a RECONSTRUCTION delegating to
 * {@link PositionUpdateService}. Business failures (e.g. "Insufficient units
 * for sale") mark the transaction FAILED and the step continues, mirroring the
 * error-counting loop of PORTTRAN.
 *
 * <p>TODO validación humana: confirmar la semántica de POSUPD00 y si POSUPDT.cbl
 * es su equivalente.
 */
@Component
public class PositionUpdateProcessor implements ItemProcessor<Transaction, Transaction> {

    private static final Logger log = LoggerFactory.getLogger(PositionUpdateProcessor.class);

    private final PositionUpdateService positionUpdateService;
    private final AtomicLong failed = new AtomicLong();

    public PositionUpdateProcessor(PositionUpdateService positionUpdateService) {
        this.positionUpdateService = positionUpdateService;
    }

    // PROVISIONAL - requiere validación humana
    @Override
    public Transaction process(Transaction trn) {
        try {
            positionUpdateService.applyTransaction(trn);
        } catch (PortfolioException e) {
            failed.incrementAndGet();
            trn.setStatus(TransactionStatus.FAILED);
            log.warn("Position update failed for {}: {}", trn.getId(), e.getMessage());
        }
        return trn;
    }

    public long getFailedCount() {
        return failed.get();
    }

    @BeforeStep
    public void resetCounters(StepExecution stepExecution) {
        failed.set(0);
    }
}

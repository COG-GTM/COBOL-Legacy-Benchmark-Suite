package com.cog.portfolio.batch;

import com.cog.portfolio.common.ProcessingResult;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionStatus;
import com.cog.portfolio.service.TransactionProcessingService;
import com.cog.portfolio.validation.PortfolioValidator;
import com.cog.portfolio.validation.ValidationResult;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

// PROVISIONAL - requiere validación humana
/**
 * Item processor for the {@code transactionValidationStep} (role of TRNVAL00 /
 * "TRNMAIN"). There is NO TRNVAL00 source in the repository; this is a
 * RECONSTRUCTION that applies the PORTVALD.cbl format rules (portfolio id,
 * account, amount range) plus the PORTTRAN.cbl pre-checks (type, quantity,
 * price, amount &gt; 0) to each PENDING transaction. Rejected transactions are
 * marked {@link TransactionStatus.FAILED}; accepted ones stay PENDING for
 * the position update step.
 *
 * <p>TODO validación humana: confirmar la semántica esperada de TRNVAL00.
 */
@Component
public class TransactionValidationProcessor implements ItemProcessor<Transaction, Transaction> {

    private static final Logger log = LoggerFactory.getLogger(TransactionValidationProcessor.class);

    private final PortfolioValidator validator;
    private final TransactionProcessingService transactionProcessingService;
    private final AtomicLong rejected = new AtomicLong();

    public TransactionValidationProcessor(PortfolioValidator validator,
                                          TransactionProcessingService transactionProcessingService) {
        this.validator = validator;
        this.transactionProcessingService = transactionProcessingService;
    }

    // PROVISIONAL - requiere validación humana
    @Override
    public Transaction process(Transaction trn) {
        String reason = reject(trn);
        if (reason != null) {
            rejected.incrementAndGet();
            trn.setStatus(TransactionStatus.FAILED);
            log.warn("Transaction {} rejected: {}", trn.getId(), reason);
        }
        return trn;
    }

    String reject(Transaction trn) {
        ValidationResult id = validator.validatePortfolioId(trn.getId().getPortfolioId());
        if (!id.isValid()) {
            return id.message();
        }
        ValidationResult amount = validator.validateAmount(trn.getAmount());
        if (!amount.isValid()) {
            return amount.message();
        }
        ProcessingResult result = transactionProcessingService.validate(trn);
        return result.isSuccess() ? null : result.message();
    }

    public long getRejectedCount() {
        return rejected.get();
    }

    @BeforeStep
    public void resetCounters(StepExecution stepExecution) {
        rejected.set(0);
    }
}

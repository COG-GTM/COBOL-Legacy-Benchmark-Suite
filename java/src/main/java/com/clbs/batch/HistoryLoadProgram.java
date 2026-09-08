package com.clbs.batch;

import com.clbs.common.ErrorMessage;
import com.clbs.common.ErrorProcessor;
import com.clbs.common.ProgramResult;
import com.clbs.db2.PositionHistoryEntity;
import com.clbs.db2.PositionHistoryRepository;
import com.clbs.domain.ErrorCodes;
import com.clbs.domain.TransactionHistoryRecord;
import com.clbs.store.DatasetCatalog;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HISTLD00 — loads TRANHIST into the POSHIST table. The EXEC SQL INSERT becomes a JPA save; the
 * COMMIT every WS-COMMIT-THRESHOLD records becomes an explicit flush plus the BCHCTL checkpoint
 * rewrite of 2310-UPDATE-CHECKPOINT.
 */
@Service
public class HistoryLoadProgram {

    public static final String PROGRAM = "HISTLD00";
    public static final int COMMIT_THRESHOLD = 1000;
    public static final int ERROR_LIMIT = 100;
    public static final String ERR_CONTROL_NOT_FOUND = "Control record not found";
    public static final String ERR_CHECKPOINT = "Error updating checkpoint";

    private final DatasetCatalog datasets;
    private final PositionHistoryRepository repository;
    private final ErrorProcessor errorProcessor;

    public HistoryLoadProgram(DatasetCatalog datasets, PositionHistoryRepository repository,
            ErrorProcessor errorProcessor) {
        this.datasets = datasets;
        this.repository = repository;
        this.errorProcessor = errorProcessor;
    }

    /** 0000-MAIN. */
    @Transactional
    public ProgramResult run(String processDate) {
        ProgramResult result = new ProgramResult(PROGRAM);
        BatchControlRecord control = initCheckpoints(processDate);

        long read = 0;
        long written = 0;
        long errors = 0;
        int sinceCommit = 0;

        if (control == null) {
            errors++;
            result.display(ERR_CONTROL_NOT_FOUND);
        }

        for (TransactionHistoryRecord history : datasets.transactionHistory().all()) {
            if (errors > ERROR_LIMIT) {
                break;
            }
            read++;
            try {
                repository.save(toEntity(history));
                written++;
            } catch (RuntimeException e) {
                errors++;
                error("Insert failed for " + history.key() + ": " + e.getMessage());
            }

            // 2300-CHECK-COMMIT
            if (++sinceCommit >= COMMIT_THRESHOLD) {
                sinceCommit = 0;
                repository.flush();
                checkpoint(control, read, written);
            }
        }

        // 3100-FINAL-COMMIT
        repository.flush();
        checkpoint(control, read, written);

        // 3400-DISPLAY-STATS
        result.display("HISTLD00 Processing Statistics:");
        result.display("  Records Read:    " + read);
        result.display("  Records Written: " + written);
        result.display("  Errors:          " + errors);
        result.count("read", read).count("written", written).count("errors", errors);
        result.setReturnCode((int) errors);
        return result;
    }

    /** 1300-INIT-CHECKPOINTS. */
    private BatchControlRecord initCheckpoints(String processDate) {
        BatchControlRecord control = datasets.batchControlFile().all().stream()
                .filter(record -> PROGRAM.equals(record.getJobName().trim()))
                .findFirst()
                .orElse(null);
        if (control == null) {
            error(ERR_CONTROL_NOT_FOUND);
            return null;
        }
        control.setProcessDate(processDate);
        control.setStatus(BatchControlRecord.ACTIVE);
        datasets.batchControlFile().rewrite(control);
        return control;
    }

    /** 2310-UPDATE-CHECKPOINT. */
    private void checkpoint(BatchControlRecord control, long read, long written) {
        if (control == null) {
            return;
        }
        control.setRecordsRead(read);
        control.setRecordsWritten(written);
        if (!com.clbs.store.FileStatus.SUCCESS.equals(datasets.batchControlFile().rewrite(control))) {
            error(ERR_CHECKPOINT);
        }
    }

    /** 2200-LOAD-TO-DB2 field moves. */
    static PositionHistoryEntity toEntity(TransactionHistoryRecord history) {
        PositionHistoryEntity entity = new PositionHistoryEntity();
        entity.setAccountNo(history.getAccountNo());
        entity.setPortfolioId(history.getPortfolioId());
        entity.setTransDate(history.getTransDate());
        entity.setTransTime(history.getTransTime());
        entity.setTransType(history.getTransType());
        entity.setInvestmentId(history.getSecurityId());
        entity.setTransUnits(history.getQuantity());
        entity.setTransPrice(history.getPrice());
        entity.setTransAmount(history.getAmount());
        entity.setTransFees(history.getFees());
        entity.setTotalAmount(history.getTotalAmount());
        entity.setCostBasis(history.getCostBasis());
        entity.setGainLoss(history.getGainLoss());
        entity.setProcessDate(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        entity.setProcessUser(PROGRAM);
        return entity;
    }

    /** 9000-ERROR-ROUTINE. */
    private void error(String text) {
        errorProcessor.process(new ErrorMessage(PROGRAM, ErrorCodes.CAT_SYSTEM,
                ErrorCodes.DB_ERROR, BatchConstants.RC_ERROR, text, ""));
    }
}

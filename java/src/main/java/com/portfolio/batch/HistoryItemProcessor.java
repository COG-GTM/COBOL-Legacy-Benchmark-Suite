package com.portfolio.batch;

import com.portfolio.common.ErrorHandlingService;
import com.portfolio.domain.PositionHistory;
import com.portfolio.domain.TransactionHistoryFileRecord;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Validation + mapping step of the HISTLD00 migration
 * (COBOL 2200-LOAD-TO-DB2 field moves TH-* → PH-*).
 *
 * <p>Invalid records are counted (WS-ERROR-COUNT), logged via the ERRPROC
 * migration, and filtered (return null) so the load continues — until the
 * error count exceeds 100, at which point the job aborts, matching
 * {@code UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100}.
 */
@Component
public class HistoryItemProcessor
        implements ItemProcessor<TransactionHistoryFileRecord, PositionHistory> {

    private static final Set<String> VALID_TRANS_TYPES = Set.of("BU", "SL", "TR", "FE");

    private final HistoryLoadStats stats;
    private final ErrorHandlingService errorHandlingService;

    public HistoryItemProcessor(HistoryLoadStats stats, ErrorHandlingService errorHandlingService) {
        this.stats = stats;
        this.errorHandlingService = errorHandlingService;
    }

    @Override
    public PositionHistory process(TransactionHistoryFileRecord item) {
        stats.incrementRecordsRead();

        String validationError = validate(item);
        if (validationError != null) {
            long errors = stats.incrementErrorCount();
            errorHandlingService.logError("HISTLD00", "V", 2, "HIST0001",
                    validationError, String.valueOf(item.getKey() == null ? null
                            : item.getKey().getPortfolioId() + "/" + item.getKey().getSequenceNo()));
            if (errors > HistoryLoadStats.MAX_ERRORS) {
                throw new ErrorLimitExceededException(errors);
            }
            return null;
        }

        PositionHistory history = new PositionHistory();
        history.setKey(new PositionHistory.Key(
                item.getAccountNo(),
                item.getKey().getPortfolioId(),
                item.getKey().getTransDate(),
                item.getKey().getTransTime()));
        history.setTransType(item.getTransType());
        history.setSecurityId(item.getSecurityId());
        history.setQuantity(item.getQuantity());
        history.setPrice(item.getPrice());
        history.setAmount(item.getAmount());
        history.setFees(item.getFees());
        history.setTotalAmount(item.getTotalAmount());
        history.setCostBasis(item.getCostBasis());
        history.setGainLoss(item.getGainLoss());

        // POSHIST audit columns (PROCESS_DATE/TIME default CURRENT DATE/TIME in DDL)
        LocalDateTime now = LocalDateTime.now();
        history.setProcessDate(now.toLocalDate());
        history.setProcessTime(now.toLocalTime());
        history.setProgramId("HISTLD00");
        history.setUserId(currentUserId());
        history.setAuditTimestamp(now);
        return history;
    }

    /** USER_ID is CHAR(8); COBOL PIC X(8) truncated silently. */
    private static String currentUserId() {
        String user = System.getProperty("user.name", "BATCH");
        return user.length() <= 8 ? user : user.substring(0, 8);
    }

    private String validate(TransactionHistoryFileRecord item) {
        if (item.getKey() == null
                || item.getKey().getTransDate() == null
                || item.getKey().getTransTime() == null
                || isBlank(item.getKey().getPortfolioId())) {
            return "Missing transaction key fields";
        }
        if (isBlank(item.getAccountNo())) {
            return "Missing account number";
        }
        if (item.getTransType() == null || !VALID_TRANS_TYPES.contains(item.getTransType())) {
            return "Invalid transaction type: " + item.getTransType();
        }
        if (isBlank(item.getSecurityId())) {
            return "Missing security ID";
        }
        if (isNullOrNegativeRequired(item.getQuantity())
                || isNullOrNegativeRequired(item.getPrice())
                || item.getAmount() == null
                || item.getTotalAmount() == null
                || item.getCostBasis() == null
                || item.getGainLoss() == null) {
            return "Missing or invalid numeric fields";
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isNullOrNegativeRequired(BigDecimal value) {
        return value == null || value.signum() < 0;
    }
}

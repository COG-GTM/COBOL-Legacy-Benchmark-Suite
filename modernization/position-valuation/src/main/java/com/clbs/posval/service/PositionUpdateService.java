package com.clbs.posval.service;

import com.clbs.posval.audit.AuditRecord;
import com.clbs.posval.audit.AuditTrailWriter;
import com.clbs.posval.cobol.CobolDecimal;
import com.clbs.posval.cobol.PackedField;
import com.clbs.posval.domain.PortfolioPosition;
import com.clbs.posval.domain.TransactionRecord;
import com.clbs.posval.domain.TransactionType;
import com.clbs.posval.repository.PortfolioPositionStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Port of the position update half of {@code src/programs/portfolio/PORTTRAN.cbl}
 * ({@code 2200-UPDATE-POSITIONS} and its subordinate paragraphs) — the reconstruction of the
 * position update step that {@code src/programs/batch/POSUPDT.cbl} is documented to perform but
 * does not contain (the file is empty; spec open question OQ-2).
 *
 * <table border="1">
 *   <caption>PORTTRAN position update to Java</caption>
 *   <tr><th>COBOL paragraph</th><th>Java method</th></tr>
 *   <tr><td>{@code 2200-UPDATE-POSITIONS}</td><td>{@link #apply(TransactionRecord)}</td></tr>
 *   <tr><td>{@code 2210-PROCESS-BUY}</td><td>{@link #processBuy(TransactionRecord)}</td></tr>
 *   <tr><td>{@code 2220-PROCESS-SELL}</td><td>{@link #processSell(TransactionRecord)}</td></tr>
 *   <tr><td>{@code 2230-PROCESS-TRANSFER}</td><td>{@link #processTransfer(TransactionRecord)}</td></tr>
 *   <tr><td>{@code 2240-PROCESS-FEE}</td><td>{@link #processFee(TransactionRecord)}</td></tr>
 *   <tr><td>{@code 2300-UPDATE-AUDIT-TRAIL}</td><td>{@link #writeAuditTrail}</td></tr>
 * </table>
 *
 * <p>All money arithmetic goes through {@link CobolDecimal} against the {@code PIC} clause of the
 * receiving field, so results truncate toward zero at the field's scale and wrap on overflow
 * exactly as the packed-decimal originals do.
 */
@Service
public class PositionUpdateService {

    public static final String PROGRAM = "PORTTRAN";
    public static final String ERR_PORTFOLIO_NOT_FOUND = "Portfolio not found for update";
    public static final String ERR_PORTFOLIO_NOT_FOUND_FEE = "Portfolio not found for fee";
    public static final String ERR_INSUFFICIENT_UNITS = "Insufficient units for sale";
    public static final String ERR_TRANSFER_NOT_IMPLEMENTED = "Transfer processing not implemented";
    public static final String ERR_UPDATE_FAILED = "Error updating portfolio";

    private final PortfolioPositionStore store;
    private final AuditTrailWriter auditTrailWriter;
    private final Clock clock;

    public PositionUpdateService(
            PortfolioPositionStore store, AuditTrailWriter auditTrailWriter, Clock clock) {
        this.store = store;
        this.auditTrailWriter = auditTrailWriter;
        this.clock = clock;
    }

    /**
     * {@code 2200-UPDATE-POSITIONS}: dispatches on {@code TRN-TYPE} and then always writes the
     * audit trail, whether the update succeeded or not.
     *
     * <p>A transaction type outside the four 88-levels falls through the {@code EVALUATE} without
     * a {@code WHEN OTHER} branch: nothing is updated, no error is raised, and an audit record is
     * still written — with {@code AUD-ACTION} left at its {@code INITIALIZE} value, because the
     * action {@code EVALUATE} in {@code 2300} has no {@code WHEN OTHER} either.
     */
    public Optional<String> apply(TransactionRecord transaction) {
        Optional<TransactionType> type = TransactionType.fromCode(transaction.type());

        Optional<String> error = type.isEmpty() ? Optional.empty() : switch (type.get()) {
            case BUY -> processBuy(transaction);
            case SELL -> processSell(transaction);
            case TRANSFER -> processTransfer(transaction);
            case FEE -> processFee(transaction);
        };

        writeAuditTrail(transaction, type);
        return error;
    }

    /**
     * {@code 2210-PROCESS-BUY}: {@code ADD TRN-QUANTITY TO PORT-TOTAL-UNITS} and
     * {@code ADD TRN-AMOUNT TO PORT-TOTAL-COST}, then {@code REWRITE}.
     *
     * <p>Neither {@code ADD} carries {@code ON SIZE ERROR}, so a total that grows past
     * {@code S9(11)V9(4)} units or {@code S9(13)V9(2)} cost silently wraps rather than failing the
     * transaction.
     */
    public Optional<String> processBuy(TransactionRecord transaction) {
        Optional<PortfolioPosition> existing = store.read(transaction.portfolioId());
        if (existing.isEmpty()) {
            return Optional.of(ERR_PORTFOLIO_NOT_FOUND);
        }
        PortfolioPosition position = existing.get();

        BigDecimal units = CobolDecimal.add(
                position.totalUnits(), transaction.quantity(), PackedField.QUANTITY);
        BigDecimal cost = CobolDecimal.add(
                position.totalCost(), transaction.amount(), PackedField.AMOUNT);

        return rewrite(position.withTotals(units, cost));
    }

    /**
     * {@code 2220-PROCESS-SELL}: rejects the sale when {@code PORT-TOTAL-UNITS < TRN-QUANTITY},
     * otherwise subtracts quantity from units and amount from cost.
     *
     * <p>The guard is on units only. Cost basis is reduced by the sale proceeds
     * ({@code TRN-AMOUNT}), not by the cost of the units sold, so a profitable sale drives the
     * cost basis down faster than the units and a large enough sale drives it negative — there is
     * no guard against that, and no realised gain or loss is computed anywhere in the slice (spec
     * open question OQ-6).
     */
    public Optional<String> processSell(TransactionRecord transaction) {
        Optional<PortfolioPosition> existing = store.read(transaction.portfolioId());
        if (existing.isEmpty()) {
            return Optional.of(ERR_PORTFOLIO_NOT_FOUND);
        }
        PortfolioPosition position = existing.get();

        if (position.totalUnits().compareTo(transaction.quantity()) < 0) {
            return Optional.of(ERR_INSUFFICIENT_UNITS);
        }

        BigDecimal units = CobolDecimal.subtract(
                position.totalUnits(), transaction.quantity(), PackedField.QUANTITY);
        BigDecimal cost = CobolDecimal.subtract(
                position.totalCost(), transaction.amount(), PackedField.AMOUNT);

        return rewrite(position.withTotals(units, cost));
    }

    /**
     * {@code 2230-PROCESS-TRANSFER}: raises {@code 'Transfer processing not implemented'} and
     * changes nothing. Transfers are accepted by validation ({@code 2130} exempts {@code TR} from
     * the price and amount checks) and then always fail here.
     */
    public Optional<String> processTransfer(TransactionRecord transaction) {
        return Optional.of(ERR_TRANSFER_NOT_IMPLEMENTED);
    }

    /**
     * {@code 2240-PROCESS-FEE}: {@code SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST} and
     * {@code REWRITE}. Units are untouched and the cost basis may go negative.
     */
    public Optional<String> processFee(TransactionRecord transaction) {
        Optional<PortfolioPosition> existing = store.read(transaction.portfolioId());
        if (existing.isEmpty()) {
            return Optional.of(ERR_PORTFOLIO_NOT_FOUND_FEE);
        }
        PortfolioPosition position = existing.get();

        BigDecimal cost = CobolDecimal.subtract(
                position.totalCost(), transaction.amount(), PackedField.AMOUNT);

        return rewrite(position.withTotals(position.totalUnits(), cost));
    }

    /**
     * {@code 2300-UPDATE-AUDIT-TRAIL} and {@code 2310-WRITE-AUDIT-RECORD}.
     *
     * <p>{@code AUD-ACTION} follows the COBOL's mapping, which is not the obvious one: a buy is
     * logged as {@code CREATE}, a sell as {@code DELETE}, and transfers and fees as {@code UPDATE}.
     * {@code AUD-BEFORE-IMAGE} is set from {@code PORT-RECORD} after the update has already been
     * rewritten, so the "before" image is in fact the after image.
     *
     * <p>{@code AUD-STATUS} is derived from {@code WS-PORT-STATUS} — the portfolio file status —
     * and not from {@code ERR-TEXT}. A transfer, which always fails with
     * {@code 'Transfer processing not implemented'}, is therefore audited as {@code SUCC}, because
     * the last file operation on the portfolio (the read in {@code 2110}) succeeded.
     */
    private void writeAuditTrail(TransactionRecord transaction, Optional<TransactionType> type) {

        String action = type.map(PositionUpdateService::auditAction).orElse("        ");
        PortfolioPosition current = store.read(transaction.portfolioId()).orElse(null);

        auditTrailWriter.write(new AuditRecord(
                Instant.now(clock).toString(),
                "        ",
                "BATCH   ",
                PROGRAM,
                "        ",
                AuditRecord.TYPE_TRANSACTION,
                action,
                current != null ? AuditRecord.STATUS_SUCCESS : AuditRecord.STATUS_FAILURE,
                transaction.portfolioId(),
                current == null ? "" : current.accountNo(),
                current == null ? "" : current.image(),
                "",
                "Transaction: %s Amount: %s Units: %s"
                        .formatted(transaction.type(), transaction.amount(), transaction.quantity())));
    }

    private static String auditAction(TransactionType type) {
        return switch (type) {
            case BUY -> AuditRecord.ACTION_CREATE;
            case SELL -> AuditRecord.ACTION_DELETE;
            case TRANSFER, FEE -> AuditRecord.ACTION_UPDATE;
        };
    }

    private Optional<String> rewrite(PortfolioPosition position) {
        return store.rewrite(position) ? Optional.empty() : Optional.of(ERR_UPDATE_FAILED);
    }
}

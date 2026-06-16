package com.clbs.portfolio.service;

import java.math.BigDecimal;
import java.util.List;

import com.clbs.portfolio.domain.CobolDecimal;
import com.clbs.portfolio.domain.PortfolioPosition;
import com.clbs.portfolio.domain.PortfolioTransaction;
import com.clbs.portfolio.domain.TransactionType;
import com.clbs.portfolio.repository.PortfolioPositionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modernized PORTTRAN — Portfolio Transaction Processing.
 *
 * <p>Each public/private method below maps 1:1 to a COBOL paragraph so the
 * behavior can be traced back to {@code src/programs/portfolio/PORTTRAN.cbl}:</p>
 * <ul>
 *   <li>{@link #processBatch}        &rarr; {@code 0000-MAIN} + {@code 2000-PROCESS-TRANSACTIONS} loop</li>
 *   <li>{@link #validateTransaction} &rarr; {@code 2100-VALIDATE-TRANSACTION}</li>
 *   <li>{@link #checkPortfolio}      &rarr; {@code 2110-CHECK-PORTFOLIO}</li>
 *   <li>{@link #checkTransactionType}&rarr; {@code 2120-CHECK-TRANSACTION-TYPE}</li>
 *   <li>{@link #checkAmounts}        &rarr; {@code 2130-CHECK-AMOUNTS}</li>
 *   <li>{@link #updatePositions}     &rarr; {@code 2200-UPDATE-POSITIONS} (+ 2210/2220/2230/2240)</li>
 * </ul>
 *
 * <p><b>Wiring defect recovered:</b> in the COBOL as written, {@code 2000} only
 * invokes {@code 2100} (validation); {@code 2200-UPDATE-POSITIONS} is defined but
 * never {@code PERFORM}ed, so positions are never actually updated. This service
 * wires validation &rarr; update as the program structure clearly intends.
 * {@link #validateTransaction} is exposed separately so the as-written validation
 * behavior can still be verified in isolation.</p>
 */
@Service
public class PortfolioTransactionService {

    // ERR-TEXT messages, byte-for-byte from PORTTRAN.cbl.
    static final String ERR_PORTFOLIO_REQUIRED = "Portfolio ID is required";
    static final String ERR_INVALID_PORTFOLIO_PREFIX = "Invalid Portfolio ID: ";
    static final String ERR_INVALID_TYPE_PREFIX = "Invalid Transaction Type: ";
    static final String ERR_QUANTITY_POSITIVE = "Quantity must be greater than zero";
    static final String ERR_PRICE_POSITIVE = "Price must be greater than zero";
    static final String ERR_AMOUNT_POSITIVE = "Amount must be greater than zero";
    static final String ERR_NOT_FOUND_UPDATE = "Portfolio not found for update";
    static final String ERR_NOT_FOUND_FEE = "Portfolio not found for fee";
    static final String ERR_INSUFFICIENT_UNITS = "Insufficient units for sale";
    static final String ERR_TRANSFER_NOT_IMPL = "Transfer processing not implemented";

    /** {@code UNTIL ... OR WS-ERROR-COUNT > 100} — circuit-breaker threshold. */
    static final long MAX_ERRORS = 100;

    private final PortfolioPositionRepository repository;
    private final AuditService auditService;

    public PortfolioTransactionService(PortfolioPositionRepository repository,
                                       AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /**
     * Drive a batch of transactions, mirroring {@code 0000-MAIN}'s loop:
     * {@code PERFORM 2000-PROCESS-TRANSACTIONS UNTIL END-OF-FILE OR WS-ERROR-COUNT > 100}.
     *
     * @param transactions sequential transaction "file"
     * @return read / processed / error counts and whether the breaker tripped
     */
    @Transactional
    public BatchResult processBatch(List<PortfolioTransaction> transactions) {
        long read = 0;
        long processed = 0;
        long errors = 0;
        boolean halted = false;

        for (PortfolioTransaction txn : transactions) {
            // Loop condition is tested before each read in COBOL.
            if (errors > MAX_ERRORS) {
                halted = true;
                break;
            }
            read++;
            TransactionResult result = processTransaction(txn);
            if (result.success()) {
                processed++;
            } else {
                errors++;
            }
        }
        return new BatchResult(read, processed, errors, halted);
    }

    /**
     * Process a single transaction end-to-end: validate ({@code 2100}) then,
     * if valid, apply the position update ({@code 2200}).
     *
     * @param txn the transaction to process
     * @return the processing outcome
     */
    @Transactional
    public TransactionResult processTransaction(PortfolioTransaction txn) {
        String validationError = validateTransaction(txn);
        if (validationError != null) {
            return TransactionResult.failure(validationError);
        }
        return updatePositions(txn);
    }

    /**
     * {@code 2100-VALIDATE-TRANSACTION}: run the three checks in order, stopping at
     * the first failure ({@code IF ERR-TEXT = SPACES} guards each step).
     *
     * @param txn the transaction
     * @return the COBOL {@code ERR-TEXT} message, or {@code null} if valid
     */
    public String validateTransaction(PortfolioTransaction txn) {
        String err = checkPortfolio(txn);
        if (err == null) {
            err = checkTransactionType(txn);
        }
        if (err == null) {
            err = checkAmounts(txn);
        }
        return err;
    }

    /** {@code 2110-CHECK-PORTFOLIO}. */
    private String checkPortfolio(PortfolioTransaction txn) {
        if (isBlank(txn.getPortfolioId())) {
            return ERR_PORTFOLIO_REQUIRED;
        }
        if (!repository.existsById(txn.getPortfolioId().trim())) {
            return ERR_INVALID_PORTFOLIO_PREFIX + txn.getPortfolioId();
        }
        return null;
    }

    /** {@code 2120-CHECK-TRANSACTION-TYPE}. */
    private String checkTransactionType(PortfolioTransaction txn) {
        if (txn.resolvedType() == null) {
            return ERR_INVALID_TYPE_PREFIX + (txn.getType() == null ? "" : txn.getType());
        }
        return null;
    }

    /** {@code 2130-CHECK-AMOUNTS}. */
    private String checkAmounts(PortfolioTransaction txn) {
        boolean isTransfer = txn.resolvedType() == TransactionType.TRANSFER;

        if (lteZero(txn.getQuantity())) {
            return ERR_QUANTITY_POSITIVE;
        }
        if (lteZero(txn.getPrice()) && !isTransfer) {
            return ERR_PRICE_POSITIVE;
        }
        if (lteZero(txn.getAmount()) && !isTransfer) {
            return ERR_AMOUNT_POSITIVE;
        }
        return null;
    }

    /** {@code 2200-UPDATE-POSITIONS} + 2210/2220/2230/2240, then {@code 2300} audit. */
    private TransactionResult updatePositions(PortfolioTransaction txn) {
        TransactionType type = txn.resolvedType();
        TransactionResult result = switch (type) {
            case BUY -> processBuy(txn);
            case SELL -> processSell(txn);
            case TRANSFER -> processTransfer();
            case FEE -> processFee(txn);
        };
        writeAuditTrail(txn, type, result);
        return result;
    }

    /** {@code 2210-PROCESS-BUY}: add units and cost. */
    private TransactionResult processBuy(PortfolioTransaction txn) {
        PortfolioPosition pos = repository.findById(txn.getPortfolioId().trim()).orElse(null);
        if (pos == null) {
            return TransactionResult.failure(ERR_NOT_FOUND_UPDATE);
        }
        pos.setTotalUnits(CobolDecimal.quantity(units(pos).add(quantity(txn))));
        pos.setTotalCost(CobolDecimal.amount(cost(pos).add(amount(txn))));
        return TransactionResult.ok(repository.save(pos));
    }

    /** {@code 2220-PROCESS-SELL}: guard insufficient units, then subtract. */
    private TransactionResult processSell(PortfolioTransaction txn) {
        PortfolioPosition pos = repository.findById(txn.getPortfolioId().trim()).orElse(null);
        if (pos == null) {
            return TransactionResult.failure(ERR_NOT_FOUND_UPDATE);
        }
        if (units(pos).compareTo(quantity(txn)) < 0) {
            return TransactionResult.failure(ERR_INSUFFICIENT_UNITS);
        }
        pos.setTotalUnits(CobolDecimal.quantity(units(pos).subtract(quantity(txn))));
        pos.setTotalCost(CobolDecimal.amount(cost(pos).subtract(amount(txn))));
        return TransactionResult.ok(repository.save(pos));
    }

    /** {@code 2230-PROCESS-TRANSFER}: unimplemented in the original program. */
    private TransactionResult processTransfer() {
        return TransactionResult.failure(ERR_TRANSFER_NOT_IMPL);
    }

    /** {@code 2240-PROCESS-FEE}: subtract amount from cost basis. */
    private TransactionResult processFee(PortfolioTransaction txn) {
        PortfolioPosition pos = repository.findById(txn.getPortfolioId().trim()).orElse(null);
        if (pos == null) {
            return TransactionResult.failure(ERR_NOT_FOUND_FEE);
        }
        pos.setTotalCost(CobolDecimal.amount(cost(pos).subtract(amount(txn))));
        return TransactionResult.ok(repository.save(pos));
    }

    /** {@code 2300-UPDATE-AUDIT-TRAIL} + {@code 2310-WRITE-AUDIT-RECORD}. */
    private void writeAuditTrail(PortfolioTransaction txn, TransactionType type, TransactionResult result) {
        String action = switch (type) {
            case BUY -> "CREATE  ";
            case SELL -> "DELETE  ";
            case TRANSFER, FEE -> "UPDATE  ";
        };
        String accountNo = result.position() != null ? result.position().getAccountNo() : null;
        auditService.record(AuditRecord.builder()
                .program("PORTTRAN")
                .type("TRAN")
                .action(action)
                .status(result.success() ? "SUCC" : "FAIL")
                .portfolioId(txn.getPortfolioId())
                .accountNo(accountNo)
                .message("Transaction: " + txn.getType()
                        + " Amount: " + amount(txn).toPlainString()
                        + " Units: " + quantity(txn).toPlainString())
                .build());
    }

    private static BigDecimal quantity(PortfolioTransaction txn) {
        return CobolDecimal.quantity(txn.getQuantity());
    }

    private static BigDecimal amount(PortfolioTransaction txn) {
        return CobolDecimal.amount(txn.getAmount());
    }

    private static BigDecimal units(PortfolioPosition pos) {
        return CobolDecimal.quantity(pos.getTotalUnits());
    }

    private static BigDecimal cost(PortfolioPosition pos) {
        return CobolDecimal.amount(pos.getTotalCost());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Mirrors COBOL {@code <= ZERO} on a signed packed-decimal field (null treated as zero). */
    private static boolean lteZero(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0;
    }
}

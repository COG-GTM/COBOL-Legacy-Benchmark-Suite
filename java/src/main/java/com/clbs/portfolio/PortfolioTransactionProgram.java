package com.clbs.portfolio;

import com.clbs.common.AuditProcessor;
import com.clbs.common.ErrorMessage;
import com.clbs.common.ErrorProcessor;
import com.clbs.common.ProgramResult;
import com.clbs.domain.AuditRecord;
import com.clbs.domain.ErrorCodes;
import com.clbs.domain.PortfolioRecord;
import com.clbs.domain.ReturnCode;
import com.clbs.domain.TransactionRecord;
import com.clbs.store.DatasetCatalog;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * PORTTRAN — validates TRANFILE transactions against PORTFILE and applies them.
 *
 * <p>In the COBOL source 2200-UPDATE-POSITIONS is never PERFORMed: 2100-VALIDATE-TRANSACTION only
 * counts validated records. {@link #run} reproduces that validate-and-count driver exactly, while
 * {@link #apply} exposes the 2200 family so the position updates are reachable from the service
 * layer. 2230-PROCESS-TRANSFER keeps its "Transfer processing not implemented" rejection.
 */
@Service
public class PortfolioTransactionProgram {

    public static final String PROGRAM = "PORTTRAN";

    /** ERR-TEXT literals. */
    public static final String ERR_PORTFOLIO_REQUIRED = "Portfolio ID is required";
    public static final String ERR_INVALID_PORTFOLIO = "Invalid Portfolio ID: ";
    public static final String ERR_INVALID_TYPE = "Invalid Transaction Type: ";
    public static final String ERR_QUANTITY = "Quantity must be greater than zero";
    public static final String ERR_PRICE = "Price must be greater than zero";
    public static final String ERR_AMOUNT = "Amount must be greater than zero";
    public static final String ERR_NOT_FOUND_UPDATE = "Portfolio not found for update";
    public static final String ERR_NOT_FOUND_FEE = "Portfolio not found for fee";
    public static final String ERR_INSUFFICIENT_UNITS = "Insufficient units for sale";
    public static final String ERR_UPDATE = "Error updating portfolio";
    public static final String ERR_TRANSFER_UNSUPPORTED = "Transfer processing not implemented";

    /** 0000-MAIN stops the driver once WS-ERROR-COUNT exceeds 100. */
    public static final int ERROR_LIMIT = 100;

    private final DatasetCatalog datasets;
    private final AuditProcessor auditProcessor;
    private final ErrorProcessor errorProcessor;

    public PortfolioTransactionProgram(DatasetCatalog datasets, AuditProcessor auditProcessor,
            ErrorProcessor errorProcessor) {
        this.datasets = datasets;
        this.auditProcessor = auditProcessor;
        this.errorProcessor = errorProcessor;
    }

    /** 0000-MAIN: read, validate and count until end of file or more than 100 errors. */
    public ProgramResult run(List<TransactionRecord> transactions) {
        ProgramResult result = new ProgramResult(PROGRAM);
        long read = 0;
        long processed = 0;
        long errors = 0;

        for (TransactionRecord transaction : transactions) {
            if (errors > ERROR_LIMIT) {
                break;
            }
            read++;
            String error = validate(transaction);
            if (error == null) {
                processed++;
            } else {
                errors++;
                reportError(error);
            }
        }

        // 3000-TERMINATE
        result.display("Transactions Read:    " + read);
        result.display("Transactions Process: " + processed);
        result.display("Errors Encountered:   " + errors);
        result.count("read", read).count("processed", processed).count("errors", errors);
        result.setReturnCode(errors == 0 ? ReturnCode.SUCCESS : ReturnCode.ERROR);
        return result;
    }

    /** 2100-VALIDATE-TRANSACTION: returns the ERR-TEXT or null when the record passes. */
    public String validate(TransactionRecord transaction) {
        String error = checkPortfolio(transaction);
        if (error == null) {
            error = checkTransactionType(transaction);
        }
        if (error == null) {
            error = checkAmounts(transaction);
        }
        return error;
    }

    /** 2110-CHECK-PORTFOLIO. */
    private String checkPortfolio(TransactionRecord transaction) {
        if (transaction.getPortfolioId() == null || transaction.getPortfolioId().isBlank()) {
            return ERR_PORTFOLIO_REQUIRED;
        }
        if (findPortfolio(transaction.getPortfolioId()) == null) {
            return ERR_INVALID_PORTFOLIO + transaction.getPortfolioId();
        }
        return null;
    }

    /** 2120-CHECK-TRANSACTION-TYPE. */
    private String checkTransactionType(TransactionRecord transaction) {
        String type = transaction.getType();
        if ("BU".equals(type) || "SL".equals(type) || "TR".equals(type) || "FE".equals(type)) {
            return null;
        }
        return ERR_INVALID_TYPE + type;
    }

    /** 2130-CHECK-AMOUNTS. */
    private String checkAmounts(TransactionRecord transaction) {
        boolean transfer = "TR".equals(transaction.getType());
        if (transaction.getQuantity().signum() <= 0) {
            return ERR_QUANTITY;
        }
        if (transaction.getPrice().signum() <= 0 && !transfer) {
            return ERR_PRICE;
        }
        if (transaction.getAmount().signum() <= 0 && !transfer) {
            return ERR_AMOUNT;
        }
        return null;
    }

    /** 2200-UPDATE-POSITIONS: applies a validated transaction and writes the audit trail. */
    public String apply(TransactionRecord transaction) {
        String error = switch (transaction.getType()) {
            case "BU" -> processBuy(transaction);
            case "SL" -> processSell(transaction);
            case "TR" -> ERR_TRANSFER_UNSUPPORTED;
            case "FE" -> processFee(transaction);
            default -> ERR_INVALID_TYPE + transaction.getType();
        };
        if (error != null) {
            reportError(error);
        }
        updateAuditTrail(transaction, error);
        return error;
    }

    /** 2210-PROCESS-BUY. */
    private String processBuy(TransactionRecord transaction) {
        PortfolioRecord portfolio = findPortfolio(transaction.getPortfolioId());
        if (portfolio == null) {
            return ERR_NOT_FOUND_UPDATE;
        }
        portfolio.setTotalUnits(portfolio.getTotalUnits().add(transaction.getQuantity()));
        portfolio.setTotalCost(portfolio.getTotalCost().add(transaction.getAmount()));
        return rewrite(portfolio);
    }

    /** 2220-PROCESS-SELL. */
    private String processSell(TransactionRecord transaction) {
        PortfolioRecord portfolio = findPortfolio(transaction.getPortfolioId());
        if (portfolio == null) {
            return ERR_NOT_FOUND_UPDATE;
        }
        if (portfolio.getTotalUnits().compareTo(transaction.getQuantity()) < 0) {
            return ERR_INSUFFICIENT_UNITS;
        }
        portfolio.setTotalUnits(portfolio.getTotalUnits().subtract(transaction.getQuantity()));
        portfolio.setTotalCost(portfolio.getTotalCost().subtract(transaction.getAmount()));
        return rewrite(portfolio);
    }

    /** 2240-PROCESS-FEE. */
    private String processFee(TransactionRecord transaction) {
        PortfolioRecord portfolio = findPortfolio(transaction.getPortfolioId());
        if (portfolio == null) {
            return ERR_NOT_FOUND_FEE;
        }
        portfolio.setTotalCost(portfolio.getTotalCost().subtract(transaction.getAmount()));
        return rewrite(portfolio);
    }

    private String rewrite(PortfolioRecord portfolio) {
        String status = datasets.portfolioFile().rewrite(portfolio);
        return com.clbs.store.FileStatus.SUCCESS.equals(status) ? null : ERR_UPDATE;
    }

    /** 2300-UPDATE-AUDIT-TRAIL / 2310-WRITE-AUDIT-RECORD. */
    private void updateAuditTrail(TransactionRecord transaction, String error) {
        PortfolioRecord portfolio = findPortfolio(transaction.getPortfolioId());
        AuditRecord audit = new AuditRecord();
        audit.setProgramId(PROGRAM);
        audit.setType(AuditRecord.TYPE_TRANSACTION);
        audit.setAction(switch (transaction.getType()) {
            case "BU" -> AuditRecord.ACTION_CREATE;
            case "SL" -> AuditRecord.ACTION_DELETE;
            default -> AuditRecord.ACTION_UPDATE;
        });
        audit.setStatus(error == null ? AuditRecord.STATUS_SUCCESS : AuditRecord.STATUS_FAILURE);
        audit.setPortfolioId(transaction.getPortfolioId());
        if (portfolio != null) {
            audit.setAccountNo(portfolio.getAccountNo());
            audit.setBeforeImage(portfolio.image());
        }
        audit.setMessage("Transaction: " + transaction.getType()
                + " Amount: " + transaction.getAmount()
                + " Units: " + transaction.getQuantity());
        auditProcessor.write(audit);
    }

    /** 9000-ERROR-ROUTINE: CALL 'ERRPROC' with category PR. */
    private void reportError(String text) {
        errorProcessor.process(new ErrorMessage(PROGRAM, ErrorCodes.CAT_PROCESSING,
                ErrorCodes.PROCESSING, ReturnCode.ERROR, text, ""));
    }

    /**
     * PORTTRAN keys PORTFILE on PORT-ID alone; PORTFLIO.cpy makes the key PORT-ID + PORT-ACCOUNT-NO,
     * so the portfolio is located by its id prefix.
     */
    private PortfolioRecord findPortfolio(String portfolioId) {
        if (portfolioId == null) {
            return null;
        }
        String id = portfolioId.trim();
        return datasets.portfolioFile().all().stream()
                .filter(record -> id.equals(record.getPortId().trim()))
                .findFirst()
                .orElse(null);
    }

    /** Exposed for tests that need the COBOL zero comparison semantics. */
    public static boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}

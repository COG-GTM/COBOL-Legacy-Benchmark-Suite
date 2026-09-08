package com.clbs.utility;

import com.clbs.common.Inputs;
import com.clbs.common.ProgramResult;
import com.clbs.domain.PositionRecord;
import com.clbs.store.DatasetCatalog;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * UTLVAL00 — data validation utility.
 *
 * <p>The COBOL source defines the VALIDATION-CONTROL layout, the
 * INTEGRITY/XREF/FORMAT/BALANCE dispatch, the WS-VALIDATION-TOTALS counters and the error handler
 * that writes WS-ERROR-LINE. The check paragraphs themselves (2210/2220, 2310/2320, 2410/2420,
 * 2510/2520) are PERFORMed but never defined; the checks below are adapted from the file layouts
 * and the documented validation criteria, and are reported as such.
 */
@Service
public class DataValidationUtility {

    public static final String PROGRAM = "UTLVAL00";

    public static final String INTEGRITY = "INTEGRITY";
    public static final String XREF = "XREF";
    public static final String FORMAT = "FORMAT";
    public static final String BALANCE = "BALANCE";
    public static final String ERR_INVALID_TYPE = "INVALID VALIDATION TYPE";
    public static final String ERR_CONTROL_TOTAL = "INVALID CONTROL TOTAL";

    /** VALIDATION-RECORD. */
    public record ValidationRequest(String type, String parameters) {
    }

    private final DatasetCatalog datasets;

    public DataValidationUtility(DatasetCatalog datasets) {
        this.datasets = datasets;
    }

    /** 2000-PROCESS. */
    public ProgramResult run(List<ValidationRequest> requests) {
        ProgramResult result = new ProgramResult(PROGRAM);
        long read = 0;
        long valid = 0;
        long errors = 0;

        for (ValidationRequest request : Inputs.records(requests)) {
            read++;
            List<String> failures = switch (request.type() == null ? "" : request.type().trim()) {
                case INTEGRITY -> checkIntegrity();
                case XREF -> checkCrossReference();
                case FORMAT -> checkFormat();
                case BALANCE -> checkBalance(request.parameters());
                default -> List.of(ERR_INVALID_TYPE);
            };

            if (failures.isEmpty()) {
                valid++;
            } else {
                errors += failures.size();
                failures.forEach(result::display);
            }
        }

        result.count("read", read).count("valid", valid).count("errors", errors);
        if (errors > 0) {
            result.setReturnCode(4);
        }
        return result;
    }

    /** 2200-CHECK-INTEGRITY: every position must carry a key and a non-negative quantity. */
    List<String> checkIntegrity() {
        return datasets.positionFile().all().stream()
                .filter(position -> position.getPortfolioId().isBlank()
                        || position.getInvestmentId().isBlank()
                        || position.getQuantity().signum() < 0)
                .map(position -> line(INTEGRITY, position, "Incomplete position record"))
                .toList();
    }

    /** 2300-CHECK-XREF: every position must reference a portfolio on the master file. */
    List<String> checkCrossReference() {
        return datasets.positionFile().all().stream()
                .filter(position -> datasets.portfolioFile().all().stream()
                        .noneMatch(portfolio -> portfolio.getPortId().trim()
                                .equals(position.getPortfolioId().trim())))
                .map(position -> line(XREF, position, "No portfolio for position"))
                .toList();
    }

    /** 2400-CHECK-FORMAT: currency, status and date must match the documented domains. */
    List<String> checkFormat() {
        return datasets.positionFile().all().stream()
                .filter(position -> position.getCurrency().isBlank()
                        || "AIC".indexOf(position.getStatus()) < 0
                        || position.getDate().trim().length() != 8)
                .map(position -> line(FORMAT, position, "Invalid position format"))
                .toList();
    }

    /** 2500-CHECK-BALANCE: accumulated market value against the supplied control total. */
    List<String> checkBalance(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return List.of();
        }
        BigDecimal control = parseControlTotal(parameters);
        if (control == null) {
            return List.of(ERR_CONTROL_TOTAL);
        }
        BigDecimal total = datasets.positionFile().all().stream()
                .map(PositionRecord::getMarketValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);

        if (total.compareTo(control) == 0) {
            return List.of();
        }
        return List.of(String.format("%-10s  %-20s  Total %s does not match control %s", BALANCE,
                "", total, control));
    }

    private static BigDecimal parseControlTotal(String parameters) {
        if (parameters == null || parameters.isBlank()) {
            return null;
        }
        try {
            // MOVE to a S9(13)V99 control total truncates rather than rounding.
            return new BigDecimal(parameters.trim()).setScale(2, RoundingMode.DOWN);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /** WS-ERROR-LINE: type, key, description. */
    private static String line(String type, PositionRecord position, String description) {
        return String.format("%-10s  %-20s  %s", type, position.key().trim(), description);
    }
}

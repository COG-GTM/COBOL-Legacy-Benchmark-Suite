package com.clbs.portfolio;

import com.clbs.common.Inputs;
import com.clbs.common.ProgramResult;
import com.clbs.domain.PortfolioRecord;
import com.clbs.store.DatasetCatalog;
import com.clbs.store.FileStatus;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * PORTUPDT — applies UPDTFILE requests to PORTFILE. UPDT-ACTION selects which field of the matched
 * record is replaced by UPDT-NEW-VALUE.
 */
@Service
public class PortfolioUpdateProgram {

    public static final String PROGRAM = "PORTUPDT";

    private final DatasetCatalog datasets;

    public PortfolioUpdateProgram(DatasetCatalog datasets) {
        this.datasets = datasets;
    }

    /** UPDATE-RECORD: UPDT-KEY, UPDT-ACTION ('S', 'N', 'V') and UPDT-NEW-VALUE. */
    public record UpdateRequest(String portId, String accountNo, char action, String newValue) {
    }

    /** 0000-MAIN. */
    public ProgramResult run(List<UpdateRequest> updates) {
        ProgramResult result = new ProgramResult(PROGRAM);
        long updated = 0;
        long errors = 0;

        for (UpdateRequest request : Inputs.records(updates)) {
            String key = keyOf(request.portId(), request.accountNo());
            PortfolioRecord record = datasets.portfolioFile().read(key);

            // 2100-PROCESS-UPDATE
            if (record == null) {
                errors++;
                result.display("Record not found: " + key);
                continue;
            }

            // 2200-APPLY-UPDATE. UPDT-NEW-VALUE is PIC X(30); an absent or non-numeric value is an
            // update error rather than a failure of the run.
            String newValue = Inputs.text(request.newValue()).trim();
            switch (request.action()) {
                case 'S' -> {
                    if (newValue.isEmpty()) {
                        errors++;
                        result.display("Invalid new value for: " + key);
                        continue;
                    }
                    record.setStatus(newValue.charAt(0));
                }
                case 'N' -> record.setClientName(newValue);
                case 'V' -> {
                    BigDecimal value = parseAmount(newValue);
                    if (value == null) {
                        errors++;
                        result.display("Invalid new value for: " + key);
                        continue;
                    }
                    record.setTotalValue(value);
                }
                default -> {
                    // COBOL EVALUATE falls through without changing the record.
                }
            }

            if (FileStatus.SUCCESS.equals(datasets.portfolioFile().rewrite(record))) {
                updated++;
            } else {
                errors++;
                result.display("Update failed for: " + key);
            }
        }

        // 3000-TERMINATE
        result.display("Updates processed: " + updated);
        result.display("Errors occurred:  " + errors);
        result.count("updated", updated).count("errors", errors);
        return result;
    }

    private static BigDecimal parseAmount(String value) {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String keyOf(String portId, String accountNo) {
        PortfolioRecord probe = new PortfolioRecord();
        probe.setPortId(portId);
        probe.setAccountNo(accountNo);
        return probe.key();
    }
}

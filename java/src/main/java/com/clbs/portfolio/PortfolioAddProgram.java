package com.clbs.portfolio;

import com.clbs.common.Inputs;
import com.clbs.common.ProgramResult;
import com.clbs.domain.PortfolioRecord;
import com.clbs.store.DatasetCatalog;
import com.clbs.store.FileStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * PORTADD — creates portfolio records from the INPTFILE sequential input, stamping
 * PORT-CREATE-DATE / PORT-LAST-MAINT with the run date and writing to PORTFILE.
 */
@Service
public class PortfolioAddProgram {

    public static final String PROGRAM = "PORTADD";

    private final DatasetCatalog datasets;

    public PortfolioAddProgram(DatasetCatalog datasets) {
        this.datasets = datasets;
    }

    /** 0000-MAIN over the INPTFILE records. */
    public ProgramResult run(List<PortfolioRecord> input) {
        ProgramResult result = new ProgramResult(PROGRAM);
        int currentDate = Integer.parseInt(LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
        long added = 0;
        long duplicates = 0;
        long errors = 0;

        for (PortfolioRecord source : Inputs.records(input)) {
            PortfolioRecord record = source.copy();

            // 2100-VALIDATE-AND-ADD
            if (isBlank(record.getPortId()) || isBlank(record.getClientName())
                    || record.getStatus() != 'A') {
                errors++;
                result.display("Invalid record data: " + record.getPortId());
                continue;
            }

            record.setCreateDate(currentDate);
            record.setLastMaint(currentDate);

            String status = datasets.portfolioFile().write(record);
            if (FileStatus.SUCCESS.equals(status)) {
                added++;
            } else if (FileStatus.DUPLICATE_KEY.equals(status)) {
                duplicates++;
                result.display("Duplicate record: " + record.getPortId());
            } else {
                errors++;
                result.display("Write error for: " + record.getPortId());
            }
        }

        // 3000-TERMINATE
        result.display("Records added:    " + added);
        result.display("Duplicate records:" + duplicates);
        result.display("Errors occurred:  " + errors);
        result.count("added", added).count("duplicates", duplicates).count("errors", errors);
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

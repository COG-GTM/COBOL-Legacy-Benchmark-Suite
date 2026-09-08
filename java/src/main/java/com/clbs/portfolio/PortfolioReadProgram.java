package com.clbs.portfolio;

import com.clbs.common.ProgramResult;
import com.clbs.domain.PortfolioRecord;
import com.clbs.store.DatasetCatalog;
import org.springframework.stereotype.Service;

/** PORTREAD — sequential browse of PORTFILE, displaying every record. */
@Service
public class PortfolioReadProgram {

    public static final String PROGRAM = "PORTREAD";

    private final DatasetCatalog datasets;

    public PortfolioReadProgram(DatasetCatalog datasets) {
        this.datasets = datasets;
    }

    /** 0000-MAIN: READ NEXT until AT END. */
    public ProgramResult run() {
        ProgramResult result = new ProgramResult(PROGRAM);
        long count = 0;

        datasets.portfolioFile().startAtBeginning();
        PortfolioRecord record = datasets.portfolioFile().readNext();
        while (record != null) {
            count++;
            // 2100-DISPLAY-RECORD
            result.display("Portfolio Record: " + count);
            result.display("  ID: " + record.getPortId());
            result.display("  Account: " + record.getAccountNo());
            result.display("  Client: " + record.getClientName());
            result.display("  Status: " + record.getStatus());
            result.display("  Total Value: " + record.getTotalValue());
            result.display(" ");
            record = datasets.portfolioFile().readNext();
        }

        result.display("Total Records Read: " + count);
        result.count("recordsRead", count);
        return result;
    }
}

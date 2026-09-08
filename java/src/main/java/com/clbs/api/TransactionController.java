package com.clbs.api;

import com.clbs.common.ProgramResult;
import com.clbs.domain.TransactionRecord;
import com.clbs.portfolio.PortfolioTransactionProgram;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for PORTTRAN. */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final PortfolioTransactionProgram program;

    public TransactionController(PortfolioTransactionProgram program) {
        this.program = program;
    }

    /** PORTTRAN as written: validate and count only. */
    @PostMapping
    public ProgramResult process(@RequestBody List<TransactionRecord> transactions) {
        return program.run(transactions);
    }

    /** PORTTRAN including 2200-UPDATE-POSITIONS, which its own driver never reaches. */
    @PostMapping("/apply")
    public ProgramResult apply(@RequestBody List<TransactionRecord> transactions) {
        return program.runAndApply(transactions);
    }
}

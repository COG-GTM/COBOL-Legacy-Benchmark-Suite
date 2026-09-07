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

    @PostMapping
    public ProgramResult process(@RequestBody List<TransactionRecord> transactions) {
        return program.run(transactions);
    }
}

package com.clbs.online.web;

import com.clbs.portfolio.domain.PortfolioMaster;
import com.clbs.portfolio.domain.TransactionRecord;
import com.clbs.portfolio.repository.PortfolioMasterRepository;
import com.clbs.portfolio.repository.TransactionRecordRepository;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only inquiry endpoints — the REST equivalent of the CICS INQPORT/INQHIST
 * transactions. Phase 0 provides the portfolio + transaction browse paths.
 */
@RestController
@RequestMapping("/api")
public class InquiryController {

    private final PortfolioMasterRepository portfolioRepository;
    private final TransactionRecordRepository transactionRepository;

    public InquiryController(PortfolioMasterRepository portfolioRepository,
                             TransactionRecordRepository transactionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
    }

    /** INQPORT: list all account records for a portfolio id (partial-key access). */
    @GetMapping("/portfolios/{portId}")
    public List<PortfolioMaster> portfolio(@PathVariable String portId) {
        return portfolioRepository.findByKeyPortId(portId);
    }

    /** INQHIST: browse the transaction history for a portfolio id. */
    @GetMapping("/portfolios/{portId}/transactions")
    public List<TransactionRecord> transactions(@PathVariable String portId) {
        return transactionRepository
                .findByKeyPortfolioIdOrderByKeyTrnDateAscKeyTrnTimeAscKeySequenceNoAsc(portId);
    }
}

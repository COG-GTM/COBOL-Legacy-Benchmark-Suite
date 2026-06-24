package com.portfolio.service;

import com.portfolio.dto.TransactionRequest;
import com.portfolio.dto.TransactionResponse;
import com.portfolio.exception.InsufficientUnitsException;
import com.portfolio.exception.PortfolioNotFoundException;
import com.portfolio.model.Portfolio;
import com.portfolio.model.Transaction;
import com.portfolio.repository.PortfolioRepository;
import com.portfolio.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for transaction processing.
 * Translated from PORTTRAN.cbl:
 * <ul>
 *   <li>2000-PROCESS-TRANSACTIONS — main processing loop</li>
 *   <li>2100-VALIDATE-TRANSACTION — input validation</li>
 *   <li>2200-UPDATE-POSITIONS — dispatch by TRN-TYPE</li>
 *   <li>2210-PROCESS-BUY — buy logic</li>
 *   <li>2220-PROCESS-SELL — sell logic with insufficient-units check</li>
 *   <li>2230-PROCESS-TRANSFER — transfer stub</li>
 *   <li>2240-PROCESS-FEE — fee deduction</li>
 * </ul>
 */
@Service
public class TransactionService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmmss");

    private final TransactionRepository transactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final AuditService auditService;

    public TransactionService(TransactionRepository transactionRepository,
                              PortfolioRepository portfolioRepository,
                              AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.portfolioRepository = portfolioRepository;
        this.auditService = auditService;
    }

    /**
     * Process a transaction.
     * Mirrors PORTTRAN.cbl paragraph 2200-UPDATE-POSITIONS:
     * <pre>
     *   EVALUATE TRN-TYPE
     *       WHEN 'BU'  PERFORM 2210-PROCESS-BUY
     *       WHEN 'SL'  PERFORM 2220-PROCESS-SELL
     *       WHEN 'TR'  PERFORM 2230-PROCESS-TRANSFER
     *       WHEN 'FE'  PERFORM 2240-PROCESS-FEE
     *   END-EVALUATE
     * </pre>
     */
    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        Portfolio portfolio = portfolioRepository.findById(request.getPortfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(request.getPortfolioId()));

        String beforeImage = portfolio.toString();

        switch (request.getTransactionType()) {
            case "BU" -> processBuy(portfolio, request);
            case "SL" -> processSell(portfolio, request);
            case "TR" -> processTransfer(portfolio, request);
            case "FE" -> processFee(portfolio, request);
            default -> throw new IllegalArgumentException(
                    "Invalid Transaction Type: " + request.getTransactionType());
        }

        portfolioRepository.save(portfolio);

        Transaction transaction = createTransactionRecord(request);
        Transaction saved = transactionRepository.save(transaction);

        auditService.logAction("TRAN",
                "BU".equals(request.getTransactionType()) ? "CREATE" : "UPDATE",
                "SUCC",
                request.getPortfolioId(), portfolio.getAccountNo(),
                beforeImage, portfolio.toString(),
                "Transaction: " + request.getTransactionType()
                        + " Amount: " + request.getAmount()
                        + " Units: " + request.getQuantity());

        return TransactionResponse.fromEntity(saved);
    }

    /**
     * Mirrors PORTTRAN.cbl paragraph 2210-PROCESS-BUY:
     * <pre>
     *   ADD TRN-QUANTITY TO PORT-TOTAL-UNITS
     *   ADD TRN-AMOUNT   TO PORT-TOTAL-COST
     * </pre>
     */
    private void processBuy(Portfolio portfolio, TransactionRequest request) {
        portfolio.setTotalValue(portfolio.getTotalValue().add(request.getAmount()));
        portfolio.setCashBalance(portfolio.getCashBalance().subtract(request.getAmount()));
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setLastTransDate(LocalDate.now());
    }

    /**
     * Mirrors PORTTRAN.cbl paragraph 2220-PROCESS-SELL:
     * <pre>
     *   IF PORT-TOTAL-UNITS < TRN-QUANTITY
     *       MOVE 'Insufficient units for sale' TO ERR-TEXT
     *   SUBTRACT TRN-QUANTITY FROM PORT-TOTAL-UNITS
     *   SUBTRACT TRN-AMOUNT   FROM PORT-TOTAL-COST
     * </pre>
     */
    private void processSell(Portfolio portfolio, TransactionRequest request) {
        if (portfolio.getTotalValue().compareTo(request.getAmount()) < 0) {
            throw new InsufficientUnitsException(portfolio.getPortId());
        }
        portfolio.setTotalValue(portfolio.getTotalValue().subtract(request.getAmount()));
        portfolio.setCashBalance(portfolio.getCashBalance().add(request.getAmount()));
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setLastTransDate(LocalDate.now());
    }

    /**
     * Mirrors PORTTRAN.cbl paragraph 2230-PROCESS-TRANSFER:
     * In the original COBOL, this was a stub: "Transfer processing not implemented"
     * We implement it as a no-op on portfolio value, just records the transaction.
     */
    private void processTransfer(Portfolio portfolio, TransactionRequest request) {
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setLastTransDate(LocalDate.now());
    }

    /**
     * Mirrors PORTTRAN.cbl paragraph 2240-PROCESS-FEE:
     * <pre>
     *   SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST
     * </pre>
     */
    private void processFee(Portfolio portfolio, TransactionRequest request) {
        portfolio.setCashBalance(portfolio.getCashBalance().subtract(request.getAmount()));
        portfolio.setLastMaintDate(LocalDate.now());
        portfolio.setLastTransDate(LocalDate.now());
    }

    private Transaction createTransactionRecord(TransactionRequest request) {
        Transaction txn = new Transaction();
        txn.setTransactionDate(LocalDate.now());
        txn.setTransactionTime(LocalDateTime.now().format(TIME_FMT));
        txn.setPortfolioId(request.getPortfolioId());
        txn.setSequenceNo("000001");
        txn.setInvestmentId(request.getInvestmentId());
        txn.setTransactionType(request.getTransactionType());
        txn.setQuantity(request.getQuantity());
        txn.setPrice(request.getPrice());
        txn.setAmount(request.getAmount());
        txn.setCurrency(request.getCurrency() != null ? request.getCurrency() : "USD");
        txn.setStatus("D");
        txn.setProcessDate(LocalDateTime.now());
        txn.setProcessUser("SYSTEM");
        return txn;
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> findByPortfolioId(String portfolioId) {
        return transactionRepository.findByPortfolioId(portfolioId).stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(Long id) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
        return TransactionResponse.fromEntity(txn);
    }
}

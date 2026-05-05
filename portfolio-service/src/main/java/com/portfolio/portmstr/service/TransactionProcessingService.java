package com.portfolio.portmstr.service;

import com.portfolio.portmstr.dto.TransactionRequest;
import com.portfolio.portmstr.exception.InsufficientUnitsException;
import com.portfolio.portmstr.exception.PortfolioNotFoundException;
import com.portfolio.portmstr.exception.PortfolioValidationException;
import com.portfolio.portmstr.model.PortfolioMaster;
import com.portfolio.portmstr.model.TransactionHistory;
import com.portfolio.portmstr.model.enums.TransactionStatus;
import com.portfolio.portmstr.model.enums.TransactionType;
import com.portfolio.portmstr.repository.PortfolioMasterRepository;
import com.portfolio.portmstr.repository.TransactionHistoryRepository;
import com.portfolio.portmstr.validation.PortfolioValidator;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Transaction processing service.
 * Direct translation of COBOL PORTTRAN.cbl PROCEDURE DIVISION.
 *
 * COBOL paragraph mapping:
 *   2000-PROCESS-TRANSACTIONS -> processTransaction()
 *   2100-VALIDATE-TRANSACTION -> delegated to PortfolioValidator
 *   2110-CHECK-PORTFOLIO      -> portfolio lookup
 *   2120-CHECK-TRANSACTION-TYPE -> type validation
 *   2130-CHECK-AMOUNTS         -> amount validation
 *   2200-UPDATE-POSITIONS      -> dispatched to processBuy/Sell/Transfer/Fee
 *   2210-PROCESS-BUY           -> processBuy()
 *   2220-PROCESS-SELL          -> processSell()
 *   2230-PROCESS-TRANSFER      -> processTransfer()
 *   2240-PROCESS-FEE           -> processFee()
 *   2300-UPDATE-AUDIT-TRAIL    -> delegated to AuditService
 */
@Service
public class TransactionProcessingService {

    private static final Logger log = LoggerFactory.getLogger(TransactionProcessingService.class);
    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final AtomicLong sequenceCounter = new AtomicLong(0);

    private final PortfolioMasterRepository portfolioRepository;
    private final TransactionHistoryRepository transactionRepository;
    private final PortfolioValidator validator;
    private final AuditService auditService;

    public TransactionProcessingService(PortfolioMasterRepository portfolioRepository,
                                        TransactionHistoryRepository transactionRepository,
                                        PortfolioValidator validator,
                                        AuditService auditService) {
        this.portfolioRepository = portfolioRepository;
        this.transactionRepository = transactionRepository;
        this.validator = validator;
        this.auditService = auditService;
    }

    @Transactional
    public TransactionHistory processTransaction(TransactionRequest request) {
        validator.validateTransactionRequest(request);

        PortfolioMaster portfolio = portfolioRepository.findById(request.portfolioId())
                .orElseThrow(() -> new PortfolioNotFoundException(request.portfolioId()));

        TransactionType transType = TransactionType.fromCode(request.transactionType());

        switch (transType) {
            case BUY -> processBuy(portfolio, request);
            case SELL -> processSell(portfolio, request);
            case TRANSFER -> processTransfer(portfolio, request);
            case FEE -> processFee(portfolio, request);
        }

        portfolioRepository.save(portfolio);

        TransactionHistory transaction = createTransactionRecord(request, transType);
        transactionRepository.save(transaction);

        auditService.logTransaction(
                request.portfolioId(),
                portfolio.getAccountNo(),
                request.transactionType(),
                request.amount().toPlainString(),
                "SYSTEM");

        log.info("Transaction processed: type={}, portfolio={}, amount={}",
                request.transactionType(), request.portfolioId(), request.amount());

        return transaction;
    }

    /**
     * Process buy transaction.
     * From COBOL 2210-PROCESS-BUY:
     *   ADD TRN-QUANTITY TO PORT-TOTAL-UNITS
     *   ADD TRN-AMOUNT TO PORT-TOTAL-COST
     */
    private void processBuy(PortfolioMaster portfolio, TransactionRequest request) {
        BigDecimal currentValue = portfolio.getTotalValue() != null ? portfolio.getTotalValue() : BigDecimal.ZERO;
        portfolio.setTotalValue(currentValue.add(request.amount()));

        BigDecimal currentCash = portfolio.getCashBalance() != null ? portfolio.getCashBalance() : BigDecimal.ZERO;
        portfolio.setCashBalance(currentCash.subtract(request.amount()));
    }

    /**
     * Process sell transaction.
     * From COBOL 2220-PROCESS-SELL:
     *   IF PORT-TOTAL-UNITS < TRN-QUANTITY -> error
     *   SUBTRACT TRN-QUANTITY FROM PORT-TOTAL-UNITS
     *   SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST
     */
    private void processSell(PortfolioMaster portfolio, TransactionRequest request) {
        BigDecimal currentValue = portfolio.getTotalValue() != null ? portfolio.getTotalValue() : BigDecimal.ZERO;

        if (currentValue.compareTo(request.amount()) < 0) {
            throw new InsufficientUnitsException(
                    portfolio.getPortfolioId(), request.amount(), currentValue);
        }

        portfolio.setTotalValue(currentValue.subtract(request.amount()));

        BigDecimal currentCash = portfolio.getCashBalance() != null ? portfolio.getCashBalance() : BigDecimal.ZERO;
        portfolio.setCashBalance(currentCash.add(request.amount()));
    }

    /**
     * Process transfer transaction.
     * From COBOL 2230-PROCESS-TRANSFER: currently not implemented in COBOL source.
     */
    private void processTransfer(PortfolioMaster portfolio, TransactionRequest request) {
        throw new PortfolioValidationException("Transfer processing not implemented", 3);
    }

    /**
     * Process fee transaction.
     * From COBOL 2240-PROCESS-FEE:
     *   SUBTRACT TRN-AMOUNT FROM PORT-TOTAL-COST
     */
    private void processFee(PortfolioMaster portfolio, TransactionRequest request) {
        BigDecimal currentCash = portfolio.getCashBalance() != null ? portfolio.getCashBalance() : BigDecimal.ZERO;
        portfolio.setCashBalance(currentCash.subtract(request.amount()));
    }

    private TransactionHistory createTransactionRecord(TransactionRequest request, TransactionType transType) {
        LocalDateTime now = LocalDateTime.now();
        long seq = sequenceCounter.incrementAndGet();

        TransactionHistory transaction = new TransactionHistory();
        transaction.setTransactionId(now.format(ID_FORMAT) + String.format("%06d", seq));
        transaction.setPortfolioId(request.portfolioId());
        transaction.setTransactionDate(now.toLocalDate());
        transaction.setTransactionTime(now.toLocalTime());
        transaction.setInvestmentId(request.investmentId());
        transaction.setTransactionType(transType);
        transaction.setQuantity(request.quantity());
        transaction.setPrice(request.price());
        transaction.setAmount(request.amount());
        transaction.setCurrencyCode(request.currencyCode() != null ? request.currencyCode() : "USD");
        transaction.setStatus(TransactionStatus.DONE);
        transaction.setProcessDate(now);
        transaction.setProcessUser("SYSTEM");
        transaction.setSequenceNo(String.format("%06d", seq));
        return transaction;
    }
}

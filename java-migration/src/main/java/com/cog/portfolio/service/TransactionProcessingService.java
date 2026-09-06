package com.cog.portfolio.service;

import com.cog.portfolio.common.PortfolioException;
import com.cog.portfolio.common.ProcessingResult;
import com.cog.portfolio.common.ScaleConverter;
import com.cog.portfolio.domain.AuditAction;
import com.cog.portfolio.domain.AuditStatus;
import com.cog.portfolio.domain.Portfolio;
import com.cog.portfolio.domain.Position;
import com.cog.portfolio.domain.PositionId;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionStatus;
import com.cog.portfolio.domain.TransactionType;
import com.cog.portfolio.repository.PortfolioRepository;
import com.cog.portfolio.repository.PositionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PORTTRAN.cbl. The COBOL program keeps PORT-TOTAL-UNITS / PORT-TOTAL-COST in its
 * local portfolio record; here those two accumulators are the {@code quantity}
 * and {@code costBasis} of the {@link Position} for (portfolio, investment).
 *
 * <p>Rules preserved verbatim:
 * <ul>
 *   <li>BUY: ADD quantity TO units, ADD amount TO cost.</li>
 *   <li>SELL: "Insufficient units for sale" when units &lt; quantity, otherwise
 *       SUBTRACT quantity FROM units and SUBTRACT amount FROM cost.</li>
 *   <li>FEE: SUBTRACT amount FROM cost.</li>
 *   <li>TRANSFER: "Transfer processing not implemented" (error, as in legacy).</li>
 * </ul>
 */
@Service
public class TransactionProcessingService {

    public static final String PROGRAM = "PORTTRAN";
    public static final String MSG_INSUFFICIENT_UNITS = "Insufficient units for sale";
    public static final String MSG_TRANSFER_NOT_IMPLEMENTED = "Transfer processing not implemented";
    public static final String MSG_PORTFOLIO_REQUIRED = "Portfolio ID is required";
    public static final String MSG_QUANTITY = "Quantity must be greater than zero";
    public static final String MSG_PRICE = "Price must be greater than zero";
    public static final String MSG_AMOUNT = "Amount must be greater than zero";
    public static final String MSG_NOT_FOUND_UPDATE = "Portfolio not found for update";
    public static final String MSG_NOT_FOUND_FEE = "Portfolio not found for fee";

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final AuditService auditService;

    public TransactionProcessingService(PortfolioRepository portfolioRepository,
                                        PositionRepository positionRepository,
                                        AuditService auditService) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.auditService = auditService;
    }

    /** 2100-VALIDATE-TRANSACTION (2110 / 2120 / 2130). */
    public ProcessingResult validate(Transaction trn) {
        String portfolioId = trn.getId().getPortfolioId();
        if (portfolioId == null || portfolioId.isBlank()) {
            return ProcessingResult.validationError(MSG_PORTFOLIO_REQUIRED);
        }
        if (portfolioRepository.findFirstByIdPortfolioId(portfolioId).isEmpty()) {
            return ProcessingResult.validationError("Invalid Portfolio ID: " + portfolioId);
        }
        if (trn.getType() == null) {
            return ProcessingResult.validationError("Invalid Transaction Type: ");
        }
        if (trn.getQuantity().signum() <= 0) {
            return ProcessingResult.validationError(MSG_QUANTITY);
        }
        boolean transfer = trn.getType() == TransactionType.TRANSFER;
        if (trn.getPrice().signum() <= 0 && !transfer) {
            return ProcessingResult.validationError(MSG_PRICE);
        }
        if (trn.getAmount().signum() <= 0 && !transfer) {
            return ProcessingResult.validationError(MSG_AMOUNT);
        }
        return ProcessingResult.success();
    }

    /** 2200-UPDATE-POSITIONS + 2300-UPDATE-AUDIT-TRAIL. */
    @Transactional(noRollbackFor = PortfolioException.class)
    public Position apply(Transaction trn) {
        ProcessingResult validation = validate(trn);
        if (!validation.isSuccess()) {
            throw PortfolioException.validation(validation.message());
        }
        String portfolioId = trn.getId().getPortfolioId();
        Portfolio portfolio = portfolioRepository.findFirstByIdPortfolioId(portfolioId)
                .orElseThrow(() -> PortfolioException.notFound(MSG_NOT_FOUND_UPDATE));
        Optional<Position> existing = positionRepository
                .findFirstByIdPortfolioIdAndIdInvestmentIdOrderByIdPositionDateDesc(portfolioId, trn.getInvestmentId());

        Position position;
        try {
            position = switch (trn.getType()) {
                case BUY -> processBuy(trn, existing);
                case SELL -> processSell(trn, existing);
                case TRANSFER -> throw PortfolioException.processing(MSG_TRANSFER_NOT_IMPLEMENTED);
                case FEE -> processFee(trn, existing);
            };
        } catch (PortfolioException e) {
            trn.setStatus(TransactionStatus.FAILED);
            auditService.recordFailure(PROGRAM, auditAction(trn.getType()), portfolioId,
                    portfolio.getId().getAccountNo(), e.getMessage());
            throw e;
        }
        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser(PROGRAM);
        Position saved = positionRepository.save(position);

        trn.setStatus(TransactionStatus.DONE);
        trn.setProcessDate(LocalDateTime.now());
        trn.setProcessUser(PROGRAM);

        auditService.record(new AuditService.AuditRequest(PROGRAM, com.cog.portfolio.domain.AuditType.TRANSACTION,
                auditAction(trn.getType()), AuditStatus.SUCCESS, portfolioId, portfolio.getId().getAccountNo(),
                existing.map(TransactionProcessingService::image).orElse(null), image(saved),
                "Transaction: " + trn.getType().code() + " Amount: " + trn.getAmount()
                        + " Units: " + trn.getQuantity()));
        return saved;
    }

    /** 2210-PROCESS-BUY. A first BUY for an investment opens the position. */
    private Position processBuy(Transaction trn, Optional<Position> existing) {
        Position position = existing.orElseGet(() -> newPosition(trn));
        position.setQuantity(position.getQuantity().add(trn.getQuantity()));
        position.setCostBasis(ScaleConverter.toMoneyScale(position.getCostBasis().add(trn.getAmount())));
        return position;
    }

    /** 2220-PROCESS-SELL. */
    private Position processSell(Transaction trn, Optional<Position> existing) {
        Position position = existing.orElseThrow(() -> PortfolioException.notFound(MSG_NOT_FOUND_UPDATE));
        if (position.getQuantity().compareTo(trn.getQuantity()) < 0) {
            throw PortfolioException.processing(MSG_INSUFFICIENT_UNITS);
        }
        position.setQuantity(position.getQuantity().subtract(trn.getQuantity()));
        position.setCostBasis(ScaleConverter.toMoneyScale(position.getCostBasis().subtract(trn.getAmount())));
        return position;
    }

    /** 2240-PROCESS-FEE. */
    private Position processFee(Transaction trn, Optional<Position> existing) {
        Position position = existing.orElseThrow(() -> PortfolioException.notFound(MSG_NOT_FOUND_FEE));
        position.setCostBasis(ScaleConverter.toMoneyScale(position.getCostBasis().subtract(trn.getAmount())));
        return position;
    }

    private static Position newPosition(Transaction trn) {
        Position position = new Position(new PositionId(trn.getId().getPortfolioId(),
                trn.getId().getTransactionDate(), trn.getInvestmentId()));
        position.setQuantity(BigDecimal.ZERO.setScale(ScaleConverter.DOMAIN_QUANTITY_SCALE));
        position.setCostBasis(BigDecimal.ZERO.setScale(ScaleConverter.MONEY_SCALE));
        position.setMarketValue(BigDecimal.ZERO.setScale(ScaleConverter.MONEY_SCALE));
        position.setCurrency(trn.getCurrency());
        return position;
    }

    /** 2300-UPDATE-AUDIT-TRAIL action mapping. */
    static AuditAction auditAction(TransactionType type) {
        return switch (type) {
            case BUY -> AuditAction.CREATE;
            case SELL -> AuditAction.DELETE;
            case TRANSFER, FEE -> AuditAction.UPDATE;
        };
    }

    static String image(Position p) {
        return p.getId() + " qty=" + p.getQuantity() + " cost=" + p.getCostBasis() + " mv=" + p.getMarketValue();
    }
}

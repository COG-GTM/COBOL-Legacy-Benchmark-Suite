package com.clbs.portfolio.service;

import com.clbs.portfolio.domain.Portfolio;
import com.clbs.portfolio.domain.PortfolioTransaction;
import com.clbs.portfolio.domain.Position;
import com.clbs.portfolio.domain.PositionHistory;
import com.clbs.portfolio.domain.TransactionType;
import com.clbs.portfolio.exception.BusinessRuleException;
import com.clbs.portfolio.exception.InvalidRequestException;
import com.clbs.portfolio.exception.NotFoundException;
import com.clbs.portfolio.repository.PortfolioRepository;
import com.clbs.portfolio.repository.PositionHistoryRepository;
import com.clbs.portfolio.repository.PositionRepository;
import com.clbs.portfolio.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static com.clbs.portfolio.service.CobolText.isSpaces;

/**
 * Port of PORTTRAN: validates a transaction (2100-VALIDATE-TRANSACTION) and applies it to the position
 * (2200-UPDATE-POSITIONS), then writes the audit trail (2300-UPDATE-AUDIT-TRAIL) and a POSHIST history row.
 */
@Service
public class TransactionService {

    static final String PROGRAM = "PORTTRAN";

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;
    private final TransactionRepository transactionRepository;
    private final PositionHistoryRepository historyRepository;
    private final AuditService auditService;
    private final CurrentUser currentUser;
    private final Clock clock;

    public TransactionService(PortfolioRepository portfolioRepository, PositionRepository positionRepository,
                              TransactionRepository transactionRepository, PositionHistoryRepository historyRepository,
                              AuditService auditService, CurrentUser currentUser, Clock clock) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
        this.transactionRepository = transactionRepository;
        this.historyRepository = historyRepository;
        this.auditService = auditService;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    /**
     * Validates and applies a transaction. Validation failures throw {@link InvalidRequestException} and are not
     * stored. Processing failures (insufficient units, transfer, missing position) store the transaction with
     * status F, audit it as FAIL and throw {@link BusinessRuleException}.
     */
    @Transactional(noRollbackFor = BusinessRuleException.class)
    public PortfolioTransaction process(TransactionCommand cmd) {
        Portfolio portfolio = validate(cmd);
        TransactionType type = TransactionType.fromCode(cmd.type()).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);
        String currency = cmd.currency() == null ? "USD" : cmd.currency();
        PortfolioTransaction trn = new PortfolioTransaction(cmd.portfolioId(), cmd.investmentId(), cmd.type(),
                cmd.quantity(), zeroIfNull(cmd.price()), zeroIfNull(cmd.amount()), currency, now);
        String user = currentUser.name();
        try {
            Position position = switch (type) {
                case BUY -> processBuy(cmd, currency, now);
                case SELL -> processSell(cmd);
                case TRANSFER -> throw new BusinessRuleException("Transfer processing not implemented");
                case FEE -> processFee(cmd);
            };
            position.setLastMaintTimestamp(now);
            position.setLastMaintUser(user);
            positionRepository.save(position);
            trn.complete(PortfolioTransaction.STATUS_DONE, null, now, user);
            transactionRepository.save(trn);
            historyRepository.save(new PositionHistory(portfolio.getAccountNo(), cmd.portfolioId(),
                    trn.getTransactionDate(), trn.getTransactionTime(), cmd.type(), cmd.investmentId(),
                    trn.getQuantity(), trn.getPrice(), trn.getAmount(), position.getCostBasis(), PROGRAM, user, now));
            audit(type, AuditService.STATUS_SUCCESS, portfolio, trn);
            return trn;
        } catch (BusinessRuleException e) {
            trn.complete(PortfolioTransaction.STATUS_FAILED, e.getMessage(), now, user);
            transactionRepository.save(trn);
            audit(type, AuditService.STATUS_FAILURE, portfolio, trn);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<PortfolioTransaction> findByPortfolio(String portfolioId) {
        if (portfolioRepository.findByKeyPortfolioId(portfolioId).isEmpty()) {
            throw new NotFoundException("Invalid Portfolio ID: " + portfolioId);
        }
        return transactionRepository.findByPortfolioIdOrderByTransactionDateDescTransactionTimeDescIdDesc(portfolioId);
    }

    /** 2110-CHECK-PORTFOLIO, 2120-CHECK-TRANSACTION-TYPE, 2130-CHECK-AMOUNTS (first error wins). */
    Portfolio validate(TransactionCommand cmd) {
        if (isSpaces(cmd.portfolioId())) {
            throw new InvalidRequestException("Portfolio ID is required");
        }
        List<Portfolio> portfolios = portfolioRepository.findByKeyPortfolioId(cmd.portfolioId());
        if (portfolios.isEmpty()) {
            throw new InvalidRequestException("Invalid Portfolio ID: " + cmd.portfolioId());
        }
        if (TransactionType.fromCode(cmd.type()).isEmpty()) {
            throw new InvalidRequestException("Invalid Transaction Type: " + cmd.type());
        }
        boolean transfer = TransactionType.TRANSFER.code().equals(cmd.type());
        if (cmd.quantity() == null || cmd.quantity().signum() <= 0) {
            throw new InvalidRequestException("Quantity must be greater than zero");
        }
        if ((cmd.price() == null || cmd.price().signum() <= 0) && !transfer) {
            throw new InvalidRequestException("Price must be greater than zero");
        }
        if ((cmd.amount() == null || cmd.amount().signum() <= 0) && !transfer) {
            throw new InvalidRequestException("Amount must be greater than zero");
        }
        if (isSpaces(cmd.investmentId())) {
            throw new InvalidRequestException("Investment ID is required");
        }
        return portfolios.getFirst();
    }

    /** 2210-PROCESS-BUY: add units and cost. */
    private Position processBuy(TransactionCommand cmd, String currency, LocalDateTime now) {
        Position position = positionRepository
                .findFirstByPortfolioIdAndInvestmentIdOrderByPositionDateDesc(cmd.portfolioId(), cmd.investmentId())
                .orElseGet(() -> new Position(cmd.portfolioId(), now.toLocalDate(), cmd.investmentId(), currency));
        position.setQuantity(position.getQuantity().add(cmd.quantity()));
        position.setCostBasis(position.getCostBasis().add(cmd.amount()));
        position.setStatus(Position.STATUS_ACTIVE);
        return position;
    }

    /** 2220-PROCESS-SELL: reject when units are insufficient, otherwise subtract units and cost. */
    private Position processSell(TransactionCommand cmd) {
        Position position = existingPosition(cmd, "Position not found for sale");
        if (position.getQuantity().compareTo(cmd.quantity()) < 0) {
            throw new BusinessRuleException("Insufficient units for sale");
        }
        position.setQuantity(position.getQuantity().subtract(cmd.quantity()));
        position.setCostBasis(position.getCostBasis().subtract(cmd.amount()));
        if (position.getQuantity().signum() == 0) {
            position.setStatus(Position.STATUS_CLOSED);
        }
        return position;
    }

    /** 2240-PROCESS-FEE: subtract the fee from the cost. */
    private Position processFee(TransactionCommand cmd) {
        Position position = existingPosition(cmd, "Position not found for fee");
        position.setCostBasis(position.getCostBasis().subtract(cmd.amount()));
        return position;
    }

    private Position existingPosition(TransactionCommand cmd, String message) {
        return positionRepository
                .findFirstByPortfolioIdAndInvestmentIdOrderByPositionDateDesc(cmd.portfolioId(), cmd.investmentId())
                .orElseThrow(() -> new BusinessRuleException(message));
    }

    /** 2300-UPDATE-AUDIT-TRAIL: BU=CREATE, SL=DELETE, TR/FE=UPDATE. */
    private void audit(TransactionType type, String status, Portfolio portfolio, PortfolioTransaction trn) {
        String action = switch (type) {
            case BUY -> "CREATE";
            case SELL -> "DELETE";
            case TRANSFER, FEE -> "UPDATE";
        };
        auditService.record(PROGRAM, AuditService.TYPE_TRANSACTION, action, status, trn.getPortfolioId(),
                portfolio.getAccountNo(), null, portfolio.toString(), null,
                "Transaction: " + trn.getType() + " Amount: " + trn.getAmount() + " Units: " + trn.getQuantity());
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

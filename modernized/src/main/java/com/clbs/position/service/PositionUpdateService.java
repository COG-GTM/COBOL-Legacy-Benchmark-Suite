package com.clbs.position.service;

import com.clbs.position.domain.MoneyScale;
import com.clbs.position.domain.PositionStatus;
import com.clbs.position.domain.PositionUpdateResult;
import com.clbs.position.domain.TradeInput;
import com.clbs.position.domain.TransactionType;
import com.clbs.position.entity.Position;
import com.clbs.position.entity.PositionHistory;
import com.clbs.position.entity.Transaction;
import com.clbs.position.repository.PositionHistoryRepository;
import com.clbs.position.repository.PositionRepository;
import com.clbs.position.repository.TransactionRepository;
import com.clbs.position.domain.PositionCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for the modernized position-update program (COBOL {@code POSUPDT}
 * / process id {@code POSUPD00}). Each {@code @Transactional} method mirrors a
 * COBOL "unit of work": the position {@code REWRITE} and the {@code POSHIST}
 * history insert commit together, exactly as the legacy job committed at a
 * checkpoint boundary.
 *
 * <p>Responsibilities ported from the POSUPDT spec
 * ({@code documentation/technical/system-architecture.md} 1.2.2):
 * "Updates position records, Maintains cost basis, Records transaction
 * history".</p>
 */
@Service
@RequiredArgsConstructor
public class PositionUpdateService {

    /** {@code PRC-PROGRAM-ID} of the legacy program this service replaces. */
    public static final String PROGRAM_ID = "POSUPD00";

    private static final String DEFAULT_USER = "BATCH";
    private static final DateTimeFormatter YYYYMMDD = DateTimeFormatter.ofPattern("yyyyMMdd");
    /** 26-char DB2/CICS timestamp format matching {@code POS-LAST-MAINT-DATE PIC X(26)}. */
    private static final DateTimeFormatter MAINT_TS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS");

    private final PositionRepository positionRepository;
    private final PositionHistoryRepository historyRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Applies one transaction to its position and records history &mdash; the
     * per-record body of the COBOL processing loop
     * ({@code PORTTRAN.cbl 2200-UPDATE-POSITIONS} + {@code 2300-UPDATE-AUDIT-TRAIL}).
     *
     * @return the calculation result (new holding + per-trade P&amp;L)
     */
    @Transactional
    public PositionUpdateResult applyTransaction(Transaction txn) {
        Position position = resolvePosition(txn);

        if (position.statusEnum() == PositionStatus.CLOSED) {
            throw new IllegalStateException(
                    "Position is closed for portfolio " + txn.getPortfolioId()
                            + " investment " + txn.getInvestmentId());
        }

        TradeInput trade = txn.toTradeInput();
        PositionUpdateResult result = PositionCalculator.apply(position.toState(), trade);

        position.applyState(result.newState());
        position.setPositionDate(txn.getTrnDate());
        position.setLastMaintDate(LocalDateTime.now().format(MAINT_TS));
        position.setLastMaintUser(safeUser(txn.getProcessUser()));
        if (result.newState().quantity().signum() == 0
                && trade.type() != TransactionType.FEE) {
            position.setStatus(PositionStatus.CLOSED.code());
        }
        positionRepository.save(position);

        historyRepository.save(buildHistory(txn, trade, result));

        txn.setStatus("D");
        transactionRepository.save(txn);

        return result;
    }

    /**
     * Resolves the running holding, creating a new ACTIVE position when the
     * investment is held for the first time (the COBOL job's implicit "add on
     * first activity" behavior).
     */
    private Position resolvePosition(Transaction txn) {
        return positionRepository
                .findFirstByPortfolioIdAndInvestmentIdOrderByPositionDateDesc(
                        txn.getPortfolioId(), txn.getInvestmentId())
                .orElseGet(() -> Position.builder()
                        .portfolioId(txn.getPortfolioId())
                        .positionDate(txn.getTrnDate())
                        .investmentId(txn.getInvestmentId())
                        .quantity(BigDecimal.ZERO.setScale(MoneyScale.QUANTITY_SCALE))
                        .costBasis(BigDecimal.ZERO.setScale(MoneyScale.AMOUNT_SCALE))
                        .marketValue(BigDecimal.ZERO.setScale(MoneyScale.AMOUNT_SCALE))
                        .currency(txn.getCurrency())
                        .status(PositionStatus.ACTIVE.code())
                        .build());
    }

    private PositionHistory buildHistory(
            Transaction txn, TradeInput trade, PositionUpdateResult result) {
        BigDecimal fees = trade.type() == TransactionType.FEE
                ? trade.amount()
                : BigDecimal.ZERO.setScale(MoneyScale.AMOUNT_SCALE);
        BigDecimal totalAmount = trade.amount().add(fees)
                .setScale(MoneyScale.AMOUNT_SCALE, MoneyScale.ROUNDING);
        LocalDateTime now = LocalDateTime.now();
        return PositionHistory.builder()
                .accountNo(txn.getPortfolioId())
                .portfolioId(txn.getPortfolioId())
                .transDate(LocalDate.parse(txn.getTrnDate(), YYYYMMDD))
                .transTime(parseTime(txn.getTrnTime()))
                .transType(txn.getType())
                .securityId(txn.getInvestmentId())
                .quantity(trade.quantity().setScale(3, MoneyScale.ROUNDING))
                .price(trade.price().setScale(3, MoneyScale.ROUNDING))
                .amount(trade.amount())
                .fees(fees)
                .totalAmount(totalAmount)
                .costBasis(result.newState().costBasis())
                .gainLoss(result.realizedGainLoss())
                .processDate(now.toLocalDate())
                .processTime(now.toLocalTime().withNano(0))
                .programId(PROGRAM_ID)
                .userId(safeUser(txn.getProcessUser()))
                .auditTimestamp(now)
                .build();
    }

    /* ----------------------- query side (keyed/sequential READ) -------------- */

    @Transactional(readOnly = true)
    public List<Position> findAll() {
        return positionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Position> findById(Long id) {
        return positionRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Position> findByPortfolio(String portfolioId) {
        return positionRepository.findByPortfolioId(portfolioId);
    }

    @Transactional(readOnly = true)
    public List<Position> findByStatus(String status) {
        return positionRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public BigDecimal realizedGainLoss(String portfolioId) {
        return historyRepository.sumRealizedGainLoss(portfolioId);
    }

    private static LocalTime parseTime(String hhmmss) {
        if (hhmmss == null || hhmmss.isBlank()) {
            return LocalTime.MIDNIGHT;
        }
        String padded = (hhmmss + "000000").substring(0, 6);
        return LocalTime.of(
                Integer.parseInt(padded.substring(0, 2)),
                Integer.parseInt(padded.substring(2, 4)),
                Integer.parseInt(padded.substring(4, 6)));
    }

    private static String safeUser(String user) {
        return (user == null || user.isBlank()) ? DEFAULT_USER : user.trim();
    }
}

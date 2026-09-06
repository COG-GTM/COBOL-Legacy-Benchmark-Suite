package com.cog.portfolio.service;

import com.cog.portfolio.common.PortfolioException;
import com.cog.portfolio.common.ScaleConverter;
import com.cog.portfolio.domain.Position;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.domain.TransactionStatus;
import com.cog.portfolio.repository.PositionRepository;
import com.cog.portfolio.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// PROVISIONAL - requiere validación humana
/**
 * Position update service standing in for {@code POSUPDT (POSUPD00)}.
 *
 * <p>The repository has {@code src/programs/batch/POSUPDT.cbl} but the file is
 * EMPTY, and there is no {@code POSUPD00} source at all. This class is therefore a
 * RECONSTRUCTION, not a translation: it reuses the position arithmetic that
 * does exist in the repo (PORTTRAN.cbl BUY/SELL/FEE via
 * {@link TransactionProcessingService}) and the architecture document's one-line
 * description ("updates position records, maintains cost basis, records
 * transaction history"). Market value is refreshed as quantity x price of the
 * last transaction; that rule is an assumption.
 *
 * <p>TODO validación humana: (a) confirmar la semántica esperada de POSUPD00,
 * (b) confirmar si POSUPDT.cbl (vacío en el checkout) es realmente su equivalente.
 */
@Service
public class PositionUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PositionUpdateService.class);
    public static final String PROGRAM = "POSUPDT";

    private final TransactionProcessingService transactionProcessingService;
    private final TransactionRepository transactionRepository;
    private final PositionRepository positionRepository;

    public PositionUpdateService(TransactionProcessingService transactionProcessingService,
                                 TransactionRepository transactionRepository,
                                 PositionRepository positionRepository) {
        this.transactionProcessingService = transactionProcessingService;
        this.transactionRepository = transactionRepository;
        this.positionRepository = positionRepository;
    }

    // PROVISIONAL - requiere validación humana
    /** Applies one validated (PENDING) transaction to its position. */
    @Transactional(noRollbackFor = PortfolioException.class)
    public Position applyTransaction(Transaction trn) {
        if (trn.getStatus() != TransactionStatus.PENDING) {
            throw PortfolioException.processing("Transaction " + trn.getId() + " is not pending");
        }
        Position position = transactionProcessingService.apply(trn);
        refreshMarketValue(position, trn);
        trn.setProcessUser(PROGRAM);
        transactionRepository.save(trn);
        return positionRepository.save(position);
    }

    // PROVISIONAL - requiere validación humana
    /** ASSUMPTION: market value = quantity x last traded price, HALF_UP to 2 decimals. */
    void refreshMarketValue(Position position, Transaction trn) {
        if (trn.getPrice().signum() > 0) {
            BigDecimal marketValue = position.getQuantity().multiply(trn.getPrice());
            position.setPreviousValue(position.getMarketValue());
            position.setMarketValue(ScaleConverter.toMoneyScale(marketValue));
        }
        position.setLastMaintDate(LocalDateTime.now());
        position.setLastMaintUser(PROGRAM);
        log.debug("Position {} refreshed: qty={} cost={} mv={}", position.getId(), position.getQuantity(),
                position.getCostBasis(), position.getMarketValue());
    }
}

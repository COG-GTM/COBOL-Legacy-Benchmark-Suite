package com.cog.portfolio.batch;

import com.cog.portfolio.common.ScaleConverter;
import com.cog.portfolio.common.ScaleConverter.ScaleConversion;
import com.cog.portfolio.domain.PositionHistory;
import com.cog.portfolio.domain.PositionHistoryId;
import com.cog.portfolio.domain.Transaction;
import com.cog.portfolio.repository.PortfolioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * HISTLD00 paragraph 2200-LOAD-TO-DB2: maps one transaction-history record to a
 * POSHIST row. The COBOL {@code MOVE TH-QUANTITY TO PH-QUANTITY} silently drops
 * the 4th decimal (V9(4) -> V9(3)); here that conversion is explicit, uses
 * HALF_UP through {@link ScaleConverter} and every non-zero residue is logged
 * and counted so it is never lost silently.
 */
@Component
public class HistoryLoadProcessor implements ItemProcessor<Transaction, PositionHistory> {

    private static final Logger log = LoggerFactory.getLogger(HistoryLoadProcessor.class);
    public static final String PROGRAM = "HISTLD00";

    private final PortfolioRepository portfolioRepository;
    private final AtomicLong precisionLossCount = new AtomicLong();

    public HistoryLoadProcessor(PortfolioRepository portfolioRepository) {
        this.portfolioRepository = portfolioRepository;
    }

    @Override
    public PositionHistory process(Transaction trn) {
        String portfolioId = trn.getId().getPortfolioId();
        String accountNo = portfolioRepository.findFirstByIdPortfolioId(portfolioId)
                .map(p -> p.getId().getAccountNo())
                .orElse("0000000000");

        PositionHistoryId id = new PositionHistoryId(accountNo, portfolioId,
                trn.getId().getTransactionDate(), trn.getId().getTransactionTime());
        PositionHistory ph = new PositionHistory(id, trn.getType(), trn.getInvestmentId());

        ph.setQuantity(convert("quantity", id, trn.getQuantity()));
        ph.setPrice(convert("price", id, trn.getPrice()));

        BigDecimal amount = ScaleConverter.toMoneyScale(trn.getAmount());
        ph.setAmount(amount);
        ph.setFees(BigDecimal.ZERO.setScale(ScaleConverter.MONEY_SCALE));
        ph.setTotalAmount(amount);
        ph.setCostBasis(amount);
        ph.setGainLoss(BigDecimal.ZERO.setScale(ScaleConverter.MONEY_SCALE));
        ph.setProcessDate(LocalDate.now());
        ph.setProcessTime(LocalTime.now().withNano(0));
        ph.setProgramId(PROGRAM);
        ph.setUserId(PROGRAM);
        return ph;
    }

    /** Scale 4 -> scale 3 with HALF_UP; residue is surfaced, never swallowed. */
    BigDecimal convert(String field, PositionHistoryId id, BigDecimal value) {
        ScaleConversion conversion = ScaleConverter.convertToPoshistScale(value);
        if (conversion.hasPrecisionLoss()) {
            precisionLossCount.incrementAndGet();
            log.warn("POSHIST {} {} rounded {} -> {} (residue {}, HALF_UP)", id, field,
                    conversion.original(), conversion.converted(), conversion.residue());
        }
        return conversion.converted();
    }

    public long getPrecisionLossCount() {
        return precisionLossCount.get();
    }
}

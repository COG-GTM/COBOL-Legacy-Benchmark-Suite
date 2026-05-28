package com.clbs.portfolio.batch.processor;

import com.clbs.portfolio.entity.HistoryRecord;
import com.clbs.portfolio.entity.PositionHistory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class HistoryLoadProcessor implements ItemProcessor<HistoryRecord, PositionHistory> {

    private static final String PROGRAM_ID = "HISTLD00";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public PositionHistory process(HistoryRecord historyRecord) {
        return PositionHistory.builder()
                .accountNo(historyRecord.getAccountNo())
                .portfolioId(historyRecord.getPortfolioId())
                .transDate(historyRecord.getTransDate())
                .transTime(historyRecord.getTransTime())
                .transType(historyRecord.getTransType())
                .securityId(historyRecord.getSecurityId())
                .quantity(historyRecord.getQuantity() != null
                        ? historyRecord.getQuantity().setScale(3, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .price(historyRecord.getPrice() != null
                        ? historyRecord.getPrice().setScale(3, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .amount(historyRecord.getAmount() != null ? historyRecord.getAmount() : BigDecimal.ZERO)
                .fees(historyRecord.getFees() != null ? historyRecord.getFees() : BigDecimal.ZERO)
                .totalAmount(historyRecord.getTotalAmount() != null ? historyRecord.getTotalAmount() : BigDecimal.ZERO)
                .costBasis(historyRecord.getCostBasis() != null ? historyRecord.getCostBasis() : BigDecimal.ZERO)
                .gainLoss(historyRecord.getGainLoss() != null ? historyRecord.getGainLoss() : BigDecimal.ZERO)
                .processDate(LocalDate.now().format(DATE_FORMATTER))
                .processTime(LocalDateTime.now().format(TIME_FORMATTER))
                .programId(PROGRAM_ID)
                .userId("BATCH")
                .auditTimestamp(LocalDateTime.now())
                .build();
    }
}

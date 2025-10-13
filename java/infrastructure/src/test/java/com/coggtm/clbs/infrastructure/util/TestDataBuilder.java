package com.coggtm.clbs.infrastructure.util;

import com.coggtm.clbs.domain.HistoryRecord;
import com.coggtm.clbs.domain.PositionRecord;
import com.coggtm.clbs.domain.TransactionRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TestDataBuilder {

    public TransactionRecord buildSampleTransactionRecord() {
        return TransactionRecord.builder()
                .transactionDate(LocalDate.of(2024, 1, 15))
                .transactionTime(LocalTime.of(10, 30, 0))
                .portfolioId("PORT001")
                .sequenceNumber("000001")
                .investmentId("INV001")
                .transactionType("BU")
                .quantity(new BigDecimal("100.0000"))
                .price(new BigDecimal("50.2500"))
                .amount(new BigDecimal("5025.00"))
                .currency("USD")
                .status("D")
                .processDate(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
                .processUser("TESTUSER")
                .build();
    }

    public TransactionRecord buildTransactionRecord(String portfolioId, String investmentId, 
                                                    String transactionType, BigDecimal quantity, 
                                                    BigDecimal price, BigDecimal amount) {
        return TransactionRecord.builder()
                .transactionDate(LocalDate.now())
                .transactionTime(LocalTime.now())
                .portfolioId(portfolioId)
                .sequenceNumber("000001")
                .investmentId(investmentId)
                .transactionType(transactionType)
                .quantity(quantity)
                .price(price)
                .amount(amount)
                .currency("USD")
                .status("D")
                .processDate(LocalDateTime.now())
                .processUser("TESTUSER")
                .build();
    }

    public PositionRecord buildSamplePositionRecord() {
        return PositionRecord.builder()
                .portfolioId("PORT001")
                .positionDate(LocalDate.of(2024, 1, 15))
                .investmentId("INV001")
                .quantity(new BigDecimal("100.0000"))
                .costBasis(new BigDecimal("5025.00"))
                .marketValue(new BigDecimal("5500.00"))
                .currency("USD")
                .status("A")
                .lastMaintenanceDate(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
                .lastMaintenanceUser("TESTUSER")
                .build();
    }

    public PositionRecord buildPositionRecord(String portfolioId, String investmentId, 
                                             BigDecimal quantity, BigDecimal costBasis, 
                                             BigDecimal marketValue) {
        return PositionRecord.builder()
                .portfolioId(portfolioId)
                .positionDate(LocalDate.now())
                .investmentId(investmentId)
                .quantity(quantity)
                .costBasis(costBasis)
                .marketValue(marketValue)
                .currency("USD")
                .status("A")
                .lastMaintenanceDate(LocalDateTime.now())
                .lastMaintenanceUser("TESTUSER")
                .build();
    }

    public HistoryRecord buildSampleHistoryRecord() {
        return HistoryRecord.builder()
                .portfolioId("PORT001")
                .historyDate(LocalDate.of(2024, 1, 15))
                .historyTime(LocalTime.of(10, 30, 0))
                .sequenceNumber("0001")
                .recordType("TR")
                .actionCode("A")
                .beforeImage("")
                .afterImage("Transaction BU INV001 100.0000 @ 50.2500")
                .reasonCode("NEWO")
                .processDate(LocalDateTime.of(2024, 1, 15, 10, 30, 0))
                .processUser("TESTUSER")
                .build();
    }

    public HistoryRecord buildHistoryRecord(String portfolioId, String recordType, 
                                           String actionCode, String beforeImage, 
                                           String afterImage) {
        return HistoryRecord.builder()
                .portfolioId(portfolioId)
                .historyDate(LocalDate.now())
                .historyTime(LocalTime.now())
                .sequenceNumber("0001")
                .recordType(recordType)
                .actionCode(actionCode)
                .beforeImage(beforeImage)
                .afterImage(afterImage)
                .reasonCode("TEST")
                .processDate(LocalDateTime.now())
                .processUser("TESTUSER")
                .build();
    }
}

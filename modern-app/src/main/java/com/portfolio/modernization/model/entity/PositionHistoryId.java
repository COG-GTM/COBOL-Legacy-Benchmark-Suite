package com.portfolio.modernization.model.entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PositionHistoryId implements Serializable {
    private String accountNumber;
    private String portfolioId;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
}

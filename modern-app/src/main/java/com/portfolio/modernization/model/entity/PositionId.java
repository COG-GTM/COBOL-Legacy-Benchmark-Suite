package com.portfolio.modernization.model.entity;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PositionId implements Serializable {
    private String portfolioId;
    private LocalDate positionDate;
    private String investmentId;
}

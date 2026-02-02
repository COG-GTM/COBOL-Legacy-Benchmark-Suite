package com.portfolio.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Composite key for Position entity
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PositionId implements Serializable {
    private String portfolioId;
    private LocalDate positionDate;
    private String investmentId;
}

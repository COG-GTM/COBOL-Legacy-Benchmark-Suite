package com.clbs.portfolio.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * Composite key for Position.
 * From COBOL copybook: src/copybook/common/POSREC.cpy (POS-KEY).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PositionId implements Serializable {

    private String portfolioId;
    private String posDate;
    private String investmentId;
}

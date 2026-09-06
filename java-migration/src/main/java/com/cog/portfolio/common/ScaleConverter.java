package com.cog.portfolio.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Single, auditable place where COBOL PIC scales are converted.
 *
 * <p>Domain quantities and prices keep scale 4 ({@code PIC S9(11)V9(4)}, TRNREC.cpy /
 * POSREC.cpy). POSHIST stores {@code PH-QUANTITY} / {@code PH-PRICE} as
 * {@code PIC S9(12)V9(3)} (scale 3), so a scale reduction is unavoidable when
 * loading history.
 *
 * <p>SUPUESTO TECNICO: la reduccion 4 -> 3 usa {@link RoundingMode#HALF_UP}.
 * Esta politica NO esta definida en el COBOL original (el MOVE COMP-3 truncaba
 * implicitamente) y debe ser validada/aprobada por el negocio antes de
 * produccion porque afecta valores monetarios de la historia de posiciones.
 * Nunca se usa truncamiento silencioso ni double/float.
 */
public final class ScaleConverter {

    public static final int DOMAIN_QUANTITY_SCALE = 4;
    public static final int MONEY_SCALE = 2;
    public static final int POSHIST_SCALE = 3;
    public static final RoundingMode POSHIST_ROUNDING = RoundingMode.HALF_UP;

    private ScaleConverter() {
    }

    /** Scale 4 (domain) to scale 3 (POSHIST), HALF_UP. */
    public static BigDecimal toPoshistScale(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        return value.setScale(POSHIST_SCALE, POSHIST_ROUNDING);
    }

    /**
     * Converts to POSHIST scale and reports the rounding residue so callers can
     * log/audit precision loss instead of silencing it.
     */
    public static ScaleConversion convertToPoshistScale(BigDecimal value) {
        BigDecimal converted = toPoshistScale(value);
        BigDecimal residue = value.subtract(converted);
        return new ScaleConversion(value, converted, residue);
    }

    /** {@code PIC S9(n)V9(4)}: exact, fails if the input has more than 4 decimals. */
    public static BigDecimal toDomainQuantityScale(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        return value.setScale(DOMAIN_QUANTITY_SCALE, RoundingMode.UNNECESSARY);
    }

    /** {@code PIC S9(n)V99}: monetary amounts, HALF_UP on arithmetic results. */
    public static BigDecimal toMoneyScale(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    /** Result of a scale reduction; {@code residue = original - converted}. */
    public record ScaleConversion(BigDecimal original, BigDecimal converted, BigDecimal residue) {
        public boolean hasPrecisionLoss() {
            return residue.signum() != 0;
        }
    }
}

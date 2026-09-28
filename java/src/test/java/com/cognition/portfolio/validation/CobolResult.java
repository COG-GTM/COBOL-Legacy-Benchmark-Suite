package com.cognition.portfolio.validation;

import java.math.BigDecimal;

/**
 * What the compiled PORTVALD returned for one case, as captured by PVDRIVER.
 *
 * @param caseId         case identifier
 * @param returnCode     LS-RETURN-CODE after the CALL
 * @param errorMessage   LS-ERROR-MSG after the CALL (exactly 50 characters)
 * @param callReturnCode RETURN-CODE special register after the CALL
 * @param moveProbe      value of a PIC S9(13)V99 field after the same MOVE PORTVALD's
 *                       4000-VALIDATE-AMOUNT performs, rendered as a signed plain decimal;
 *                       a leading '-' is kept on zero so negative zero stays visible
 */
record CobolResult(String caseId, int returnCode, String errorMessage, int callReturnCode, String moveProbe) {

    BigDecimal moveProbeValue() {
        return new BigDecimal(moveProbe);
    }
}

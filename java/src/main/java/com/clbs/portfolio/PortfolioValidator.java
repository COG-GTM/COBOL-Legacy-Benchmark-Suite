package com.clbs.portfolio;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * PORTVALD — portfolio validation subroutine. The four LS-VALIDATE-TYPE branches become four
 * methods; each returns the VAL-* return code and the PORTVAL.cpy error message.
 */
@Service
public class PortfolioValidator {

    /** PORTVAL.cpy VAL-RETURN-CODES. */
    public static final int VAL_SUCCESS = 0;
    public static final int VAL_INVALID_ID = 1;
    public static final int VAL_INVALID_ACCT = 2;
    public static final int VAL_INVALID_TYPE = 3;
    public static final int VAL_INVALID_AMT = 4;

    /** PORTVAL.cpy VAL-ERROR-MESSAGES. */
    public static final String ERR_ID = "Invalid Portfolio ID format";
    public static final String ERR_ACCT = "Invalid Account Number format";
    public static final String ERR_TYPE = "Invalid Investment Type";
    public static final String ERR_AMT = "Amount outside valid range";
    public static final String ERR_REQUEST = "Invalid validation type";

    /** PORTVAL.cpy VAL-CONSTANTS. */
    public static final String ID_PREFIX = "PORT";
    public static final BigDecimal MIN_AMOUNT = new BigDecimal("-9999999999999.99");
    public static final BigDecimal MAX_AMOUNT = new BigDecimal("9999999999999.99");

    /** LS-VALIDATION-REQUEST result: LS-RETURN-CODE plus LS-ERROR-MSG. */
    public record Result(int returnCode, String errorMessage) {

        public boolean valid() {
            return returnCode == VAL_SUCCESS;
        }
    }

    private static final Result OK = new Result(VAL_SUCCESS, "");

    /** 0000-MAIN dispatch on LS-VALIDATE-TYPE. */
    public Result validate(char type, String value) {
        return switch (type) {
            case 'I' -> validateId(value);
            case 'A' -> validateAccount(value);
            case 'T' -> validateType(value);
            case 'M' -> validateAmount(value);
            default -> new Result(VAL_INVALID_ID, ERR_REQUEST);
        };
    }

    /** 1000-VALIDATE-ID: 'PORT' prefix followed by four numeric digits. */
    public Result validateId(String value) {
        String v = value == null ? "" : value;
        if (v.length() < 8 || !ID_PREFIX.equals(v.substring(0, 4))) {
            return new Result(VAL_INVALID_ID, ERR_ID);
        }
        if (!isNumeric(v.substring(4, 8))) {
            return new Result(VAL_INVALID_ID, ERR_ID);
        }
        return OK;
    }

    /** 2000-VALIDATE-ACCOUNT: numeric and non-zero. */
    public Result validateAccount(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty() || !isNumeric(v) || isZeros(v)) {
            return new Result(VAL_INVALID_ACCT, ERR_ACCT);
        }
        return OK;
    }

    /** 3000-VALIDATE-TYPE. */
    public Result validateType(String value) {
        String v = value == null ? "" : value.trim();
        if (!v.equals("STK") && !v.equals("BND") && !v.equals("MMF") && !v.equals("ETF")) {
            return new Result(VAL_INVALID_TYPE, ERR_TYPE);
        }
        return OK;
    }

    /** 4000-VALIDATE-AMOUNT: within the S9(13)V99 range of VAL-TEMP-NUM. */
    public Result validateAmount(String value) {
        BigDecimal amount;
        try {
            amount = new BigDecimal(value == null ? "" : value.trim());
        } catch (NumberFormatException e) {
            return new Result(VAL_INVALID_AMT, ERR_AMT);
        }
        return validateAmount(amount);
    }

    public Result validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(MIN_AMOUNT) < 0 || amount.compareTo(MAX_AMOUNT) > 0) {
            return new Result(VAL_INVALID_AMT, ERR_AMT);
        }
        return OK;
    }

    private static boolean isNumeric(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isZeros(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }
}

package com.clbs.posval.validation;

import com.clbs.posval.cobol.CobolString;
import org.springframework.stereotype.Service;

/**
 * Port of {@code src/programs/portfolio/PORTVALD.cbl} — the portfolio validation subroutine.
 *
 * <p>Paragraph mapping:
 *
 * <table border="1">
 *   <caption>PORTVALD to Java</caption>
 *   <tr><th>COBOL paragraph</th><th>Java method</th></tr>
 *   <tr><td>{@code 0000-MAIN}</td><td>{@link #validate(char, String)}</td></tr>
 *   <tr><td>{@code 1000-VALIDATE-ID}</td><td>{@link #validatePortfolioId(String)}</td></tr>
 *   <tr><td>{@code 2000-VALIDATE-ACCOUNT}</td><td>{@link #validateAccountNumber(String)}</td></tr>
 *   <tr><td>{@code 3000-VALIDATE-TYPE}</td><td>{@link #validateInvestmentType(String)}</td></tr>
 *   <tr><td>{@code 4000-VALIDATE-AMOUNT}</td><td>{@link #validateAmount(String)}</td></tr>
 * </table>
 *
 * <p><b>This is a behaviour-preserving port, defects included.</b> Two paragraphs cannot return
 * success for the inputs the rest of the system feeds them (see the spec, rules R-1.3 and R-2.2,
 * and open questions OQ-3 and OQ-4). The behaviour is reproduced here, not corrected, and is
 * pinned by golden vectors captured from the compiled COBOL.
 */
@Service
public class PortfolioValidator {

    /** {@code LS-INPUT-VALUE PIC X(50)}. */
    public static final int INPUT_WIDTH = 50;

    /** {@code VAL-ID-PREFIX PIC X(4) VALUE 'PORT'}. */
    public static final String ID_PREFIX = "PORT";

    /** {@code VAL-NUMERIC-CHECK PIC X(10)} of the PORTVAL copybook. */
    private static final int NUMERIC_CHECK_WIDTH = 10;

    private static final String TYPE_STOCK = "STK";
    private static final String TYPE_BOND = "BND";
    private static final String TYPE_MONEY_MARKET = "MMF";
    private static final String TYPE_ETF = "ETF";

    /**
     * {@code 0000-MAIN}: dispatches on {@code LS-VALIDATE-TYPE}.
     *
     * <p>An unrecognised request type returns {@code VAL-INVALID-ID} (+1) with the message
     * {@code 'Invalid validation type'} — the paragraph reuses the portfolio-ID return code for a
     * condition that has nothing to do with portfolio IDs, so callers cannot tell the two apart
     * from the return code alone.
     */
    public ValidationResult validate(char requestType, String inputValue) {
        return ValidationType.fromCode(requestType)
                .map(type -> switch (type) {
                    case PORTFOLIO_ID -> validatePortfolioId(inputValue);
                    case ACCOUNT_NUMBER -> validateAccountNumber(inputValue);
                    case INVESTMENT_TYPE -> validateInvestmentType(inputValue);
                    case AMOUNT -> validateAmount(inputValue);
                })
                .orElseGet(() -> new ValidationResult(
                        ValidationResult.VAL_INVALID_ID, ValidationResult.ERR_VALIDATE_TYPE));
    }

    /**
     * {@code 1000-VALIDATE-ID}: the field must start with {@code 'PORT'} and the following four
     * characters must be numeric.
     *
     * <p>The second test cannot pass. {@code MOVE LS-INPUT-VALUE(5:4) TO VAL-NUMERIC-CHECK} moves
     * four characters into a {@code PIC X(10)} item, which space-fills positions 5 to 10; the
     * subsequent {@code IF VAL-NUMERIC-CHECK IS NOT NUMERIC} therefore always evaluates true.
     * Every portfolio ID — including a well-formed {@code PORT0001} — is rejected with +1.
     */
    public ValidationResult validatePortfolioId(String inputValue) {
        String input = CobolString.move(inputValue, INPUT_WIDTH);

        if (!CobolString.refmod(input, 1, 4).equals(ID_PREFIX)) {
            return new ValidationResult(ValidationResult.VAL_INVALID_ID, ValidationResult.ERR_ID);
        }

        String numericCheck = CobolString.move(CobolString.refmod(input, 5, 4), NUMERIC_CHECK_WIDTH);
        if (!CobolString.isNumeric(numericCheck)) {
            return new ValidationResult(ValidationResult.VAL_INVALID_ID, ValidationResult.ERR_ID);
        }

        return ValidationResult.success();
    }

    /**
     * {@code 2000-VALIDATE-ACCOUNT}: the account number must be numeric and non-zero.
     *
     * <p>The class test is applied to the whole {@code PIC X(50)} linkage field rather than to the
     * ten account digits, so a ten digit account number followed by forty spaces fails. Only an
     * input that is fifty digits wide and not all zeros returns success.
     */
    public ValidationResult validateAccountNumber(String inputValue) {
        String input = CobolString.move(inputValue, INPUT_WIDTH);

        if (!CobolString.isNumeric(input) || CobolString.isZeros(input)) {
            return new ValidationResult(ValidationResult.VAL_INVALID_ACCT, ValidationResult.ERR_ACCT);
        }

        return ValidationResult.success();
    }

    /**
     * {@code 3000-VALIDATE-TYPE}: the investment type must be {@code STK}, {@code BND},
     * {@code MMF} or {@code ETF}.
     *
     * <p>The comparison is between a {@code PIC X(50)} field and a three character literal, which
     * COBOL pads with spaces, so trailing spaces are accepted and any trailing non-space content
     * is rejected. The comparison is case sensitive.
     */
    public ValidationResult validateInvestmentType(String inputValue) {
        String input = CobolString.move(inputValue, INPUT_WIDTH);

        boolean known = matches(input, TYPE_STOCK)
                || matches(input, TYPE_BOND)
                || matches(input, TYPE_MONEY_MARKET)
                || matches(input, TYPE_ETF);

        if (!known) {
            return new ValidationResult(ValidationResult.VAL_INVALID_TYPE, ValidationResult.ERR_TYPE);
        }

        return ValidationResult.success();
    }

    /**
     * {@code 4000-VALIDATE-AMOUNT}: the amount must lie between {@code VAL-MIN-AMOUNT}
     * (-9999999999999.99) and {@code VAL-MAX-AMOUNT} (+9999999999999.99).
     *
     * <p>The bounds are exactly the representable range of the receiving field
     * {@code VAL-TEMP-NUM PIC S9(13)V99}, so no value that can be moved into it can ever fall
     * outside them: the paragraph returns {@code VAL-SUCCESS} unconditionally, for every input,
     * numeric or not. {@code VAL-INVALID-AMT} (+4) is unreachable.
     */
    public ValidationResult validateAmount(String inputValue) {
        return ValidationResult.success();
    }

    private static boolean matches(String paddedInput, String literal) {
        return paddedInput.equals(CobolString.move(literal, INPUT_WIDTH));
    }
}
